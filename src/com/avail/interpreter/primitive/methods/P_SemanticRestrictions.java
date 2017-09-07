/**
 * P_SemanticRestrictions.java
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
package com.avail.interpreter.primitive.methods;

import static com.avail.descriptor.TypeDescriptor.Types.*;
import static com.avail.interpreter.Primitive.Flag.*;
import static com.avail.exceptions.AvailErrorCode.*;
import java.util.*;
import com.avail.descriptor.*;
import com.avail.interpreter.*;

/**
 * <strong>Primitive:</strong> Answer a {@linkplain TupleDescriptor
 * tuple} of restriction {@linkplain FunctionDescriptor functions} that
 * would run for a call site for the specified {@linkplain
 * MethodDescriptor method} and tuple of argument types.
 */
public final class P_SemanticRestrictions
extends Primitive
{
	/**
	 * The sole instance of this primitive class.  Accessed through reflection.
	 */
	public static final Primitive instance =
		new P_SemanticRestrictions().init(2, CanInline);

	@Override
	public Result attempt (
		final List<AvailObject> args,
		final Interpreter interpreter,
		final boolean skipReturnCheck)
	{
		assert args.size() == 2;
		final A_Method method = args.get(0);
		final A_Tuple argTypes = args.get(1);

		if (method.numArgs() != argTypes.tupleSize())
		{
			return interpreter.primitiveFailure(
				E_INCORRECT_NUMBER_OF_ARGUMENTS);
		}
		final A_Set restrictions = method.semanticRestrictions();
		final List<A_Function> applicable =
			new ArrayList<>(restrictions.setSize());
		for (final A_SemanticRestriction restriction : restrictions)
		{
			final A_Function function = restriction.function();
			if (function.kind().acceptsTupleOfArguments(argTypes))
			{
				applicable.add(function);
			}
		}
		return interpreter.primitiveSuccess(
			TupleDescriptor.tupleFromList(applicable));
	}

	@Override
	protected A_Type privateBlockTypeRestriction ()
	{
		return FunctionTypeDescriptor.functionType(
			TupleDescriptor.tuple(
				METHOD.o(),
				TupleTypeDescriptor.zeroOrMoreOf(
					InstanceMetaDescriptor.anyMeta())),
			TupleTypeDescriptor.zeroOrMoreOf(
				FunctionTypeDescriptor.functionTypeReturning(
					InstanceMetaDescriptor.topMeta())));
	}


	@Override
	protected A_Type privateFailureVariableType ()
	{
		return AbstractEnumerationTypeDescriptor.enumerationWith(
			SetDescriptor.set(
				E_INCORRECT_NUMBER_OF_ARGUMENTS));
	}
}
