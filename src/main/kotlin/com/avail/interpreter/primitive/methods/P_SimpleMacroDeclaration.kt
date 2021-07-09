/*
 * P_SimpleMacroDeclaration.kt
 * Copyright © 1993-2021, The Avail Foundation, LLC.
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

import com.avail.compiler.splitter.MessageSplitter
import com.avail.compiler.splitter.MessageSplitter.Companion.possibleErrors
import com.avail.compiler.splitter.MessageSplitter.Metacharacter
import com.avail.descriptor.functions.A_RawFunction.Companion.methodName
import com.avail.descriptor.functions.A_RawFunction.Companion.numArgs
import com.avail.descriptor.functions.FunctionDescriptor
import com.avail.descriptor.methods.MethodDescriptor
import com.avail.descriptor.representation.NilDescriptor.Companion.nil
import com.avail.descriptor.sets.A_Set.Companion.setUnionCanDestroy
import com.avail.descriptor.sets.SetDescriptor.Companion.set
import com.avail.descriptor.tuples.A_Tuple.Companion.tupleSize
import com.avail.descriptor.tuples.ObjectTupleDescriptor.Companion.tuple
import com.avail.descriptor.tuples.StringDescriptor.Companion.stringFrom
import com.avail.descriptor.tuples.TupleDescriptor
import com.avail.descriptor.types.A_Type
import com.avail.descriptor.types.A_Type.Companion.argsTupleType
import com.avail.descriptor.types.A_Type.Companion.isSubtypeOf
import com.avail.descriptor.types.A_Type.Companion.returnType
import com.avail.descriptor.types.A_Type.Companion.typeAtIndex
import com.avail.descriptor.types.AbstractEnumerationTypeDescriptor.Companion.enumerationWith
import com.avail.descriptor.types.FunctionTypeDescriptor.Companion.functionType
import com.avail.descriptor.types.FunctionTypeDescriptor.Companion.functionTypeReturning
import com.avail.descriptor.types.FunctionTypeDescriptor.Companion.mostGeneralFunctionType
import com.avail.descriptor.types.PhraseTypeDescriptor.PhraseKind.PARSE_PHRASE
import com.avail.descriptor.types.TupleTypeDescriptor.Companion.stringType
import com.avail.descriptor.types.TupleTypeDescriptor.Companion.zeroOrMoreOf
import com.avail.descriptor.types.TypeDescriptor.Types.TOP
import com.avail.exceptions.AvailErrorCode.E_AMBIGUOUS_NAME
import com.avail.exceptions.AvailErrorCode.E_CANNOT_DEFINE_DURING_COMPILATION
import com.avail.exceptions.AvailErrorCode.E_INCORRECT_NUMBER_OF_ARGUMENTS
import com.avail.exceptions.AvailErrorCode.E_LOADING_IS_OVER
import com.avail.exceptions.AvailErrorCode.E_MACRO_ARGUMENT_MUST_BE_A_PARSE_NODE
import com.avail.exceptions.AvailErrorCode.E_MACRO_MUST_RETURN_A_PARSE_NODE
import com.avail.exceptions.AvailErrorCode.E_MACRO_PREFIX_FUNCTIONS_MUST_RETURN_TOP
import com.avail.exceptions.AvailErrorCode.E_MACRO_PREFIX_FUNCTION_ARGUMENT_MUST_BE_A_PARSE_NODE
import com.avail.exceptions.AvailErrorCode.E_MACRO_PREFIX_FUNCTION_INDEX_OUT_OF_BOUNDS
import com.avail.exceptions.AvailErrorCode.E_REDEFINED_WITH_SAME_ARGUMENT_TYPES
import com.avail.exceptions.AvailException
import com.avail.exceptions.MalformedMessageException
import com.avail.interpreter.Primitive
import com.avail.interpreter.Primitive.Flag.CanSuspend
import com.avail.interpreter.Primitive.Flag.HasSideEffect
import com.avail.interpreter.execution.Interpreter

/**
 * **Primitive:** Simple macro definition.  The first argument is the macro
 * name, and the second argument is a [tuple][TupleDescriptor] of
 * [functions][FunctionDescriptor] returning ⊤, one for each occurrence of a
 * [section&#32;sign][Metacharacter.SECTION_SIGN] (§) in the macro name.  The
 * third argument is the function to invoke for the complete macro.  It is
 * constrained to answer a [method][MethodDescriptor].
 */
