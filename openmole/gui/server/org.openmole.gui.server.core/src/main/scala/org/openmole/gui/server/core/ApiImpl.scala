package org.openmole.gui.server.core

import java.io.File
import java.text.SimpleDateFormat
import org.openmole.core.buildinfo
import org.openmole.core.event._
import org.openmole.core.pluginmanager._
import org.openmole.gui.ext.data
import org.openmole.gui.ext.data._

import java.io._
import java.net.URL
import java.nio.file._
import java.util.zip.GZIPInputStream
import org.openmole.core.compiler._
import org.openmole.core.expansion.ScalaCompilation
import org.openmole.core.market.{ MarketIndex, MarketIndexEntry }

import scala.util.{ Failure, Success, Try }
import org.openmole.core.workflow.mole.{ MoleExecution, MoleExecutionContext, MoleServices }
import org.openmole.tool.stream.StringPrintStream

import scala.concurrent.stm._
import org.openmole.tool.tar._
import org.openmole.core.outputmanager.OutputManager
import org.openmole.core.module
import org.openmole.core.market
import org.openmole.core.preference.{ ConfigurationString, Preference, PreferenceLocation }
import org.openmole.core.project._
import org.openmole.core.services.Services
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.dsl._
import org.openmole.core.workspace.{ TmpDirectory, Workspace }
import org.openmole.core.fileservice.FileServiceCache
import org.openmole.gui.ext.server.{ GUIPluginRegistry, utils }
import org.openmole.gui.ext.server.utils._
import org.openmole.gui.server.core.GUIServer.{ ApplicationControl, lockFile }
import org.openmole.plugin.hook.omr.OMROutputFormat
import org.openmole.tool.crypto.Cypher
import org.openmole.tool.outputredirection.OutputRedirection

import scala.collection.JavaConverters._

