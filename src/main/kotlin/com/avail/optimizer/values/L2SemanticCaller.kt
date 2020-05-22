/*
 * L2SemanticCaller.java
 * Copyright © 1993-2019, The Avail Foundation, LLC.
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
package com.avail.optimizer.values

/**
 * A semantic value which represents the fully reified caller of the current
 * [Frame].  When inlining, it can be equated with parent frame's
 * [L2SemanticLabel].
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 *
 * @constructor
 * Create a new `L2SemanticCaller` semantic value.
 *
 * @param frame
 *   The frame for which this represents the reified caller.
 */
internal class L2SemanticCaller constructor(frame: Frame)
	: L2FrameSpecificSemanticValue(frame, 0x5A9556AA)
{
	override fun equals(obj: Any?): Boolean =
		obj is L2SemanticCaller && super.equals(obj)

	override fun transform(
		semanticValueTransformer: (L2SemanticValue) -> L2SemanticValue,
		frameTransformer: (Frame) -> Frame): L2SemanticValue =
			frameTransformer.invoke(frame).let {
				if (it == frame) this else L2SemanticCaller(it)
			}

	override fun toString(): String =
		"ReifiedCaller${if (frame.depth() == 1) "" else "[of $frame]"}"
}