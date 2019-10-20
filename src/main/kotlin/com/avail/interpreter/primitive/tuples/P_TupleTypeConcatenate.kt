/*
 * P_TupleTypeConcatenate.kt
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
package com.avail.interpreter.primitive.tuples

import com.avail.descriptor.A_Type
import com.avail.descriptor.ConcatenatedTupleTypeDescriptor.concatenatingAnd
import com.avail.descriptor.FunctionTypeDescriptor.functionType
import com.avail.descriptor.ObjectTupleDescriptor.tuple
import com.avail.descriptor.TupleTypeDescriptor
import com.avail.descriptor.TupleTypeDescriptor.tupleMeta
import com.avail.descriptor.TypeDescriptor
import com.avail.interpreter.Interpreter
import com.avail.interpreter.Primitive
import com.avail.interpreter.Primitive.Flag.*

/**
 * **Primitive:** Answer the [type][TypeDescriptor] that is the type of all
 * possible concatenations of instances of the given [tuple
 * types][TupleTypeDescriptor]. This is basically the semantic restriction of
 * the two-argument concatenation operation.
 */
@Suppress("unused")
object P_TupleTypeConcatenate : Primitive(2, CannotFail, CanFold, CanInline)
{
	override fun attempt(interpreter: Interpreter): Result
	{
		interpreter.checkArgumentCount(2)
		val tupleType1 = interpreter.argument(0)
		val tupleType2 = interpreter.argument(1)
		return interpreter.primitiveSuccess(
			concatenatingAnd(tupleType1, tupleType2))
	}

	override fun privateBlockTypeRestriction(): A_Type =
		functionType(
			tuple(
				tupleMeta(),
				tupleMeta()),
			tupleMeta())
}