/*
 * Copyright (C) 21/07/14 // mathieu.leclaire@openmole.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See theMarketIndexEntry
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

class ApiImpl(val services: Services, applicationControl: Option[ApplicationControl]) {

  import ExecutionInfo._

  val outputSize = PreferenceLocation[Int]("gui", "outputsize", Some(10 * 1024 * 1024))

  val execution = new Execution

  //GENERAL
  def settings: OMSettings = {
    import services._
    import org.openmole.gui.ext.data.ServerFileSystemContext.project

    OMSettings(
      utils.projectsDirectory.toSafePath,
      buildinfo.version.value,
      buildinfo.name,
      org.openmole.gui.ext.server.utils.formatDate(buildinfo.BuildInfo.buildTime),
      buildinfo.development
    )
  }

  def shutdown() = applicationControl.foreach(_.stop())
  def restart() = applicationControl.foreach(_.restart())

  def isAlive() = true

  def jvmInfos() = {
    val runtime = Runtime.getRuntime
    val totalMemory = runtime.totalMemory
    val allocatedMemory = totalMemory - runtime.freeMemory
    val javaVersion = System.getProperty("java.version")
    val jvmName = System.getProperty("java.vm.name")

    JVMInfos(
      javaVersion,
      jvmName,
      Runtime.getRuntime.availableProcessors,
      allocatedMemory,
      totalMemory
    )
  }

  //AUTHENTICATIONS
  def renameKey(keyName: String, newName: String): Unit = {
    import services._
    Files.move(new File(authenticationKeysFile, keyName).toPath, new File(authenticationKeysFile, newName).toPath, StandardCopyOption.REPLACE_EXISTING)
  }

  //WORKSPACE
  def isPasswordCorrect(pass: String): Boolean = Preference.passwordIsCorrect(Cypher(pass), services.preference)

  //def passwordState = Utils.passwordState

  def resetPassword(): Unit = {
    import services._
    org.openmole.core.services.Services.resetPassword
  }

  // FILES
  def createFile(safePath: SafePath, name: String, directory: Boolean): Boolean =
    import services._
    import org.openmole.gui.ext.data.ServerFileSystemContext.project
    if directory
    then new File(safePath.toFile, name).createNewFile
    else new File(safePath.toFile, name).mkdirs


//  def deleteFile(safePath: SafePath, context: ServerFileSystemContext): Unit = {
//    unplug(safePath)
//    import services._
//    utils.deleteFile(safePath, context)
//  }

  def deleteFiles(safePaths: Seq[SafePath]): Unit = {
    import services.*
    import org.openmole.tool.file.*
    
    val allPlugins = listPlugins()
    import org.openmole.gui.ext.data.ServerFileSystemContext.project

    def unplug(f: File) =
      if utils.isPlugged(f, allPlugins.toSeq)(workspace) then removePlugin(utils.fileToSafePath(f))

    for
      safePath <- safePaths
      file = safePathToFile(safePath)
    do
     if file.isDirectory
     then file.applyRecursive(unplug)
     else unplug(file)

    utils.deleteFiles(safePaths)
  }

  private def getExtractedArchiveTo(from: File, to: File)(implicit context: ServerFileSystemContext): Seq[SafePath] = {
    import services._
    extractArchiveFromFiles(from, to)
    to.listFilesSafe.map(utils.fileToSafePath).toSeq
  }

  def unknownFormat(name: String) = Some(ErrorData("Unknown compression format for " + name))

  private def extractArchiveFromFiles(from: File, to: File)(implicit context: ServerFileSystemContext) = {
    Try {
      val ext = FileExtension(from.getName)
      ext match {
        case org.openmole.gui.ext.data.Tar ⇒
          from.extract(to)
          to.applyRecursive((f: File) ⇒ f.setWritable(true))
        case TarGz ⇒
          from.extractUncompress(to, true)
          to.applyRecursive((f: File) ⇒ f.setWritable(true))
        case Zip ⇒ utils.unzip(from, to)
        case TarXz ⇒
          from.extractUncompressXZ(to, true)
          to.applyRecursive((f: File) ⇒ f.setWritable(true))
        case _ ⇒ throw new Throwable("Unknown compression format for " + from.getName)
      }
    } match {
      case Success(_) ⇒ None
      case Failure(t) ⇒ Some(ErrorData(t))
    }
  }

  def extract(safePath: SafePath) = {
    import services.*
    FileExtension(safePath.name) match {
      case FileExtension.TGZ | FileExtension.TAR | FileExtension.ZIP | FileExtension.TXZ ⇒
        val archiveFile = safePathToFile(safePath)
        val toFile: File = safePathToFile(safePath.parent)
        extractArchiveFromFiles(archiveFile, toFile)(ServerFileSystemContext.project)
      case _ ⇒ unknownFormat(safePath.name)
    }
  }

  def temporaryDirectory(): SafePath =
    import services.*
    import org.openmole.gui.ext.data.ServerFileSystemContext.absolute
    val dir = services.tmpDirectory.newDir("openmoleGUI")
    dir.mkdirs()
    dir.toSafePath

  def exists(safePath: SafePath): Boolean = {
    import services._
    utils.exists(safePath)
  }

//  def existsExcept(exception: SafePath, exceptItSelf: Boolean): Boolean = {
//    import services._
//    val li = listFiles(exception.parent)
//    val count = li.list.count(l ⇒ treeNodeToSafePath(l, exception.parent).path == exception.path)
//
//    val bound = if (exceptItSelf) 1 else 0
//    if (count > bound) true else false
//    // utils.existsExcept(exception, exceptItSelf)
//  }

//  def copyFromTmp(tmpSafePath: SafePath, filesToBeMovedTo: Seq[SafePath]): Unit = {
//    import services._
//    utils.copyFromTmp(tmpSafePath, filesToBeMovedTo)
//  }

//  def copyAllTmpTo(tmpSafePath: SafePath, to: SafePath): Unit = {
//    import services._
//    utils.copyAllFromTmp(tmpSafePath, to)
//  }

  def copyFiles(safePaths: Seq[SafePath], to: SafePath, overwrite: Boolean) = {
    import services._
    utils.copyFilesTo(safePaths, to, overwrite)
  }

//  def testExistenceAndCopyProjectFilesTo(safePaths: Seq[SafePath], to: SafePath): Seq[SafePath] = {
//    import services._
//    utils.testExistenceAndCopyProjectFilesTo(safePaths, to)
//  }

  // Test whether safePathToTest exists in "in"
//  def extractAndTestExistence(safePathToTest: SafePath, in: SafePath): Seq[SafePath] = {
//    import services.*
//    // import org.openmole.gui.ext.data.ServerFileSystemContext.absolute
//
//    def test(sps: Seq[SafePath], inDir: SafePath = in) = {
//      import org.openmole.gui.ext.data.ServerFileSystemContext.absolute
//
//      val toTest: Seq[SafePath] =
//        if (sps.size == 1) sps.flatMap { f ⇒
//          if (f.toFile.isDirectory) f.toFile.listFilesSafe.map { _.toSafePath }
//          else Seq(f)
//        } else sps
//
//      toTest.filter { sp ⇒ exists(inDir ++ sp.name) }.map { sp ⇒ inDir ++ sp.name }
//    }
//
//    FileType(safePathToTest) match {
//      case Archive ⇒
//        // case j: JavaLikeLanguage ⇒ test(Seq(safePathToTest))
//        // val emptyFile = new File("")
//        val from: File = safePathToFile(safePathToTest)
//        val to: File = safePathToFile(safePathToTest.parent)
//        val extracted = getExtractedArchiveTo(from, to)(ServerFileSystemContext.absolute).filterNot {
//          _ == safePathToTest
//        }
//        val toTest = in ++ safePathToTest.nameWithNoExtension
//        val toTestFile: File = safePathToFile(in ++ safePathToTest.nameWithNoExtension)
//        new File(to, from.getName).recursiveDelete
//
//        if (toTestFile.exists) test(extracted, toTest)
//        else Seq()
//      case _ ⇒ test(Seq(safePathToTest))
//    }
//  }

  def listFiles(sp: SafePath, fileFilter: data.FileFilter = data.FileFilter.defaultFilter): ListFilesData = {
    import services.*
    utils.listFiles(sp, fileFilter, listPlugins())
  }

   def recursiveListFiles(sp: SafePath, findString: Option[String]): Seq[(SafePath, Boolean)] = {
    import services._
    utils.recursiveListFiles(sp, findString)
  }

  def isEmpty(sp: SafePath): Boolean = {
    import services._
    import org.openmole.gui.ext.data.ServerFileSystemContext.project
    val f: File = safePathToFile(sp)
    f.isDirectoryEmpty
  }

  def move(from: SafePath, to: SafePath): Unit = {
    import services._
    import org.openmole.gui.ext.data.ServerFileSystemContext.project
    val fromFile = safePathToFile(from)
    val toFile = safePathToFile(to)

    fromFile.move(to.toFile)
    //utils.move(fromFile, toFile)
  }

//  def renameFile(safePath: SafePath, name: String): SafePath = {
//    import services._
//    import org.openmole.gui.ext.data.ServerFileSystemContext.project
//
//    val targetFile = new File(safePath.parent.toFile, name)
//
//    Files.move(safePath.toFile, targetFile, StandardCopyOption.REPLACE_EXISTING)
//    targetFile.toSafePath
//  }

  def duplicate(safePath: SafePath, newName: String): SafePath = {
    import services._
    utils.copyProjectFile(safePath, newName, followSymlinks = true)
  }

  def mdToHtml(safePath: SafePath): String = {
    import services._
    import org.openmole.gui.ext.data.ServerFileSystemContext.project
    MarkDownProcessor(safePathToFile(safePath).content)
  }



  def saveFile(path: SafePath, fileContent: String, hash: Option[String], overwrite: Boolean): (Boolean, String) = {
    import services._
    import org.openmole.gui.ext.data.ServerFileSystemContext.project

    val file = safePathToFile(path)

    file.withLock { _ ⇒
      def save() = {
        file.content = fileContent

        def newHash = services.fileService.hashNoCache(file).toString

        (true, newHash)
      }

      if (overwrite) save()
      else hash match {
        case Some(expectedHash: String) ⇒
          val hashOnDisk = services.fileService.hashNoCache(file).toString
          if (hashOnDisk == expectedHash) save() else (false, hashOnDisk)
        case _ ⇒ save()
      }
    }
  }

  //  def saveFiles(fileContents: Seq[AlterableFileContent]): Seq[(SafePath, Boolean)] = fileContents.map { fc ⇒
  //    fc.path -> saveFile(fc.path, fc.content, Some(fc.hash), false)
  //  }

  def size(safePath: SafePath): Long = {
    import services._
    import org.openmole.gui.ext.data.ServerFileSystemContext.project
    safePathToFile(safePath).length
  }

  def sequence(safePath: SafePath, separator: Char = ','): SequenceData = {
    import services._
    import org.openmole.gui.ext.data.ServerFileSystemContext.project
    val content = safePath.toFile.content.split("\n")
    val regex = """\[[^\]]+\]|[+-]?[0-9][0-9]*\.?[0-9]*([Ee][+-]?[0-9]+)?|true|false""".r

    content.headOption.map { h ⇒
      SequenceData(
        h.split(',').toSeq,
        content.tail.map { row ⇒ regex.findAllIn(row).toSeq }.toSeq
      )
    }.getOrElse(SequenceData())
  }

  // EXECUTIONS
  def cancelExecution(id: ExecutionId): Unit = execution.cancel(id)

  def removeExecution(id: ExecutionId): Unit = execution.remove(id)

  def compileScript(scriptData: ScriptData) = {
    val (execId, outputStream) = compilationData(scriptData)
    synchronousCompilation(execId, scriptData, outputStream)
  }

  def runScript(scriptData: ScriptData, validateScript: Boolean) = {
    asynchronousCompilation(
      scriptData,
      Some(execId ⇒ execution.compiled(execId)),
      Some(processRun(_, _, validateScript))
    )
  }

  private def compilationData(scriptData: ScriptData) = {
    import services._
    (ExecutionId(DataUtils.uuID) /*, safePathToFile(scriptData.scriptPath)*/ , StringPrintStream(Some(preference(outputSize))))
  }

  def synchronousCompilation(
    execId:       ExecutionId,
    scriptData:   ScriptData,
    outputStream: StringPrintStream,
    onCompiled:   Option[ExecutionId ⇒ Unit]                  = None,
    onEvaluated:  Option[(MoleExecution, ExecutionId) ⇒ Unit] = None): Option[ErrorData] = {

    import org.openmole.gui.ext.data.ServerFileSystemContext.project

    def error(t: Throwable): ErrorData = {
      t match {
        case ce: Interpreter.CompilationError ⇒
          def toErrorWithLocation(em: Interpreter.ErrorMessage) =
            ErrorWithLocation(em.rawMessage, em.position.map {
              _.line
            }, em.position.map {
              _.start
            }, em.position.map {
              _.end
            })

          ErrorData(ce.errorMessages.map(toErrorWithLocation), t)
        case _ ⇒ ErrorData(t)
      }
    }

    def message(message: String) = MessageErrorData(message, None)

    val script: File = {
      import services._
      safePathToFile(scriptData.scriptPath)
    }

    val executionOutputRedirection = OutputRedirection(outputStream)
    val executionTmpDirectory = services.tmpDirectory.newDir("execution")

    val runServices = {
      import services._
      Services.copy(services)(outputRedirection = executionOutputRedirection, newFile = TmpDirectory(executionTmpDirectory), fileServiceCache = FileServiceCache())
    }

    try {
      Project.compile(script.getParentFileSafe, script)(runServices) match {
        case ScriptFileDoesNotExists() ⇒ Some(message("Script file does not exist"))
        case ErrorInCode(e)            ⇒ Some(error(e))
        case ErrorInCompiler(e)        ⇒ Some(error(e))
        case compiled: Compiled ⇒
          import runServices._

          val executionServices =
            MoleServices.create(
              applicationExecutionDirectory = runServices.workspace.tmpDirectory,
              moleExecutionDirectory = Some(executionTmpDirectory),
              outputRedirection = Some(executionOutputRedirection),
              compilationContext = Some(compiled.compilationContext))

          onCompiled.foreach {
            _(execId)
          }
          catchAll(OutputManager.withStreamOutputs(outputStream, outputStream)(compiled.eval(Seq.empty)(runServices))) match {
            case Failure(e) ⇒ Some(error(e))
            case Success(dsl) ⇒
              Try(DSL.toPuzzle(dsl).toExecution()(executionServices)) match {
                case Success(ex) ⇒
                  onEvaluated.foreach {
                    _(ex, execId)
                  }
                  None
                case Failure(e) ⇒
                  MoleServices.clean(executionServices)
                  Some(error(e))
              }
          }
      }

    }
    catch {
      case t: Throwable ⇒ Some(error(t))
    }

  }

  def asynchronousCompilation(scriptData: ScriptData, onEvaluated: Option[ExecutionId ⇒ Unit] = None, onCompiled: Option[(MoleExecution, ExecutionId) ⇒ Unit] = None): Unit = {
    import services._
    val (execId, outputStream) = compilationData(scriptData)

    import org.openmole.gui.ext.data.ServerFileSystemContext.project

    val content = safePathToFile(scriptData.scriptPath).content

    execution.addStaticInfo(execId, StaticExecutionInfo(scriptData.scriptPath, content, System.currentTimeMillis()))
    execution.addOutputStreams(execId, outputStream)

    val compilationFuture: java.util.concurrent.Future[_] = threadProvider.submit(ThreadProvider.maxPriority) { () ⇒
      val errorData = synchronousCompilation(execId, scriptData, outputStream, onEvaluated, onCompiled)
      errorData.foreach { ed ⇒ execution.addError(execId, Failed(Vector.empty, ed, Seq.empty)) }
    }

    execution.addCompilation(execId, compilationFuture)
  }

  def processRun(ex: MoleExecution, execId: ExecutionId, validateScript: Boolean) = {
    import services._
    val envIds = (ex.allEnvironments).map {
      env ⇒ EnvironmentId(DataUtils.uuID) → env
    }
    execution.addRunning(execId, envIds)
    envIds.foreach {
      case (envId, env) ⇒ env.listen(execution.environmentListener(envId))
    }

    catchAll(ex.start(validateScript)) match {
      case Failure(e) ⇒ execution.addError(execId, Failed(Vector.empty, ErrorData(e), Seq.empty))
      case Success(_) ⇒
        val inserted = execution.addMoleExecution(execId, ex)
        if (!inserted) ex.cancel
    }
  }

  def allStates(lines: Int) = execution.allStates(lines)

  def staticInfos() = execution.staticInfos()

  def clearEnvironmentErrors(environmentId: EnvironmentId): Unit = execution.deleteEnvironmentErrors(environmentId)

  def runningErrorEnvironmentData(environmentId: EnvironmentId, lines: Int): EnvironmentErrorData = atomic {
    implicit ctx ⇒
      val environmentErrors = execution.environmentErrors(environmentId)

      def groupedErrors = environmentErrors.groupBy {
        _.errorMessage
      }.toSeq.map {
        case (_, err) ⇒
          val dates = err.map {
            _.date
          }.sorted
          (err.head, dates.max, dates.size)
      }.takeRight(lines)

      EnvironmentErrorData(groupedErrors)
    //    EnvironmentErrorData(Seq(
    //      (EnvironmentError(environmentId, "YOur error man", Error("stansatienasitenasiruet a anuisetnasirte "), 2334454L, ErrorLevel()), 33345L, 2),
    //      (EnvironmentError(environmentId, "YOur error man 4", Error("stansatienasitenasiruet a anuaeiaiueaiueaieisetnasirte "), 2334454L, ErrorLevel()), 31345L, 1)
    //    ))
  }

  def marketIndex() = {
    import services._
    def mapToMd(marketIndex: MarketIndex) =
      marketIndex.copy(entries = marketIndex.entries.map {
        e ⇒
          e.copy(readme = e.readme.map {
            MarkDownProcessor(_)
          })
      })

    mapToMd(market.marketIndex)
  }

  def getMarketEntry(entry: MarketIndexEntry, path: SafePath) = {
    import services._
    import org.openmole.gui.ext.data.ServerFileSystemContext.project

    market.downloadEntry(entry, safePathToFile(path))
    //autoAddPlugins(path)
  }

  //PLUGINS
