/*
 * L2_JUMP_IF_OBJECTS_EQUAL.java
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

package com.avail.interpreter.levelTwo.operation;

import com.avail.descriptor.A_BasicObject;
import com.avail.descriptor.A_Type;
import com.avail.interpreter.levelTwo.L2Instruction;
import com.avail.interpreter.levelTwo.L2OperandType;
import com.avail.interpreter.levelTwo.operand.L2PcOperand;
import com.avail.interpreter.levelTwo.operand.L2ReadPointerOperand;
import com.avail.interpreter.levelTwo.register.L2ObjectRegister;
import com.avail.optimizer.L1Translator;
import com.avail.optimizer.L2Generator;
import com.avail.optimizer.RegisterSet;
import com.avail.optimizer.jvm.JVMTranslator;
import org.objectweb.asm.MethodVisitor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

import static com.avail.interpreter.levelTwo.L2NamedOperandType.Purpose.FAILURE;
import static com.avail.interpreter.levelTwo.L2NamedOperandType.Purpose.SUCCESS;
import static com.avail.interpreter.levelTwo.L2OperandType.PC;
import static com.avail.interpreter.levelTwo.L2OperandType.READ_POINTER;
import static com.avail.interpreter.levelTwo.operation.L2ConditionalJump.BranchReduction.*;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Type.*;

/**
 * Branch based on whether the two values are equal to each other.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
public final class L2_JUMP_IF_OBJECTS_EQUAL
extends L2ConditionalJump
{
	/**
	 * Construct an {@code L2_JUMP_IF_OBJECTS_EQUAL}.
	 */
	private L2_JUMP_IF_OBJECTS_EQUAL ()
	{
		super(
			READ_POINTER.is("first value"),
			READ_POINTER.is("second value"),
			PC.is("is equal", SUCCESS),
			PC.is("is not equal", FAILURE));
	}

	/**
	 * Initialize the sole instance.
	 */
	public static final L2_JUMP_IF_OBJECTS_EQUAL instance =
		new L2_JUMP_IF_OBJECTS_EQUAL();

	@Override
	public BranchReduction branchReduction (
		final L2Instruction instruction,
		final RegisterSet registerSet,
		final L2Generator generator)
	{
		final L2ReadPointerOperand firstReg =
			instruction.readObjectRegisterAt(0);
		final L2ReadPointerOperand secondReg =
			instruction.readObjectRegisterAt(1);
//		final L2PcOperand ifEqual = instruction.pcAt(2);
//		final L2PcOperand notEqual = instruction.pcAt(3);

		final @Nullable A_BasicObject constant1 = firstReg.constantOrNull();
		final @Nullable A_BasicObject constant2 = secondReg.constantOrNull();
		if (constant1 != null && constant2 != null)
		{
			// Compare them right now.
			return constant1.equals(constant2) ? AlwaysTaken : NeverTaken;
		}
		if (firstReg.type().typeIntersection(secondReg.type()).isBottom())
		{
			// They can't be equal.
			return NeverTaken;
		}
		// Otherwise it's still contingent.
		return SometimesTaken;
	}

	@Override
	protected void propagateTypes (
		final L2Instruction instruction,
		final List<RegisterSet> registerSets,
		final L2Generator generator)
	{
		final L2ReadPointerOperand firstReg =
			instruction.readObjectRegisterAt(0);
		final L2ReadPointerOperand secondReg =
			instruction.readObjectRegisterAt(1);
//		final L2PcOperand ifEqual = instruction.pcAt(2);
//		final L2PcOperand notEqual = instruction.pcAt(3);

		assert registerSets.size() == 2;
//		final RegisterSet fallThroughSet = registerSets.get(0);
		final RegisterSet postJumpSet = registerSets.get(1);

		// In the path where the registers compared equal, we can deduce that
		// both registers' origin registers must be constrained to the type
		// intersection of the two registers.
		final A_Type intersection =
			postJumpSet.typeAt(firstReg.register()).typeIntersection(
				postJumpSet.typeAt(secondReg.register()));
		postJumpSet.strengthenTestedTypeAtPut(
			firstReg.register(), intersection);
		postJumpSet.strengthenTestedTypeAtPut(
			secondReg.register(), intersection);

		// Furthermore, if one register is a constant, then in the path where
		// the registers compared equal we can deduce that both registers'
		// origin registers hold that same constant.
		if (postJumpSet.hasConstantAt(firstReg.register()))
		{
			postJumpSet.strengthenTestedValueAtPut(
				secondReg.register(),
				postJumpSet.constantAt(firstReg.register()));
		}
		else if (postJumpSet.hasConstantAt(secondReg.register()))
		{
			postJumpSet.strengthenTestedValueAtPut(
				firstReg.register(),
				postJumpSet.constantAt(secondReg.register()));
		}
	}

	@Override
	public void toString (
		final L2Instruction instruction,
		final Set<L2OperandType> desiredTypes,
		final StringBuilder builder)
	{
		assert this == instruction.operation();
		final L2ObjectRegister firstReg =
			instruction.readObjectRegisterAt(0).register();
		final L2ObjectRegister secondReg =
			instruction.readObjectRegisterAt(1).register();
//		final L2PcOperand ifEqual = instruction.pcAt(2);
//		final L2PcOperand notEqual = instruction.pcAt(3);

		renderPreamble(instruction, builder);
		builder.append(' ');
		builder.append(firstReg);
		builder.append(" = ");
		builder.append(secondReg);
		renderOperandsStartingAt(instruction, 2, desiredTypes, builder);
	}

	@Override
	public void translateToJVM (
		final JVMTranslator translator,
		final MethodVisitor method,
		final L2Instruction instruction)
	{
		final L2ObjectRegister firstReg =
			instruction.readObjectRegisterAt(0).register();
		final L2ObjectRegister secondReg =
			instruction.readObjectRegisterAt(1).register();
		final L2PcOperand ifEqual = instruction.pcAt(2);
		final L2PcOperand notEqual = instruction.pcAt(3);

		// :: if (first.equals(second)) goto ifEqual;
		// :: else goto notEqual;
		translator.load(method, firstReg);
		translator.load(method, secondReg);
		method.visitMethodInsn(
			INVOKEINTERFACE,
			getInternalName(A_BasicObject.class),
			"equals",
			getMethodDescriptor(BOOLEAN_TYPE, getType(A_BasicObject.class)),
			true);
		emitBranch(translator, method, instruction, IFNE, ifEqual, notEqual);
	}
}
