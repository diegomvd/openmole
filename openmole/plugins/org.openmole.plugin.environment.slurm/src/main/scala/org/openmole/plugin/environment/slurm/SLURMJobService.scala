package org.openmole.plugin.environment.slurm

import gridscale.cluster.HeadNode
import org.openmole.plugin.environment.batch.environment.{ AccessControl, BatchEnvironment, SerializedJob }
import org.openmole.plugin.environment.batch.storage.{ HierarchicalStorageInterface, StorageInterface }
import org.openmole.plugin.environment.gridscale.GridScaleJobService
import org.openmole.plugin.environment.ssh.{ RuntimeInstallation, SharedStorage }
import _root_.gridscale.effectaside

class SLURMJobService[S, H](
  s:                 S,
  tmpDirectory:      String,
  installation:      RuntimeInstallation[_],
  parameters:        SLURMEnvironment.Parameters,
  h:                 H,
  val accessControl: AccessControl)(implicit storageInterface: StorageInterface[S], hierarchicalStorageInterface: HierarchicalStorageInterface[S], headNode: HeadNode[H], services: BatchEnvironment.Services, systemInterpreter: effectaside.Effect[effectaside.System]) {

  import services._

  def submit(serializedJob: SerializedJob, outputPath: String, jobDirectory: String) = {
    val workDirectory = parameters.workDirectory getOrElse "/tmp"

    def buildScript(serializedJob: SerializedJob, outputPath: String) = {
      SharedStorage.buildScript(
        installation.apply,
        jobDirectory,
        workDirectory,
        parameters.openMOLEMemory,
        parameters.threads,
        serializedJob,
        outputPath,
        s,
        debug = parameters.debug,
        modules = parameters.modules
      )
    }

    val remoteScript = buildScript(serializedJob, outputPath)

    val description = _root_.gridscale.slurm.SLURMJobDescription(
      command = s"/bin/bash ${remoteScript.content}",
      partition = parameters.partition,
      workDirectory = jobDirectory,
      time = parameters.time,
      memory = parameters.memory,
      nodes = parameters.nodes,
      ntasks = parameters.nTasks,
      cpuPerTask = parameters.cpuPerTask orElse parameters.threads,
      qos = parameters.qos,
      gres = parameters.gres.toList,
      constraints = parameters.constraints.toList,
      reservation = parameters.reservation,
      wckey = parameters.wckey
    )

    accessControl { gridscale.slurm.submit(h, description) }
  }

  def state(id: gridscale.cluster.BatchScheduler.BatchJob) =
    accessControl { GridScaleJobService.translateStatus(gridscale.slurm.state(h, id)) }

  def delete(id: gridscale.cluster.BatchScheduler.BatchJob) =
    accessControl { gridscale.slurm.clean(h, id) }

  def stdOutErr(id: gridscale.cluster.BatchScheduler.BatchJob) =
    accessControl { (gridscale.slurm.stdOut(h, id), gridscale.slurm.stdErr(h, id)) }

}