@Suppress("unused")
object P_SimpleMacroDeclaration : Primitive(3, CanSuspend, HasSideEffect)
{
	override fun attempt(interpreter: Interpreter): Result
	{
		interpreter.checkArgumentCount(3)
		val string = interpreter.argument(0)
		val prefixFunctions = interpreter.argument(1)
		val function = interpreter.argument(2)

		val fiber = interpreter.fiber()
		val loader = fiber.availLoader() ?:
			return interpreter.primitiveFailure(E_LOADING_IS_OVER)
		if (!loader.phase().isExecuting)
		{
			return interpreter.primitiveFailure(
				E_CANNOT_DEFINE_DURING_COMPILATION)
		}
		for (prefixFunction in prefixFunctions)
		{
			val numArgs = prefixFunction.code().numArgs()
			val kind = prefixFunction.kind()
			val argsKind = kind.argsTupleType
			for (argIndex in 1 .. numArgs)
			{
				if (!argsKind.typeAtIndex(argIndex).isSubtypeOf(
						PARSE_PHRASE.mostGeneralType()))
				{
					return interpreter.primitiveFailure(
						E_MACRO_PREFIX_FUNCTION_ARGUMENT_MUST_BE_A_PARSE_NODE)
				}
			}
			if (!kind.returnType.isTop)
			{
				return interpreter.primitiveFailure(
					E_MACRO_PREFIX_FUNCTIONS_MUST_RETURN_TOP)
			}
		}
		try
		{
			val splitter = MessageSplitter(string)
			if (prefixFunctions.tupleSize !=
				splitter.numberOfSectionCheckpoints)
			{
				return interpreter.primitiveFailure(
					E_MACRO_PREFIX_FUNCTION_INDEX_OUT_OF_BOUNDS)
			}
		}
		catch (e: MalformedMessageException)
		{
			return interpreter.primitiveFailure(e.errorCode)
		}

		val numArgs = function.code().numArgs()
		val kind = function.kind()
		val argsKind = kind.argsTupleType
		for (argIndex in 1 .. numArgs)
		{
			if (!argsKind.typeAtIndex(argIndex).isSubtypeOf(
					PARSE_PHRASE.mostGeneralType()))
			{
				return interpreter.primitiveFailure(
					E_MACRO_ARGUMENT_MUST_BE_A_PARSE_NODE)
			}
		}
		if (!kind.returnType.isSubtypeOf(PARSE_PHRASE.mostGeneralType()))
		{
			return interpreter.primitiveFailure(
				E_MACRO_MUST_RETURN_A_PARSE_NODE)
		}
		return interpreter.suspendInLevelOneSafeThen {
			try
			{
				val atom = loader.lookupName(string)
				loader.addMacroBody(
					atom,
					function,
					prefixFunctions,
					false)
				prefixFunctions.forEachIndexed { zeroIndex, prefixFunction ->
					prefixFunction.code().methodName =
						stringFrom("Macro prefix #${zeroIndex + 1} of $string")
				}
				function.code().methodName = stringFrom("Macro body of $string")
				succeed(nil)
			}
			catch (e: AvailException)
			{
				// MalformedMessageException
				// SignatureException
				// AmbiguousNameException
				fail(e.errorCode)
			}
		}
	}

	override fun privateBlockTypeRestriction(): A_Type =
		functionType(
			tuple(
				stringType,
				zeroOrMoreOf(mostGeneralFunctionType()),
				functionTypeReturning(PARSE_PHRASE.mostGeneralType())),
			TOP.o)

	override fun privateFailureVariableType(): A_Type =
		enumerationWith(set(
				E_LOADING_IS_OVER,
				E_CANNOT_DEFINE_DURING_COMPILATION,
				E_AMBIGUOUS_NAME,
				E_INCORRECT_NUMBER_OF_ARGUMENTS,
				E_REDEFINED_WITH_SAME_ARGUMENT_TYPES,
				E_MACRO_PREFIX_FUNCTION_ARGUMENT_MUST_BE_A_PARSE_NODE,
				E_MACRO_PREFIX_FUNCTIONS_MUST_RETURN_TOP,
				E_MACRO_ARGUMENT_MUST_BE_A_PARSE_NODE,
				E_MACRO_MUST_RETURN_A_PARSE_NODE,
				E_MACRO_PREFIX_FUNCTION_INDEX_OUT_OF_BOUNDS)
			.setUnionCanDestroy(possibleErrors, true))
}
