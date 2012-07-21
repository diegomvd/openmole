/*
 * Copyright (C) 2012 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.method.evolution

import fr.iscpif.mgo._
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.task.Task
import org.openmole.core.implementation.task.TaskBuilder
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.task.IPluginSet
import scala.collection.mutable.ListBuffer

object ToIndividualArrayTask {

  def apply[T <: GAGenome](
    name: String, genome: IPrototype[T],
    individual: IPrototype[Individual[T]])(implicit plugins: IPluginSet) =
    new TaskBuilder { builder ⇒

      private var objectives = new ListBuffer[(IPrototype[Double], Double)]

      def addObjective(p: IPrototype[Double], v: Double) = {
        this addInput p
        objectives += (p -> v)
        this
      }

      def toTask = new ToIndividualArrayTask[T](name, genome, individual) {
        val inputs = builder.inputs + genome
        val outputs = builder.outputs + individual.toArray
        val parameters = builder.parameters
        val objectives = builder.objectives.toList
      }
    }

}

sealed abstract class ToIndividualArrayTask[T <: GAGenome](
    val name: String,
    genome: IPrototype[T],
    individual: IPrototype[Individual[T]])(implicit val plugins: IPluginSet) extends Task { task ⇒

  def objectives: List[(IPrototype[Double], Double)]

  override def process(context: IContext) =
    context + new Variable(
      individual.toArray,
      Array[Individual[T]](
        new Individual[T] {
          val genome = context.valueOrException(task.genome)
          val fitness = new Fitness {
            val values = objectives.map {
              case (o, v) ⇒ math.abs(context.valueOrException(o) - v)
            }.toIndexedSeq
          }
        }))

}
