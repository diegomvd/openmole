package org.openmole.plugin.task.gama

import monocle.macros._
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

  implicit def isTask: InputOutputBuilder[GAMATask] = InputOutputBuilder(GAMATask.config)
  implicit def isExternal: ExternalBuilder[GAMATask] = ExternalBuilder(GAMATask.external)
  implicit def isInfo = InfoBuilder(info)
  implicit def isMapped = MappedInputOutputBuilder(GAMATask.mapped)

  def gamaImage(version: String) = DockerImage("gamaplatform/gama", version)

  def inputXML = "/_model_input_.xml"
  def workspaceDirectory = "/_workspace_"

  def volumes(
    workspace: File,
    model:     OptionalArgument[String]) =
    model.option match {
      case Some(model) ⇒ (model, Seq(workspace -> workspaceDirectory))
      case None        ⇒ (workspace.getName, Seq(workspace -> s"$workspaceDirectory/${workspace.getName}"))
    }

  def prepare(
    workspace:              File,
    model:                  OptionalArgument[String],
    experiment:             String,
    install:                Seq[String],
    installContainerSystem: ContainerSystem,
    version:                String)(implicit tmpDirectory: TmpDirectory, serializerService: SerializerService, outputRedirection: OutputRedirection, networkService: NetworkService, threadProvider: ThreadProvider, preference: Preference, _workspace: Workspace) = {

    val (modelName, volumesValue) = volumes(workspace, model)

    def installCommands =
      install ++ Seq(s"gama-headless -xml $experiment $workspaceDirectory/$modelName $inputXML", s"ls ${inputXML}")

    ContainerTask.prepare(installContainerSystem, gamaImage(version), installCommands, volumesValue.map { case (lv, cv) ⇒ lv.getAbsolutePath -> cv })
  }

  def apply(
    workspace:              File,
    experiment:             String,
    finalStep:              FromContext[Int],
    model:                  OptionalArgument[String]      = None,
    seed:                   OptionalArgument[Val[Long]]   = None,
    frameRate:              OptionalArgument[Int]         = None,
    install:                Seq[String]                   = Seq.empty,
    version:                String                        = "1.8.0",
    errorOnReturnValue:     Boolean                       = true,
    returnValue:            OptionalArgument[Val[Int]]    = None,
    stdOut:                 OptionalArgument[Val[String]] = None,
    stdErr:                 OptionalArgument[Val[String]] = None,
    environmentVariables:   Seq[EnvironmentVariable]      = Vector.empty,
    hostFiles:              Seq[HostFile]                 = Vector.empty,
    workDirectory:          OptionalArgument[String]      = None,
    containerSystem:        ContainerSystem               = ContainerSystem.default,
    installContainerSystem: ContainerSystem               = ContainerSystem.default)(implicit name: sourcecode.Name, definitionScope: DefinitionScope, newFile: TmpDirectory, _workspace: Workspace, preference: Preference, fileService: FileService, threadProvider: ThreadProvider, outputRedirection: OutputRedirection, networkService: NetworkService, serializerService: SerializerService): GAMATask = {

    (model.option.isDefined, workspace.isDirectory) match {
      case (false, true) ⇒ throw new UserBadDataError(s"""$workspace is a directory, in this case you must specify you model path, model = "model.gaml"""")
      case (true, false) ⇒ throw new UserBadDataError(s"""$workspace is a file in this case you cannot provide a model path (model = "$model")""")
      case (true, true) if !(workspace / model.get).exists() ⇒ throw new UserBadDataError(s"The model file you specify does not exist: ${workspace / model.get}")
      case _ ⇒
    }

    val preparedImage = prepare(workspace, model, experiment, install, installContainerSystem, version)

    GAMATask(
      workspace = workspace,
      model = model,
      experiment = experiment,
      finalStep = finalStep,
      seed = seed,
      frameRate = frameRate,
      image = preparedImage,
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
      mapped = MappedInputOutputConfig(),
      version = version
    ) set (
        inputs += (seed.option.toSeq: _*),
        outputs += (Seq(returnValue.option, stdOut.option, stdErr.option).flatten: _*)
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

}

@Lenses case class GAMATask(
  workspace:            File,
  model:                OptionalArgument[String],
  experiment:           String,
  finalStep:            FromContext[Int],
  seed:                 OptionalArgument[Val[Long]],
  frameRate:            OptionalArgument[Int],
  image:                PreparedImage,
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
  mapped:               MappedInputOutputConfig,
  version:              String) extends Task with ValidateTask {

  lazy val containerPoolKey = ContainerTask.newCacheKey

  override def validate =
    container.validateContainer(Vector(), environmentVariables, external, inputs) ++ {
      import xml._

      val inputXML = XML.loadFile(image.file / _root_.container.FlatImage.rootfsName / GAMATask.inputXML)
      val parameters = (inputXML \ "Simulation" \ "Parameters" \ "Parameter").collect { case x: Elem => x }

      def parameter(name: String) =
        parameters.filter(e => e.attribute("var").flatMap(_.headOption).map(_.text) == Some(name) || e.attribute("name").flatMap(_.headOption).map(_.text) == Some(name)).headOption

      def typeMatch(v: Val[_], t: String) =
        v match {
          case Val.caseInt(v) => t == "INT" | t == "FLOAT"
          case Val.caseDouble(v) => t == "INT" | t == "FLOAT"
          case Val.caseString(v) => t == "STRING"
          case Val.caseBoolean(v) => t == "BOOLEAN"
          case _ => false
        }

      def validateInputs =
        mapped.inputs.flatMap { m =>
          parameter(m.name) match {
            case Some(p) =>
              val gamaType = p.attribute("type").get.head.text
              if(!typeMatch(m.v, gamaType)) Some(new UserBadDataError(s"Type mismatch between mapped input ${m.v} and input ${m.name} of type ${gamaType}.")) else None
            case None => Some(new UserBadDataError(s"Mapped input ${m.name} has not been found in the simulation, make sure it is defined in your gaml file"))
          }
        }

      if ((inputXML \ "Simulation").isEmpty) Seq(new UserBadDataError(s"Experiment ${experiment} has not been found, make sure it is defined in your gaml file"))
      else validateInputs
    }

  override def process(executionContext: TaskExecutionContext) = FromContext { p ⇒
    import p._

    newFile.withTmpFile("inputs", ".xml") { inputFile ⇒
      val seedValue = seed.map(_.from(context)).getOrElse(random().nextLong)

      def inputMap = mapped.inputs.map { m ⇒ m.name -> context(m.v).toString }.toMap
      val inputXML = GAMATask.modifyInputXML(inputMap, finalStep.from(context), seedValue, frameRate.option).transform(XML.loadFile(image.file / _root_.container.FlatImage.rootfsName / GAMATask.inputXML))
      def inputFileName = "/_inputs_openmole_.xml"
      def outputDirectory = "/_output_"

      inputFile.content =
        s"""<?xml version="1.0" encoding="UTF-8" standalone="no"?>${inputXML.mkString("")}"""


      val (_, volumes) = GAMATask.volumes(workspace, model)

      def launchCommand = s"gama-headless -hpc 1 $inputFileName $outputDirectory"

        newFile.withTmpDir { tmpOutputDirectory =>
          def containerTask =
            ContainerTask(
              containerSystem = containerSystem,
              image = image,
              command = launchCommand,
              workDirectory = None,
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
              volumes.map { case (lv, cv) ⇒ resources +=[ContainerTask](lv, cv, true) },
              resources += (tmpOutputDirectory, outputDirectory, true)
            )

          val resultContext = containerTask.process(executionContext).from(context)

          frameRate.option match {
            case None =>
              import xml._

              def toVariable(v: Val[_], value: String) =
                v match {
                  case Val.caseInt(v) => Variable(v, value.toInt)
                  case Val.caseDouble(v) => Variable(v, value.toDouble)
                  case Val.caseString(v) => Variable(v, value)
                  case Val.caseBoolean(v) => Variable(v, value.toBoolean)
                  case _ => throw new UserBadDataError(s"Unsupported type of output variable $v")
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

              val simulationOutput = XML.loadFile(tmpOutputDirectory / "simulation-outputs0.xml") \ "Step"
              resultContext ++ extractOutputs(simulationOutput.last)
            case Some(f) =>
              import xml._

              def toVariable(v: Val[_], value: Array[String]) =
                v match {
                  case Val.caseArrayInt(v) => Variable(v, value.map(_.toInt))
                  case Val.caseArrayDouble(v) => Variable(v, value.map(_.toDouble))
                  case Val.caseArrayString(v) => Variable(v, value)
                  case Val.caseArrayBoolean(v) => Variable(v, value.map(_.toBoolean))
                  case _ => throw new UserBadDataError(s"Unsupported type of output variable $v")
                }

              def outputValue(e: Elem, name: String) =
                for {
                  a <- e.attribute("name").flatMap(_.headOption)
                  if a.text == name
                } yield e.child.text

              val simulationOutput = XML.loadFile(tmpOutputDirectory / "simulation-outputs0.xml") \ "Step"

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
