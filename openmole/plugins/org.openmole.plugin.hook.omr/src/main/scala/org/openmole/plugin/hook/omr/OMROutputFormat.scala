package org.openmole.plugin.hook.omr

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.workflow.format.CSVOutputFormat
import org.openmole.core.project._
import org.openmole.plugin.tool.json._
import io.circe._
import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.workflow.format.OutputFormat.*
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*
import org.openmole.core.project.Imports.ImportedFile

object OMROutputFormat {

  def methodFileName = "method.omr"

  def methodNameField = "method"
  def dataFileField = "data"
  def omrVersionField = "version"
  def omrVersion = "0.2"
  def scriptField = "script"
  def importsField = "imports"
  def openMOLEVersionField = "openmole-version"

  def startTime = "start-time"
  def saveTime = "save-time"

  implicit def outputFormat[MD](implicit methodData: MethodData[MD], scriptData: ScriptSourceData): OutputFormat[OMROutputFormat, MD] = new OutputFormat[OMROutputFormat, MD] {
    override def write(executionContext: HookExecutionContext)(format: OMROutputFormat, output: WritableOutput, content: OutputContent, method: MD): FromContext[Unit] = FromContext { p ⇒
      import p._
      import org.json4s._
      import org.json4s.jackson.JsonMethods._

      implicit val encoder = methodData.encoder

      output match {
        case WritableOutput.Display(_) ⇒
          implicitly[OutputFormat[CSVOutputFormat, Any]].write(executionContext)(CSVOutputFormat(), output, content, method).from(context)
        case WritableOutput.Store(file) ⇒
          def dataDirectory = "data"

          val directory = file.from(context)

          def methodFormat(json: Json, fileName: String, existingData: Seq[String]) = 
            import executionContext.timeService

            def updatedData = Json.fromValues((existingData :+ fileName).map(Json.fromString))

            json.deepDropNullValues.mapObject { o ⇒
              val o2 =
                o.add(methodNameField, Json.fromString(methodData.name(method)))
                  .add(dataFileField, updatedData)
                  .add(omrVersionField, Json.fromString(omrVersion))
                  .add(openMOLEVersionField, Json.fromString(org.openmole.core.buildinfo.version.value))
                  .add(startTime, Json.fromLong(executionContext.moleLaunchTime))
                  .add(saveTime, Json.fromLong(TimeService.currentTime))

              scriptData match {
                case data: ScriptSourceData.ScriptData if format.script ⇒
                  case class OMRImport(`import`: String, content: String)

                  val scriptContent = ScriptSourceData.scriptContent(scriptData)
                  val imports = Imports.directImportedFiles(data.script)

                  o2.add(scriptField, Json.fromString(scriptContent))
                    .add(importsField, Json.fromValues(imports.map(i ⇒ OMRImport(ImportedFile.identifier(i), i.file.content).asJson)))
                case _ ⇒ o2
              }
            }

          def parseExistingData(file: File): Seq[String] = 
            import org.json4s.DefaultReaders.*
            try 
              if file.exists 
              then
                val j = parse(file.content(gz = true)) 
                (j \ dataFileField).getAs[Seq[String]].get
              else Seq()
            catch 
             case e: Throwable => throw new InternalProcessingError(s"Error parsing existing method file ${file}", e)
            

          directory.withLockInDirectory {
            val methodFile = directory / methodFileName
            val existingData =
              if methodFile.exists then parseExistingData(methodFile) else Seq()

            content match 
              case PlainContent(variables) =>
                val fileName = s"$dataDirectory/data.json.gz"
                val dataFile = directory / dataDirectory / "data.json.gz"

                dataFile.withPrintStream(append = false, create = true, gz = true) { ps ⇒
                  ps.print(compact(render(variablesToJValue(variables))))
                }

                methodFile.withPrintStream(create = true, gz = true)(_.print(methodFormat(method.asJson, fileName, existingData).noSpaces))
              case NamedContent(variables, name) ⇒
                //def fromContextValue = name.stringValue.getOrElse(throw new InternalProcessingError("From context for name should have a clean string value"))
                  
                //val fileName = s"""$dataDirectory/${fromContextValue}.json.gz"""
                val fileName = s"$dataDirectory/${name.from(context)}.json.gz"
                val dataFile = directory / fileName 
                   
                dataFile.withPrintStream(append = false, create = true, gz = true) { ps ⇒
                  ps.print(compact(render(variablesToJValue(variables))))
                }

                methodFile.withPrintStream(create = true, gz = true)(_.print(methodFormat(method.asJson, fileName, existingData).noSpaces))
              case sections: SectionContent ⇒
                def sectionContent(sections: SectionContent) =
                  JObject(
                    sections.sections.map { section ⇒ section.name.from(context) -> variablesToJValue(section.variables) }.toList
                  )

                val fileName = s"$dataDirectory/data.json.gz"
                val f = directory / fileName

                f.withPrintStream(append = false, create = true, gz = true) { ps ⇒
                  ps.print(compact(render(sectionContent(sections))))
                }

                methodFile.withPrintStream(create = true, gz = true)(_.print(methodFormat(method.asJson, fileName, existingData).noSpaces))
          }
      }
    }

    override def validate(format: OMROutputFormat) = Validate.success
  }

  case class OMRData(method: String, fileName: String, version: String)

  def omrData(file: File): OMRData = {
    import io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
    val content = file.content(gz = true)
    val parseResult = parse(content)
    parseResult match {
      case Right(r) ⇒
        def readField(f: String) =
          r.hcursor.downField(f).as[String] match {
            case Right(r) ⇒ r
            case Left(e)  ⇒ throw new InternalProcessingError(s"Unable to get field $f in json:\n$content", e)
          }
        OMRData(
          method = readField(methodNameField),
          fileName = readField(dataFileField),
          version = readField(omrVersionField)
        )
      case Left(e) ⇒ throw new InternalProcessingError(s"Error while parsing omr file $file with content:\n$content", e)
    }

  }

}

case class OMROutputFormat(script: Boolean = true)
