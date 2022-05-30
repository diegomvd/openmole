/**
 * Created by Romain Reuillon on 28/11/16.
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
package org.openmole.gui.plugin.authentication.egi

import scala.concurrent.ExecutionContext.Implicits.global
import org.openmole.gui.ext.data.{AuthenticationPlugin, AuthenticationPluginFactory}
import org.openmole.gui.ext.client._
import scaladget.bootstrapnative.bsn._
import boopickle.Default._
import autowire._

import scala.concurrent.Future
import scala.scalajs.js.annotation._
import com.raquo.laminar.api.L._

import scala.scalajs.js

object TopLevelExports {
  @JSExportTopLevel("egi")
  val egi = js.Object {
    new org.openmole.gui.plugin.authentication.egi.EGIAuthenticationGUIFactory
  }
}

class EGIAuthenticationGUIFactory extends AuthenticationPluginFactory {
  type AuthType = EGIAuthenticationData

  def buildEmpty: AuthenticationPlugin = new EGIAuthenticationGUI

  def build(data: AuthType): AuthenticationPlugin = new EGIAuthenticationGUI(data)

  def name = "EGI"

  def getData: Future[Seq[AuthType]] = OMPost()[EGIAuthenticationAPI].egiAuthentications().call()
}

class EGIAuthenticationGUI(val data: EGIAuthenticationData = EGIAuthenticationData()) extends AuthenticationPlugin {
  type AuthType = EGIAuthenticationData

  val password = inputTag(data.cypheredPassword).amend(placeholder := "Password", `type` := "password")

  def shorten(path: String): String = path.split("/").last//if (path.length > 10) s"...${path.takeRight(10)}" else path

  val privateKey =
    FileUploaderUI(data.privateKey.map(shorten).getOrElse(""), data.privateKey.isDefined, Some("egi.p12"))

  val voInput = inputTag("").amend(placeholder := "vo1,vo2")

  OMPost()[EGIAuthenticationAPI].geVOTest().call().foreach {
    _.foreach { c ⇒
      voInput.ref.value = c
    }
  }

  def factory = new EGIAuthenticationGUIFactory

  def remove(onremove: () ⇒ Unit) = OMPost()[EGIAuthenticationAPI].removeAuthentication().call().foreach { _ ⇒
    onremove()
  }

  lazy val panel = {
    import scaladget.tools._
    div(
      flexColumn, width := "400px", height := "220",
      div(cls := "verticalFormItem", div("Password", width := "150px"), password),
      div(cls := "verticalFormItem", div("Certificate", width := "150px"), display.flex, div(privateKey.view.amend(flexRow, justifyContent.flexEnd), width := "100%")),
      div(cls := "verticalFormItem", div("Test EGI credential on", width := "150px"), voInput)
    )
  }

  def save(onsave: () ⇒ Unit) = {
    OMPost()[EGIAuthenticationAPI].removeAuthentication().call().foreach {
      d ⇒
        OMPost()[EGIAuthenticationAPI].addAuthentication(EGIAuthenticationData(
          cypheredPassword = password.ref.value,
          privateKey = if (privateKey.pathSet.now) Some(EGIAuthenticationData.authenticationDirectory + "/egi.p12") else None
        )).call().foreach {
          b ⇒
            onsave()
        }
    }

    OMPost()[EGIAuthenticationAPI].setVOTest(voInput.ref.value.split(",").map(_.trim).toSeq).call()
  }

  def test = OMPost()[EGIAuthenticationAPI].testAuthentication(data).call()

}
