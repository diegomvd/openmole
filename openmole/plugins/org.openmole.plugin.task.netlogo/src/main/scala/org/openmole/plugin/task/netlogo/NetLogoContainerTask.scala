package org.openmole.plugin.task.netlogo

import java.io.FileNotFoundException
import monocle.Focus
import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.openmole.core.exception.{InternalProcessingError, UserBadDataError}
import org.openmole.core.fileservice.FileService
import org.openmole.core.networkservice.NetworkService
import org.openmole.core.preference.Preference
import org.openmole.core.serializer.SerializerService
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.workflow.builder.*
import org.openmole.core.workflow.task.*
import org.openmole.core.workflow.validation.*
import org.openmole.core.workspace.{TmpDirectory, Workspace}
import org.openmole.plugin.task.container
import org.openmole.plugin.task.container.*
import org.openmole.plugin.task.external.*
import org.openmole.tool.outputredirection.OutputRedirection

import scala.xml.*

object NetLogoContainerTask:

  implicit def isTask: InputOutputBuilder[NetLogoContainerTask] = InputOutputBuilder(Focus[NetLogoContainerTask](_.config))
  implicit def isExternal: ExternalBuilder[NetLogoContainerTask] = ExternalBuilder(Focus[NetLogoContainerTask](_.external))
  implicit def isInfo: InfoBuilder[NetLogoContainerTask] = InfoBuilder(Focus[NetLogoContainerTask](_.info))
  implicit def isMapped: MappedInputOutputBuilder[NetLogoContainerTask] = MappedInputOutputBuilder(Focus[NetLogoContainerTask](_.mapped))

  def workspaceDirectory = "/_workspace_"

  def volumes(
    script: File,
    embedWorkspace: Boolean) =
    if embedWorkspace
    then script.getParentFile.listFiles.map { f => f -> s"$workspaceDirectory/${f.getName}"}.toSeq
    else Seq(script -> s"$workspaceDirectory/${script.getName}")

  def apply(
    script: File,
    go: Seq[FromContext[String]],
    setup: Seq[FromContext[String]]                          = Seq(),
    embedWorkspace: Boolean                                  = false,
    seed: OptionalArgument[Val[Int]]                         = None,
    switch3d: Boolean                                        = false,
    //steps:                  Option[FromContext[Int]]         = None,
    install:                Seq[String]                      = Seq.empty,
    memory:                 Information                      = 1.gigabyte,
    version:                String                           = "6.3.0",
    errorOnReturnValue:     Boolean                          = true,
    returnValue:            OptionalArgument[Val[Int]]       = None,
    stdOut:                 OptionalArgument[Val[String]]    = None,
    stdErr:                 OptionalArgument[Val[String]]    = None,
    environmentVariables:   Seq[EnvironmentVariable]         = Vector.empty,
    hostFiles:              Seq[HostFile]                    = Vector.empty,
    //    workDirectory:          OptionalArgument[String]       = None,
    clearContainerCache:    Boolean                          = false,
    containerSystem:        ContainerSystem                  = ContainerSystem.default,
    installContainerSystem: ContainerSystem                  = ContainerSystem.default)(implicit name: sourcecode.Name, definitionScope: DefinitionScope, newFile: TmpDirectory, _workspace: Workspace, preference: Preference, fileService: FileService, threadProvider: ThreadProvider, outputRedirection: OutputRedirection, networkService: NetworkService, serializerService: SerializerService): NetLogoContainerTask = {

    val image:  ContainerImage                   = s"openmole/netlogo:${version}"

    val volumesValue = volumes(script, embedWorkspace)
    val preparedImage = ContainerTask.install(installContainerSystem, image, install, volumesValue.map { case (lv, cv) ⇒ lv.getAbsolutePath -> cv }, clearCache = clearContainerCache)

    NetLogoContainerTask(
      script = script,
      go = go,
      setup = setup,
      embedWorkspace = embedWorkspace,
      seed = seed,
      switch3d = switch3d,
      //steps = steps,
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


case class NetLogoContainerTask(
  script: File,
  go: Seq[FromContext[String]],
  setup: Seq[FromContext[String]],
  embedWorkspace: Boolean,
  seed: OptionalArgument[Val[Int]],
  switch3d: Boolean,
  image:                InstalledImage,
  memory:               Information,
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
  mapped:               MappedInputOutputConfig) extends Task with ValidateTask:

  lazy val containerPoolKey = ContainerTask.newCacheKey

  override def validate = Validate { p ⇒
    import p._
    val allInputs = External.PWD :: p.inputs.toList
    go.flatMap(_.validate(allInputs)) ++
      External.validate(external)(allInputs) ++
      NetLogoTask.validateNetLogoInputTypes(mapped.inputs.map(_.v))
  }

  override def process(executionContext: TaskExecutionContext) = FromContext { p ⇒
    import p._

    newFile.withTmpFile("inputs", ".bin") { inputFile ⇒
      def inputFileName = "/_inputs_openmole_.bin"
      def outputFileName = "/_outputs_openmole_.bin"
      val outputFileVal = Val[File]("outputFile", Namespace("NetLogoTask"))

      def createInputFile(inputFile: File) =
        val model = s"${NetLogoContainerTask.workspaceDirectory}/${script.getName}"
        val inputs =
          val inputMap = new java.util.TreeMap[String, Any]()
          for
            m <- Mapped.noFile(mapped.inputs)
          do inputMap.put(m.name, NetLogoTask.netLogoCompatibleType(context(m.v)))
          inputMap
        val outputs: Array[String] = mapped.outputs.map(_.name).toArray[String]
        val goValue: Array[String] = go.map(_.from(context)).toArray[String]
        val setupValue: Array[String] = setup.map(_.from(context)).toArray[String]
        val seedValue: java.util.Optional[java.lang.Integer] = java.util.Optional.ofNullable[java.lang.Integer](seed.map(_.from(context)).map(new java.lang.Integer(_)).orNull)

        val content = Array[AnyRef](model, inputs, outputs, setupValue, goValue, seedValue)

        executionContext.serializerService.serialize(content, inputFile)

      def readOutputData(file: File) = 
        val outputValues = executionContext.serializerService.deserialize[Array[AnyRef]](file)
        (outputValues zip Mapped.noFile(mapped.outputs)).map { (value, v) ⇒
          NetLogoTask.netLogoValueToVal(value, v)
        }

      createInputFile(inputFile)


      val volumes = NetLogoContainerTask.volumes(script, embedWorkspace)

      def param3D = if switch3d then "-Dorg.nlogo.is3d=true" else ""
      def paramJVM = s"-XX:+UseG1GC -XX:ParallelGCThreads=1 -XX:CICompilerCount=2 -XX:ConcGCThreads=1 -XX:G1ConcRefinementThreads=1 -Xmx${memory.toMegabytes.toInt}m $param3D"
      val launchCommand = s"netlogo-headless $inputFileName $outputFileName"

      def containerTask =
        ContainerTask(
          image = image,
          command = launchCommand,
          containerSystem = containerSystem,
          workDirectory = None,
          relativePathRoot = Some(NetLogoContainerTask.workspaceDirectory),
          errorOnReturnValue = errorOnReturnValue,
          returnValue = returnValue,
          hostFiles = hostFiles,
          environmentVariables = environmentVariables ++ Seq(EnvironmentVariable("JAVA_OPT", paramJVM)),
          reuseContainer = true,
          stdOut = stdOut,
          stdErr = stdErr,
          config = config,
          external = external,
          info = info,
          containerPoolKey = containerPoolKey) set(
          resources += (inputFile, inputFileName, true),
          volumes.map { case (lv, cv) ⇒ resources += (lv, cv, true) },
          outputFiles += (outputFileName, outputFileVal),
          Mapped.files(mapped.inputs).map { m ⇒ inputFiles += (m.v, m.name, true) },
          Mapped.files(mapped.outputs).map { m ⇒ outputFiles += (m.name, m.v) }
        )

      val resultContext = containerTask.process(executionContext).from(context)

      def resultFile = resultContext(outputFileVal)
      resultContext ++ readOutputData(resultFile)
    }
  }

