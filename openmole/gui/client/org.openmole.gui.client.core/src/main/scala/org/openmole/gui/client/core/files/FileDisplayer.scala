package org.openmole.gui.client.core.files

import org.openmole.gui.shared.data.*

import scala.concurrent.ExecutionContext.Implicits.global
import org.openmole.gui.client.core.*
import org.openmole.gui.client.core.files.TabContent.TabData
import org.openmole.gui.client.tool.plot.Plotter
import org.openmole.gui.shared.api.*
import scaladget.bootstrapnative.bsn

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

class FileDisplayer:

  def display(safePath: SafePath, content: String, hash: String, fileExtension: FileExtension)(using panels: Panels, api: ServerAPI, path: BasePath, plugins: GUIPlugins) = {
    panels.tabContent.alreadyDisplayed(safePath) match {
      case Some(tabID: bsn.TabID) ⇒ panels.tabContent.tabsUI.setActive(tabID)
      case _ ⇒
        FileContentType(fileExtension) match {
          case FileContentType.OpenMOLEScript ⇒ OMSContent.addTab(safePath, content, hash)
          case FileContentType.CSV => CSVContent.addTab(safePath, content, hash)
          case FileContentType.MDScript ⇒
            api.mdToHtml(safePath).foreach { htmlString ⇒
              val htmlDiv = com.raquo.laminar.api.L.div()
              htmlDiv.ref.innerHTML = htmlString
              HTMLContent.addTab(safePath, htmlDiv)
            }
          case e if FileContentType.isText(e) => AnyTextContent.addTab(safePath, content, hash)
          case FileContentType.OpenMOLEResult ⇒
            api.omrMethod(safePath).foreach { method =>
              plugins.analysisPlugins.get(method) match
                case Some(analysis) ⇒
                //  val tab = TreeNodeTab.HTML(safePath, analysis.panel(safePath, pluginServices))
                // treeNodeTabs add tab
                case None ⇒
            }
          case FileContentType.SVGExtension ⇒ HTMLContent.addTab(safePath, TreeNodeTab.rawBlock(content))

          //          case editableFile: EditableFile ⇒
          //            if (DataUtils.isCSV(safePath))
          //              Post()[Api].sequence(safePath).call().foreach { seq ⇒
          //                val tab = TreeNodeTab.Editable(
          //                  safePath,
          //                  DataTab.build(seq, view = TreeNodeTab.Table, editing = !editableFile.onDemand),
          //                  content,
          //                  hash,
          //                  Plotter.default)
          //                treeNodeTabs add tab
          //              }
          //            else {
          //              val tab = TreeNodeTab.Editable(
          //                safePath,
          //                DataTab.build(SequenceData(Seq(), Seq()), view = TreeNodeTab.Raw),
          //                content,
          //                hash,
          //                Plotter.default)
          //
          //              treeNodeTabs add tab
          //            }

          case _ ⇒ //FIXME for GUI workflows
        }
    }
  }

