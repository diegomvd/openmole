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
package org.openmole.gui.plugin.wizard.netlogo

import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.client.OMPost
import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import autowire._
import org.openmole.gui.ext.client
import org.scalajs.dom.raw.HTMLElement

import scala.concurrent.Future
import scala.scalajs.js.annotation._
import com.raquo.laminar.api.L._
import scala.scalajs.js

object TopLevelExports {
  @JSExportTopLevel("netlogo")
  val netlogo = js.Object {
    new org.openmole.gui.plugin.wizard.netlogo.NetlogoWizardFactory
  }
}

class NetlogoWizardFactory extends WizardPluginFactory {
  type WizardType = NetlogoWizardData

  val fileType = CodeFile(NetLogoLanguage())

  def build(safePath: SafePath, onPanelFilled: (LaunchingCommand) ⇒ Unit = (LaunchingCommand) ⇒ {}): WizardGUIPlugin = new NetlogoWizardGUI()

  def parse(safePath: SafePath): Future[Option[LaunchingCommand]] = OMPost()[NetlogoWizardAPI].parse(safePath).call()

  def help: String = "If your NetLogo script contains several files (.nls files) or depends on plugins, you should upload an archive (tar.gz or tgz) containing the entire root workspace. Then, set the embedWorkspace option to true."

  def name: String = "NetLogo"
}

class NetlogoWizardGUI extends WizardGUIPlugin {
  type WizardType = NetlogoWizardData

  def factory = new NetlogoWizardFactory

  val yes = ToggleState("Yes", btn_primary_string, () ⇒ {})
  val no = ToggleState("No", btn_secondary_string, () ⇒ {})

  lazy val embedWorkspaceToggle = toggle(yes, true, no, () ⇒ {})

  lazy val panel =
    hForm(
      embedWorkspaceToggle.element.withLabel("embedWorkspace"),
      div(client.modelHelp, client.columnCSS, "If your NetLogo script contains several files (.nls files) or depends on plugins, you should upload an archive (tar.gz or tgz) containing the entire root workspace. Then, set the embedWorkspace option to true.")
    )

  def save(
    target:         SafePath,
    executableName: String,
    command:        String,
    inputs:         Seq[ProtoTypePair],
    outputs:        Seq[ProtoTypePair],
    libraries:      Option[String],
    resources:      Resources) =
    OMPost()[NetlogoWizardAPI].toTask(
      target,
      executableName,
      command,
      inputs,
      outputs,
      libraries,
      resources,
      NetlogoWizardData(embedWorkspaceToggle.toggled.now)).call()
}