package org.openmole.gui.client.core

import org.openmole.gui.shared.data.*
import org.scalajs.dom.raw.MouseEvent
import scaladget.bootstrapnative.bsn.*

import scala.concurrent.ExecutionContext.Implicits.global
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.core.files.TreeNodeManager
import org.openmole.gui.shared.api.ServerAPI
import scaladget.bootstrapnative.bsn

//
///*
// * Copyright (C) 10/08/15 // mathieu.leclaire@openmole.org
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU Affero General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU Affero General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//
class PluginPanel:

  private lazy val plugins: Var[Seq[Plugin]] = Var(Seq())

  def getPlugins(using api: ServerAPI) = api.listPlugins().map { p ⇒ plugins.set(p.toSeq) }

  def pluginTable(using api: ServerAPI, panels: Panels) =
    div(
      children <-- plugins.signal.combineWith(panels.expandablePanel.signal).map {
        case (ps, _) ⇒
          ps.zipWithIndex.map { case (p, i) ⇒
            div(
              cls := "docEntry",
              backgroundColor := {
                if (i % 2 == 0) "white" else "#ececec"
              },
              div(p.projectSafePath.name, width := "350px"),
              div(
                cls := "badgeOM",
                bsn.badge_dark, p.time
              ), onClick --> { _ ⇒
                panels.treeNodePanel.treeNodeManager.switch(p.projectSafePath.parent)
                panels.treeNodePanel.treeNodeManager.computeCurrentSons
              }
            )
          }
      }
    )

  def render(using api: ServerAPI, panels: Panels): HtmlElement =
    div(
      div(
        cls := "expandable-title",
        div(cls := "close-button bi-chevron-down", onClick --> { _ ⇒ Panels.closeExpandable })
      ),
      pluginTable
    )
