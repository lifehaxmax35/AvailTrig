/*
 * L2_SUBTRACT_CONSTANT_INT_MINUS_INT.java
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

import com.avail.interpreter.levelTwo.L2Instruction;
import com.avail.interpreter.levelTwo.L2NamedOperandType;
import com.avail.interpreter.levelTwo.L2OperandType;
import com.avail.interpreter.levelTwo.L2Operation;
import com.avail.interpreter.levelTwo.operand.L2Operand;
import com.avail.interpreter.levelTwo.operand.L2PcOperand;
import com.avail.interpreter.levelTwo.register.L2IntRegister;
import com.avail.optimizer.jvm.JVMTranslator;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.util.Set;

import static com.avail.interpreter.levelTwo.L2NamedOperandType.Purpose.FAILURE;
import static com.avail.interpreter.levelTwo.L2NamedOperandType.Purpose.SUCCESS;
import static com.avail.interpreter.levelTwo.L2OperandType.*;
import static com.avail.utility.Strings.increaseIndentation;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.INT_TYPE;

/**
 * Extract an int from the specified constant, and subtract an int register from
 * it, jumping to the target label if the result won't fit in an int.
 *
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
public class L2_SUBTRACT_INT_CONSTANT_MINUS_INT
extends L2Operation
{
	/**
	 * Initialize the sole instance.
	 */
	public static final L2Operation instance =
		new L2_SUBTRACT_INT_CONSTANT_MINUS_INT().init(
			INT_IMMEDIATE.is("minuend"),
			READ_INT.is("subtrahend"),
			WRITE_INT.is("difference"),
			PC.is("in range", SUCCESS),
			PC.is("out of range", FAILURE));

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
		renderPreamble(instruction, builder);
		final L2NamedOperandType[] types = operandTypes();
		final L2Operand[] operands = instruction.operands;
		builder.append(' ');
		builder.append(instruction.writeIntRegisterAt(2).register());
		builder.append(" ← ");
		builder.append(operands[0]);
		builder.append(" - ");
		builder.append(instruction.readIntRegisterAt(1).register());
		for (int i = 3, limit = operands.length; i < limit; i++)
		{
			final L2NamedOperandType type = types[i];
			if (desiredTypes.contains(type.operandType()))
			{
				final L2Operand operand = operands[i];
				builder.append("\n\t");
				assert operand.operandType() == type.operandType();
				builder.append(type.name());
				builder.append(" = ");
				builder.append(increaseIndentation(operand.toString(), 1));
			}
		}
	}

	@Override
	public void translateToJVM (
		final JVMTranslator translator,
		final MethodVisitor method,
		final L2Instruction instruction)
	{
		final int minuend = instruction.intImmediateAt(0);
		final L2IntRegister subtrahend =
			instruction.readIntRegisterAt(1).register();
		final L2IntRegister differenceReg =
			instruction.writeIntRegisterAt(2).register();
		final L2PcOperand inRange = instruction.pcAt(3);
		final int outOfRangeOffset = instruction.pcOffsetAt(4);

		// :: longDifference = (long) minuend - (long) subtrahend;
		translator.literal(method, minuend);
		method.visitInsn(I2L);
		translator.load(method, subtrahend);
		method.visitInsn(I2L);
		method.visitInsn(LSUB);
		method.visitInsn(DUP2);
		// :: intDifference = (int) longDifference;
		method.visitInsn(L2I);
		method.visitInsn(DUP);
		final int intDifferenceLocal = translator.nextLocal(INT_TYPE);
		final Label intDifferenceStart = new Label();
		method.visitLabel(intDifferenceStart);
		method.visitVarInsn(ISTORE, intDifferenceLocal);
		// :: if (longDifference != intDifference) goto outOfRange;
		method.visitInsn(I2L);
		method.visitInsn(LCMP);
		method.visitJumpInsn(IFNE, translator.labelFor(outOfRangeOffset));
		// :: else {
		// ::    sum = intDifference;
		// ::    goto inRange;
		// :: }
		method.visitVarInsn(ILOAD, intDifferenceLocal);
		final Label intDifferenceEnd = new Label();
		method.visitLabel(intDifferenceEnd);
		method.visitLocalVariable(
			"intDifference",
			INT_TYPE.getDescriptor(),
			null,
			intDifferenceStart,
			intDifferenceEnd,
			intDifferenceLocal);
		translator.store(method, differenceReg);
		translator.branch(method, instruction, inRange);
	}
}
