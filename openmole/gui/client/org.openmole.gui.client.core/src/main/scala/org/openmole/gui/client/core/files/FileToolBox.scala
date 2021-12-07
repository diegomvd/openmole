package org.openmole.gui.client.core.files

import org.openmole.gui.client.core._
import autowire._
import boopickle.Default._
import org.openmole.gui.client.core.alert.AbsolutePositioning._
import scala.concurrent.ExecutionContext.Implicits.global
import org.openmole.gui.ext.api.Api
import scaladget.bootstrapnative.bsn._
import org.openmole.gui.client.core.panels._
import org.openmole.gui.client.tool.OMTags
import org.openmole.gui.ext.client
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.client._
import com.raquo.laminar.api.L._

class FileToolBox(initSafePath: SafePath, showExecution: () ⇒ Unit, treeNodeTabs: TreeNodeTabs) {

  def iconAction(icon: HESetters, text: String, todo: () ⇒ Unit) =
    div(fileActionItems, icon, text, onClick --> { _ ⇒ todo() })

  def closeToolBox = treeNodePanel.currentLine.set(-1)

  def download = withSafePath { sp ⇒
    closeToolBox
    org.scalajs.dom.document.location.href = routes.downloadFile(client.Utils.toURI(sp.path))
  }

  def trash = withSafePath { safePath ⇒
    closeToolBox
    CoreUtils.trashNode(safePath) {
      () ⇒
        treeNodeTabs remove safePath
        treeNodeTabs.checkTabs
        treeNodePanel.invalidCacheAndDraw
    }
  }

  def duplicate = withSafePath { sp ⇒
    val newName = {
      val prefix = sp.path.last
      if (prefix.contains(".")) prefix.replaceFirst("[.]", "_1.")
      else prefix + "_1"
    }
    closeToolBox
    CoreUtils.duplicate(sp, newName)
  }

  def extract = withSafePath { sp ⇒
    Post()[Api].extractTGZ(sp).call().foreach {
      r ⇒
        r.error match {
          case Some(e: org.openmole.gui.ext.data.ErrorData) ⇒
            panels.alertPanel.detail("An error occurred during extraction", ErrorData.stackTrace(e), transform = RelativeCenterPosition, zone = FileZone)
          case _ ⇒ treeNodePanel.invalidCacheAndDraw
        }
    }
    closeToolBox
  }

  def execute = {
    import scala.concurrent.duration._
    withSafePath { sp ⇒
      Post(timeout = 120 seconds, warningTimeout = 60 seconds)[Api].runScript(ScriptData(sp), true).call().foreach { execInfo ⇒
        showExecution()
      }
      closeToolBox
    }
  }

  def toScript =
    withSafePath { sp ⇒
      closeToolBox
      Plugins.fetch { p ⇒
        val wizardPanel = panels.modelWizardPanel(p.wizardFactories)
        wizardPanel.dialog.show
        wizardPanel.fromSafePath(sp)
      }
    }

  def testRename(safePath: SafePath, to: String) = {
    val newSafePath = safePath.parent ++ to
    Post()[Api].existsExcept(newSafePath, false).call().foreach {
      b ⇒
        if (b) {
          actionEdit.set(None)
          actionConfirmation.set(Some(confirmation(s"Overwrite ${safePath.name} ?", () ⇒ rename(safePath, to, () ⇒ closeToolBox))))
        }
        else {
          rename(safePath, to, () ⇒ closeToolBox)
          actionEdit.set(None)
          actionConfirmation.set(None)
        }
    }
  }

  def rename(safePath: SafePath, to: String, replacing: () ⇒ Unit) = {
    Post()[Api].renameFile(safePath, to).call().foreach {
      newNode ⇒
        treeNodeTabs.rename(safePath, newNode)
        treeNodePanel.invalidCacheAndDraw
        treeNodeTabs.checkTabs
        treeNodePanel.currentSafePath.set(Some(safePath.parent ++ to))
        replacing()
    }
  }

  def withSafePath(action: SafePath ⇒ Unit) = {
    treeNodePanel.currentSafePath.now.foreach { sp ⇒
      action(sp)
    }
  }

  def glyphItemize(icon: HESetter) = icon.appended(cls := "glyphitem popover-item")

  val actionConfirmation: Var[Option[Div]] = Var(None)
  val actionEdit: Var[Option[Div]] = Var(None)

  def editForm(sp: SafePath): Div = {
    val renameInput = inputTag(sp.name).amend(
      placeholder := "File name",
      onMountFocus
    )

    div(
      child <-- actionConfirmation.signal.map { ac ⇒
        ac match {
          case Some(c) ⇒ c
          case None ⇒
            form(
              renameInput,
              onSubmit.preventDefault --> { _ ⇒
                withSafePath { sp ⇒
                  testRename(sp, renameInput.ref.value)
                }
              }
            )
        }
      }
    )
  }

  def confirmation(text: String, todo: () ⇒ Unit) =
    div(
      fileActions,
      div(text, width := "50%", margin := "10px"),
      div(fileItemCancel, "Cancel", onClick --> {
        _ ⇒ actionConfirmation.set(None)
      }),
      div(fileItemWarning, "OK", onClick --> {
        _ ⇒
          todo()
          actionConfirmation.set(None)
      })
    )

  def contentRoot = {
    div(
      height := "80px",
      child <-- actionConfirmation.signal.combineWith(actionEdit.signal).map {
        a ⇒
          a match {
            case (Some(ac), _) ⇒ ac
            case (_, Some(ae)) ⇒ ae
            case (None, None) ⇒
              div(
                fileActions,
                iconAction(glyphItemize(OMTags.glyph_arrow_left_right), "duplicate", () ⇒ duplicate),
                iconAction(glyphItemize(glyph_edit), "rename", () ⇒ actionEdit.set(Some(editForm(initSafePath)))),
                iconAction(glyphItemize(glyph_download), "download", () ⇒ download),
                iconAction(glyphItemize(glyph_trash), "delete", () ⇒ actionConfirmation.set(Some(confirmation(s"Delete ${
                  initSafePath.name
                } ?", () ⇒ trash)))),
                FileExtension(initSafePath.name) match {
                  case FileExtension.TGZ | FileExtension.TAR | FileExtension.ZIP | FileExtension.TXZ ⇒
                    iconAction(glyphItemize(OMTags.glyph_extract), "extract", () ⇒ extract)
                  case _ ⇒ emptyMod
                },
                FileExtension(initSafePath.name) match {
                  case FileExtension.OMS ⇒
                    iconAction(glyphItemize(OMTags.glyph_flash), "run", () ⇒ execute)
                  case _ ⇒ emptyMod
                },
                FileExtension(initSafePath.name) match {
                  case FileExtension.JAR | FileExtension.NETLOGO | FileExtension.R | FileExtension.TGZ ⇒
                    iconAction(OMTags.glyph_share, "to OMS", () ⇒ toScript)
                  case _ ⇒ emptyMod
                }
              )
          }
      }
    )
  }

}