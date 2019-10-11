/*
 * P_GetContinuationOfOtherFiber.java
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

package com.avail.interpreter.primitive.fibers

import com.avail.descriptor.A_Fiber
import com.avail.descriptor.A_Type
import com.avail.interpreter.Interpreter
import com.avail.interpreter.Primitive
import com.avail.optimizer.jvm.ReferencedInGeneratedCode

import com.avail.descriptor.AbstractEnumerationTypeDescriptor.enumerationWith
import com.avail.descriptor.ContinuationTypeDescriptor.mostGeneralContinuationType
import com.avail.descriptor.FiberTypeDescriptor.mostGeneralFiberType
import com.avail.descriptor.FunctionTypeDescriptor.functionType
import com.avail.descriptor.ObjectTupleDescriptor.tuple
import com.avail.descriptor.SetDescriptor.set
import com.avail.exceptions.AvailErrorCode.E_FIBER_IS_TERMINATED
import com.avail.interpreter.Primitive.Flag.CanSuspend
import com.avail.interpreter.Primitive.Flag.Unknown

/**
 * **Primitive:** Ask another fiber what it's doing.  Fail if
 * the fiber's continuation chain is empty (i.e., it is terminated).
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 */
object P_GetContinuationOfOtherFiber : Primitive(1, CanSuspend, Unknown)
{

	override fun attempt(
		interpreter: Interpreter): Primitive.Result
	{
		interpreter.checkArgumentCount(1)
		val otherFiber = interpreter.argument(0)

		return interpreter.suspendAndDo { toSucceed, toFail ->
			otherFiber.whenContinuationIsAvailableDo { theContinuation ->
				if (!theContinuation.equalsNil())
				{
					toSucceed.value(theContinuation)
				}
				else
				{
					toFail.value(E_FIBER_IS_TERMINATED)
				}
			}
		}
	}

	override fun privateFailureVariableType(): A_Type
	{
		return enumerationWith(set(E_FIBER_IS_TERMINATED))
	}

	override fun privateBlockTypeRestriction(): A_Type
	{
		return functionType(
			tuple(
				mostGeneralFiberType()),
			mostGeneralContinuationType())
	}

}