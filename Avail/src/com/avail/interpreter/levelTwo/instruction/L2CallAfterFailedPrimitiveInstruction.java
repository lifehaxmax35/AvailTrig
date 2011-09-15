/**
 * interpreter/levelTwo/instruction/L2CallInstruction.java
 * Copyright (c) 2010, Mark van Gulik.
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

package com.avail.interpreter.levelTwo.instruction;

import static com.avail.interpreter.levelTwo.L2Operation.L2_doSendAfterFailedPrimitive_argumentsVector_;
import java.util.*;
import com.avail.annotations.NotNull;
import com.avail.descriptor.*;
import com.avail.interpreter.Primitive;
import com.avail.interpreter.levelTwo.*;
import com.avail.interpreter.levelTwo.register.*;

/**
 * {@code L2CallInstruction} attempts to execute a specific {@linkplain
 * MethodSignatureDescriptor method} by matching the actual {@linkplain
 * AvailObject arguments} against an {@linkplain ImplementationSetDescriptor
 * implementation set}.
 *
 * @author Mark van Gulik &lt;ghoul137@gmail.com&gt;
 * @author Todd L Smith &lt;anarakul@gmail.com&gt;
 */
public final class L2CallAfterFailedPrimitiveInstruction
extends L2CallInstruction
{
	/**
	 * The {@linkplain L2ObjectRegister register} holding failure information
	 * about a recently failed {@linkplain Primitive primitive} invocation
	 * attempt.
	 */
	private final @NotNull L2ObjectRegister failureObjectReg;

	/**
	 * Construct a new {@link L2CallAfterFailedPrimitiveInstruction}.
	 *
	 * @param implementationSet
	 *        The {@linkplain ImplementationSetDescriptor implementation set}
	 *        from which a {@linkplain MethodSignatureDescriptor method} should
	 *        be selected and called based on the exact {@linkplain #arguments
	 *        arguments}.
	 * @param arguments
	 *        The {@linkplain AvailObject arguments} of the {@linkplain
	 *        MethodSignatureDescriptor method} call.
	 * @param failureObjectReg
	 *        The {@linkplain L2ObjectRegister register} which holds the most
	 *        recent primitive failure value, specifically an attempt at the
	 *        current primitive {@linkplain CompiledCodeDescriptor compiled
	 *        code} object.
	 */
	public L2CallAfterFailedPrimitiveInstruction (
		final @NotNull AvailObject implementationSet,
		final @NotNull L2RegisterVector arguments,
		final @NotNull L2ObjectRegister failureObjectReg)
	{
		super(implementationSet, arguments);
		this.failureObjectReg = failureObjectReg;
	}

	@Override
	public @NotNull List<L2Register> sourceRegisters ()
	{
		List<L2Register> result = new ArrayList<L2Register>(
			super.sourceRegisters());
		result.add(failureObjectReg);
		return result;
	}

	@Override
	public void emitOn (final @NotNull L2CodeGenerator codeGenerator)
	{
		codeGenerator.emitL2Operation(
			L2_doSendAfterFailedPrimitive_argumentsVector_);
		codeGenerator.emitLiteral(implementationSet);
		codeGenerator.emitVector(arguments);
		codeGenerator.emitObjectRegister(failureObjectReg);
	}

	@Override
	public void propagateTypeInfoFor (final @NotNull L2Translator translator)
	{
		translator.restrictPropagationInformationToArchitecturalRegisters();
	}
}