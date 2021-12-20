package org.openmole.gui.client.core

import org.openmole.gui.ext.data._
import org.scalajs.dom.raw.MouseEvent
import scaladget.bootstrapnative.bsn._
import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import autowire._
import org.openmole.gui.ext.api.Api
import com.raquo.laminar.api.L._

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
class PluginPanel {

  private lazy val plugins: Var[Seq[Plugin]] = Var(Seq())
  getPlugins

  def getPlugins = {
    Post()[Api].listPlugins.call().foreach { p ⇒
      plugins.set(p.toSeq)
    }
  }

  val pluginTable = {
    div(
      div(
        cls := "expandable-table",
        children <-- plugins.signal.combineWith(panels.expandablePanel.signal).map {
          case (ps, _) ⇒
            ps.map { p ⇒
              println("PLUG " + p.projectSafePath)
              div(
                cls := "docEntry",
                backgroundColor := "#3f3d56",
                div(p.projectSafePath.name, justifyContent.flexStart),
                div(p.time, cls := "table-time"),
                onClick --> { (e: MouseEvent) ⇒
                  panels.treeNodePanel.draw(p.projectSafePath.parent)
                }
              )
            }
        }
      )
    )
  }

  def render: HtmlElement = {

    div(
      div(
        cls := "expandable-title",
        div("Plugins"),
        div(cls := "close-button bi-chevron-down", onClick --> { _ ⇒ panels.closeExpandable })
      ),
      pluginTable
    )
  }
}