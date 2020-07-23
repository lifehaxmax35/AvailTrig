/*
 * P_MethodHasDefinitionForArgumentTypes.kt
 * Copyright © 1993-2019, The Avail Foundation, LLC.
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

package com.avail.interpreter.primitive.methods

import com.avail.descriptor.atoms.A_Atom.Companion.bundleOrNil
import com.avail.descriptor.atoms.AtomDescriptor.Companion.falseObject
import com.avail.descriptor.atoms.AtomDescriptor.Companion.trueObject
import com.avail.descriptor.bundles.A_Bundle.Companion.bundleMethod
import com.avail.descriptor.methods.MethodDescriptor
import com.avail.descriptor.sets.SetDescriptor.Companion.set
import com.avail.descriptor.tuples.ObjectTupleDescriptor.Companion.tuple
import com.avail.descriptor.tuples.TupleDescriptor
import com.avail.descriptor.types.A_Type
import com.avail.descriptor.types.AbstractEnumerationTypeDescriptor.Companion.enumerationWith
import com.avail.descriptor.types.EnumerationTypeDescriptor.Companion.booleanType
import com.avail.descriptor.types.FunctionTypeDescriptor.Companion.functionType
import com.avail.descriptor.types.InstanceMetaDescriptor.Companion.anyMeta
import com.avail.descriptor.types.TupleTypeDescriptor.Companion.zeroOrMoreOf
import com.avail.descriptor.types.TypeDescriptor
import com.avail.descriptor.types.TypeDescriptor.Types.ATOM
import com.avail.exceptions.AvailErrorCode.E_AMBIGUOUS_METHOD_DEFINITION
import com.avail.exceptions.AvailErrorCode.E_INCORRECT_NUMBER_OF_ARGUMENTS
import com.avail.exceptions.AvailErrorCode.E_NO_METHOD
import com.avail.exceptions.AvailErrorCode.E_NO_METHOD_DEFINITION
import com.avail.exceptions.MethodDefinitionException
import com.avail.interpreter.Primitive
import com.avail.interpreter.Primitive.Flag.CanInline
import com.avail.interpreter.execution.Interpreter

/**
 * **Primitive:** Does the [method][MethodDescriptor] have a unique definition
 * for the specified [tuple][TupleDescriptor] of parameter
 * [types][TypeDescriptor]?
 *
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
@Suppress("unused")
object P_BundleHasDefinitionForArgumentTypes : Primitive(2, CanInline)
{
	override fun attempt(interpreter: Interpreter): Result
	{
		interpreter.checkArgumentCount(2)
		val methodName = interpreter.argument(0)
		val argTypes = interpreter.argument(1)
		val bundle = methodName.bundleOrNil()
		if (bundle.equalsNil())
		{
			return interpreter.primitiveFailure(E_NO_METHOD)
		}
		val method = bundle.bundleMethod()
		if (argTypes.tupleSize() != method.numArgs())
		{
			return interpreter.primitiveFailure(
				E_INCORRECT_NUMBER_OF_ARGUMENTS)
		}
		return interpreter.primitiveSuccess(
			try
			{
				val definition =
					method.lookupByTypesFromTuple(argTypes)
				assert(!definition.equalsNil())
				trueObject
			}
			catch (e: MethodDefinitionException)
			{
				falseObject
			})
	}

	override fun privateBlockTypeRestriction(): A_Type =
		functionType(tuple(ATOM.o(), zeroOrMoreOf(anyMeta())), booleanType())

	override fun privateFailureVariableType(): A_Type =
		enumerationWith(
			set(
				E_AMBIGUOUS_METHOD_DEFINITION,
				E_INCORRECT_NUMBER_OF_ARGUMENTS,
				E_NO_METHOD,
				E_NO_METHOD_DEFINITION))
}
