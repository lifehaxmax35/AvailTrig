/*
 * P_Serialize.kt
 * Copyright © 1993-2022, The Avail Foundation, LLC.
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

package avail.interpreter.primitive.general

import avail.descriptor.representation.A_BasicObject
import avail.descriptor.sets.SetDescriptor.Companion.set
import avail.descriptor.tuples.ByteArrayTupleDescriptor.Companion.tupleForByteArray
import avail.descriptor.tuples.ObjectTupleDescriptor.Companion.tuple
import avail.descriptor.types.A_Type
import avail.descriptor.types.AbstractEnumerationTypeDescriptor.Companion.enumerationWith
import avail.descriptor.types.FunctionTypeDescriptor.Companion.functionType
import avail.descriptor.types.IntegerRangeTypeDescriptor.Companion.bytes
import avail.descriptor.types.TupleTypeDescriptor.Companion.oneOrMoreOf
import avail.descriptor.types.PrimitiveTypeDescriptor.Types.ANY
import avail.exceptions.AvailErrorCode.E_SERIALIZATION_FAILED
import avail.interpreter.Primitive
import avail.interpreter.Primitive.Flag.CanInline
import avail.interpreter.execution.Interpreter
import avail.serialization.Serializer
import java.io.ByteArrayOutputStream

/**
 * **Primitive:** Answer the serial representation of the specified
 * [value][A_BasicObject].
 *
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
@Suppress("unused")
object P_Serialize : Primitive(1, CanInline)
{
	override fun attempt(interpreter: Interpreter): Result
	{
		interpreter.checkArgumentCount(1)
		val value = interpreter.argument(0)
		val out = ByteArrayOutputStream(100)
		val serializer = Serializer(out)
		try
		{
			serializer.serialize(value)
		}
		catch (e: Exception)
		{
			return interpreter.primitiveFailure(E_SERIALIZATION_FAILED)
		}

		return interpreter.primitiveSuccess(
			tupleForByteArray(out.toByteArray()))
	}

	override fun privateBlockTypeRestriction(): A_Type =
		functionType(tuple(ANY.o), oneOrMoreOf(bytes))

	override fun privateFailureVariableType(): A_Type =
		enumerationWith(set(E_SERIALIZATION_FAILED))
}
