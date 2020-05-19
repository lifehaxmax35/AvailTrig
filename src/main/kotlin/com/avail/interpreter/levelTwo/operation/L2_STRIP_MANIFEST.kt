/*
 * L2_STRIP_MANIFEST.java
 * Copyright © 1993-2019, The Avail Foundation, LLC.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice, this
 *     list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice, this
 *     list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 *  * Neither the name of the copyright holder nor the names of the contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
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
package com.avail.interpreter.levelTwo.operation

import com.avail.interpreter.levelTwo.L2Instruction
import com.avail.interpreter.levelTwo.L2NamedOperandType
import com.avail.interpreter.levelTwo.L2OperandType
import com.avail.interpreter.levelTwo.L2Operation
import com.avail.interpreter.levelTwo.operand.L2ReadBoxedVectorOperand
import com.avail.interpreter.levelTwo.register.L2Register
import com.avail.optimizer.L2ValueManifest
import com.avail.optimizer.jvm.JVMTranslator
import com.avail.optimizer.values.L2SemanticValue
import org.objectweb.asm.MethodVisitor
import java.util.*

/**
 * This is a helper operation which produces no JVM code, but is useful to limit
 * which [L2Register]s and [L2SemanticValue]s are live when reaching
 * a back-edge.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 */
object L2_STRIP_MANIFEST : L2Operation(
	L2OperandType.READ_BOXED_VECTOR.`is`("live values"))
{
	// Prevent this instruction from being removed, because it constrains
	// the manifest along a back-edge, even after optimization.
	override fun hasSideEffect(): Boolean = true

	override fun instructionWasAdded(
		instruction: L2Instruction,
		manifest: L2ValueManifest)
	{
		val liveVector = instruction.operand<L2ReadBoxedVectorOperand>(0)

		// Clear the manifest, other than the mentioned semantic values and
		// registers.
		val liveSemanticValues: MutableSet<L2SemanticValue> = HashSet()
		val liveRegisters: MutableSet<L2Register> = HashSet()
		for (read in liveVector.elements())
		{
			liveSemanticValues.add(read.semanticValue())
			liveRegisters.add(read.register())
		}
		manifest.retainSemanticValues(liveSemanticValues)
		manifest.retainRegisters(liveRegisters)
		liveVector.instructionWasAdded(manifest)
	}

	override fun updateManifest(
		instruction: L2Instruction,
		manifest: L2ValueManifest,
		optionalPurpose: L2NamedOperandType.Purpose?)
	{
		assert(this == instruction.operation())
		assert(optionalPurpose == null)
		val liveVector = instruction.operand<L2ReadBoxedVectorOperand>(0)

		// Clear the manifest, other than the mentioned semantic values and
		// registers.
		val liveSemanticValues =
			liveVector.elements().mapTo(mutableSetOf()) { it.semanticValue() }
		val liveRegisters =
			liveVector.elements().mapTo(mutableSetOf()) { it.register() }

		manifest.retainSemanticValues(liveSemanticValues)
		manifest.retainRegisters(liveRegisters.toSet())
	}

	override fun translateToJVM(
		translator: JVMTranslator,
		method: MethodVisitor,
		instruction: L2Instruction)
	{
		// No effect.
	}
}