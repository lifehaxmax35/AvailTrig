/*
 * ServerFileWrapper.kt
 * Copyright © 1993-2019, The Avail Foundation, LLC.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice, this
 *     list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice, this
 *     list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 *  * Neither the name of the copyright holder nor the names of the contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.avail.server.io.files

import com.avail.compiler.problems.SimpleProblemHandler
import com.avail.io.SimpleCompletionHandler
import org.apache.tika.Tika
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A `ServerFileWrapper` holds an [AvailServerFile]. The purpose of this is to
 * enable the Avail server [FileManager] cache to delay establishing the type
 * of `AvailServerFile` until the file type can be read without delaying adding
 * a file object to the `FileManager` cache.
 *
 * @author Richard Arriaga &lt;rich@availlang.org&gt;
 *
 * @property path
 *   The String path to the target file on disk.
 *
 * @constructor
 * Construct a [ServerFileWrapper].
 *
 * @param path
 *   The String path to the target file on disk.
 * @param fileChannel
 *   The [AsynchronousFileChannel] used to access the file.
 */
internal class ServerFileWrapper constructor(
	val path: String, fileChannel: AsynchronousFileChannel)
{
	/**
	 * The [AvailServerFile] wrapped by this [ServerFileWrapper].
	 */
	lateinit var file: AvailServerFile
		private set

	/**
	 * `true` indicates the file is being loaded; `false` indicates the file
	 * has been loaded.
	 */
	@Volatile
	var isLoadingFile: Boolean = true
		private set

	/**
	 * The [queue][ConcurrentLinkedQueue] of [FileRequestHandler]s that are
	 * requesting receipt of the [file]'s
	 * [raw bytes][AvailServerFile.rawContent].
	 */
	private val handlerQueue = ConcurrentLinkedQueue<FileRequestHandler>()

	/**
	 * The [Stack] of [EditAction] that when applied in Stack order reverts
	 * `EditAction`s applied to the [AvailServerFile] to get it to its current
	 * state. This represents the _undo_ stack.
	 */
	private val revertUpdateStack = Stack<EditAction>()

	/**
	 * The next position in the [revertUpdateStack] to begin saving to disk the
	 * next time the [file]'s local history is saved.
	 */
	private var revertUpdateStackSavePointer: Int = 0

	/**
	 * Save the [revertUpdateStack] to local history starting from the position
	 * at [revertUpdateStackSavePointer].
	 */
	fun conditionallySaveToLocalHistory ()
	{
		val currentSize = revertUpdateStack.size
		if (revertUpdateStackSavePointer < currentSize)
		{
			val start = revertUpdateStackSavePointer
			for (i in start until  currentSize)
			{
				// TODO write this somewhere!
				revertUpdateStack[i]
			}
			revertUpdateStackSavePointer = currentSize
		}
	}

	/**
	 * The [queue][ConcurrentLinkedQueue] of [EditAction]s that are waiting to
	 * be applied to the contained [AvailServerFile].
	 */
	private val updateQueue = ConcurrentLinkedQueue<EditAction>()

	/**
	 * Is the [file] being updated from the [updateQueue]? `true` indicates it
	 * is; `false` otherwise.
	 */
	private val isReadyToUpdate = AtomicBoolean(true)

	/**
	 * Remove all the [EditAction]s from the [updateQueue] and perform them
	 * in FIFO order if this [ServerFileWrapper] [is ready][isReadyToUpdate]
	 * to update.
	 */
	private fun performEdits ()
	{
		if (isReadyToUpdate.getAndSet(false))
		{
			var action = updateQueue.poll()
			while (action != null)
			{
				action.update(file).forEach {reverse ->
					revertUpdateStack.push(reverse)
				}
				action = updateQueue.poll()
			}
			// TODO is this safe
			synchronized(updateQueue)
			{
				isReadyToUpdate.set(true)
			}
			// TODO Save file
		}
	}

	/**
	 * [Update][EditAction.update] the wrapped [AvailServerFile] with the
	 * provided [EditAction].
	 *
	 * @param editAction
	 *   The `EditAction` to perform.
	 */
	fun update (editAction: EditAction)
	{
		synchronized(updateQueue)
		{
			updateQueue.add(editAction)
		}
		performEdits()
	}

	init
	{
		FileManager.executeFileTask (Runnable {
			val p = Paths.get(path)
			val mimeType = Tika().detect(p)
			file = createFile(path, fileChannel, mimeType, this)
		})
	}

	/** Close this [ServerFileWrapper]. */
	fun close () { file.close() }

	/**
	 * Provide the [raw bytes][AvailServerFile.rawContent] of the enclosed
	 * [AvailServerFile] to the requesting consumer.
	 *
	 * @param consumer
	 *   A function that accepts the [raw bytes][AvailServerFile.rawContent] of
	 *   an [AvailServerFile].
	 * @param failureHandler
	 *   A function that accepts TODO figure out how error handling will happen
	 */
	fun provide(consumer: (ByteArray) -> Unit, failureHandler: () -> Unit)
	{
		// TODO should be run on a separate thread or is that handled inside the
		//  consumer?
		if (!isLoadingFile)
		{
			// Opportunistic
			file.provideContent(consumer)
			return
		}
		synchronized(this)
		{
			if (!isLoadingFile)
			{
				// Opportunistic
				file.provideContent(consumer)
				return
			}
			handlerQueue.add(FileRequestHandler(consumer, failureHandler))
		}
	}

	/**
	 * Notify this [ServerFileWrapper] that the [AvailServerFile] has been fully
	 * read.
	 */
	fun notifyReady ()
	{
		synchronized(this)
		{
			isLoadingFile = false
		}
		// TODO should each be run on a separate thread or is that handled
		//  inside the consumer?
		handlerQueue.forEach { file.provideContent(it.requestConsumer) }
	}

	/**
	 * Notify this [ServerFileWrapper]
	 */
	fun notifyFailure ()
	{
		// TODO figure out input and how to handle this
		//  this includes handling new requests...
	}

	companion object
	{
		/**
		 * Answer a new [AvailServerFile] that will be associated with the
		 * [ServerFileWrapper].
		 *
		 * @param path
		 *   The String path to the target file on disk.
		 * @param file
		 *   The [AsynchronousFileChannel] used to access the file.
		 * @param mimeType
		 *   The MIME type of the file.
		 * @param serverFileWrapper
		 *   The owning `ServerFileWrapper`.
		 */
		fun createFile (
			path: String,
			file: AsynchronousFileChannel,
			mimeType: String,
			serverFileWrapper: ServerFileWrapper): AvailServerFile
		{
			return when (mimeType)
			{
				"text/plain", "text/json" ->
					AvailServerTextFile(path, file, mimeType, serverFileWrapper)
				else ->
					AvailServerBinaryFile(path, file, mimeType, serverFileWrapper)
			}
		}
	}
}

/**
 * A `FileRequestHandler` handles success and failure cases for requesting a
 * file's raw binary data.
 *
 * @author Richard Arriaga &lt;rich@availlang.org&gt;
 *
 * @property requestConsumer
 *   A function that accepts the [raw bytes][AvailServerFile.rawContent] of an
 *   [AvailServerFile].
 * @property failureHandler
 *   A function that accepts TODO figure out how error handling will happen
 *
 * @constructor
 * Construct a [FileRequestHandler].
 *
 * @param requestConsumer
 *   A function that accepts the [raw bytes][AvailServerFile.rawContent] of an
 *   [AvailServerFile].
 * @param failureHandler
 *   A function that accepts TODO figure out how error handling will happen
 */
internal class FileRequestHandler constructor(
	val requestConsumer: (ByteArray) -> Unit,
	val failureHandler: () -> Unit)