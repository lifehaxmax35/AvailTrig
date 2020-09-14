/*
 * FileManager.kt
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

import com.avail.AvailRuntime
import com.avail.AvailThread
import com.avail.server.AvailServer
import com.avail.server.error.ServerErrorCode
import com.avail.server.error.ServerErrorCode.*
import com.avail.utility.LRUCache
import com.avail.utility.Mutable
import java.io.IOException
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*
import java.util.concurrent.ThreadPoolExecutor

/**
 * `FileManager` manages the opened files of the Avail Server. It provides an
 * LRU caching mechanism by which files can be added and removed as needed to
 * control the number of open files in memory.
 *
 * @author Richard Arriaga &lt;rich@availlang.org&gt;
 *
 * @property runtime
 *   The [AvailRuntime] that is associated with the running [AvailServer].
 *
 * @constructor
 * Construct a [FileManager].
 *
 * @param runtime
 *   The [AvailRuntime] that is associated with the running [AvailServer].
 */
abstract class FileManager constructor(protected val runtime: AvailRuntime)
{
	/**
	 * The [EnumSet] of [StandardOpenOption]s used when creating files.
	 */
	protected val fileCreateOptions: EnumSet<StandardOpenOption> =
		EnumSet.of(
			StandardOpenOption.READ,
			StandardOpenOption.WRITE,
			StandardOpenOption.CREATE_NEW)

	companion object
	{
		// TODO make the softCapacity and strongCapacity configurable,
		//  not magic numbers
		// The LRUCache capacities
		const val SOFT_CAPACITY = 10000
		const val STRONG_CAPACITY = 10
	}

	/**
	 * The [thread pool executor][ThreadPoolExecutor] for asynchronous file
	 * operations performed on behalf of this [FileManager].
	 */
	protected val fileExecutor = runtime.ioSystem().fileExecutor

	/**
	 * Maintain an [LRUCache] of [AvailServerFile]s opened by the Avail server.
	 *
	 * This cache works in conjunction with [pathToIdMap] to maintain links
	 * between
	 */

	protected val fileCache = LRUCache<UUID, Mutable<AbstractServerFileWrapper?>>(
		SOFT_CAPACITY,
		STRONG_CAPACITY,
		{
			var path = ""
			pathToIdMap.forEach { (k, v) ->
				if (v == it)
				{
					path = k
					return@forEach
				}
			}
			val wrapper: AbstractServerFileWrapper? =
				try
				{
					if (path.isEmpty())
					{
						null
					}
					else
					{
						serverFileWrapper(it, path)
					}
				}
				catch (e: NoSuchFileException)
				{
					ErrorServerFileWrapper(
						it, path, this, e, FILE_NOT_FOUND)
					null
				}

				catch (e: Throwable)
				{
					ErrorServerFileWrapper(
						it, path, this, e, UNSPECIFIED)
					null
				}
			Mutable(wrapper)
		},
		{ _, value ->
			try
			{
				value.value?.close()
			}
			catch (e: IOException)
			{
				// Do nothing
			}
		})

	/**
	 * Answer a [ServerFileWrapper] for the targeted file.
	 *
	 * @param id
	 *   The [ServerFileWrapper.id].
	 * @param path
	 *   The location of the file.
	 * @return
	 *   A `ServerFileWrapper`.
	 */
	protected abstract fun serverFileWrapper(
		id: UUID, path: String): ServerFileWrapper

	/**
	 * Fully remove the file associated with the provided [fileCache] id. This
	 * also removes it from [pathToIdMap].
	 *
	 * @param id
	 *   The [UUID] that uniquely identifies the target file in the cache.
	 */
	fun remove (id: UUID)
	{
		fileCache[id].value?.let {
			fileCache.remove(id)
			pathToIdMap.remove(it.path)
			idToPathMap.remove(id)
		} ?: idToPathMap.remove(id)?.let { pathToIdMap.remove(it) }
	}

	/**
	 * Deregister interest in the file associated with the provided [fileCache]
	 * id. If the resulting [interest count][ServerFileWrapper.interestCount]
	 * is 0, the file is closed and fully removed from the [fileCache].
	 *
	 * @param id
	 *   The [UUID] that uniquely identifies the target file in the cache.
	 */
	fun deregisterInterest (id: UUID)
	{
		fileCache[id].value.let {
			if (it?.interestCount?.decrementAndGet() == 0)
			{
				remove(id)
				it.close()
			}
		}
	}

	/**
	 * A [Map] from the String [Path] location of a [file][AvailServerFile] to
	 * the [UUID] that uniquely identifies that file in the [fileCache].
	 *
	 * This map will never be cleared of values as cached files that have been
	 * removed from the `fileCache` must maintain association with the
	 * server-assigned [UUID] that identifies the file for all interested
	 * clients. If a client requests a file action with a given UUID and it is
	 * not found in the `fileCache`, this map will be used to retrieve the
	 * associated file from disk and placed back in the `fileCache`.
	 */
	protected val pathToIdMap = mutableMapOf<String, UUID>()

