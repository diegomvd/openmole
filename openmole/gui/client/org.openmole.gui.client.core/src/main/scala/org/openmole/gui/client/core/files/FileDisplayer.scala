package org.openmole.gui.client.core.files

import org.openmole.gui.ext.data._

import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import autowire._
import org.openmole.gui.ext.api.Api
import org.openmole.gui.client.core._
import org.openmole.gui.client.tool.plot.Plotter

/*
 * Copyright (C) 07/05/15 // mathieu.leclaire@openmole.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

class FileDisplayer(treeNodeTabs: TreeNodeTabs, showExecution: () ⇒ Unit) {



  def display(safePath: SafePath, content: String, hash: String, fileExtension: FileExtension, pluginServices: PluginServices) = {
    treeNodeTabs.alreadyDisplayed(safePath) match {
      case Some(t: TreeNodeTab) ⇒ treeNodeTabs.tabsElement.setActive(t.tabID)
      case _ ⇒
        fileExtension match {
          case OpenMOLEScript ⇒
            val tab = TreeNodeTab.OMS(
              treeNodeTabs,
              safePath,
              content,
              hash,
              showExecution,
            //  TreeNodeTabs.setErrors(treeNodeTabs, safePath, _)
            )
            treeNodeTabs add tab
            tab.omsEditor.editor.focus
          case OpenMOLEResult ⇒
            Post()[Api].findAnalysisPlugin(safePath).call.foreach {
              case Some(plugin) ⇒
                val analysis = Plugins.buildJSObject[MethodAnalysisPlugin](plugin)
                val tab = TreeNodeTab.HTML(safePath, analysis.panel(safePath, pluginServices))
                treeNodeTabs add tab
              case None ⇒
            }
          case MDScript ⇒
            Post()[Api].mdToHtml(safePath).call().foreach { htmlString ⇒
              treeNodeTabs add TreeNodeTab.HTML(safePath, TreeNodeTab.mdBlock(htmlString))
            }
          case SVGExtension ⇒ treeNodeTabs add TreeNodeTab.HTML(safePath, TreeNodeTab.rawBlock(content))
          case editableFile: EditableFile ⇒
            if (DataUtils.isCSV(safePath))
              Post()[Api].sequence(safePath).call().foreach { seq ⇒
                val tab = TreeNodeTab.Editable(
                  treeNodeTabs,
                  safePath,
                  content, hash,
                  DataTab.build(seq, view = TreeNodeTab.Table, editing = !editableFile.onDemand),
                  Plotter.default)
                treeNodeTabs add tab
              }
            else {
              val tab = TreeNodeTab.Editable(
                treeNodeTabs,
                safePath,
                content,
                hash,
                DataTab.build(SequenceData(Seq(), Seq()), view = TreeNodeTab.Raw),
                Plotter.default)

              treeNodeTabs add tab
            }

          case _ ⇒ //FIXME for GUI workflows
        }
    }
  }

}
