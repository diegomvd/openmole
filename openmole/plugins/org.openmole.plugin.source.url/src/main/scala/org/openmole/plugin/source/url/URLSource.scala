/*
 * Copyright (C) 07/07/2022 Juste Raimbault
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.source.url

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import gridscale.http

import java.io.File

object URLSource {

  def apply[T](url: FromContext[String], prototype: Val[T])(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    Source("URLSource") { p ⇒
      import p._

      val response = http.get(url.from(context))

      val value: AnyRef = prototype.`type`.runtimeClass match {
        case s if s == classOf[String] ⇒ response
        case f if f == classOf[File] ⇒
          val v = newFile.newFile()
          v.withWriter()(_.write(response))
          v
        case _ ⇒ throw new UserBadDataError(s"URL can not be mapped to a ${prototype.`type`} prototype (only String and File supported)")
      }

      Variable.unsecure(prototype, value)
    } set (outputs += prototype)

}

