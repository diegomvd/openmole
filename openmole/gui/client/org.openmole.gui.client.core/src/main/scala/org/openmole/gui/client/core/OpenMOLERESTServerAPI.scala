package org.openmole.gui.client.core

/*
 * Copyright (C) 2022 Romain Reuillon
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.openmole.core.market.{MarketIndex, MarketIndexEntry}
import org.openmole.gui.client.core.NotificationManager.toService
import org.openmole.gui.shared.data.*
import org.openmole.gui.client.ext.*
import org.openmole.gui.client.ext.wizard.*
import org.openmole.gui.shared.api.*
import org.scalajs.dom.*

import scala.concurrent.duration.*
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import java.io.IOException
import scala.util.Failure
import com.raquo.laminar.api.L.*

class OpenMOLERESTServerAPI(fetch: CoreFetch, notificationService: NotificationService) extends ServerAPI:
  override def size(safePath: SafePath)(using BasePath) = fetch.futureError(_.size(safePath).future)
  override def copyFiles(paths: Seq[(SafePath, SafePath)], overwrite: Boolean)(using BasePath) = fetch.futureError(_.copyFiles(paths, overwrite).future)
  override def saveFile(safePath: SafePath, content: String, hash: Option[String], overwrite: Boolean)(using BasePath): Future[(Boolean, String)] = fetch.futureError(_.saveFile(safePath, content, hash, overwrite).future)
  override def createFile(path: SafePath, name: String, directory: Boolean)(using BasePath): Future[Boolean] = fetch.futureError(_.createFile(path, name, directory).future)
  override def extractArchive(path: SafePath, to: SafePath)(using BasePath): Future[Unit] = fetch.futureError(_.extractArchive(path, to).future)
  override def listFiles(path: SafePath, filter: FileSorting)(using BasePath): Future[FileListData] = fetch.futureError(_.listFiles(path, filter).future)
  override def listRecursive(path: SafePath, findString: Option[String])(using BasePath): Future[Seq[(SafePath, Boolean)]] = fetch.futureError(_.listRecursive(path, findString).future)
  override def move(paths: Seq[(SafePath, SafePath)])(using BasePath): Future[Unit] = fetch.futureError(_.move(paths).future)
  override def deleteFiles(path: Seq[SafePath])(using BasePath): Future[Unit] = fetch.futureError(_.deleteFiles(path).future)
  override def exists(path: SafePath)(using BasePath): Future[Boolean] = fetch.futureError(_.exists(path).future)
  override def temporaryDirectory()(using BasePath): Future[SafePath] = fetch.futureError(_.temporaryDirectory(()).future)
  override def executionState(line: Int, ids: Seq[ExecutionId])(using BasePath): Future[Seq[ExecutionData]] = fetch.futureError(_.executionState(line, ids).future)
  override def cancelExecution(id: ExecutionId)(using BasePath): Future[Unit] = fetch.futureError(_.cancelExecution(id).future)
  override def removeExecution(id: ExecutionId)(using BasePath): Future[Unit] = fetch.futureError(_.removeExecution(id).future)
  override def compileScript(script: SafePath)(using BasePath): Future[Option[ErrorData]] = fetch.futureError(_.compileScript(script).future, timeout = Some(600 seconds), warningTimeout = None)
  override def launchScript(script: SafePath, validate: Boolean)(using BasePath): Future[ExecutionId] = fetch.futureError(_.launchScript(script, validate).future, timeout = Some(600 seconds), warningTimeout = Some(300 seconds))
  override def clearEnvironmentError(environment: EnvironmentId)(using BasePath): Future[Unit] = fetch.futureError(_.clearEnvironmentErrors(environment).future)
  override def listEnvironmentError(environment: EnvironmentId, lines: Int)(using BasePath): Future[Seq[EnvironmentErrorGroup]] = fetch.futureError(_.listEnvironmentErrors(environment, lines).future)
  override def listPlugins()(using BasePath): Future[Seq[Plugin]] = fetch.futureError(_.listPlugins(()).future)
  override def addPlugin(path: SafePath)(using BasePath): Future[Seq[ErrorData]] = fetch.futureError(_.addPlugin(path).future)
  override def removePlugin(path: SafePath)(using BasePath): Future[Unit] = fetch.futureError(_.removePlugin(path).future)
  override def omrMethod(path: SafePath)(using BasePath): Future[String] = fetch.futureError(_.omrMethod(path).future)
  override def downloadHTTP(url: String, path: SafePath, extract: Boolean, overwrite: Boolean)(using BasePath): Future[Unit] = fetch.futureError(_.downloadHTTP(url, path, extract, overwrite).future)
  override def marketIndex()(using BasePath): Future[MarketIndex] = fetch.futureError(_.marketIndex(()).future)
  override def getMarketEntry(entry: MarketIndexEntry, safePath: SafePath)(using BasePath): Future[Unit] = fetch.futureError(_.getMarketEntry(entry, safePath).future)
  override def omSettings()(using BasePath): Future[OMSettings] = fetch.futureError(_.omSettings(()).future)
  override def shutdown()(using BasePath): Future[Unit] = fetch.futureError(_.shutdown(()).future)
  override def restart()(using BasePath): Future[Unit] = fetch.futureError(_.restart(()).future)

  override def isAlive()(using BasePath): Future[Boolean] =
    import scala.util.*
    fetch.future(_.isAlive(()).future, timeout = Some(3 seconds), warningTimeout = None, notifyError = false).transform {
      case Success(value) => Success(value)
      case Failure(_) => Success(false)
    }

  override def jvmInfos()(using BasePath): Future[JVMInfos] = fetch.futureError(_.jvmInfos(()).future)
  override def mdToHtml(safePath: SafePath)(using BasePath): Future[String] = fetch.futureError(_.mdToHtml(safePath).future)
  override def sequence(safePath: SafePath)(using BasePath): Future[SequenceData] = fetch.futureError(_.sequence(safePath).future)
  override def listNotification()(using BasePath): Future[Seq[NotificationEvent]] = fetch.futureError(_.listNotification(()).future)
  override def clearNotification(ids: Seq[Long])(using BasePath): Future[Unit] = fetch.futureError(_.clearNotification(ids).future)

  override def upload(
    files: Seq[(File, SafePath)],
    fileTransferState: ProcessState ⇒ Unit)(using basePath: BasePath): Future[Seq[(RelativePath, SafePath)]] = {
    val formData = new FormData

    val destinationPaths = files.unzip._2

    formData.append("fileType", destinationPaths.map(_.context.typeName).mkString(","))

    for
      (file, destination) <- files
    do
      formData.append(Utils.toURI(destination.path.value), file)

    val xhr = new XMLHttpRequest

    xhr.upload.onprogress = e ⇒
      fileTransferState(Processing((e.loaded.toDouble * 100 / e.total).toInt))

    xhr.upload.onloadend = e ⇒
      fileTransferState(Finalizing())

    xhr.onloadend = e ⇒
      fileTransferState(Processed())

    val p = scala.concurrent.Promise[Seq[(RelativePath, SafePath)]]()

    xhr.onload = e =>
      xhr.status match
        case s if s < 300 => p.success(files.map(_._1.path) zip destinationPaths)
        case s => p.failure(new IOException(s"Upload of files ${files} failed with status $s"))

    xhr.onerror = e =>
      p.failure(new IOException(s"Upload of files ${files} failed"))

    xhr.onabort = e =>
      p.failure(new IOException(s"Upload of file ${files} was aborted"))

    xhr.ontimeout = e =>
      p.failure(new IOException(s"Upload of file ${files} timed out"))


    xhr.open("POST", org.openmole.gui.shared.data.uploadFilesRoute, true)
    xhr.send(formData)

    p.future
  }.andThen {
    case Failure(t) => notificationService.notify(NotificationLevel.Error, s"Error while uploading file", div(ErrorData.stackTrace(ErrorData(t))))
  }

  override def download(
    safePath: SafePath,
    fileTransferState: ProcessState ⇒ Unit = _ ⇒ (),
    hash: Boolean = false)(using basePath: BasePath): Future[(String, Option[String])] =
    size(safePath).flatMap { size ⇒
      val xhr = new XMLHttpRequest

      xhr.onprogress = (e: ProgressEvent) ⇒
        fileTransferState(Processing((e.loaded * 100 / size).toInt))

      xhr.onloadend = e ⇒
        fileTransferState(Processed())

      val p = scala.concurrent.Promise[(String, Option[String])]()

      xhr.onload = e =>
        val h = Option(xhr.getResponseHeader(hashHeader))
        xhr.status match
          case s if s < 300 => p.success((xhr.responseText, h))
          case s => p.failure(new IOException(s"Download of file ${safePath} failed with error ${s}"))

      xhr.onerror = e =>
        p.failure(new IOException(s"Download of file ${safePath} failed"))

      xhr.onabort = e =>
        p.failure(new IOException(s"Download of file ${safePath} was aborted"))

      xhr.ontimeout = e =>
        p.failure(new IOException(s"Download of file ${safePath} timed out"))


      xhr.open("GET", downloadFile(Utils.toURI(safePath.path.value.map { Encoding.encode }), hash = hash, fileType = safePath.context), true)
      xhr.send()

      p.future
    }.andThen {
      case Failure(t) => notificationService.notify(NotificationLevel.Error, s"Error while downloading file", div(ErrorData.stackTrace(ErrorData(t))))
    }

  override def fetchGUIPlugins(f: GUIPlugins ⇒ Unit)(using BasePath) =
    def successOrNotify[T](t: util.Try[T]) =
      t match
        case util.Success(r) => Some(r)
        case util.Failure(t) =>
          notificationService.notify(NotificationLevel.Error, s"Error while instantiating plugin", div(ErrorData.stackTrace(ErrorData(t))))
          None

    fetch.futureError(_.guiPlugins(()).future).map { p ⇒
      val authFact = p.authentications.flatMap { gp ⇒ successOrNotify(Plugins.buildJSObject[AuthenticationPluginFactory](gp)) }
      val wizardFactories = p.wizards.flatMap { gp ⇒ successOrNotify(Plugins.buildJSObject[WizardPluginFactory](gp)) }
      val analysisFactories = p.analysis.flatMap { (method, gp) ⇒ successOrNotify(Plugins.buildJSObject[MethodAnalysisPlugin](gp)).map(p => (method, p)) }.toMap
      f(GUIPlugins(authFact, wizardFactories, analysisFactories))
    }