/**
  * Created by Mathieu Leclaire on 19/04/18.
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as published by
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
  *
  */
package org.openmole.gui.client.ext

import org.openmole.gui.client.ext.WizardUtils.mkVals
import org.openmole.gui.shared.data.*

object WizardUtils:
  def preamble =
    """/*
    |This is skeleton to plug your model into OpenMOLE. To go further you should complete it.
    |Define some hook, some exploration methods, and optionally some execution environment.
    |To do all that please refer to the OpenMOLE documentation.
    |*/""".stripMargin



  def mkVals(modelMetadata: ModelMetadata) =
    def vals =
      ((modelMetadata.inputs ++ modelMetadata.outputs).map { p ⇒ (p.name, p.`type`.scalaString) } distinct).map { p =>
        "val " + p._1 + " = Val[" + p._2 + "]"
      }
    vals.mkString("\n")

  def mkTaskParameters(s: String*) = s.filter(!_.trim.isEmpty).map(s => s"    $s").mkString(",\n")

  def mkSet(modelData: ModelMetadata, s: String*) =
    def setElements(inputs: Seq[PrototypePair], outputs: Seq[PrototypePair]) =
      def testBoolean(protoType: PrototypePair) = protoType.`type` match
        case PrototypeData.Boolean ⇒ if (protoType.default == "1") "true" else "false"
        case _ ⇒ protoType.default

      def ioString(protos: Seq[PrototypePair], keyString: String) = if (protos.nonEmpty) Seq(Seq(s"$keyString += (", ")").mkString(protos.map { i ⇒ s"${i.name}" }.mkString(", "))) else Seq()

      def imapString(protos: Seq[PrototypePair], keyString: String) = protos.flatMap { i ⇒
        i.mapping.map { mapping => s"""$keyString += ${i.name} mapped "${mapping}"""" }
      }

      def omapString(protos: Seq[PrototypePair], keyString: String) = protos.flatMap { o ⇒
        o.mapping.map { mapping =>
          s"""$keyString += ${o.name} mapped "${mapping}""""
        }
      }

      def default(key: String, value: String) = s"$key := $value"

      val (rawimappings, ins) = inputs.partition(i ⇒ i.mapping.isDefined)
      val (rawomappings, ous) = outputs.partition(o ⇒ o.mapping.isDefined)
      val (ifilemappings, imappings) = rawimappings.partition(_.`type` == PrototypeData.File)
      val (ofilemappings, omappings) = rawomappings.partition(_.`type` == PrototypeData.File)

      //val resourcesString = if (!resources.isEmpty) s"""  resources += (${resources.map { r ⇒ s"workDirectory / $r" }.mkString(",")})\n""" else ""

      val defaultValues =
        (inputs.map { p ⇒ (p.name, testBoolean(p)) } ++
          ifilemappings.map { p ⇒ (p.name, " workDirectory / \"" + p.mapping.getOrElse("") + "\"") }).filterNot {
          _._2.isEmpty
        }.map { p ⇒ default(p._1, p._2) }

      ioString(ins, "inputs") ++
        ioString(ous, "outputs") ++
        imapString(ifilemappings, "inputFiles") ++
        omapString(ofilemappings, "outputFiles") ++
        imapString(rawimappings, "inputs") ++
        omapString(rawomappings, "outputs") ++
        defaultValues

    end setElements

    val elements = (setElements(modelData.inputs, modelData.outputs) ++ s).filter(!_.trim.isEmpty).map(s => s"    $s").mkString(",\n")
    if elements.isEmpty
    then ""
    else
      s"""set (
         |$elements
         |  )""".stripMargin


  def singleFolderContaining(files: Seq[(RelativePath, SafePath)], f: ((RelativePath, SafePath)) => Boolean): Option[(RelativePath, SafePath)] =
    val firstLevel = files.map(f => f._1.value.take(1)).distinct
    if firstLevel.size == 1
    then files.filter(f => f._1.value.size == 2).find(f)
    else None

  def toTaskName(r: RelativePath) =
    def lowerCase(s: String) = s.take(1).toLowerCase ++ s.drop(1)
    lowerCase(r.nameWithoutExtension) + "Task"

  def toOMSName(r: RelativePath) =
    r.nameWithoutExtension.capitalize + ".oms"

  def toVariableName(s: String) =
    val capital: String = s.split('-').reduce(_ + _.capitalize)
    capital.replace("?", "").replace(" ", "").replace("%", "percent")



