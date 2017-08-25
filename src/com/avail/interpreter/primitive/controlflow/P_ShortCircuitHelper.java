/**
 * P_ShortCircuitHelper.java
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
package com.avail.interpreter.primitive.controlflow;

import static com.avail.descriptor.TypeDescriptor.Types.*;
import static com.avail.interpreter.Primitive.Flag.*;
import java.util.*;

import com.avail.descriptor.TypeDescriptor.Types;
import com.avail.interpreter.levelTwo.L2Chunk;
import com.avail.optimizer.ReifyStackThrowable;
import org.jetbrains.annotations.Nullable;
import com.avail.descriptor.*;
import com.avail.interpreter.*;
import com.avail.interpreter.levelTwo.register.L2ObjectRegister;
import com.avail.optimizer.L2Translator.L1NaiveTranslator;

/**
 * <strong>Primitive:</strong> Run the zero-argument {@linkplain
 * FunctionDescriptor function}, ignoring the leading {@linkplain Types#ANY any}
 * argument. This is used for short-circuit evaluation.
 */
public final class P_ShortCircuitHelper extends Primitive
{
	/**
	 * The sole instance of this primitive class.  Accessed through reflection.
	 */
	public static final Primitive instance =
		new P_ShortCircuitHelper().init(
			2, Invokes, CanInline, CannotFail);

	@Override
	public Result attempt (
		final List<AvailObject> args,
		final Interpreter interpreter,
		final boolean skipReturnCheck)
	{
		assert args.size() == 2;
//		final A_Atom ignoredBool = args.get(0);
		final A_Function function = args.get(1);

		final A_RawFunction code = function.code();
		assert code.numArgs() == 0;

		// Function takes no arguments.
		interpreter.argsBuffer.clear();
		interpreter.function = function;
		return Result.READY_TO_INVOKE;
	}

	@Override
	public A_Type returnTypeGuaranteedByVM (
		final List<? extends A_Type> argumentTypes)
	{
		final A_Type blockType = argumentTypes.get(1);
		return blockType.returnType();
	}

	/**
	 * Clear the arguments list (to correspond with the arguments being sent to
	 * the function in the second argument), then answer the register holding
	 * that function.
	 */
	@Override
	public @Nullable L2ObjectRegister foldOutInvoker (
		final List<L2ObjectRegister> args,
		final L1NaiveTranslator naiveTranslator)
	{
		assert hasFlag(Invokes);

		final L2ObjectRegister functionReg = args.get(1);
		args.clear();
		return functionReg;
	}

	@Override
	protected A_Type privateBlockTypeRestriction ()
	{
		return FunctionTypeDescriptor.create(
			TupleDescriptor.from(
				ANY.o(),
				FunctionTypeDescriptor.create(
					TupleDescriptor.empty(),
					TOP.o())),
			TOP.o());
	}
}