	/**
	 * Answer the [FileManager] file id for the provided path.
	 *
	 * @param path
	 *   The path of the file to get the file id for.
	 * @return The file id or `null` if not in file manager.
	 */
	fun fileId (path: String): UUID? = pathToIdMap[path]

	/**
	 * A [Map] from the file cache [id][UUID] that uniquely identifies that file
	 * in the [fileCache] to the String [Path] location of a
	 * [file][AvailServerFile].
	 *
	 * This map will never be cleared of values as cached files that have been
	 * removed from the `fileCache` must maintain association with the
	 * server-assigned [UUID] that identifies the file for all interested
	 * clients. If a client requests a file action with a given UUID and it is
	 * not found in the `fileCache`, this map will be used to retrieve the
	 * associated file from disk and placed back in the `fileCache`.
	 */
	protected val idToPathMap = mutableMapOf<UUID, String>()

	/**
	 * Schedule the specified task for eventual execution
	 * by the [thread pool executor][ThreadPoolExecutor] for
	 * asynchronous file operations. The implementation is free to run the task
	 * immediately or delay its execution arbitrarily. The task will not execute
	 * on an [Avail thread][AvailThread].
	 *
	 * @param task
	 *   A task.
	 */
	fun executeFileTask(task: () -> Unit)
	{
		fileExecutor.execute(task)
	}

	/**
	 * Retrieve the [ServerFileWrapper] and provide it with a request to obtain
	 * the [raw file bytes][AvailServerFile.rawContent].
	 *
	 * @param id
	 *   The [ServerFileWrapper] cache id of the file to act upon.
	 * @param fileAction
	 *   The [FileAction] to execute.
	 * @param continuation
	 *   What to do when sufficient processing has occurred.
	 * @param failureHandler
	 *   A function that accepts a [ServerErrorCode] that describes the nature of
	 *   the failure and an optional [Throwable].
	 */
	fun executeAction (
		id: UUID,
		fileAction: FileAction,
		continuation: () -> Unit,
		failureHandler: (ServerErrorCode, Throwable?) -> Unit)
	{
		fileCache[id].value?.execute(fileAction, continuation)
			?: failureHandler(BAD_FILE_ID, null)
	}

	/**
	 * Save the file to d
	 */
	abstract fun saveFile (
		availServerFile: AvailServerFile,
		failureHandler: (ServerErrorCode, Throwable?) -> Unit)

	/**
	 * Delete the file at the provided path.
	 *
	 * @param path
	 *   The String path to the file to be deleted.
	 * @param success
	 *   Accepts the [FileManager] file id if remove successful. Maybe `null`
	 *   if file not present in `FileManager`.
	 * @param failure
	 *   A function that accepts a [ServerErrorCode] that describes the nature
	 *   of the failure and an optional [Throwable].
	 */
	abstract fun delete (
		path: String,
		success: (UUID?) -> Unit,
		failure: (ServerErrorCode, Throwable?) -> Unit)

	/**
	 * Retrieve the [ServerFileWrapper] and provide it with a request to obtain
	 * the [raw file bytes][AvailServerFile.rawContent].
	 *
	 * @param path
	 *   The String path location of the file.
	 * @param consumer
	 *   A function that accepts the [FileManager.fileCache] [UUID] that
	 *   uniquely identifies the file, the String mime type, and the
	 *   [raw bytes][AvailServerFile.rawContent] of an [AvailServerFile].
	 * @param failureHandler
	 *   A function that accepts a [ServerErrorCode] that describes the nature
	 *   of the failure and an optional [Throwable].
	 * @return
	 *   The [FileManager] file id for the file.
	 */
	abstract fun readFile (
		path: String,
		consumer: (UUID, String, ByteArray) -> Unit,
		failureHandler: (ServerErrorCode, Throwable?) -> Unit): UUID

	/**
	 * Create a [ServerFileWrapper] and provide it with a request to obtain
	 * the [raw file bytes][AvailServerFile.rawContent].
	 *
	 * @param path
	 *   The String path location of the file.
	 * @param consumer
	 *   A function that accepts the [FileManager.fileCache] [UUID] that
	 *   uniquely identifies the file, the String mime type, and the
	 *   [raw bytes][AvailServerFile.rawContent] of an [AvailServerFile].
	 * @param failureHandler
	 *   A function that accepts a [ServerErrorCode] that describes the failure
	 *   and an optional [Throwable].
	 * @return
	 *   The [FileManager] file id for the file.
	 */
	abstract fun createFile (
		path: String,
		consumer: (UUID, String, ByteArray) -> Unit,
		failureHandler: (ServerErrorCode, Throwable?) -> Unit): UUID?
}