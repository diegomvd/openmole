/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.ide.plugin.task.netlogo

import javax.swing.filechooser.FileNameExtensionFilter
import org.openmole.ide.core.model.data.ITaskDataUI
import org.openmole.ide.core.model.panel.ITaskPanelUI
import org.openmole.ide.misc.widget.ChooseFileTextField
import java.awt.Dimension
import org.openmole.ide.misc.widget.MigPanel
import scala.swing._
import swing.Swing._

class NetLogoTaskPanelUI(ndu: NetLogoTaskDataUI) extends MigPanel("","[left]rel[grow,fill]","") with ITaskPanelUI{
 
  val nlogoTextField = new ChooseFileTextField(ndu.nlogoPath,"Select a nlogo file","Netlogo files","nlogo")
  val workspaceTextField = new ChooseFileTextField(ndu.workspacePath)
  val launchingCommandTextArea = new TextArea(ndu.lauchingCommands) 
  
  contents+= new Label("Nlogo file")
  contents+= (nlogoTextField,"growx,wrap")
  contents+= new Label("Workspace directory")
  contents+= (workspaceTextField,"span,growx,wrap")
  contents+= (new Label("Commands"),"wrap")
  contents+= (new ScrollPane(launchingCommandTextArea){minimumSize = new Dimension(150,100)},"span,growx")
  
  
  override def saveContent(name: String): ITaskDataUI = new NetLogoTaskDataUI(name, workspaceTextField.text, nlogoTextField.text, launchingCommandTextArea.text)
}