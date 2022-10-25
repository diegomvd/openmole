package org.openmole.core.workflow.format

import java.io.PrintStream

import org.openmole.core.context.{ Val, Variable }
import org.openmole.core.csv
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.hook.HookExecutionContext
import org.openmole.core.workflow.tools.OptionalArgument
import org.openmole.core.exception.UserBadDataError

object CSVOutputFormat {

  implicit def format: OutputFormat[CSVOutputFormat, Any] = new OutputFormat[CSVOutputFormat, Any] {
    override def write(executionContext: HookExecutionContext)(format: CSVOutputFormat, output: WritableOutput, content: OutputFormat.OutputContent, method: Any): FromContext[Unit] = FromContext { p ⇒
      import p._

      def headerLine(variables: Seq[Variable[_]]) = format.header.map(_.from(context)) getOrElse csv.header(variables.map(_.prototype), variables.map(_.value), arrayOnRow = format.arrayOnRow)

      import OutputFormat.*
      import WritableOutput.*

      (output, content) match {
        case (Store(file), s) ⇒
          def writeFile(f: File, variables: Seq[Variable[_]]) = 
            val create = !format.append || f.isEmpty
            val h = if (create || f.isEmpty) Some(headerLine(variables)) else None
            if (create) f.atomicWithPrintStream { ps ⇒ csv.writeVariablesToCSV(ps, h, variables.map(_.value), unrollArray = format.unrollArray, arrayOnRow = format.arrayOnRow) }
            else f.withPrintStream(append = true, create = true) { ps ⇒ csv.writeVariablesToCSV(ps, h, variables.map(_.value), unrollArray = format.unrollArray, arrayOnRow = format.arrayOnRow) }  

          def directoryMode = format.directory || s.section.size > 1

          def f(sectionName: String) = 
            val postfix = format.postfix.map(_.from(context)).getOrElse("") 
            if directoryMode
            then file.from(context) / (sectionName + postfix + ".csv")
            else file.from(context) 
            
          for { (section, i) ← s.section.zipWithIndex } writeFile(f(section.name.getOrElse(s"$i")), section.variables)
        case (Display(ps), s) ⇒
          def writeStream(ps: PrintStream, section: Option[String], variables: Seq[Variable[_]]) =
            section match 
              case None ⇒
                val header = Some(headerLine(variables))
                csv.writeVariablesToCSV(ps, header, variables.map(_.value), unrollArray = format.unrollArray, arrayOnRow = format.arrayOnRow)
              case Some(section) ⇒
                ps.println(section + ":")
                val header = Some(headerLine(variables))
                csv.writeVariablesToCSV(ps, header, variables.map(_.value), unrollArray = format.unrollArray, arrayOnRow = format.arrayOnRow, margin = "  ")

          for { (section, i) ← s.section.zipWithIndex } 
            val sectionName = if s.section.size > 1 then Some(section.name.getOrElse(s"$i")) else None
            writeStream(ps, sectionName, section.variables)
      }
    }

    override def validate(format: CSVOutputFormat) = 
      format.header.toOption.toSeq.map(_.validate) ++ 
      format.postfix.option.toSeq.map(_.validate)
  }

}

case class CSVOutputFormat(
  header:      OptionalArgument[FromContext[String]]  = None,
  unrollArray: Boolean                                = false,
  arrayOnRow:  Boolean                                = false,
  append:      Boolean                                = false,
  postfix:     OptionalArgument[FromContext[String]]  = None,
  directory:   Boolean                                = false)