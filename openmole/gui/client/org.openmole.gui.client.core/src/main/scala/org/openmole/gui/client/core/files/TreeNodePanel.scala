package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.CoreUtils
import org.openmole.gui.client.core.Waiter.*
import org.openmole.gui.shared.data.*
import org.openmole.gui.client.ext.*
import scaladget.bootstrapnative.bsn.*
import org.scalajs.dom.raw.*
import org.openmole.gui.client.core.*
import org.openmole.gui.client.ext.*

import scala.concurrent.ExecutionContext.Implicits.global
import TreeNode.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.ext.FileManager
import org.openmole.gui.client.tool.OMTags
import org.openmole.gui.shared.api.*

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

object TreeNodePanel:
  extension (p: TreeNodePanel)
    def refresh = p.refresh

class TreeNodePanel { panel =>

  val treeNodeManager: TreeNodeManager = new TreeNodeManager
  val treeWarning = Var(true)
  val draggedNode: Var[Option[SafePath]] = Var(None)
  val update: Var[Long] = Var(0)

  def refresh = update.update(_ + 1)

  //val selectionModeObserver = Observer[Boolean] { b ⇒ if !b then treeNodeManager.clearSelection }

  val fileToolBar = new FileToolBar(this, treeNodeManager)


  val editNodeInput = inputTag("").amend(
    placeholder := "Name",
    width := "240px",
    height := "24px",
    onMountFocus
  )

  // New file tool
  val newNodeInput =
    inputTag().amend(
      placeholder <-- directoryToggle.toggled.signal.map { d => if d then "New directory" else "New file" },
      width := "270px",
      marginLeft := "12px",
      onMountFocus
    )

  lazy val directoryToggle =
    object FileType
    val folder = ToggleState(FileType, "Folder", "btn purple-button", _ ⇒ {})
    val file = ToggleState(FileType, "File", "btn purple-button", _ ⇒ {})
    toggle(folder, false, file, () ⇒ {})

  def createNewNode(newFile: String)(using api: ServerAPI, basePath: BasePath, panels: Panels) =
    val currentDirNode = treeNodeManager.directory
    directoryToggle.toggled.now() match
      case true ⇒ CoreUtils.createFile(currentDirNode.now(), newFile, directory = true).map(_ => refresh)
      case false ⇒ CoreUtils.createFile(currentDirNode.now(), newFile).map(_ => refresh)

  //Upload tool
  val transferring: Var[ProcessState] = Var(Processed())

  def fInputMultiple(todo: Input ⇒ Unit) =
    inputTag().amend(cls := "upload", `type` := "file", multiple := true, OMTags.webkitdirectory <-- directoryToggle.toggled.signal, inContext { thisNode ⇒ onChange --> { _ ⇒ todo(thisNode) } })

  def upbtn(todo: Input ⇒ Unit): HtmlElement =
    span(aria.hidden := true, cls <-- directoryToggle.toggled.signal.map { d => if !d then "bi-cloud-upload" else "bi-cloud-upload-fill" }, cls := "fileUpload glyphmenu", margin := "10 0 10 12", fInputMultiple(todo)).
      tooltip("eranu").amend(dataAttr("original-title") <-- directoryToggle.toggled.signal.map { d => if !d then "Upload files" else "Upload directories" })

  private def upButton(using api: ServerAPI, basePath: BasePath) = upbtn((fileInput: Input) ⇒ {
    val current = treeNodeManager.directory.now()
    api.upload(
      fileInput.ref.files.toSeq.map(f => f -> current / f.path),
      (p: ProcessState) ⇒ transferring.set(p)
    ).map { _ =>
      fileInput.ref.value = ""
      refresh
    }
  })

