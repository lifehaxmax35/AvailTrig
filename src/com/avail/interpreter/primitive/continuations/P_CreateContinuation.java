/**
 * P_CreateContinuation.java
 * Copyright © 1993-2017, The Avail Foundation, LLC.
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
package com.avail.interpreter.primitive.continuations;

import com.avail.descriptor.A_Continuation;
import com.avail.descriptor.A_Function;
import com.avail.descriptor.A_Number;
import com.avail.descriptor.A_Tuple;
import com.avail.descriptor.A_Type;
import com.avail.descriptor.A_Variable;
import com.avail.descriptor.AvailObject;
import com.avail.descriptor.ContinuationDescriptor;
import com.avail.interpreter.Interpreter;
import com.avail.interpreter.Primitive;
import com.avail.interpreter.levelTwo.L2Chunk;

import javax.annotation.Nullable;
import java.util.List;

import static com.avail.descriptor.AbstractEnumerationTypeDescriptor
	.enumerationWith;
import static com.avail.descriptor.ContinuationDescriptor
	.createContinuationExceptFrame;
import static com.avail.descriptor.ContinuationTypeDescriptor
	.mostGeneralContinuationType;
import static com.avail.descriptor.FunctionTypeDescriptor.functionType;
import static com.avail.descriptor.FunctionTypeDescriptor
	.mostGeneralFunctionType;
import static com.avail.descriptor.IntegerRangeTypeDescriptor.naturalNumbers;
import static com.avail.descriptor.IntegerRangeTypeDescriptor.wholeNumbers;
import static com.avail.descriptor.SetDescriptor.set;
import static com.avail.descriptor.TupleDescriptor.tuple;
import static com.avail.descriptor.TupleTypeDescriptor.mostGeneralTupleType;
import static com.avail.descriptor.VariableTypeDescriptor.variableTypeFor;
import static com.avail.exceptions.AvailErrorCode
	.E_CANNOT_CREATE_CONTINUATION_FOR_INFALLIBLE_PRIMITIVE_FUNCTION;
import static com.avail.interpreter.Primitive.Flag.*;

/**
 * <strong>Primitive:</strong> Create a {@linkplain ContinuationDescriptor
 * continuation}. It will execute as unoptimized code via the {@linkplain
 * L2Chunk#unoptimizedChunk()}.  Fail if the provided function is an infallible
 * primitive.
 */
public final class P_CreateContinuation extends Primitive
{
	/**
	 * The sole instance of this primitive class.  Accessed through reflection.
	 */
	public static final Primitive instance =
		new P_CreateContinuation().init(
			5, CanFold, CanInline);

	@Override
	public Result attempt (
		final List<AvailObject> args,
		final Interpreter interpreter,
		final boolean skipReturnCheck)
	{
		assert args.size() == 5;
		final A_Function function = args.get(0);
		final A_Number pc = args.get(1);
		final A_Tuple stack = args.get(2);
		final A_Number stackp = args.get(3);
		final A_Variable callerHolder = args.get(4);

		final @Nullable Primitive primitive = function.code().primitive();
		if (primitive != null && primitive.hasFlag(CannotFail))
		{
			return interpreter.primitiveFailure(
				E_CANNOT_CREATE_CONTINUATION_FOR_INFALLIBLE_PRIMITIVE_FUNCTION);
		}
		final A_Continuation cont = createContinuationExceptFrame(
			function,
			callerHolder.value(),
			pc.extractInt(),
			stackp.extractInt(),
			false,
			L2Chunk.unoptimizedChunk(),
			L2Chunk.offsetToReturnIntoUnoptimizedChunk());
		for (int i = 1, end = stack.tupleSize(); i <= end; i++)
		{
			cont.argOrLocalOrStackAtPut(i, stack.tupleAt(i));
		}
		return interpreter.primitiveSuccess(cont);
	}

	@Override
	protected A_Type privateBlockTypeRestriction ()
	{
		return functionType(
			tuple(
				mostGeneralFunctionType(),
				wholeNumbers(),
				mostGeneralTupleType(),
				naturalNumbers(),
				variableTypeFor(
					mostGeneralContinuationType())),
			mostGeneralContinuationType());
	}

	@Override
	protected A_Type privateFailureVariableType ()
	{
		return enumerationWith(set(
			E_CANNOT_CREATE_CONTINUATION_FOR_INFALLIBLE_PRIMITIVE_FUNCTION));
	}
}
