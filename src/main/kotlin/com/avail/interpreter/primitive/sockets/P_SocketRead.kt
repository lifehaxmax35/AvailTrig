/*
 * P_SocketRead.kt
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

package com.avail.interpreter.primitive.sockets

import com.avail.AvailRuntime.currentRuntime
import com.avail.descriptor.*
import com.avail.descriptor.AbstractEnumerationTypeDescriptor.enumerationWith
import com.avail.descriptor.AtomDescriptor.SpecialAtom.SOCKET_KEY
import com.avail.descriptor.AtomDescriptor.objectFromBoolean
import com.avail.descriptor.ByteBufferTupleDescriptor.tupleForByteBuffer
import com.avail.descriptor.EnumerationTypeDescriptor.booleanType
import com.avail.descriptor.FiberDescriptor.newFiber
import com.avail.descriptor.FiberTypeDescriptor.mostGeneralFiberType
import com.avail.descriptor.FunctionTypeDescriptor.functionType
import com.avail.descriptor.InstanceTypeDescriptor.instanceType
import com.avail.descriptor.IntegerRangeTypeDescriptor.bytes
import com.avail.descriptor.IntegerRangeTypeDescriptor.inclusive
import com.avail.descriptor.ObjectTupleDescriptor.tuple
import com.avail.descriptor.SetDescriptor.set
import com.avail.descriptor.StringDescriptor.formatString
import com.avail.descriptor.TupleTypeDescriptor.zeroOrMoreOf
import com.avail.descriptor.TypeDescriptor.Types.ATOM
import com.avail.descriptor.TypeDescriptor.Types.TOP
import com.avail.exceptions.AvailErrorCode
import com.avail.exceptions.AvailErrorCode.*
import com.avail.interpreter.Interpreter
import com.avail.interpreter.Primitive
import com.avail.interpreter.Primitive.Flag.CanInline
import com.avail.interpreter.Primitive.Flag.HasSideEffect
import com.avail.io.SimpleCompletionHandler
import java.lang.Integer.MAX_VALUE
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.util.Arrays.asList

/**
 * **Primitive:** Initiate an asynchronous read from the
 * [socket][AsynchronousSocketChannel] referenced by the specified
 * [handle][AtomDescriptor]. Create a new [ fiber][FiberDescriptor] to respond to the asynchronous completion of the operation; the fiber
 * will run at the specified [ priority][IntegerRangeTypeDescriptor.bytes]. If the operation succeeds, then eventually start the new fiber to
 * apply the [success function][FunctionDescriptor] to the [ ] and a [ ][EnumerationTypeDescriptor.booleanType] that is [ ][AtomDescriptor.trueObject] if the socket is exhausted. If the
 * operation fails, then eventually start the new fiber to apply the [ ] to the [ numeric][IntegerDescriptor] [error code][AvailErrorCode]. Answer the new fiber.
 *
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
object P_SocketRead : Primitive(5, CanInline, HasSideEffect)
{

	override fun attempt(
		interpreter: Interpreter): Result
	{
		interpreter.checkArgumentCount(5)
		val size = interpreter.argument(0)
		val handle = interpreter.argument(1)
		val succeed = interpreter.argument(2)
		val fail = interpreter.argument(3)
		val priority = interpreter.argument(4)
		val pojo = handle.getAtomProperty(SOCKET_KEY.atom)
		if (pojo.equalsNil())
		{
			return interpreter.primitiveFailure(
				if (handle.isAtomSpecial) E_SPECIAL_ATOM else E_INVALID_HANDLE)
		}
		val socket = pojo.javaObjectNotNull<AsynchronousSocketChannel>()
		val buffer = ByteBuffer.allocateDirect(size.extractInt())
		val current = interpreter.fiber()
		val newFiber = newFiber(
			succeed.kind().returnType().typeUnion(fail.kind().returnType()),
			priority.extractInt()
		) { formatString("Socket read, %s", handle.atomName()) }
		// If the current fiber is an Avail fiber, then the new one should be
		// also.
		newFiber.availLoader(current.availLoader())
		// Share and inherit any heritable variables.
		newFiber.heritableFiberGlobals(
			current.heritableFiberGlobals().makeShared())
		// Inherit the fiber's text interface.
		newFiber.textInterface(current.textInterface())
		// Share everything that will potentially be visible to the fiber.
		newFiber.makeShared()
		succeed.makeShared()
		fail.makeShared()
		// Now start the asynchronous read.
		val runtime = currentRuntime()
		try
		{
			socket.read<Any>(
				buffer,
				null,
				SimpleCompletionHandler(
					{ bytesRead ->
						buffer.flip()
						Interpreter.runOutermostFunction(
							runtime,
							newFiber,
							succeed,
							asList(
								tupleForByteBuffer(buffer),
								objectFromBoolean(bytesRead == -1)))
						Unit
					},
					{ killer ->
						Interpreter.runOutermostFunction(
							runtime,
							newFiber,
							fail,
							listOf(E_IO_ERROR.numericCode()))
						Unit
					}))
		}
		catch (e: IllegalArgumentException)
		{
			// This should only happen if the buffer is read only, which is
			// impossible by construction here.
			assert(false)
			return interpreter.primitiveFailure(E_IO_ERROR)
		}
		catch (e: IllegalStateException)
		{
			return interpreter.primitiveFailure(E_INVALID_HANDLE)
		}

		return interpreter.primitiveSuccess(newFiber)
	}

	override fun privateBlockTypeRestriction(): A_Type
	{
		return functionType(
			tuple(
				inclusive(0, MAX_VALUE.toLong()),
				ATOM.o(),
				functionType(
					tuple(
						zeroOrMoreOf(bytes()),
						booleanType()),
					TOP.o()),
				functionType(
					tuple(instanceType(E_IO_ERROR.numericCode())),
					TOP.o()),
				bytes()),
			mostGeneralFiberType())
	}

	override fun privateFailureVariableType(): A_Type
	{
		return enumerationWith(
			set(
				E_INVALID_HANDLE,
				E_SPECIAL_ATOM,
				E_IO_ERROR))
	}

}