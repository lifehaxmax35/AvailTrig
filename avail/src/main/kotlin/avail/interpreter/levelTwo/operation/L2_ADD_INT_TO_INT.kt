/*
 * L2_ADD_INT_TO_INT.kt
 * Copyright © 1993-2022, The Avail Foundation, LLC.
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
package avail.interpreter.levelTwo.operation

import avail.descriptor.types.IntegerRangeTypeDescriptor.Companion.int32
import avail.interpreter.levelTwo.L2Instruction
import avail.interpreter.levelTwo.L2NamedOperandType.Purpose.FAILURE
import avail.interpreter.levelTwo.L2NamedOperandType.Purpose.SUCCESS
import avail.interpreter.levelTwo.L2OperandType
import avail.interpreter.levelTwo.L2OperandType.PC
import avail.interpreter.levelTwo.L2OperandType.READ_INT
import avail.interpreter.levelTwo.L2OperandType.WRITE_INT
import avail.interpreter.levelTwo.operand.L2PcOperand
import avail.interpreter.levelTwo.operand.L2ReadIntOperand
import avail.interpreter.levelTwo.operand.L2WriteIntOperand
import avail.optimizer.L2ValueManifest
import avail.optimizer.jvm.JVMTranslator
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

/**
 * Add the value in one int register to another int register, jumping to the
 * specified target if the result does not fit in an int.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
object L2_ADD_INT_TO_INT : L2ControlFlowOperation(
	READ_INT.named("augend"),
	READ_INT.named("addend"),
	WRITE_INT.named("sum", SUCCESS),
	PC.named("out of range", FAILURE),
	PC.named("in range", SUCCESS))
{
	override fun instructionWasAdded(
		instruction: L2Instruction,
		manifest: L2ValueManifest)
	{
		assert(this == instruction.operation)
		//		final L2ReadIntOperand augendReg = instruction.operand(0);
//		final L2ReadIntOperand addendReg = instruction.operand(1);
		val sumReg = instruction.operand<L2WriteIntOperand>(2)
		//		final L2PcOperand outOfRange = instruction.operand(3);
		val inRange = instruction.operand<L2PcOperand>(4)
		super.instructionWasAdded(instruction, manifest)
		inRange.manifest().intersectType(
			sumReg.pickSemanticValue(), int32)
	}

	// It jumps if the result doesn't fit in an int.
	override val hasSideEffect get() = true

	override fun appendToWithWarnings(
		instruction: L2Instruction,
		desiredTypes: Set<L2OperandType>,
		builder: StringBuilder,
		warningStyleChange: (Boolean) -> Unit)
	{
		assert(this == instruction.operation)
		val augend = instruction.operand<L2ReadIntOperand>(0)
		val addend = instruction.operand<L2ReadIntOperand>(1)
		val sum = instruction.operand<L2WriteIntOperand>(2)
		//		final L2PcOperand outOfRange = instruction.operand(3);
//		final L2PcOperand inRange = instruction.operand(4);
		renderPreamble(instruction, builder)
		builder.append(' ')
		builder.append(sum.registerString())
		builder.append(" ← ")
		builder.append(augend.registerString())
		builder.append(" + ")
		builder.append(addend.registerString())
		renderOperandsStartingAt(instruction, 3, desiredTypes, builder)
	}

	override fun translateToJVM(
		translator: JVMTranslator,
		method: MethodVisitor,
		instruction: L2Instruction)
	{
		val augendReg = instruction.operand<L2ReadIntOperand>(0)
		val addendReg = instruction.operand<L2ReadIntOperand>(1)
		val sumReg = instruction.operand<L2WriteIntOperand>(2)
		val outOfRange = instruction.operand<L2PcOperand>(3)
		val inRange = instruction.operand<L2PcOperand>(4)

		// :: longSum = (long) augend + (long) addend;
		translator.load(method, augendReg.register())
		method.visitInsn(Opcodes.I2L)
		translator.load(method, addendReg.register())
		method.visitInsn(Opcodes.I2L)
		method.visitInsn(Opcodes.LADD)
		val longSumStart = Label()
		val longSumEnd = Label()
		val longSumLocal = translator.nextLocal(Type.LONG_TYPE)
		method.visitLocalVariable(
			"longSum",
			Type.LONG_TYPE.descriptor,
			null,
			longSumStart,
			longSumEnd,
			longSumLocal)
		method.visitVarInsn(Opcodes.LSTORE, longSumLocal)
		method.visitLabel(longSumStart)
		// :: if ((long) (int) longSum != longSum) goto outOfRange;
		method.visitVarInsn(Opcodes.LLOAD, longSumLocal)
		method.visitInsn(Opcodes.L2I)
		method.visitInsn(Opcodes.I2L)
		method.visitVarInsn(Opcodes.LLOAD, longSumLocal)
		method.visitInsn(Opcodes.LCMP)
		translator.jumpIf(method, Opcodes.IFNE, outOfRange)
		// :: else {
		// ::    sum = (int)longSum;
		// ::    goto inRange;
		// :: }
		method.visitVarInsn(Opcodes.LLOAD, longSumLocal)
		method.visitInsn(Opcodes.L2I)
		translator.store(method, sumReg.register())
		translator.jump(method, instruction, inRange)
		method.visitLabel(longSumEnd)
		translator.endLocal(longSumLocal, Type.LONG_TYPE)
	}
}