  def createFileTool(using api: ServerAPI, basePath: BasePath, panels: Panels) =
    form(flexRow, alignItems.center, height := "70px", color.white, margin := "0 10 0 10",
      directoryToggle.element,
      newNodeInput.amend(marginLeft := "10px"),
      upButton.amend(justifyContent.flexEnd),
      transferring.withTransferWaiter {
        _ ⇒
          div()
      }.amend(marginLeft := "10px"),
      onSubmit.preventDefault --> { _ ⇒
        createNewNode(newNodeInput.ref.value)
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
      div(fileItemWarning, okText, onClick --> { _ ⇒ todo() })
    )

  def copyOrTrashTool(using api: ServerAPI, basePath: BasePath) = div(
    height := "70px", flexRow, alignItems.center, color.white, justifyContent.spaceBetween,
    children <-- confirmationDiv.signal.map { ac ⇒
      val selected = treeNodeManager.selected
      val isSelectionEmpty = selected.signal.map {
        _.isEmpty
      }
      ac match {
        case Some(c) ⇒ Seq(c)
        case None ⇒ Seq(
          button(cls := "btn btn-primary", marginLeft := "80px", "Copy", onClick --> { _ ⇒
            multiTool.set(Paste)
            confirmationDiv.set(Some(confirmation(s"${selected.now().size} files copied. Browse to the target folder and press Paste", "Paste", () ⇒
              val target = treeNodeManager.directory.now()
              api.copyFiles(selected.now().map(p => p -> (target ++ p.name)), overwrite = false).foreach { existing ⇒
                if (existing.isEmpty) {
                  refresh
                  closeMultiTool
                }
                else {
                  confirmationDiv.set(Some(confirmation(s"${existing.size} files have already the same name. Overwrite them ?", "Overwrite", () ⇒
                    val target = treeNodeManager.directory.now()
                    api.copyFiles(selected.now().map(p => p -> (target ++ p.name)), overwrite = true).foreach { b ⇒
                      refresh
                      closeMultiTool
                    })))
                }
              })))
          },
            disabled <-- isSelectionEmpty
          ),
          button(btn_danger, "Delete", marginRight := "80px", onClick --> { _ ⇒
            confirmationDiv.set(
              Some(confirmation(s"Delete ${treeNodeManager.selected.now().size} files ?", "OK", () ⇒
                CoreUtils.trashNodes(this, treeNodeManager.selected.now()).andThen { _ ⇒ closeMultiTool }
              )
              )
            )
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

  def fileControler(using panels: Panels, api: ServerAPI, basePath: BasePath) =
    div(
      cls := "file-content",
      child <-- treeNodeManager.directory.signal.map { curr ⇒
        val parent = curr.parent
        div(
          cls := "tree-path",
          goToDirButton(treeNodeManager.root).amend(OMTags.glyph_house, padding := "0 10 5 0"),
          Seq(parent.parent, parent, curr).filterNot { sp ⇒
            sp.isEmpty || sp == treeNodeManager.root
          }.map { sp ⇒
            goToDirButton(sp, s"${sp.name} / ")
          },
          div(glyph_plus, cls <-- plusFile.signal.map { pf ⇒
            "plus-button" + {
              if (pf) " selected" else ""
            }
          }, onClick --> { _ ⇒ plusFile.update(!_) })
        )
      },
      div(
        display.flex, justifyContent.flexEnd, marginTop := "20",
        div(OMTags.glyph_search,
          cls := "filtering-files-item-selected",
          onClick --> { _ ⇒ fileToolBar.filterToolOpen.update(!_) }),
        div(glyph_refresh, cls := "treePathItems file-refresh", onClick --> { _ ⇒ refresh }),
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
          multiTool.now() match {
            case Off ⇒ refresh
            case _ ⇒
          }
        })
      ),
      plusFile.signal.expand(createFileTool),
      multiTool.signal.map { m ⇒ m != Off }.expand(copyOrTrashTool)
    )

  def downloadFile(safePath: SafePath, hash: Boolean)(using api: ServerAPI, basePath: BasePath) =
    api.download(
      safePath,
      (p: ProcessState) ⇒ { transferring.set(p) },
      hash = hash
    )


  def goToDirButton(safePath: SafePath, name: String = "")(using panels: Panels, api: ServerAPI, basePath: BasePath): HtmlElement =
    div(cls := "treePathItems", paddingLeft := "4px", name,
      onClick --> { _ ⇒
        treeNodeManager.switch(safePath)
      },
      dropPairs,
      onDrop --> { e ⇒
        e.dataTransfer
        e.preventDefault()
        dropAction(safePath, true)
      }
    )

  def treeView(using panels: Panels, pluginServices: PluginServices, api: ServerAPI, basePath: BasePath, plugins: GUIPlugins): Div =
    val size = Var(100)
    div(
      cls := "file-scrollable-content",
      children <--
        (treeNodeManager.directory.signal combineWith treeNodeManager.findFilesContaining.signal combineWith multiTool.signal combineWith treeNodeManager.fileFilter.signal combineWith update.signal combineWith size.signal).flatMap { (currentDir, findString, foundFiles, multiTool, fileFilter, _, sizeValue) ⇒
          EventStream.fromFuture(CoreUtils.listFiles(currentDir, fileFilter.copy(size = Some(sizeValue))).map(Some(_)), true).toSignal(None).map {
            case None =>
              Seq(
                i(cls := "bi bi-hourglass-split", marginLeft := "250", textAlign := "center")
              )
            case Some(nodes) =>
              val content =
                if !foundFiles.isEmpty
                then
                  foundFiles.map { (sp, isDir) =>
                    div(s"${sp.normalizedPathString}", cls := "findFile",
                      onClick --> { _ =>
                        fileToolBar.filterToolOpen.set(false)
                        treeNodeManager.resetFileFinder
                        fileToolBar.findInput.ref.value = ""
                        val switchTarget = if isDir then sp else sp.parent
                        treeNodeManager.switch(switchTarget)
                        //treeNodeManager.computeCurrentSons
                        displayNode(sp)
                      }
                    )
                  }
                else if currentDir == treeNodeManager.root && nodes.data.isEmpty
                then Seq(div("Create a first OpenMOLE script (.oms)", cls := "message"))
                else
                  val checked =
                    if multiTool == CopyOrTrash
                    then
                      val allCheck: Input = checkbox(false)
                      allCheck.amend(
                        cls := "file0", marginBottom := "3px", onClick --> { _ ⇒
                          treeNodeManager.switchAllSelection(nodes.data.map { tn => currentDir ++ tn.name }, allCheck.ref.checked)
                        }
                      )
                    else emptyNode

                  checked +: nodes.data.zipWithIndex.flatMap { case (tn, id) => Seq(drawNode(tn, id).render) }

              def more =
                if nodes.listed < nodes.total
                then
                  Seq(
                    div(position := "absolute", bottom := "20", left := "250", cursor.pointer, textAlign := "center",
                      i(cls := "bi bi-plus"),
                      br(),
                      i(fontSize := "12", s"${nodes.listed}/${nodes.total}"),
                      onClick --> { _ => size.update(_ * 2) }
                    )
                  )
                else Seq()

              content ++ more
          }
        },
      treeNodeManager.directory.toObservable --> Observer { _ => size.set(100) }
    )

  def displayNode(safePath: SafePath, refresh: Boolean = false)(using panels: Panels, api: ServerAPI, basePath: BasePath, plugins: GUIPlugins): Unit =
    def isDisplayable(e: FileContentType) =
      e match
        case FileContentType.OpaqueFileType => false
        case FileContentType.OpenMOLEResult => true
        case r: ReadableFileType => r.text

    if isDisplayable(FileContentType(FileExtension(safePath)))
    then
      downloadFile(
        safePath,
        hash = true
      ).map { (content: String, hash: Option[String]) ⇒
        if refresh then panels.tabContent.removeTab(safePath)
        panels.fileDisplayer.display(safePath, content, hash.get, FileExtension(safePath))
        refresh
      }

  def displayNode(tn: TreeNode)(using panels: Panels, api: ServerAPI, basePath: BasePath, plugins: GUIPlugins): Unit =
    tn match
      case tn: TreeNode.File ⇒
        val tnSafePath = treeNodeManager.directory.now() ++ tn.name
        displayNode(tnSafePath)
      case _ ⇒

  val currentSafePath: Var[Option[SafePath]] = Var(None)
  val currentLine = Var(-1)

  def timeOrSize(tn: TreeNode): String = treeNodeManager.fileFilter.now().fileSorting match {
    case ListSorting.TimeSorting ⇒ CoreUtils.longTimeToString(tn.time)
    case _ ⇒ CoreUtils.readableByteCountAsString(tn.size)
  }

  def fileClick(todo: () ⇒ Unit)(using api: ServerAPI, basePath: BasePath) =
    onClick --> { _ ⇒
      plusFile.set(false)
      val currentMultiTool = multiTool.signal.now()
      if (currentMultiTool == Off || currentMultiTool == Paste) todo()
      fileToolBar.filterToolOpen.set(false)
      //treeNodeManager.computeCurrentSons
    }

  case class ReactiveLine(id: Int, tn: TreeNode, treeNodeType: TreeNodeType, todo: () ⇒ Unit) {
    val tnSafePath = treeNodeManager.directory.now() ++ tn.name

    def isSelected(selection: Seq[SafePath]) = selection.contains(tnSafePath)

    def dirBox(tn: TreeNode) =
      div(
        child <-- multiTool.signal.combineWith(treeNodeManager.selected.signal).map { case (mcot, selected) ⇒
          if (mcot == CopyOrTrash) checkbox(isSelected(selected)).amend(onClick --> { _ ⇒
            treeNodeManager.switchSelection(tnSafePath)
          })
          else {
            tn match
              case _: TreeNode.Directory ⇒ div(cls := "dir plus bi-plus", cursor.pointer)
              case f: TreeNode.File ⇒
                if (f.pluginState.isPlugin) {
                  div("P", cls := "plugin-file" + {
                    if (f.pluginState.isPlugged) " plugged"
                    else " unplugged"
                  })
                }
                else emptyNode
          }
        }
      )


    def toolBox(using api: ServerAPI, basePath: BasePath, panels: Panels, plugins: GUIPlugins) =
      val showExecution = () ⇒ ExecutionPanel.open
      new FileToolBox(
        tnSafePath,
        showExecution,
        tn match
          case f: TreeNode.File ⇒ PluginState(f.pluginState.isPlugin, f.pluginState.isPlugged)
          case _ ⇒ PluginState(false, false)
      )

    def render(using panels: Panels, api: ServerAPI, basePath: BasePath, plugins: GUIPlugins): HtmlElement = {
      div(display.flex, flexDirection.column,
        div(display.flex, alignItems.center, lineHeight := "27px",
          backgroundColor <-- treeNodeManager.selected.signal.map { s ⇒ if (isSelected(s)) toolBoxColor else "" },
          dropPairs,
          onDragStart --> { e ⇒
            e.dataTransfer.setData("text/plain", "nothing") //  FIREFOX TRICK
            draggedNode.set(Some(tnSafePath))
          },
          onDrop --> { e ⇒
            e.dataTransfer
            e.preventDefault()
            dropAction(treeNodeManager.directory.now() ++ tn.name, TreeNode.isDir(tn))
          },
          dirBox(tn).amend(cls := "file0", fileClick(todo), draggable := true),
          div(tn.name,
            cls.toggle("cursor-pointer") <-- multiTool.signal.map { mt ⇒
              mt == Off || mt == Paste
            },
            cls := "file1", fileClick(todo), draggable := true),
          i(timeOrSize(tn), cls := "file2"),
          button(cls := "bi-three-dots transparent-button", cursor.pointer, opacity := "0.5", onClick --> { _ ⇒
            currentSafePath.set(Some(tnSafePath))
            currentLine.update( cl => if cl == id then  -1 else id)
          })
        ),
        currentLine.signal.map { i ⇒ i == id }.expand(toolBox.contentRoot),
        treeNodeManager.directory.toObservable --> Observer { _ => currentLine.set(-1) }
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
      e.preventDefault()
    }
  )

  def dropAction(to: SafePath, isDir: Boolean)(using panels: Panels, api: ServerAPI, basePath: BasePath) =
    draggedNode.now().map {
      dragged ⇒
        if (isDir) {
          if (dragged != to) {
            //treeNodeTabs.saveAllTabs(() ⇒ {
            api.move(Seq(dragged -> (to ++ dragged.name))).foreach {
              b ⇒
                //treeNodeManager.invalidCache(to)
                //treeNodeManager.invalidCache(dragged)
                refresh
                panels.tabContent.checkTabs
            }
            //})
          }
        }
    }
    draggedNode.set(None)

  def drawNode(node: TreeNode, i: Int)(using panels: Panels, plugins: GUIPlugins, api: ServerAPI, basePath: BasePath) =
    node match
      case fn: TreeNode.File ⇒
        ReactiveLine(i, fn, TreeNodeType.File, () ⇒ displayNode(fn))
      case dn: TreeNode.Directory ⇒
        ReactiveLine(
          i,
          dn,
          TreeNodeType.Folder,
          () ⇒
            treeNodeManager switch (dn.name)
            treeWarning.set(true)
        )
}
