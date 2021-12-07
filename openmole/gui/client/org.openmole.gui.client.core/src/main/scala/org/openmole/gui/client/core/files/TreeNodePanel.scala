package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.alert.AbsolutePositioning.{ FileZone, RelativeCenterPosition }
import org.openmole.gui.client.core.CoreUtils
import org.openmole.gui.client.core.Waiter._
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.client._
import scaladget.bootstrapnative.bsn._
import org.scalajs.dom.raw._
import org.openmole.gui.client.core._

import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import TreeNode._
import autowire._
import org.openmole.gui.ext.api.Api
import org.openmole.gui.ext.client.FileManager
import com.raquo.laminar.api.L._
import org.openmole.gui.client.tool.OMTags

/*
 * Copyright (C) 16/04/15 // mathieu.leclaire@openmole.org
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

class TreeNodePanel(val treeNodeManager: TreeNodeManager, fileDisplayer: FileDisplayer, showExecution: () ⇒ Unit, treeNodeTabs: TreeNodeTabs, services: PluginServices) {

  val treeWarning = Var(true)
  val draggedNode: Var[Option[SafePath]] = Var(None)

  val selectionModeObserver = Observer[Boolean] { b ⇒
    if (!b) treeNodeManager.clearSelection
  }

  val fileToolBar = new FileToolBar(this)
  val tree: Var[HtmlElement] = Var(div())

  val editNodeInput = inputTag("").amend(
    placeholder := "Name",
    width := "240px",
    height := "24px",
    onMountFocus
  )

  // New file tool
  val newNodeInput = inputTag().amend(
    placeholder := "File name",
    width := "130px",
    marginLeft := "10px",
    onMountFocus
  )

  lazy val addRootDirButton = {

    val folder = ToggleState("Folder", "btn blue-button", () ⇒ {})
    val file = ToggleState("File", "btn blue-button", () ⇒ {})

    toggle(folder, true, file, () ⇒ {})
  }

  def createNewNode = {
    val newFile = newNodeInput.ref.value
    val currentDirNode = treeNodeManager.current
    addRootDirButton.toggled.now match {
      case true  ⇒ CoreUtils.addDirectory(currentDirNode.now, newFile, () ⇒ invalidCacheAndDraw)
      case false ⇒ CoreUtils.addFile(currentDirNode.now, newFile, () ⇒ invalidCacheAndDraw)
    }
  }

  //Upload tool
  val transferring: Var[ProcessState] = Var(Processed())

  def fInputMultiple(todo: Input ⇒ Unit) =
    inputTag().amend(cls := "upload", `type` := "file", multiple := true,
      inContext { thisNode ⇒
        onChange --> { _ ⇒
          todo(thisNode)
        }
      }
    )

  def upbtn(todo: Input ⇒ Unit): HtmlElement =
    span(aria.hidden := true, glyph_upload, cls := "fileUpload glyphmenu", margin := "10 0 10 160",
      fInputMultiple(todo)
    )

  private val upButton = upbtn((fileInput: Input) ⇒ {
    FileManager.upload(fileInput, treeNodeManager.current.now, (p: ProcessState) ⇒ transferring.set(p), UploadProject(), () ⇒ invalidCacheAndDraw)
  })

  lazy val createFileTool =
    form(flexRow, alignItems.center, height := "70px", color.white, margin := "0 10 0 10",
      addRootDirButton.element,
      newNodeInput.amend(marginLeft := "10px"),
      upButton.amend(justifyContent.flexEnd).tooltip("Upload a file"),
      transferring.withTransferWaiter {
        _ ⇒
          div()
      }.amend(marginLeft := "10px"),
      onSubmit.preventDefault --> { _ ⇒
        createNewNode
        newNodeInput.ref.value = ""
        plusFile.set(false)
      })

  val confirmationDiv: Var[Option[Div]] = Var(None)

  def confirmation(text: String, okText: String, todo: () ⇒ Unit) =
    div(
      fileActions,
      div(text, width := "50%", margin := "10px"),
      div(fileItemCancel, "Cancel", onClick --> {
        _ ⇒
          closeMultiTool
      }),
      div(fileItemWarning, okText, onClick --> {
        _ ⇒
          todo()
          closeMultiTool
      })
    )

  lazy val copyOrTrashTool = div(
    height := "70px", flexRow, alignItems.center, color.white, justifyContent.spaceBetween,
    children <-- confirmationDiv.signal.map { ac ⇒
      println("ACCC " + ac)
      val selected = treeNodeManager.selected
      val isSelectionEmpty = selected.signal.map {
        _.isEmpty
      }
      ac match {
        case Some(c) ⇒ Seq(c)
        case None ⇒ Seq(
          button(cls := "btn blue-button", marginLeft := "80px", "Copy", onClick --> { _ ⇒
            multiTool.set(Paste)
            confirmationDiv.set(Some(confirmation(s"${selected.now().size} files copied. Browse to the target folder and press Paste", "Paste", () ⇒
              CoreUtils.testExistenceAndCopyProjectFilesTo(selected.now(), treeNodeManager.current.now()).foreach { existing ⇒
                if (existing.isEmpty) {
                  refreshAndDraw
                  closeMultiTool
                }
                else {
                  confirmationDiv.set(Some(confirmation(s"${existing.size} files have already the same name. Overwrite them ?", "Overwrite", () ⇒
                    CoreUtils.copyProjectFilesTo(selected.now(), treeNodeManager.current.now()).foreach { b ⇒
                      refreshAndDraw
                      closeMultiTool
                    })))
                }
              })))
          },
            disabled <-- isSelectionEmpty
          ),
          button(btn_danger, "Delete", marginRight := "80px", onClick --> { _ ⇒
            confirmationDiv.set(Some(confirmation(s"Delete ${treeNodeManager.selected.now().size} files ?", "OK", () ⇒
              CoreUtils.trashNodes(treeNodeManager.selected.now) { () ⇒
                refreshAndDraw
                closeMultiTool
              })))
          },
            disabled <-- isSelectionEmpty)
        )
      }
    }
  )

  def closeMultiTool = {
    multiTool.set(Off)
    confirmationDiv.set(None)
    treeNodeManager.clearSelection

  }

  val plusFile = Var(false)

  trait MultiTool

  object CopyOrTrash extends MultiTool

  object Paste extends MultiTool

  object Off extends MultiTool

  val multiTool: Var[MultiTool] = Var(Off)

  lazy val fileControler =
    div(
      cls := "file-content",
      child <-- treeNodeManager.current.signal.map { curr ⇒
        val parent = curr.parent
        div(
          cls := "tree-path",
          goToDirButton(treeNodeManager.root).amend(OMTags.glyph_house, padding := "5"),
          Seq(parent.parent, parent, curr).filterNot { sp ⇒
            sp.isEmpty || sp == treeNodeManager.root
          }.map { sp ⇒
            goToDirButton(sp, s" ${sp.name} / ")
          },
          div(glyph_plus, cls <-- plusFile.signal.map { pf ⇒
            "plus-button" + {
              if (pf) " selected" else ""
            }
          }, onClick --> { _ ⇒ plusFile.update(!_) })
        )
      },
      div(
        display.flex, justifyContent.flexEnd,
        div(glyph_refresh, cls := "treePathItems file-refresh", onClick --> { _ ⇒ refreshAndDraw }),
        div(cls := "bi-three-dots-vertical treePathItems", fontSize := "20px", onClick --> { _ ⇒
          multiTool.update { mcot ⇒
            mcot match {
              case Off ⇒ CopyOrTrash
              case _ ⇒
                confirmationDiv.set(None)
                treeNodeManager.clearSelection
                Off
            }
          }
          multiTool.now match {
            case Off ⇒ invalidCacheAndDraw
            case _   ⇒
          }
        })
      ),
      plusFile.signal.expand(createFileTool),
      multiTool.signal.map { m ⇒ m != Off }.expand(copyOrTrashTool),
      treeNodeManager.error --> treeNodeManager.errorObserver,
      treeNodeManager.comment --> treeNodeManager.commentObserver
    )

  lazy val view = {
    drawTree
    div(child <-- tree.signal, cls := "file-scrollable-content")
  }

  def filter: FileFilter = fileToolBar.fileFilter.now

  def downloadFile(safePath: SafePath, onLoaded: (String, Option[String]) ⇒ Unit, saveFile: Boolean, hash: Boolean) = {

    //    if (FileExtension.isOMS(safePath.name))
    //      OMPost()[Api].hash(safePath).call().foreach { h ⇒
    //        HashService.set(safePath, h)
    //      }

    FileManager.download(
      safePath,
      (p: ProcessState) ⇒ {
        transferring.set(p)
      },
      hash = hash,
      onLoaded = onLoaded
    )
  }

  def goToDirButton(safePath: SafePath, name: String = ""): HtmlElement =
    span(cls := "treePathItems", name,
      onClick --> { _ ⇒
        treeNodeManager.switch(safePath)
        drawTree
      },
      dropPairs,
      onDrop --> { e ⇒
        e.dataTransfer
        e.preventDefault()
        dropAction(safePath, true)
      }
    )

  def invalidCacheAndDraw = {
    invalidCacheAnd(() ⇒ {
      drawTree
    })
  }

  def invalidCacheAnd(todo: () ⇒ Unit) = {
    treeNodeManager.invalidCurrentCache
    todo()
  }

  def refreshAnd(todo: () ⇒ Unit) = invalidCacheAnd(todo)

  def refreshAndDraw = invalidCacheAndDraw

  //  def computePluggables = fileToolBar.selectedTool.now match {
  //    case Some(PluginTool) ⇒ treeNodeManager.computePluggables(() ⇒ if (!treeNodeManager.pluggables.now.isEmpty) turnSelectionTo(true))
  //    case _                ⇒
  //  }

  def drawTree: Unit = {
    // computePluggables
    tree.set(treeNodeManager.computeCurrentSons(filter).withFutureWaiter("Get files", (sons: ListFiles) ⇒ {

      if (treeNodeManager.isRootCurrent && treeNodeManager.isProjectsEmpty) {
        div("Create a first OpenMOLE script (.oms)", cls := "message")
      }
      else {
        //          tbody(
        //            backgroundColor <-- selectionMode.signal.map { sM ⇒
        //              if (sM) omsheet.BLUE else omsheet.DARK_GREY
        //            },
        //            omsheet.fileList,
        //            child <-- treeWarning.signal.map { tW ⇒
        //              if (sons.list.length < sons.nbFilesOnServer && tW) {
        //                div(
        //                  omsheet.moreEntries,
        //                  div(
        //                    omsheet.moreEntriesText,
        //                    div(
        //                      s"Max of 1,000 files (${100000 / sons.nbFilesOnServer}%) displayed simultaneously",
        //                      div(
        //                        "Use the ",
        //                        span(
        //                          "Filter tool",
        //                          cursor.pointer, color := omsheet.BLUE,
        //                          onClick --> { _ ⇒ fileToolBar.selectTool(FilterTool) }
        //                        ), " to refine your search"
        //                      )
        //                    )
        //                  )
        //                )
        //              }
        //              else div()
        //            },
        //            for (tn ← sons.list) yield {
        //              drawNode(tn).render
        //            },
        //            //            onscroll := { () ⇒
        //            //              Popover.hide
        //            //            },
        //            selectionMode --> selectionModeObserver
        //          )
        div(
          sons.list.zipWithIndex.map {
            case (tn, id) ⇒
              Seq(drawNode(tn, id).render)
          }
        )
      }
    })
    )

  }

  def displayNode(tn: TreeNode) = tn match {
    case fn: FileNode ⇒
      val ext = FileExtension(tn.name)
      val tnSafePath = treeNodeManager.current.now ++ tn.name
      if (ext.displayable) {
        downloadFile(
          tnSafePath,
          saveFile = false,
          hash = true,
          onLoaded = (content: String, hash: Option[String]) ⇒ {
            fileDisplayer.display(tnSafePath, content, hash.get, ext, services)
            invalidCacheAndDraw
          }
        )
      }
    case _ ⇒
  }

  def stringAlert(message: String, okaction: () ⇒ Unit) =
    panels.alertPanel.string(message, okaction, transform = RelativeCenterPosition, zone = FileZone)

  def stringAlertWithDetails(message: String, detail: String) =
    panels.alertPanel.detail(message, detail, transform = RelativeCenterPosition, zone = FileZone)

  val currentSafePath: Var[Option[SafePath]] = Var(None)
  val currentLine = Var(-1)

  def timeOrSize(tn: TreeNode): String = fileToolBar.fileFilter.now.fileSorting match {
    case TimeSorting() ⇒ CoreUtils.longTimeToString(tn.time)
    case _             ⇒ CoreUtils.readableByteCountAsString(tn.size)
  }

  def fileClick(todo: () ⇒ Unit) =
    onClick --> { _ ⇒
      plusFile.set(false)
      val currentMultiTool = multiTool.signal.now()
      if (currentMultiTool == Off || currentMultiTool == Paste) todo()
    }

  case class ReactiveLine(id: Int, tn: TreeNode, treeNodeType: TreeNodeType, todo: () ⇒ Unit) {

    val tnSafePath = treeNodeManager.current.now ++ tn.name
    val selected = Var(false)

    def dirBox(tn: TreeNode) =
      div(
        child <-- multiTool.signal.map { mcot ⇒
          if (mcot == CopyOrTrash) checkbox(selected.now()).amend(onClick --> { _ ⇒
            selected.update { s ⇒
              treeNodeManager.setSelected(tnSafePath, !s)
              !s
            }
          })
          else tn match {
            case _: DirNode ⇒ div(cls := "dir plus bi-plus", cursor.pointer)
            case _          ⇒ emptyNode
          }
        }
      )

    val toolBox = new FileToolBox(tnSafePath, showExecution, treeNodeTabs)

    val render: HtmlElement = {
      // val settingsGlyph = Seq(cls := "glyphitem", glyph_settings, color := WHITE, paddingLeft := "4")
      div(display.flex, flexDirection.column,
        div(display.flex, alignItems.center, lineHeight := "27px",
          backgroundColor <-- selected.signal.map { s ⇒ if (s) toolBoxColor else "" },
          //        child <-- selectionMode.signal.combineWith(treeStates.signal, fileToolBar.selectedTool.signal, treeNodeManager.pluggables.signal).map {
          //          case (sM, tS, sTools, pluggables) ⇒
          //            onClick --> { e ⇒ {
          //              if (sM) {
          //                addToSelection
          //                if (e.ctrlKey) clearSelectionExecpt(tnSafePath)
          //              }
          //            }
          //            },
          dropPairs,
          onDragStart --> { e ⇒
            e.dataTransfer.setData("text/plain", "nothing") //  FIREFOX TRICK
            draggedNode.set(Some(tnSafePath))
          },
          onDrop --> { e ⇒
            e.dataTransfer
            e.preventDefault()
            dropAction(treeNodeManager.current.now ++ tn.name, tn match {
              case _: DirNode ⇒ true
              case _          ⇒ false
            })
          },
          dirBox(tn).amend(cls := "file0", fileClick(todo), draggable := true),
          div(tn.name, cls.toggle("cursor-pointer") <-- multiTool.signal.map { mt ⇒
            mt == Off || mt == Paste
          },
            cls := "file1", fileClick(todo), draggable := true),
          i(timeOrSize(tn), cls := "file2"),
          button(cls := "bi-three-dots transparent-button", cursor.pointer, opacity := "0.5", onClick --> { _ ⇒
            currentSafePath.set(Some(tnSafePath))
            currentLine.set(
              if (id == currentLine.now) -1
              else id
            )
          })
        ),
        currentLine.signal.map { i ⇒ i == id }.expand(toolBox.contentRoot)
      )
    }
  }

  def dropPairs = Seq(
    draggable := true,
    onDragEnter --> { e ⇒
      val el = e.target.asInstanceOf[HTMLElement]
      val style = new CSSStyleDeclaration()
      style.backgroundColor = "red"
      el.style = style
    },
    onDragLeave --> { e ⇒
      val style = new CSSStyleDeclaration
      style.backgroundColor = "transparent"
      e.target.asInstanceOf[HTMLElement].style = style
    },
    onDragOver --> { e ⇒
      e.dataTransfer.dropEffect = "move"
      e.preventDefault
    }
  )

  def dropAction(to: SafePath, isDir: Boolean) = {
    draggedNode.now.map {
      dragged ⇒
        if (isDir) {
          if (dragged != to) {
            //treeNodeTabs.saveAllTabs(() ⇒ {
            Post()[Api].move(dragged, to).call().foreach {
              b ⇒
                treeNodeManager.invalidCache(to)
                treeNodeManager.invalidCache(dragged)
                refreshAndDraw
                treeNodeTabs.checkTabs
            }
            //})
          }
        }
    }
    draggedNode.set(None)
  }

  def drawNode(node: TreeNode, i: Int) = node match {
    case fn: FileNode ⇒
      ReactiveLine(i, fn, TreeNodeType.file, () ⇒ {
        displayNode(fn)
      })
    case dn: DirNode ⇒ ReactiveLine(i, dn, TreeNodeType.folder, () ⇒ {
      treeNodeManager switch (dn.name)
      treeWarning.set(true)
      drawTree
    })
  }

}
