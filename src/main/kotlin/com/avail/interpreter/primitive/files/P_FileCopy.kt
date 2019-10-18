/*
 * P_FileCopy.kt
 * Copyright © 1993-2018, The Avail Foundation, LLC.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of the contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
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

package com.avail.interpreter.primitive.files

import com.avail.descriptor.A_Type
import com.avail.descriptor.AbstractEnumerationTypeDescriptor.enumerationWith
import com.avail.descriptor.EnumerationTypeDescriptor.booleanType
import com.avail.descriptor.FunctionTypeDescriptor.functionType
import com.avail.descriptor.NilDescriptor.nil
import com.avail.descriptor.ObjectTupleDescriptor.tuple
import com.avail.descriptor.SetDescriptor.set
import com.avail.descriptor.TupleTypeDescriptor.stringType
import com.avail.descriptor.TypeDescriptor.Types.TOP
import com.avail.exceptions.AvailErrorCode.*
import com.avail.interpreter.Interpreter
import com.avail.interpreter.Primitive
import com.avail.interpreter.Primitive.Flag.CanInline
import com.avail.interpreter.Primitive.Flag.HasSideEffect
import com.avail.io.IOSystem
import com.avail.utility.Mutable
import java.io.IOException
import java.nio.file.*
import java.nio.file.FileVisitResult.CONTINUE
import java.nio.file.attribute.BasicFileAttributes
import java.util.*

/**
 * **Primitive:** Recursively copy the source [ path][Path] to the destination path.
 *
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
object P_FileCopy : Primitive(5, CanInline, HasSideEffect)
{

	override fun attempt(
		interpreter: Interpreter): Result
	{
		interpreter.checkArgumentCount(5)
		val source = interpreter.argument(0)
		val destination = interpreter.argument(1)
		val followSymlinks = interpreter.argument(2)
		val replace = interpreter.argument(3)
		val copyAttributes = interpreter.argument(4)
		val sourcePath: Path
		val destinationPath: Path
		try
		{
			sourcePath = IOSystem.fileSystem.getPath(
				source.asNativeString())
			destinationPath = IOSystem.fileSystem.getPath(
				destination.asNativeString())
		}
		catch (e: InvalidPathException)
		{
			return interpreter.primitiveFailure(E_INVALID_PATH)
		}

		val optionList = ArrayList<CopyOption>(2)
		if (replace.extractBoolean())
		{
			optionList.add(StandardCopyOption.REPLACE_EXISTING)
		}
		if (copyAttributes.extractBoolean())
		{
			optionList.add(StandardCopyOption.COPY_ATTRIBUTES)
		}
		val options = optionList.toTypedArray()
		try
		{
			val visitOptions = if (followSymlinks.extractBoolean())
				EnumSet.of(FileVisitOption.FOLLOW_LINKS)
			else
				EnumSet.noneOf(FileVisitOption::class.java)
			val partialSuccess = Mutable(false)
			Files.walkFileTree(
				sourcePath,
				visitOptions,
				Integer.MAX_VALUE,
				object : FileVisitor<Path>
				{
					@Throws(IOException::class)
					override fun preVisitDirectory(
						dir: Path?,
						unused: BasicFileAttributes?): FileVisitResult
					{
						assert(dir !== null)
						val dstDir = destinationPath.resolve(
							sourcePath.relativize(dir!!))
						try
						{
							Files.copy(dir, dstDir, *options)
						}
						catch (e: FileAlreadyExistsException)
						{
							if (!Files.isDirectory(dstDir))
							{
								throw e
							}
						}

						return CONTINUE
					}

					@Throws(IOException::class)
					override fun visitFile(
						file: Path?,
						unused: BasicFileAttributes?): FileVisitResult
					{
						assert(file !== null)
						Files.copy(
							file!!,
							destinationPath.resolve(
								sourcePath.relativize(file)),
							*options)
						return CONTINUE
					}

					override fun visitFileFailed(
						file: Path?,
						unused: IOException?): FileVisitResult
					{
						partialSuccess.value = true
						return CONTINUE
					}

					override fun postVisitDirectory(
						dir: Path?,
						e: IOException?): FileVisitResult
					{
						if (e !== null)
						{
							partialSuccess.value = true
						}
						return CONTINUE
					}
				})
			if (partialSuccess.value)
			{
				return interpreter.primitiveFailure(E_PARTIAL_SUCCESS)
			}
		}
		catch (e: SecurityException)
		{
			return interpreter.primitiveFailure(E_PERMISSION_DENIED)
		}
		catch (e: AccessDeniedException)
		{
			return interpreter.primitiveFailure(E_PERMISSION_DENIED)
		}
		catch (e: IOException)
		{
			return interpreter.primitiveFailure(E_IO_ERROR)
		}

		return interpreter.primitiveSuccess(nil)
	}

	override fun privateBlockTypeRestriction(): A_Type
	{
		return functionType(
			tuple(stringType(), stringType(), booleanType(), booleanType(),
			      booleanType()), TOP.o())
	}

	override fun privateFailureVariableType(): A_Type
	{
		return enumerationWith(
			set(E_INVALID_PATH, E_PERMISSION_DENIED, E_IO_ERROR,
			    E_PARTIAL_SUCCESS))
	}

}