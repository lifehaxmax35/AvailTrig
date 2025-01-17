/*
 * SettingsView.kt
 * Copyright © 1993-2023, The Avail Foundation, LLC.
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

package avail.anvil.settings

import avail.anvil.AvailWorkbench
import avail.anvil.environment.GlobalEnvironmentSettings
import avail.anvil.settings.editor.EditorSettingsSelection
import avail.anvil.versions.MavenCentralAPI
import avail.anvil.versions.SearchResponse
import java.awt.Color
import java.awt.Dialog.ModalityType.DOCUMENT_MODAL
import java.awt.Dimension
import java.awt.Font
import java.io.PrintWriter
import java.io.StringWriter
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.ScrollPaneConstants

/**
 * A [JFrame] that displays configurable global settings.
 *
 * @author Richard Arriaga
 *
 * @property globalSettings
 *   The environment's [GlobalEnvironmentSettings] window.
 *
 * @constructor
 * Construct a [SettingsView].
 *
 * @param globalSettings
 *   The environment's [GlobalEnvironmentSettings] window.
 * @param parent
 *   The parent [JFrame] that owns this [SettingsView].
 * @param latestVersion
 *   The latest Avail Standard Library version or an empty string if not known.
 */
class SettingsView constructor (
	internal val globalSettings: GlobalEnvironmentSettings,
	parent: JFrame,
	latestVersion: String = "",
	internal val onUpdate: (Set<SettingsCategory>) -> Unit = {}
): JDialog(parent, "Settings", DOCUMENT_MODAL)
{
	/**
	 * The latest Avail Standard Library version.
	 */
	internal var latestVersion: String = latestVersion
		private set

	/**
	 * The currently selected [SettingPanelSelection].
	 */
	var selected: SettingPanelSelection

	init
	{
		if (latestVersion.isEmpty())
		{
			MavenCentralAPI.searchAvailStdLib(
				{
					val rsp = SearchResponse.parse(it)
					if (rsp == null)
					{
						System.err.println(
							"Failed to refresh latest Avail Standard Library " +
								"version from Maven Central, couldn't parse " +
								"response:\n$it")
						return@searchAvailStdLib
					}
					this.latestVersion = rsp.latestLibVersion
				}
			) { c, m, e->
				StringWriter().apply {
					this.write(
						"Failed to refresh latest Avail Standard Library " +
							"version from Maven Central:\n\tResponse " +
							"Code:$c\n\tResponse Message$m\n")
					e?.let {
						val pw = PrintWriter(this)
						it.printStackTrace(pw)
					}
					System.err.println(this.toString())
				}
			}
		}
	}

	/**
	 * The top panel that has sorting options and can open a project.
	 */
	internal val rightPanel = JPanel().apply {
		minimumSize = Dimension(700, 800)
		preferredSize = Dimension(700, 800)
		maximumSize = Dimension(700, 800)
	}

	/**
	 * The top panel that has sorting options and can open a project.
	 */
	private val leftPanel = JPanel().apply {
		layout = BoxLayout(this, BoxLayout.Y_AXIS)
		minimumSize = Dimension(150, 750)
		preferredSize = Dimension(150, 750)
		maximumSize = Dimension(150, 750)
		border = BorderFactory.createLineBorder(Color(0x616365))
		val editor = EditorSettingsSelection(this@SettingsView).apply {
			label.font = label.font.deriveFont(font.style or Font.BOLD)
			background = Color(0x55, 0x58, 0x5A)
		}
		selected = editor
		add(editor)
		if (parent is AvailWorkbench)
		{
			add(GlobalSettingsSelection(this@SettingsView, parent))
		}
		add(ShortcutsSelection(this@SettingsView))
		add(StandardLibrariesSelection(this@SettingsView))
	}

	init
	{
		minimumSize = Dimension(850, 800)
		preferredSize = Dimension(850, 800)
		maximumSize = Dimension(850, 800)
		add(JPanel().apply {
			layout = BoxLayout(this, BoxLayout.X_AXIS)
			add(JScrollPane(leftPanel).apply {
				horizontalScrollBarPolicy =
					ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
				verticalScrollBarPolicy =
					ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
			})
			add(rightPanel)
		})
		selected.updateSettingsPane()
		setLocationRelativeTo(parent)
		isVisible = true
	}
}