//  private def autoAddPlugins(path: SafePath) = {
//    import services._
//    import org.openmole.gui.ext.data.ServerFileSystemContext.project
//
//    val file = safePathToFile(path)
//
//    def recurse(f: File): List[File] = {
//      val subPlugins: List[File] = if (f.isDirectory) f.listFilesSafe.toList.flatMap(recurse) else Nil
//      PluginManager.listBundles(f).toList ::: subPlugins
//    }
//
//    module.addPluginsFiles(recurse(file), false, module.moduleDirectory)
//  }

  private def toPluginList(currentPlugins: Seq[String]) =
    import services.*
    import org.openmole.gui.ext.data.ServerFileSystemContext.project
    val currentPluginsSafePath = currentPlugins.map { s ⇒ SafePath(s.split("/")) }

    currentPluginsSafePath.map { csp ⇒
      val file = safePathToFile(csp)
      val date = org.openmole.gui.ext.server.utils.formatDate(file.lastModified)
      Plugin(csp, date, file.exists && PluginManager.bundle(file).isDefined)
    }

  def activatePlugins =
    import services.*
    import org.openmole.gui.ext.data.ServerFileSystemContext.project
    val plugins = services.preference.preferenceOption(GUIServer.plugins).getOrElse(Seq()).map(s ⇒ safePathToFile(SafePath(s.split("/")))).filter(_.exists)
    PluginManager.tryLoad(plugins)

  private def isPlugged(safePath: SafePath) =
    import services._
    import org.openmole.gui.ext.data.ServerFileSystemContext.project
    utils.isPlugged(safePathToFile(safePath), listPlugins())(workspace)

  private def updatePluggedList(set: Seq[String] ⇒ Seq[String]): Unit =
    import services._
    preference.updatePreference(GUIServer.plugins)(p => Some(set(p.getOrElse(Seq()))))

  def addPlugin(safePath: SafePath): Seq[ErrorData] = 
    import services._
    import org.openmole.gui.ext.data.ServerFileSystemContext.project
    val errors = utils.addPlugin(safePath)
    if (errors.isEmpty) { updatePluggedList { pList ⇒ (pList :+ safePath.path.mkString("/")).distinct } }
    errors

  def listPlugins(): Seq[Plugin] =
    val currentPlugins = services.preference.preferenceOption(GUIServer.plugins).getOrElse(Seq())
    toPluginList(currentPlugins)

  def removePlugin(safePath: SafePath) =
    import services.*
    updatePluggedList { _.filterNot(_ == safePath.path.mkString("/")) }
    utils.removePlugin(safePath)(workspace)

  def pluginRoutes =
    GUIPluginRegistry.all.flatMap(_.router).map(p => p(services))

  //GUI OM PLUGINS
  def getGUIPlugins(): PluginExtensionData = {
    PluginExtensionData(GUIPluginRegistry.authentications, GUIPluginRegistry.wizards)
  }

  def isOSGI(safePath: SafePath): Boolean = {
    import services._
    import org.openmole.gui.ext.data.ServerFileSystemContext.project

    PluginManager.isOSGI(safePathToFile(safePath))
  }

  // Analysis plugins
  def findAnalysisPlugin(result: SafePath): Option[GUIPluginAsJS] =
    import services.*
    val omrFile = safePathToFile(result)
    val methodName = OMROutputFormat.methodName(omrFile)
    GUIPluginRegistry.analysis.find(_._1 == methodName).map(_._2)

  //MODEL WIZARDS

  //Extract models from an archive
  def models(archivePath: SafePath): Seq[SafePath] = {
    val toDir = archivePath.toNoExtention
    // extractTGZToAndDeleteArchive(archivePath, toDir)
    (for {
      tnd ← listFiles(toDir).list if FileType.isSupportedLanguage(tnd.name)
    } yield tnd).map {
      nd ⇒ toDir ++ nd.name
    }
  }

  def expandResources(resources: Resources): Resources = {
    import services._
    import org.openmole.gui.ext.data.ServerFileSystemContext.project

    val paths = resources.all.map(_.safePath).distinct.map {
      sp ⇒ Resource(sp, sp.toFile.length)
    }
    val implicitResource = resources.implicits.map {
      r ⇒ Resource(r.safePath, r.safePath.toFile.length)
    }

    Resources(
      paths,
      implicitResource,
      paths.size + implicitResource.size
    )
  }

  // FIXME use network service provider
  def downloadHTTP(url: String, path: SafePath, extract: Boolean): Option[ErrorData] =
    import services.*
    import org.openmole.tool.stream.*

    val result =
      Try {
        val checkedURL =
          java.net.URI.create(url).getScheme match {
            case null ⇒ "http://" + url
            case _    ⇒ url
          }

        gridscale.http.getResponse(checkedURL) {
          response ⇒
            def extractName = checkedURL.split("/").last

            val name =
              response.headers.flatMap {
                case ("Content-Disposition", value) ⇒
                  value.split(";").map(_.split("=")).find(_.head.trim == "filename").map {
                    filename ⇒
                      val name = filename.last.trim
                      if (name.startsWith("\"") && name.endsWith("\"")) name.drop(1).dropRight(1) else name
                  }
                case _ ⇒ None
              }.headOption.getOrElse(extractName)

            val is = response.inputStream

            if (extract) {
              val dest = safePathToFile(path)
              val tis = new TarInputStream(new GZIPInputStream(is))
              try tis.extract(dest)
              finally tis.close
            }
            else {
              val dest = safePathToFile(path / name)
              dest.withOutputStream(os ⇒ copy(is, os))
            }
        }
      }

    result match {
      case Success(value) ⇒ None
      case Failure(e)     ⇒ Some(ErrorData(e))
    }


}
