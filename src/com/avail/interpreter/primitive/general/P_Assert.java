/**
 * P_Assert.java
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
package com.avail.interpreter.primitive.general;

import com.avail.descriptor.*;
import com.avail.descriptor.FiberDescriptor.ExecutionState;
import com.avail.exceptions.AvailAssertionFailedException;
import com.avail.interpreter.Interpreter;
import com.avail.interpreter.Primitive;

import java.util.List;

import static com.avail.descriptor.EnumerationTypeDescriptor.booleanType;
import static com.avail.descriptor.FunctionTypeDescriptor.functionType;
import static com.avail.descriptor.NilDescriptor.nil;
import static com.avail.descriptor.TupleDescriptor.tuple;
import static com.avail.descriptor.TupleTypeDescriptor.stringType;
import static com.avail.descriptor.TypeDescriptor.Types.TOP;
import static com.avail.interpreter.Primitive.Flag.CannotFail;
import static com.avail.interpreter.Primitive.Flag.Unknown;
import static com.avail.utility.Nulls.stripNull;
import static java.lang.String.format;

/**
 * <strong>Primitive:</strong> Assert the specified {@link
 * EnumerationTypeDescriptor#booleanType() predicate} or raise an
 * {@link AvailAssertionFailedException} (in Java) that contains the
 * provided {@linkplain TupleTypeDescriptor#stringType() message}.
 */
public final class P_Assert extends Primitive
{
	/**
	 * The sole instance of this primitive class.  Accessed through reflection.
	 */
	public static final Primitive instance =
		new P_Assert().init(
			2, Unknown, CannotFail);

	@Override
	public Result attempt (
		final List<AvailObject> args,
		final Interpreter interpreter,
		final boolean skipReturnCheck)
	{
		assert args.size() == 2;
		final A_Atom predicate = args.get(0);
		final A_String failureMessage = args.get(1);
		if (!predicate.extractBoolean())
		{
			final A_Fiber fiber = interpreter.fiber();
			final A_Continuation continuation = interpreter.reifiedContinuation;
			interpreter.primitiveSuspend(
				stripNull(interpreter.function).code());
			ContinuationDescriptor.dumpStackThen(
				interpreter.runtime(),
				fiber.textInterface(),
				continuation,
				stack ->
				{
					final StringBuilder builder = new StringBuilder();
					builder.append(failureMessage.asNativeString());
					for (final String frame : stack)
					{
						builder.append(format("%n\t-- %s", frame));
					}
					builder.append("\n\n");
					final AvailAssertionFailedException killer =
						new AvailAssertionFailedException(
							builder.toString());
					killer.fillInStackTrace();
					fiber.executionState(ExecutionState.ABORTED);
					fiber.failureContinuation().value(killer);
				});
			return Result.FIBER_SUSPENDED;
		}
		return interpreter.primitiveSuccess(nil());
	}

	@Override
	protected A_Type privateBlockTypeRestriction ()
	{
		return functionType(
			tuple(
				booleanType(),
				stringType()),
			TOP.o());
	}
}
