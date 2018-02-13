/*
 * L2_JUMP_IF_UNBOX_FLOAT.java
 * Copyright © 1993-2018, The Avail Foundation, LLC.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  Neither the name of the copyright holder nor the names of the contributors
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

package com.avail.interpreter.levelTwo.operation;

import com.avail.descriptor.A_BasicObject;
import com.avail.descriptor.A_Number;
import com.avail.descriptor.AvailObject;
import com.avail.interpreter.levelTwo.L2Instruction;
import com.avail.interpreter.levelTwo.L2OperandType;
import com.avail.interpreter.levelTwo.L2Operation;
import com.avail.interpreter.levelTwo.operand.L2PcOperand;
import com.avail.interpreter.levelTwo.register.L2FloatRegister;
import com.avail.interpreter.levelTwo.register.L2ObjectRegister;
import com.avail.optimizer.jvm.JVMTranslator;
import org.objectweb.asm.MethodVisitor;

import java.util.Set;

import static com.avail.interpreter.levelTwo.L2NamedOperandType.Purpose.FAILURE;
import static com.avail.interpreter.levelTwo.L2NamedOperandType.Purpose.SUCCESS;
import static com.avail.interpreter.levelTwo.L2OperandType.*;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Type.*;

/**
 * Jump to {@code "if unboxed"} if a {@code double} was unboxed from an {@link
 * AvailObject}, otherwise jump to {@code "if not unboxed"}.
 *
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
public class L2_JUMP_IF_UNBOX_FLOAT
extends L2Operation
{
	/**
	 * Construct an {@code L2_JUMP_IF_UNBOX_FLOAT}.
	 */
	private L2_JUMP_IF_UNBOX_FLOAT ()
	{
		super(
			READ_POINTER.is("source"),
			WRITE_FLOAT.is("destination"),
			PC.is("if unboxed", SUCCESS),
			PC.is("if not unboxed", FAILURE));
	}

	/**
	 * Initialize the sole instance.
	 */
	public static final L2_JUMP_IF_UNBOX_FLOAT instance =
		new L2_JUMP_IF_UNBOX_FLOAT();

	@Override
	public boolean hasSideEffect ()
	{
		return true;
	}

	@Override
	public void toString (
		final L2Instruction instruction,
		final Set<L2OperandType> desiredTypes,
		final StringBuilder builder)
	{
		assert this == instruction.operation;
		final L2ObjectRegister sourceReg =
			instruction.readObjectRegisterAt(0).register();
		final L2FloatRegister destinationReg =
			instruction.writeFloatRegisterAt(1).register();
//		final L2PcOperand ifUnboxed = instruction.pcAt(2);
//		final int ifNotUnboxed = instruction.pcOffsetAt(3);

		renderPreamble(instruction, builder);
		builder.append(' ');
		builder.append(destinationReg);
		builder.append(" ←? ");
		builder.append(sourceReg);
		renderOperandsStartingAt(instruction, 2, desiredTypes, builder);
	}

	@Override
	public void translateToJVM (
		final JVMTranslator translator,
		final MethodVisitor method,
		final L2Instruction instruction)
	{
		final L2ObjectRegister sourceReg =
			instruction.readObjectRegisterAt(0).register();
		final L2FloatRegister destinationReg =
			instruction.writeFloatRegisterAt(1).register();
		final L2PcOperand ifUnboxed = instruction.pcAt(2);
		final int ifNotUnboxed = instruction.pcOffsetAt(3);

		// :: if (!source.isDouble()) goto ifNotUnboxed;
		translator.load(method, sourceReg);
		method.visitMethodInsn(
			INVOKEINTERFACE,
			getInternalName(A_BasicObject.class),
			"isDouble",
			getMethodDescriptor(BOOLEAN_TYPE),
			true);
		method.visitJumpInsn(IFEQ, translator.labelFor(ifNotUnboxed));
		// :: else {
		// ::    destination = source.extractDouble();
		// ::    goto ifUnboxed;
		// :: }
		translator.load(method, sourceReg);
		method.visitMethodInsn(
			INVOKEINTERFACE,
			getInternalName(A_Number.class),
			"extractDouble",
			getMethodDescriptor(DOUBLE_TYPE),
			true);
		translator.store(method, destinationReg);
		translator.branch(method, instruction, ifUnboxed);
	}
}
