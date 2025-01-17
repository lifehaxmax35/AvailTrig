/*
 * CreateProjectDialog.kt
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

package avail.anvil.manager

import avail.anvil.AvailWorkbench
import org.availlang.artifact.environment.project.AvailProject
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.WindowConstants

/**
 * A [JFrame] used to provide a dialog by which a user can create a new
 * [AvailProject] in a new [AvailWorkbench].
 *
 * @author Richard Arriaga
 *
 * @property manager
 *   The running [AvailProjectManager].
 *
 * @constructor
 * Construct a new [CreateProjectDialog].
 * @param manager
 *   The running [AvailProjectManager].
 * @param workbench
 *   The workbench that launched this [CreateProjectDialog].
 */
internal class CreateProjectDialog constructor(
	val manager: AvailProjectManager,
	workbench: AvailWorkbench
): JFrame("Create Project")
{
	/**
	 * Close this [CreateProjectDialog].
	 */
	fun close ()
	{
		manager.createProjectDialog = null
		dispatchEvent(WindowEvent(this, WindowEvent.WINDOW_CLOSING))
	}

	init
	{
		manager.createProjectDialog = this
		minimumSize = Dimension(750, 350)
		preferredSize = Dimension(750, 350)
		maximumSize = Dimension(750, 350)
		defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
		addWindowListener(object: WindowAdapter()
		{
			override fun windowClosing(e: WindowEvent?)
			{
				manager.openKnownProjectDialog = null
			}
		})
		add(CreateProjectPanel(
			manager.globalSettings,
			{ project, path ->
				AvailWorkbench.launchWorkbenchWithProject(
					project,
					manager.globalSettings,
					path,
					projectManager = manager)
				SwingUtilities.invokeLater {
					close()
				}
			})
		{
			close()
		})
		setLocationRelativeTo(workbench)
		isVisible = true
	}
}
