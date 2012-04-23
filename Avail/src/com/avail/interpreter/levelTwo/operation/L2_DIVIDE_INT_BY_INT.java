/**
 * L2_DIVIDE_INT_BY_INT.java
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

import static com.avail.descriptor.AvailObject.error;
import static com.avail.interpreter.levelTwo.L2OperandType.*;
import com.avail.annotations.NotNull;
import com.avail.interpreter.levelTwo.*;

public class L2_DIVIDE_INT_BY_INT extends L2Operation
{
	/**
	 * Initialize the sole instance.
	 */
	public final static L2Operation instance = new L2_DIVIDE_INT_BY_INT();

	static
	{
		instance.init(
			READ_INT.is("dividend"),
			READ_INT.is("divisor"),
			WRITE_INT.is("quotient"),
			WRITE_INT.is("remainder"),
			PC.is("if out of range"),
			PC.is("if zero divisor"));
	}

	@Override
	public void step (final @NotNull L2Interpreter interpreter)
	{
		@SuppressWarnings("unused")
		final int divideIndex = interpreter.nextWord();
		@SuppressWarnings("unused")
		final int byIndex = interpreter.nextWord();
		@SuppressWarnings("unused")
		final int quotientIndex = interpreter.nextWord();
		@SuppressWarnings("unused")
		final int remainderIndex = interpreter.nextWord();
		@SuppressWarnings("unused")
		final int ifIndex = interpreter.nextWord();
		@SuppressWarnings("unused")
		final int zeroIndex = interpreter.nextWord();
		error("not implemented");
	}

	@Override
	public boolean hasSideEffect ()
	{
		// It jumps for division by zero.
		return true;
	}
}