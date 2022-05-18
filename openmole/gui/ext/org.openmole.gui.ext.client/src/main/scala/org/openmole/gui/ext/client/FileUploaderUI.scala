package org.openmole.gui.ext.client

/*
 * Copyright (C) 03/07/15 // mathieu.leclaire@openmole.org
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

import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import org.openmole.gui.ext.api.Api
import org.openmole.gui.ext.data._
import org.scalajs.dom.raw.HTMLInputElement
import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import autowire._
import com.raquo.laminar.api.L._

object FileUploaderUI {
  def empty = FileUploaderUI("", false)
}

case class FileUploaderUI(
  keyName:  String,
  keySet:   Boolean,
  renaming: Option[String] = None) {

  val fileName = if (keyName == "") renaming.getOrElse(DataUtils.uuID) else keyName
  val pathSet: Var[Boolean] = Var(keySet)

  val view = upButton

  lazy val upButton = label(
    fileInput((fInput: Input) ⇒ {
      FileManager.upload(
        fInput,
        SafePath.empty,
        (p: ProcessState) ⇒ {
        },
        UploadAuthentication(),
        () ⇒ {
          if (fInput.ref.files.length > 0) {
            val leaf = fInput.ref.files.item(0).name
            pathSet.set(false)
            OMPost()[Api].renameKey(leaf, fileName).call().foreach {
              b ⇒
                pathSet.set(true)
            }
          }
        }
      )
    }),
    child <-- pathSet.signal.map { ps ⇒
      if (ps) fileName
      else "No certificate"
    },
    paddingTop := "5",
    marginTop := "40",
    omsheet.certificate,
    cls := "inputFileStyle"
  )
}