/**
 * interpreter/levelTwo/instruction/L2SetInstruction.java
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

import static com.avail.descriptor.TypeDescriptor.Types.CONTAINER;
import static com.avail.interpreter.levelTwo.L2Operation.L2_doSetVariable_sourceObject_;
import java.util.*;
import com.avail.annotations.NotNull;
import com.avail.compiler.AbstractAvailCompiler;
import com.avail.compiler.node.AssignmentNodeDescriptor;
import com.avail.descriptor.*;
import com.avail.interpreter.levelTwo.*;
import com.avail.interpreter.levelTwo.register.*;

/**
 * {@code L2SetInstruction} stores an {@linkplain AvailObject object} into a
 * {@linkplain ContainerDescriptor container}.
 *
 * @author Mark van Gulik &lt;ghoul137@gmail.com&gt;
 * @author Todd L Smith &lt;anarakul@gmail.com&gt;
 */
public final class L2SetInstruction
extends L2Instruction
{
	/**
	 * The source {@linkplain L2ObjectRegister register} holding the {@linkplain
	 * ContainerDescriptor container} into which the {@linkplain AvailObject
	 * object} will be written.
	 */
	private final @NotNull L2ObjectRegister container;

	/**
	 * The source {@linkplain L2ObjectRegister register} containing the
	 * {@linkplain AvailObject object} that should be written into the
	 * {@linkplain ContainerDescriptor container}.
	 */
	private final @NotNull L2ObjectRegister value;

	/**
	 * Construct a new {@link L2SetInstruction}.
	 *
	 * @param container
	 *        The source {@linkplain L2ObjectRegister register} holding the
	 *        {@linkplain ContainerDescriptor container} into which the
	 *        {@linkplain AvailObject object} will be written.
	 * @param value
	 *        The source {@linkplain L2ObjectRegister register} containing the
	 *        {@linkplain AvailObject object} that should be written into the
	 *        {@linkplain ContainerDescriptor container}.
	 */
	public L2SetInstruction (
		final @NotNull L2ObjectRegister container,
		final @NotNull L2ObjectRegister value)
	{
		this.container = container;
		this.value = value;
	}

	@Override
	public List<L2Register> sourceRegisters ()
	{
		final List<L2Register> result = new ArrayList<L2Register>(2);
		result.add(container);
		result.add(value);
		return result;
	}

	@Override
	public @NotNull List<L2Register> destinationRegisters ()
	{
		return Collections.emptyList();
	}

	@Override
	public void emitOn (final @NotNull L2CodeGenerator codeGenerator)
	{
		codeGenerator.emitL2Operation(
			L2_doSetVariable_sourceObject_);
		codeGenerator.emitObjectRegister(container);
		codeGenerator.emitObjectRegister(value);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>This is kind of strange.  Because of the way outers can lose all type
	 * information, we use the fact that the {@linkplain AbstractAvailCompiler
	 * compiler} set up an {@linkplain AssignmentNodeDescriptor assignment} to a
	 * variable to indicate that the variable really is a {@linkplain
	 * ContainerDescriptor variable}.</p>
	 */
	@Override
	public void propagateTypeInfoFor (final @NotNull L2Translator translator)
	{
		//  Propagate type information due to this instruction.
		//
		//  This is kind of strange.  Because of the way outer variables can lose all type information,
		//  we use the fact that the compiler set up an assignment to a variable to indicate that the
		//  variable really is a variable.

		AvailObject varType;
		if (translator.registerHasTypeAt(container))
		{
			varType = translator.registerTypeAt(container);
			varType = varType.typeIntersection(CONTAINER.o());
		}
		else
		{
			varType = CONTAINER.o();
		}
		translator.registerTypeAtPut(container, varType);
	}
}
