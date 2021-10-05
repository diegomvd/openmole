/**
 * Created by Mathieu Leclaire on 23/04/18.
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmole.gui.plugin.wizard.r

import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import org.openmole.gui.ext.client.OMPost
import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import autowire._
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.client
import org.scalajs.dom.raw.HTMLElement

import scala.concurrent.Future
import scala.scalajs.js.annotation._
import com.raquo.laminar.api.L._
import scala.scalajs.js

object TopLevelExports {
  @JSExportTopLevel("r")
  val r = js.Object {
    new org.openmole.gui.plugin.wizard.r.RWizardFactory
  }
}

class RWizardFactory extends WizardPluginFactory {
  val fileType = CodeFile(RLanguage())

  def build(safePath: SafePath, onPanelFilled: (LaunchingCommand) ⇒ Unit = (LaunchingCommand) ⇒ {}): WizardGUIPlugin = new RWizardGUI

  def parse(safePath: SafePath): Future[Option[LaunchingCommand]] = OMPost()[RWizardAPI].parse(safePath).call()

  def name: String = "R"
}

case class RWizardData() extends WizardData

class RWizardGUI extends WizardGUIPlugin {

  def factory = new RWizardFactory

  lazy val panel = div(client.modelHelp, "If your R sript depends on plugins, you should upload an archive (tar.gz, tgz) containing the root workspace.")

  def save(
    target:         SafePath,
    executableName: String,
    command:        String,
    inputs:         Seq[ProtoTypePair],
    outputs:        Seq[ProtoTypePair],
    libraries:      Option[String],
    resources:      Resources) =
    OMPost()[RWizardAPI].toTask(
      target,
      executableName,
      command,
      inputs,
      outputs,
      libraries,
      resources,
      RWizardData()).call()
}
