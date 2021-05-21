/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.workflow.task

import java.util.concurrent.locks.ReentrantLock

import monocle.macros.Lenses
import org.openmole.core.context._
import org.openmole.core.event._
import org.openmole.core.exception._
import org.openmole.core.expansion._
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow._
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.execution._
import org.openmole.core.workflow.job.Job
import org.openmole.core.workflow.mole._
import org.openmole.core.workspace.TmpDirectory
import org.openmole.tool.lock._
import org.openmole.tool.random.Seeder

object MoleTask {

  implicit def isTask = InputOutputBuilder(MoleTask.config)
  implicit def isInfo = InfoBuilder(MoleTask.info)

  /**
   * Constructor used to construct the MoleTask corresponding to the full puzzle
   *
   * @param dsl
   * @return
   */
  def apply(dsl: DSL)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): MoleTask = {
    val puzzle = dslToPuzzle(dsl)
    apply(puzzle.toMole, puzzle.lasts.head)
  }

  /**
   * @param mole the mole executed by this task.
   * @param last the capsule which returns the results
   */
  def apply(mole: Mole, last: MoleCapsule)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): MoleTask = {
    val mt = new MoleTask(mole, last, Vector.empty, InputOutputConfig(), InfoConfig())

    mt set (
      dsl.inputs += (mole.root.inputs(mole, Sources.empty, Hooks.empty).toSeq: _*),
      dsl.outputs += (last.outputs(mole, Sources.empty, Hooks.empty).toSeq: _*),
      isTask.defaults.set(mole.root.task(mole, Sources.empty, Hooks.empty).defaults)
    )
  }

  /**
   * Check if a given [[Job]] contains a [[MoleTask]] (a mole job wraps a task which is not necessarily a Mole Task).
   *
   * @param moleJob
   * @return
   */
  def containsMoleTask(moleJob: Job) = isMoleTask(moleJob.task.task)

  def isMoleTask(task: Task) =
    task match {
      case _: MoleTask ⇒ true
      case _           ⇒ false
    }

}

/**
 * Task executing a Mole
 *
 * @param mole the Mole to be executed
 * @param last the MoleCapsule finishing the Mole
 * @param implicits names of implicits, which values are imported explicitly from the context
 * @param config inputs and outputs prototypes, and defaults
 * @param info name and definition scope
 */
@Lenses case class MoleTask(
  mole:      Mole,
  last:      MoleCapsule,
  implicits: Vector[String],
  config:    InputOutputConfig,
  info:      InfoConfig
) extends Task {

  protected def process(executionContext: TaskExecutionContext) = FromContext[Context] { p ⇒
    import p._

    @volatile var lastContext: Option[Context] = None
    val lastContextLock = new ReentrantLock()

    val (execution, executionNewFile) = {
      implicit val eventDispatcher = EventDispatcher()
      val implicitsValues = implicits.flatMap(i ⇒ context.get(i))
      implicit val seeder = Seeder(random().nextLong())
      implicit val newFile = TmpDirectory(executionContext.moleExecutionDirectory)
      import executionContext.preference
      import executionContext.threadProvider
      import executionContext.workspace
      import executionContext.outputRedirection
      import executionContext.loggerService
      import executionContext.serializerService
      import executionContext.networkService

      val localEnvironment =
        LocalEnvironment(1, executionContext.localEnvironment.deinterleave)

      val moleServices =
        MoleServices.create(executionContext.applicationExecutionDirectory, Some(executionContext.moleExecutionDirectory))

      val execution = MoleExecution(
        mole,
        implicits = implicitsValues,
        defaultEnvironment = localEnvironment,
        cleanOnFinish = false,
        taskCache = executionContext.cache,
        lockRepository = executionContext.lockRepository
      )(moleServices)

      execution listen {
        case (_, ev: MoleExecution.JobFinished) ⇒
          lastContextLock { if (ev.capsule == last) lastContext = Some(ev.context) }
      }

      (execution, moleServices.tmpDirectory)
    }

    val listenerKey =
      executionContext.moleExecution.map { parentExecution ⇒
        implicit val ev = parentExecution.executionContext.services.eventDispatcher
        parentExecution listen {
          case (_, ev: MoleExecution.Finished) ⇒
            MoleExecution.cancel(execution, Some(MoleExecution.MoleExecutionError(new InterruptedException("Parent execution has been canceled"))))
        }
      }

    try execution.run(Some(context), validate = false)
    finally {
      fileService.deleteWhenEmpty(executionNewFile.directory)
      (executionContext.moleExecution zip listenerKey).foreach { case (moleExecution, key) ⇒ moleExecution.executionContext.services.eventDispatcher.unregister(key) }
    }

    lastContext.getOrElse(throw new UserBadDataError("Last capsule " + last + " has never been executed."))
  }

}
