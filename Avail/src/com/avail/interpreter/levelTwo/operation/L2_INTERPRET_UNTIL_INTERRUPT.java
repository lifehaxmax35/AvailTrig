/**
 * L2_INTERPRET_UNTIL_INTERRUPT.java
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
package com.avail.interpreter.levelTwo.operation;

import static com.avail.interpreter.levelTwo.L2Interpreter.*;
import static com.avail.interpreter.levelTwo.L2OperandType.PC;
import static com.avail.interpreter.levelTwo.register.FixedRegister.*;
import java.util.logging.Level;
import com.avail.descriptor.AvailObject;
import com.avail.interpreter.Interpreter;
import com.avail.interpreter.levelOne.L1Operation;
import com.avail.interpreter.levelTwo.*;
import com.avail.interpreter.levelTwo.register.FixedRegister;
import com.avail.optimizer.RegisterSet;

/**
 * Execute a single nybblecode of the current continuation, found in {@link
 * FixedRegister#CALLER caller register}.  If no interrupt is indicated,
 * move the L2 {@link L2Interpreter#offset()} back to the same instruction
 * (which always occupies a single word, so the address is implicit).
 */
public class L2_INTERPRET_UNTIL_INTERRUPT extends L2Operation
{
	/**
	 * Initialize the sole instance.
	 */
	public final static L2Operation instance =
		new L2_INTERPRET_UNTIL_INTERRUPT();

	static
	{
		instance.init(
			PC.is("return here after a call"));
	}

	@Override
	public void step (final L2Interpreter interpreter)
	{
		final AvailObject function = interpreter.pointerAt(FUNCTION);
		final AvailObject code = function.code();
		final AvailObject nybbles = code.nybbles();
		final int pc = interpreter.integerAt(pcRegister());

		if (!interpreter.isInterruptRequested())
		{
			// Branch back to this (operandless) instruction by default.
			interpreter.offset(interpreter.offset() - 1);
		}

		int depth = 0;
		if (debugL1)
		{
			for (
				AvailObject c = interpreter.pointerAt(CALLER);
				!c.equalsNull();
				c = c.caller())
			{
				depth++;
			}
		}

		// Before we extract the nybblecode, make sure that the PC hasn't
		// passed the end of the instruction sequence. If we have, then
		// execute an L1Implied_doReturn.
		if (pc > nybbles.tupleSize())
		{
			assert pc == nybbles.tupleSize() + 1;
			if (Interpreter.logger.isLoggable(Level.FINEST))
			{
				Interpreter.logger.finest(String.format(
					"simulating %s (pc = %d)",
					L1Operation.L1Implied_Return,
					pc));
			}
			if (debugL1)
			{
				System.out.printf("%n%d  Step L1: return", depth);
			}
			interpreter.levelOneStepper.L1Implied_doReturn();
			return;
		}
		final int nybble = nybbles.extractNybbleFromTupleAt(pc);
		interpreter.integerAtPut(pcRegister(), (pc + 1));

		final L1Operation operation = L1Operation.values()[nybble];
		if (Interpreter.logger.isLoggable(Level.FINEST))
		{
			Interpreter.logger.finest(String.format(
				"simulating %s (pc = %d)",
				operation,
				pc));
		}
		if (debugL1)
		{
			System.out.printf("%n%d  Step L1: %s", depth, operation);
		}
		operation.dispatch(interpreter.levelOneStepper);
	}

	@Override
	public void propagateTypesInFor (
		final L2Instruction instruction,
		final RegisterSet registers)
	{
		// No real optimization should ever be done near this wordcode.
		// Do nothing.
	}

	@Override
	public boolean hasSideEffect ()
	{
		// Keep this instruction from being removed, since it's only used
		// by the default chunk.
		return true;
	}
}