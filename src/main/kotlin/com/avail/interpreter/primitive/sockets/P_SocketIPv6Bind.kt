/*
 * P_SocketIPv6Bind.java
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

import com.avail.descriptor.A_Type
import com.avail.descriptor.AtomDescriptor
import com.avail.descriptor.AvailObject
import com.avail.interpreter.Interpreter
import com.avail.interpreter.Primitive
import com.avail.optimizer.jvm.ReferencedInGeneratedCode

import java.io.IOException
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.net.UnknownHostException
import java.nio.channels.AsynchronousSocketChannel

import com.avail.descriptor.AbstractEnumerationTypeDescriptor.enumerationWith
import com.avail.descriptor.AtomDescriptor.SpecialAtom.SOCKET_KEY
import com.avail.descriptor.FunctionTypeDescriptor.functionType
import com.avail.descriptor.IntegerRangeTypeDescriptor.bytes
import com.avail.descriptor.IntegerRangeTypeDescriptor.singleInt
import com.avail.descriptor.IntegerRangeTypeDescriptor.unsignedShorts
import com.avail.descriptor.NilDescriptor.nil
import com.avail.descriptor.ObjectTupleDescriptor.tuple
import com.avail.descriptor.SetDescriptor.set
import com.avail.descriptor.TupleDescriptor.emptyTuple
import com.avail.descriptor.TupleTypeDescriptor.tupleTypeForSizesTypesDefaultType
import com.avail.descriptor.TypeDescriptor.Types.ATOM
import com.avail.descriptor.TypeDescriptor.Types.TOP
import com.avail.exceptions.AvailErrorCode.E_INVALID_HANDLE
import com.avail.exceptions.AvailErrorCode.E_IO_ERROR
import com.avail.exceptions.AvailErrorCode.E_PERMISSION_DENIED
import com.avail.exceptions.AvailErrorCode.E_SPECIAL_ATOM
import com.avail.interpreter.Primitive.Flag.CanInline
import com.avail.interpreter.Primitive.Flag.HasSideEffect

/**
 * **Primitive:** Bind the [ ] referenced by the
 * specified [handle][AtomDescriptor] to an [ IPv6 address][Inet6Address] and port. The bytes of the address are specified in network
 * byte order, i.e., big-endian.
 *
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
object P_SocketIPv6Bind : Primitive(3, CanInline, HasSideEffect)
{

	override fun attempt(
		interpreter: Interpreter): Primitive.Result
	{
		interpreter.checkArgumentCount(3)
		val handle = interpreter.argument(0)
		val addressTuple = interpreter.argument(1)
		val port = interpreter.argument(2)
		val pojo = handle.getAtomProperty(SOCKET_KEY.atom)
		if (pojo.equalsNil())
		{
			return interpreter.primitiveFailure(
				if (handle.isAtomSpecial)
					E_SPECIAL_ATOM
				else
					E_INVALID_HANDLE)
		}
		val socket = pojo.javaObjectNotNull<AsynchronousSocketChannel>()
		// Build the big-endian address byte array.
		val addressBytes = ByteArray(16)
		for (i in addressBytes.indices)
		{
			val addressByte = addressTuple.tupleAt(i + 1)
			addressBytes[i] = addressByte.extractUnsignedByte().toByte()
		}
		try
		{
			val inetAddress = InetAddress.getByAddress(addressBytes) as Inet4Address
			val address = InetSocketAddress(inetAddress, port.extractUnsignedShort())
			socket.bind(address)
			return interpreter.primitiveSuccess(nil)
		}
		catch (e: IllegalStateException)
		{
			return interpreter.primitiveFailure(E_INVALID_HANDLE)
		}
		catch (e: UnknownHostException)
		{
			// This shouldn't actually happen, since we carefully enforce the
			// range of addresses.
			assert(false)
			return interpreter.primitiveFailure(E_IO_ERROR)
		}
		catch (e: IOException)
		{
			return interpreter.primitiveFailure(E_IO_ERROR)
		}
		catch (e: SecurityException)
		{
			return interpreter.primitiveFailure(E_PERMISSION_DENIED)
		}

	}

	override fun privateBlockTypeRestriction(): A_Type
	{
		return functionType(
			tuple(
				ATOM.o(),
				tupleTypeForSizesTypesDefaultType(
					singleInt(16),
					emptyTuple(),
					bytes()),
				unsignedShorts()),
			TOP.o())
	}

	override fun privateFailureVariableType(): A_Type
	{
		return enumerationWith(
			set(
				E_INVALID_HANDLE,
				E_SPECIAL_ATOM,
				E_IO_ERROR,
				E_PERMISSION_DENIED))
	}

}