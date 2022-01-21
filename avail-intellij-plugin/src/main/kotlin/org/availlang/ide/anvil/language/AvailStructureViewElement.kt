/*
 * AvailStructureViewElement.kt
 * Copyright © 1993-2021, The Avail Foundation, LLC.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
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

package org.availlang.ide.anvil.language

import avail.compiler.ModuleManifestEntry
import avail.compiler.ModuleManifestEntry.Kind.*
import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.NavigatablePsiElement
import org.availlang.ide.anvil.language.psi.AvailFile
import org.availlang.ide.anvil.language.psi.AvailPsiElement
import javax.swing.Icon

/**
 * A `AvailStructureViewElement` is used to display an Avail module's top level
 * statements from the [ModuleManifestEntry]s.
 *
 * @author Richard Arriaga &lt;rich@availlang.org&gt;
 *
 * @property myElement
 */
open class AvailStructureViewElement constructor(
	val myElement: NavigatablePsiElement
): StructureViewTreeElement, SortableTreeElement
{
	override fun getPresentation(): ItemPresentation = myElement.presentation
		?: PresentationData("NO PRESENTATION", null, null, null)

	override fun getChildren(): Array<TreeElement>
	{
		if (myElement is AvailFile)
		{
			val grouped: Array<TreeElement> = myElement.refreshAndGetManifest()
				.mapIndexed { i, it->
					AvailSingleModuleManifestItemPresentationTreeElement(
						it,
						AvailPsiElement(
							myElement,
							it,
							i,
							myElement.manager))
				}
				.groupBy { it.entry.summaryText }
				.map {
					if (it.value.size == 1) it.value[0]
					else AvailModuleManifestGroupItemPresentationTreeElement(
						it.key,
						it.value,
						myElement)
				}.toTypedArray()
			return grouped
		}
		return arrayOf()
	}

	override fun navigate(requestFocus: Boolean)
	{
		myElement.navigate(requestFocus)
	}

	override fun canNavigate(): Boolean =
		myElement.canNavigate()

	override fun canNavigateToSource(): Boolean =
		myElement.canNavigateToSource()

	override fun getValue(): Any = myElement

	override fun getAlphaSortKey(): String =
		myElement.name ?: ""

	override fun toString(): String =
		myElement.name ?: super.toString()
}

/**
 * [ItemPresentation] that represents a group.
 *
 * @property text
 *   The presentable text.
 */
class GroupItemPresentation constructor(
	val text: String
): ItemPresentation
{
	override fun getPresentableText(): String = text

	override fun getIcon(unused: Boolean): Icon = AllIcons.Actions.GroupBy
}

/**
 * An [ItemPresentation] that presents an [ModuleManifestEntry].
 *
 * @property entry
 *   The [ModuleManifestEntry] to present.
 */
abstract class AvailManifestItemPresentation constructor (
	val entry: ModuleManifestEntry
): ItemPresentation
{
	override fun getPresentableText(): String = entry.summaryText
//		entry.run { "$summaryText ($kind)" }

	override fun getLocationString(): String
	{
		return entry.topLevelStartingLine.toString()
	}
}

abstract class AvailModuleManifestItemPresentationTreeElement constructor(
	myElement: NavigatablePsiElement
): AvailStructureViewElement(myElement)


/**
 * A [TreeElement] that is a leaf that is used to display a
 * [ModuleManifestEntry].
 */
class AvailSingleModuleManifestItemPresentationTreeElement constructor(
	val entry: ModuleManifestEntry,
	myElement: NavigatablePsiElement
): AvailModuleManifestItemPresentationTreeElement(myElement)
{
	/**
	 * The [AvailManifestItemPresentation] of the [ModuleManifestEntry].
	 */
	val itemPresentation =
		when (entry.kind)
		{
			ATOM_DEFINITION_KIND -> AtomTreeElement(entry)
			METHOD_DEFINITION_KIND -> MethodTreeElement(entry)
			ABSTRACT_METHOD_DEFINITION_KIND -> AbstractMethodTreeElement(entry)
			FORWARD_METHOD_DEFINITION_KIND -> ForwardMethodTreeElement(entry)
			MACRO_DEFINITION_KIND -> MacroTreeElement(entry)
			SEMANTIC_RESTRICTION_KIND -> SemanticRestrictionTreeElement(entry)
			LEXER_KIND -> LexerTreeElement(entry)
			MODULE_CONSTANT_KIND -> ModuleConstantTreeElement(entry)
			MODULE_VARIABLE_KIND -> ModuleVariableTreeElement(entry)
		}

	override fun navigate(requestFocus: Boolean)
	{
		myElement.navigate(requestFocus)
	}

	override fun canNavigate(): Boolean = true

	override fun canNavigateToSource(): Boolean = true

	override fun getValue(): Any = entry

	override fun getPresentation(): ItemPresentation = itemPresentation

	override fun getChildren(): Array<TreeElement> = arrayOf()

	override fun toString(): String =
		"${entry.summaryText} (${entry.kind})"
}

class AvailModuleManifestGroupItemPresentationTreeElement constructor (
	summaryText: String,
	val entries: List<AvailSingleModuleManifestItemPresentationTreeElement>,
	myElement: NavigatablePsiElement
): AvailModuleManifestItemPresentationTreeElement(myElement)
{
	/**
	 * The [AvailManifestItemPresentation] of the [ModuleManifestEntry].
	 */
	val itemPresentation = GroupItemPresentation(summaryText)
	override fun canNavigate(): Boolean = true
	override fun canNavigateToSource(): Boolean = true
	override fun getValue(): Any = entries
	override fun getPresentation(): ItemPresentation = itemPresentation
	override fun getChildren(): Array<TreeElement> = entries.toTypedArray()
}

class AtomTreeElement constructor(
	entry: ModuleManifestEntry
): AvailManifestItemPresentation(entry)
{
	override fun getIcon(unused: Boolean): Icon = AvailIcons.atom
}

class AbstractMethodTreeElement constructor(
	entry: ModuleManifestEntry
): AvailManifestItemPresentation(entry)
{
	override fun getIcon(unused: Boolean): Icon = AvailIcons.abstractMethod
}

class MethodTreeElement constructor(
	entry: ModuleManifestEntry
): AvailManifestItemPresentation(entry)
{
	override fun getIcon(unused: Boolean): Icon = AvailIcons.method
}

class ForwardMethodTreeElement constructor(
	entry: ModuleManifestEntry
): AvailManifestItemPresentation(entry)
{
	override fun getIcon(unused: Boolean): Icon = AvailIcons.forwardMethod
}

class MacroTreeElement constructor(
	entry: ModuleManifestEntry
): AvailManifestItemPresentation(entry)
{
	override fun getIcon(unused: Boolean): Icon = AvailIcons.macro
}

class SemanticRestrictionTreeElement constructor(
	entry: ModuleManifestEntry
): AvailManifestItemPresentation(entry)
{
	override fun getIcon(unused: Boolean): Icon = AvailIcons.semanticRestriction
}

class LexerTreeElement constructor(
	entry: ModuleManifestEntry
): AvailManifestItemPresentation(entry)
{
	override fun getIcon(unused: Boolean): Icon = AvailIcons.lexer
}

class ModuleConstantTreeElement constructor(
	entry: ModuleManifestEntry
): AvailManifestItemPresentation(entry)
{
	override fun getIcon(unused: Boolean): Icon =AvailIcons.constant
}

class ModuleVariableTreeElement constructor(
	entry: ModuleManifestEntry
): AvailManifestItemPresentation(entry)
{
	override fun getIcon(unused: Boolean): Icon = AvailIcons.variable
}
