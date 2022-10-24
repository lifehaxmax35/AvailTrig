/*
 * ModuleRootNode.kt
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

package avail.anvil.nodes

import avail.builder.AvailBuilder
import avail.builder.ModuleRoot

/**
 * This is a tree node representing a [ModuleRoot].
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 *
 * @property moduleRoot
 *   The [ModuleRoot] that this represents.
 * @constructor
 *   Construct a new [ModuleRootNode].
 *
 * @param builder
 *   The builder for which this node is being built.
 * @param moduleRoot
 *   The [ModuleRoot] that this represents.
 */
class ModuleRootNode constructor(
	builder: AvailBuilder,
	private val isEditable: Boolean,
	val moduleRoot: ModuleRoot) : AbstractBuilderFrameTreeNode(builder)
{
	override fun modulePathString(): String = "/" + moduleRoot.name

	override fun iconResourceName(): String? = when (isEditable)
	{
		true -> null
		else -> "ReadOnlyModuleRoot"
	}

	override fun text(selected: Boolean): String = moduleRoot.name

	override fun htmlStyle(selected: Boolean): String =
		fontStyle(bold = true) + colorStyle(selected, true, false)
}
