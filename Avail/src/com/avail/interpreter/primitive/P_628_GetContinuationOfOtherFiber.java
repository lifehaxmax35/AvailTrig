/**
 * P_628_GetContinuationOfOtherFiber.java
 * Copyright © 1993-2012, Mark van Gulik and Todd L Smith.
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

package com.avail.interpreter.primitive;

import static com.avail.exceptions.AvailErrorCode.*;
import static com.avail.interpreter.Primitive.Flag.*;
import java.util.ArrayList;
import java.util.List;
import com.avail.AvailRuntime;
import com.avail.annotations.Nullable;
import com.avail.descriptor.*;
import com.avail.interpreter.*;
import com.avail.utility.Continuation0;
import com.avail.utility.Continuation1;

/**
 * <strong>Primitive 628</strong>: Ask another fiber what it's doing.  Fail if
 * the fiber's continuation chain is empty (i.e., it is terminated).
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 */
public final class P_628_GetContinuationOfOtherFiber
extends Primitive
{
	/**
	 * The sole instance of this primitive class. Accessed through reflection.
	 */
	public final static Primitive instance =
		new P_628_GetContinuationOfOtherFiber().init(
			1, CannotFail, CanFold, CanInline);

	@Override
	public Result attempt (
		final List<AvailObject> args,
		final Interpreter interpreter,
		final boolean skipReturnCheck)
	{
		assert args.size() == 1;
		final A_Fiber otherFiber = args.get(0);

		final A_Fiber thisFiber = interpreter.fiber();
		final Result suspended = interpreter.primitiveSuspend();
		final A_Function failureFunction =
			interpreter.primitiveFunctionBeingAttempted();
		final List<AvailObject> copiedArgs = new ArrayList<>(args);
		assert failureFunction.code().primitiveNumber() == primitiveNumber;
		interpreter.postExitContinuation(new Continuation0()
		{
			@Override
			public void value ()
			{
				otherFiber.whenContinuationIsAvailableDo(
					new Continuation1<A_Continuation>()
					{
						@Override
						public final void value (
							final @Nullable A_Continuation theContinuation)
						{
							assert theContinuation != null;
							if (!theContinuation.equalsNil())
							{
								Interpreter.resumeFromSuccessfulPrimitive(
									AvailRuntime.current(),
									thisFiber,
									theContinuation,
									skipReturnCheck);
							}
							else
							{
								Interpreter.resumeFromFailedPrimitive(
									AvailRuntime.current(),
									thisFiber,
									E_FIBER_IS_TERMINATED.numericCode(),
									failureFunction,
									copiedArgs,
									skipReturnCheck);
							}
						}
					});
			}
		});
		return suspended;
	}

	@Override
	protected A_Type privateFailureVariableType ()
	{
		return AbstractEnumerationTypeDescriptor.withInstances(
			TupleDescriptor.from(
				E_FIBER_IS_TERMINATED.numericCode()
			).asSet());
	}

	@Override
	protected A_Type privateBlockTypeRestriction ()
	{
		return FunctionTypeDescriptor.create(
			TupleDescriptor.from(
				FiberTypeDescriptor.mostGeneralType()),
			ContinuationTypeDescriptor.mostGeneralType());
	}
}