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

import avail.anvil.AvailWorkbench
import avail.builder.ModuleRoot

/**
 * This is a tree node representing a [ModuleRoot].
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 *
 * @property moduleRoot
 *   The [ModuleRoot] that this represents.
 * @property visible
 *   `true` indicates this node is visible; `false` otherwise.
 *
 * @constructor
 *   Construct a new [ModuleRootNode].
 *
 * @param workbench
 *   The running [AvailWorkbench].
 * @param moduleRoot
 *   The [ModuleRoot] that this represents.
 */
class ModuleRootNode constructor(
	workbench: AvailWorkbench,
	private val isEditable: Boolean,
	val moduleRoot: ModuleRoot,
	val visible: Boolean
) : AbstractWorkbenchTreeNode(workbench)
{
	override fun modulePathString(): String = "/" + moduleRoot.name

	override fun iconResourceName(): String =
		when (isEditable)
		{
			true -> "root"
			else -> "root-locked"
		}

	override fun text(selected: Boolean) = buildString {
		append(moduleRoot.name)
		moduleRoot.resolver.uri.fragment?.let { f -> append(" [$f]") }
		if (!visible) append(" (invisible)")
	}

	override fun htmlStyle(selected: Boolean): String =
		fontStyle(bold = true, strikethrough = !visible) +
			colorStyle(selected, loaded = visible, false)
}
