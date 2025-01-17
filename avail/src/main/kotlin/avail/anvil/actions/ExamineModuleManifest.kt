/*
 * ExamineModuleManifest.kt
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

package avail.anvil.actions

import avail.anvil.AvailWorkbench
import avail.anvil.streams.StreamStyle
import avail.descriptor.fiber.FiberDescriptor
import avail.persistence.cache.Repository.ModuleCompilation
import avail.persistence.cache.RepositoryDescriber
import avail.utility.Strings.buildUnicodeBox
import java.awt.event.ActionEvent
import javax.swing.Action
import javax.swing.JOptionPane

/**
 * A `ExamineModuleManifest` presents information about a specific
 * compilation of the selected module.
 *
 * @constructor
 * Construct a new `ExamineModuleManifest`.
 *
 * @param workbench
 *   The owning [AvailWorkbench].
 */
class ExamineModuleManifest constructor (
	workbench: AvailWorkbench
) : AbstractWorkbenchAction(workbench, "Examine module manifest")
{
	override fun updateIsEnabled(busy: Boolean)
	{
		isEnabled = !busy && workbench.selectedModule() !== null
	}

	override fun actionPerformed(event: ActionEvent)
	{
		workbench.clearTranscript()
		workbench.runtime.execute(FiberDescriptor.commandPriority)
		execute@{
			val moduleName = workbench.selectedModule()!!
			moduleName.repository.use { repository ->
				repository.reopenIfNecessary()
				val archive = repository.getArchive(moduleName.rootRelativeName)
				val compilations = archive.allKnownVersions.flatMap {
					it.value.allCompilations
				}
				val compilationsArray = compilations.toTypedArray()
				val selectedCompilation = JOptionPane.showInputDialog(
					workbench,
					"Select module manifest to examine",
					"Examine module manifest",
					JOptionPane.PLAIN_MESSAGE,
					null,
					compilationsArray,
					if (compilationsArray.isNotEmpty())
					{
						compilationsArray[0]
					}
					else
					{
						null
					})
				when (selectedCompilation)
				{
					is ModuleCompilation ->
					{
						val describer = RepositoryDescriber(repository)
						val description = describer.describeManifest(
							selectedCompilation.recordNumberOfManifest)
						val report = buildUnicodeBox(
							"Module Manifest Report"
						) {
							append(description)
						}
						workbench.writeText(report, StreamStyle.REPORT)
					}
					is Any -> assert(false) { "Unknown type selected" }
				}
			}
		}
	}

	init
	{
		putValue(
			Action.SHORT_DESCRIPTION,
			"Disassemble an existing module compilation's manifest")
	}
}
