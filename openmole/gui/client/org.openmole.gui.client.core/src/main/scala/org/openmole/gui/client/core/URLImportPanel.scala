package org.openmole.gui.client.core

import org.openmole.gui.client.core.alert.{ AbsolutePositioning, AlertPanel }
import AbsolutePositioning.CenterPagePosition
import org.openmole.gui.ext.tool.client._

import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import org.openmole.gui.client.core.files.TreeNodePanel
import org.openmole.gui.client.core.files.treenodemanager.{ instance ⇒ manager }
import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import org.openmole.gui.client.core.CoreUtils._
import org.openmole.gui.ext.data._
import Waiter._
import autowire._
import org.openmole.core.market.MarketIndexEntry
import rx._
import scalatags.JsDom.tags
import scalatags.JsDom.all._
import org.openmole.gui.ext.api.Api
import Waiter._

class URLImportPanel {

  case class URLFile(name: String, extension: String) {
    def file = s"$name.$extension"
  }

  //private val downloadedFile: Var[Option[SafePath]] = Var(None)

  lazy val downloading: Var[ProcessState] = Var(Processed())

  val overwriteAlert: Var[Option[SafePath]] = Var(None)

  def exists(sp: SafePath, ifNotExists: () ⇒ {}) =
    post()[Api].exists(sp).call().foreach { b ⇒
      if (b) overwriteAlert.update(Some(sp))
      else ifNotExists()
    }

  def download(url: String) =
    for {
      project ← url.split("/").lastOption
      args = project.split('.')
      name ← args.headOption
      ext = args.tail.mkString(".")
      urlFile = URLFile(name, ext)
      path = manager.current.now ++ urlFile.file
    } yield {
      exists(path, () ⇒ {
        downloading.update(Processing())
        post()[Api].downloadHTTP(url, path, extractCheckBox.checked).call().foreach { d ⇒
          downloading.update(Processed())
          dialog.hide
          TreeNodePanel.refreshAndDraw
        }
      }
      )
    }

  def deleteFileAndDownloadURL(sp: SafePath, url: String) =
    post()[Api].deleteFile(sp, ServerFileSystemContext.project).call().foreach { d ⇒
      download(url)
    }

  lazy val urlInput = input(placeholder := "Project URL (.oms / .tar.gz)", width := "100%").render

  lazy val extractCheckBox = checkbox(false).render

  lazy val downloadButton = button(
    btn_primary,
    downloading.withTransferWaiter { _ ⇒
      tags.span("Download")
    }(height := 20),
    onclick := { () ⇒ download(urlInput.value) })

  val dialog = ModalDialog(
    omsheet.panelWidth(92),
    onopen = () ⇒ {
    }
  )

  dialog.header(
    tags.span(tags.b("Import project from URL"))
  )
  dialog.body({
    Rx {
      overwriteAlert() match {
        case Some(sp: SafePath) ⇒
          AlertPanel.string(
            sp.name + " already exists. Overwrite ? ",
            () ⇒ {
              overwriteAlert() = None
              deleteFileAndDownloadURL(manager.current() ++ sp.name, urlInput.value)
            }, () ⇒ {
              overwriteAlert() = None
            }, CenterPagePosition
          )
          tags.div
        case _ ⇒
      }
    }
    tags.div(
      urlInput,
      span(display.flex, flexDirection.row, alignItems.flexEnd, paddingTop := 20)(
        extractCheckBox,
        span("Extract archive (where applicable)", paddingLeft := 10, fontWeight.bold))
    )
  })

  dialog.footer(buttonGroup()(
    downloadButton,
    ModalDialog.closeButton(dialog, btn_default, "Close")
  ))

}