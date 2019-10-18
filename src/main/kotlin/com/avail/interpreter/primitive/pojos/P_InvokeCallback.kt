/*
 * P_InvokeCallback.kt
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
package com.avail.interpreter.primitive.pojos

import com.avail.CallbackSystem
import com.avail.CallbackSystem.Callback
import com.avail.descriptor.A_Type
import com.avail.descriptor.BottomTypeDescriptor.bottom
import com.avail.descriptor.ObjectTupleDescriptor.tupleFromList
import com.avail.descriptor.PojoDescriptor
import com.avail.descriptor.PojoDescriptor.newPojo
import com.avail.descriptor.PojoTypeDescriptor.pojoTypeForClass
import com.avail.descriptor.RawPojoDescriptor.identityPojo
import com.avail.interpreter.Interpreter
import com.avail.interpreter.Primitive
import com.avail.interpreter.Primitive.Flag.CanSuspend
import com.avail.interpreter.Primitive.Flag.Private
import com.avail.interpreter.levelOne.L1InstructionWriter
import com.avail.utility.Nulls.stripNull
import java.util.*

/**
 * **Primitive:** Given zero or more arguments, invoke the [ ] that's in a [PojoDescriptor] stored in the sole outer
 * variable.
 *
 *
 * If a Java [Throwable] is thrown while executing the [ ], or if the specific callback indicates failure of some other form,
 * invoke TODO the handler for Java exceptions in callbacks.  Otherwise, answer
 * the result of successfully executing the callback.  The callback body runs in
 * the [CallbackSystem]'s thread pool.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 */
object P_InvokeCallback : Primitive(-1, Private, CanSuspend)
{

	override fun attempt(interpreter: Interpreter): Result
	{
		val loader = interpreter.availLoaderOrNull()
		loader?.statementCanBeSummarized(false)
		val runtime = interpreter.runtime()
		val primitiveFunction = stripNull(interpreter.function)
		assert(primitiveFunction.code().primitive() === this)
		val callbackPojo = primitiveFunction.outerVarAt(1)
		val copiedArgs = ArrayList(interpreter.argsBuffer)
		val argumentsTuple = tupleFromList(copiedArgs).makeShared()
		return interpreter.suspendAndDoWithFailureObject { toSucceed, toFail ->
			runtime.callbackSystem().executeCallbackTask(
				callbackPojo.javaObjectNotNull<Callback>(),
				argumentsTuple,
				CallbackSystem.CallbackCompletion {
					result -> toSucceed.value(result.makeShared()) },
				CallbackSystem.CallbackFailure { throwable ->
					toFail.value(
						newPojo(
							identityPojo(throwable),
							pojoTypeForClass(throwable.javaClass)
						).makeShared())
				})
		}
	}

	override fun privateBlockTypeRestriction(): A_Type
	{
		// This primitive is suitable for any block signature.
		return bottom()
	}

	override fun privateFailureVariableType(): A_Type
	{
		return pojoTypeForClass(Throwable::class.java)
	}

	override fun writeDefaultFailureCode(
		lineNumber: Int,
		writer: L1InstructionWriter,
		numArgs: Int)
	{
		// Raw functions using this primitive should not be constructed through
		// this default mechanism.  See CallbackSystem for details.
		throw UnsupportedOperationException(
			this.javaClass.simpleName
			+ " must not create a function through the bootstrap "
			+ "mechanism")
	}

}