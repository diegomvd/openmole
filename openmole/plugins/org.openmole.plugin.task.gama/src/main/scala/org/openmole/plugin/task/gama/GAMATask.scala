package org.openmole.plugin.task.gama

import java.io.FileNotFoundException

import monocle.Focus
import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.exception.{InternalProcessingError, UserBadDataError}
import org.openmole.core.fileservice.FileService
import org.openmole.core.networkservice.NetworkService
import org.openmole.core.preference.Preference
import org.openmole.core.serializer.SerializerService
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.validation._
import org.openmole.core.workspace.{TmpDirectory, Workspace}
import org.openmole.plugin.task.container
import org.openmole.plugin.task.container._
import org.openmole.plugin.task.external._
import org.openmole.tool.outputredirection.OutputRedirection

import scala.xml.XML

object GAMATask {

  implicit def isTask: InputOutputBuilder[GAMATask] = InputOutputBuilder(Focus[GAMATask](_.config))
  implicit def isExternal: ExternalBuilder[GAMATask] = ExternalBuilder(Focus[GAMATask](_.external))
  implicit def isInfo: InfoBuilder[GAMATask] = InfoBuilder(Focus[GAMATask](_.info))
  implicit def isMapped: MappedInputOutputBuilder[GAMATask] = MappedInputOutputBuilder(Focus[GAMATask](_.mapped))

  def inputXML = "/_model_input_.xml"
  def workspaceDirectory = "/_workspace_"

  def volumes(
    workspace: File,
    model:     String) = (model, Seq(workspace -> workspaceDirectory))

  def prepare(
    workspace:              File,
    model:                  String,
    experiment:             String,
    install:                Seq[String],
    installContainerSystem: ContainerSystem,
    image:                  ContainerImage,
    clearCache:             Boolean)(implicit tmpDirectory: TmpDirectory, serializerService: SerializerService, outputRedirection: OutputRedirection, networkService: NetworkService, threadProvider: ThreadProvider, preference: Preference, _workspace: Workspace, fileService: FileService) = {

    val (modelName, volumesValue) = volumes(workspace, model)

    def installCommands =
      install ++ Seq(s"gama-headless -xml '$experiment' '$workspaceDirectory/$modelName' '$inputXML'", s"ls '${inputXML}'")

    def error(retCode: Int) =
      retCode match {
        case 2 => Some(s"""the file $inputXML has not been generated by "gama-headless -xml"""")
        case _ => None
      }

    ContainerTask.prepare(installContainerSystem, image, installCommands, volumesValue.map { case (lv, cv) ⇒ lv.getAbsolutePath -> cv }, error, clearCache = clearCache)
  }

  def apply(
    project:                File,
    gaml:                   String,
    experiment:             String,
    finalStep:              FromContext[Int],
    seed:                   OptionalArgument[Val[Long]]      = None,
    frameRate:              OptionalArgument[Int]            = None,
    install:                Seq[String]                      = Seq.empty,
    containerImage:         ContainerImage                   = "gamaplatform/gama:1.8.1",
    memory:                 OptionalArgument[Information]    = None,
    version:                OptionalArgument[String]         = None,
    errorOnReturnValue:     Boolean                          = true,
    returnValue:            OptionalArgument[Val[Int]]       = None,
    stdOut:                 OptionalArgument[Val[String]]    = None,
    stdErr:                 OptionalArgument[Val[String]]    = None,
    environmentVariables:   Seq[EnvironmentVariable]         = Vector.empty,
    hostFiles:              Seq[HostFile]                    = Vector.empty,
    //    workDirectory:          OptionalArgument[String]       = None,
    clearContainerCache:    Boolean                          = false,
    containerSystem:        ContainerSystem                  = ContainerSystem.default,
    installContainerSystem: ContainerSystem                  = ContainerSystem.default)(implicit name: sourcecode.Name, definitionScope: DefinitionScope, newFile: TmpDirectory, _workspace: Workspace, preference: Preference, fileService: FileService, threadProvider: ThreadProvider, outputRedirection: OutputRedirection, networkService: NetworkService, serializerService: SerializerService): GAMATask = {

    if (!project.exists()) throw new UserBadDataError(s"The project directory you specify does not exist: ${project}")
    if (!(project / gaml).exists()) throw new UserBadDataError(s"The model file you specify does not exist: ${project / gaml}")

    val gamaContainerImage: ContainerImage =
      (version.option, containerImage) match {
        case (None, c) => c
        case (Some(v), c: DockerImage) => c.copy(tag = v)
        case (Some(_), _: SavedDockerImage) => throw new UserBadDataError(s"Can not set both, a saved docker image, and, set the version of the container.")
      }

    val preparedImage = prepare(project, gaml, experiment, install, installContainerSystem, gamaContainerImage, clearCache = clearContainerCache)

    GAMATask(
      project = project,
      gaml = gaml,
      experiment = experiment,
      finalStep = finalStep,
      seed = seed,
      frameRate = frameRate,
      image = preparedImage,
      memory = memory,
      errorOnReturnValue = errorOnReturnValue,
      returnValue = returnValue,
      stdOut = stdOut,
      stdErr = stdErr,
      hostFiles = hostFiles,
      environmentVariables = environmentVariables,
      containerSystem = containerSystem,
      config = InputOutputConfig(),
      external = External(),
      info = InfoConfig(),
      mapped = MappedInputOutputConfig()
    ) set (
        inputs ++= seed.option.toSeq,
        outputs ++= Seq(returnValue.option, stdOut.option, stdErr.option).flatten
      )
  }


  def modifyInputXML(values: Map[String, String], finalStep: Int, seed: Long, frameRate: Option[Int]) = {
    import xml._
    import xml.transform._

    def value(n: Node): Option[String] =
      if (n.label != "Parameter") None
      else n.attribute("var").flatMap(_.headOption.map(_.text)).flatMap(values.get) orElse n.attribute("name").flatMap(_.headOption.map(_.text)).flatMap(values.get)

    val rewrite =
      new RewriteRule {
        override def transform(n: Node): Seq[Node] =
          (n, value(n), frameRate) match {
            case (n: Elem, Some(v), _) ⇒ n.copy(attributes = n.attributes.remove("value").append(Attribute(null, "value", v, Null)))
            case (n: Elem, _, _) if n.label == "Simulation" =>
              n.copy(attributes =
                n.attributes
                  .remove("finalStep").append(Attribute(null, "finalStep", finalStep.toString, Null))
                  .remove("seed").append(Attribute(null, "seed", seed.toDouble.toString, Null))
              )
            case (n: Elem, _, Some(f)) if n.label == "Output" =>
              n.copy(attributes = n.attributes.remove("framerate").append(Attribute(null, "framerate", f.toString, Null)))
            case _                  ⇒ n
          }
      }

    new RuleTransformer(rewrite)
  }


  def acceptedOutputType(frame: Boolean): Seq[Manifest[_]] = {
    def scalar =
      Seq(
        manifest[Double],
        manifest[Int],
        manifest[String],
        manifest[Boolean]
      )

    if(!frame) scalar else scalar.map(_.arrayManifest)
  }

}

case class GAMATask(
  project:              File,
  gaml:                 String,
  experiment:           String,
  finalStep:            FromContext[Int],
  seed:                 OptionalArgument[Val[Long]],
  frameRate:            OptionalArgument[Int],
  image:                PreparedImage,
  memory:               OptionalArgument[Information],
  errorOnReturnValue:   Boolean,
  returnValue:          Option[Val[Int]],
  stdOut:               Option[Val[String]],
  stdErr:               Option[Val[String]],
  hostFiles:            Seq[HostFile],
  environmentVariables: Seq[EnvironmentVariable],
  containerSystem:      ContainerSystem,
  config:               InputOutputConfig,
  external:             External,
  info:                 InfoConfig,
  mapped:               MappedInputOutputConfig) extends Task with ValidateTask {

  lazy val containerPoolKey = ContainerTask.newCacheKey

  override def validate =
    container.validateContainer(Vector(), environmentVariables, external) ++ finalStep.validate ++ {
      import xml._

      val inputXML = XML.loadFile(image.file / _root_.container.FlatImage.rootfsName / GAMATask.inputXML)
      val parameters = (inputXML \ "Simulation" \ "Parameters" \ "Parameter").collect { case x: Elem => x }
      val outputs = (inputXML \ "Simulation" \ "Outputs" \ "Output").collect { case x: Elem => x }

      def gamaParameters = parameters.flatMap(e => e.attribute("var").flatMap(_.headOption).map(_.text)) ++ parameters.flatMap(e => e.attribute("name").flatMap(_.headOption).map(_.text))
      def gamaParameterByName(name: String) =
        parameters.filter(e => e.attribute("var").flatMap(_.headOption).map(_.text) == Some(name) || e.attribute("name").flatMap(_.headOption).map(_.text) == Some(name)).headOption

      def gamaOutputs = outputs.flatMap(e => e.attribute("name").flatMap(_.headOption).map(_.text))
      def gamaOutputByName(name: String) =
        outputs.filter(e => e.attribute("name").flatMap(_.headOption).map(_.text) == Some(name)).headOption


      def validateInputs = {
        def typeMatch(v: Val[_], t: String) =
          v match {
            case Val.caseInt(v) => t == "INT" | t == "FLOAT"
            case Val.caseDouble(v) => t == "INT" | t == "FLOAT"
            case Val.caseString(v) => t == "STRING"
            case Val.caseBoolean(v) => t == "BOOLEAN"
            case _ => false
          }

        mapped.inputs.flatMap { m =>
          gamaParameterByName(m.name) match {
            case Some(p) =>
              val gamaType = p.attribute("type").get.head.text
              if(!typeMatch(m.v, gamaType)) Some(new UserBadDataError(s"""Type mismatch between mapped input ${m.v} and input "${m.name}" of type ${gamaType}.""")) else None
            case None => Some(new UserBadDataError(s"""Mapped input "${m.name}" has not been found in the simulation among: ${gamaParameters.mkString(", ")}. Make sure it is defined in your gaml file"""))
          }
        }
      }

      def validateOutputs = {
        val acceptedOutputsTypes = GAMATask.acceptedOutputType(frameRate.option.isDefined)
        def accepted(c: Manifest[_]) = acceptedOutputsTypes.exists(t => t == c)

        mapped.outputs.flatMap { m =>
          gamaOutputByName(m.name) match {
            case Some(_) => if(!accepted(m.v.`type`.manifest)) Some(new UserBadDataError(s"""Mapped output ${m} type is not supported (frameRate is ${frameRate.option.isDefined}, it implies that supported types are: ${acceptedOutputsTypes.mkString(", ")})""")) else None
            case None => Some(new UserBadDataError(s"""Mapped output "${m.name}" has not been found in the simulation among: ${gamaOutputs.mkString(", ")}. Make sure it is defined in your gaml file."""))
          }
        }
      }

      if ((inputXML \ "Simulation").isEmpty) Seq(new UserBadDataError(s"Experiment ${experiment} has not been found, make sure it is defined in your gaml file"))
      else validateInputs ++ validateOutputs
    }

  override def process(executionContext: TaskExecutionContext) = FromContext { p ⇒
    import p._

    newFile.withTmpFile("inputs", ".xml") { inputFile ⇒
      val seedValue = math.abs(seed.map(_.from(context)).getOrElse(random().nextLong))

      def inputMap = mapped.inputs.map { m ⇒ m.name -> context(m.v).toString }.toMap
      val inputXML = GAMATask.modifyInputXML(inputMap, finalStep.from(context), seedValue, frameRate.option).transform(XML.loadFile(image.file / _root_.container.FlatImage.rootfsName / GAMATask.inputXML))
      def inputFileName = "/_inputs_openmole_.xml"
      def outputDirectory = "/_output_"

      inputFile.content =
        s"""<?xml version="1.0" encoding="UTF-8" standalone="no"?>${inputXML.mkString("")}"""

      val (_, volumes) = GAMATask.volumes(project, gaml)

      def launchCommand =
        memory.option match {
          case None => s"gama-headless -hpc 1 $inputFileName $outputDirectory"
          case Some(m) => s"gama-headless -m ${m.toMegabytes.toLong}m -hpc 1 $inputFileName $outputDirectory"
      }

      newFile.withTmpDir { tmpOutputDirectory =>
        def containerTask =
          ContainerTask(
            image = image,
            command = launchCommand,
            containerSystem = containerSystem,
            workDirectory = None,
            relativePathRoot = Some(GAMATask.workspaceDirectory),
            errorOnReturnValue = errorOnReturnValue,
            returnValue = returnValue,
            hostFiles = hostFiles,
            environmentVariables = environmentVariables,
            reuseContainer = true,
            stdOut = stdOut,
            stdErr = stdErr,
            config = config,
            external = external,
            info = info,
            containerPoolKey = containerPoolKey) set(
            resources += (inputFile, inputFileName, true),
            volumes.map { case (lv, cv) ⇒ resources.+=[ContainerTask](lv, cv, true) },
            resources += (tmpOutputDirectory, outputDirectory, true)
          )

        val resultContext = containerTask.process(executionContext).from(context)

        def gamaOutputFile =
          tmpOutputDirectory.
            listFilesSafe.
            filter(f => f.isFile && f.getName.startsWith("simulation-outputs") && f.getName.endsWith(".xml")).
            sortBy(_.getName).headOption.getOrElse(throw new InternalProcessingError(s"""GAMA result file (simulation-outputsXX.xml) has not been found, the content of the output folder is: [${tmpOutputDirectory.list.mkString(", ")}]"""))

        (mapped.outputs.isEmpty, frameRate.option) match {
          case (true, _) => resultContext
          case (false, None) =>
            import xml._

            def toVariable(v: Val[_], value: String) =
              v match {
                case Val.caseInt(v) => Variable(v, value.toInt)
                case Val.caseDouble(v) => Variable(v, value.toDouble)
                case Val.caseString(v) => Variable(v, value)
                case Val.caseBoolean(v) => Variable(v, value.toBoolean)
                case _ => throw new UserBadDataError(s"Unsupported type of output variable $v (supported types are Int, Double, String, Boolean)")
              }

            val outputs = Map[String, Val[_]]() ++ mapped.outputs.map { m => (m.name, m.v) }
            def outputValue(e: Elem) =
              for {
                a <- e.attribute("name").flatMap(_.headOption)
                value <- outputs.get(a.text)
              } yield toVariable(value, e.child.text)

            def extractOutputs(n: Node) =
              (n \ "Variable").flatMap {
                case e: Elem => outputValue(e)
                case _ => None
              }

            val simulationOutput = XML.loadFile(gamaOutputFile) \ "Step"

            resultContext ++ extractOutputs(simulationOutput.last)
          case (false, Some(f)) =>
            import xml._

            def toVariable(v: Val[_], value: Array[String]) =
              v match {
                case Val.caseArrayInt(v) => Variable(v, value.map(_.toInt))
                case Val.caseArrayDouble(v) => Variable(v, value.map(_.toDouble))
                case Val.caseArrayString(v) => Variable(v, value)
                case Val.caseArrayBoolean(v) => Variable(v, value.map(_.toBoolean))
                case _ => throw new UserBadDataError(s"Unsupported type of output variable $v (supported types are Array[Int], Array[Double], Array[String], Array[Boolean])")
              }

            def outputValue(e: Elem, name: String) =
              for {
                a <- e.attribute("name").flatMap(_.headOption)
                if a.text == name
              } yield e.child.text

            val simulationOutput = XML.loadFile(gamaOutputFile) \ "Step"

            resultContext ++ mapped.outputs.map { m =>
              val values =
                for {
                  o <- simulationOutput
                  v <- o \ "Variable"
                } yield
                  v match {
                    case o: Elem => outputValue(o, m.name)
                    case _ => None
                  }

              toVariable(m.v, values.flatten.toArray)
            }
          }
        }
    }
  }
}
