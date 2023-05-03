package org.openmole.gui.server.stub

import cats.effect.IO
import org.http4s.*
import org.http4s.blaze.server.*
import org.http4s.server.*
import org.http4s.dsl.io.*

import scala.concurrent.duration.Duration

/*
 * Copyright (C) 2023 Romain Reuillon
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

@main def server =
  implicit val runtime = cats.effect.unsafe.IORuntime.global
  def hello =
    import org.http4s.headers.{`Content-Type`}
    val routes: HttpRoutes[IO] = HttpRoutes.of {
      case _ => Ok("Hello world").map(_.withContentType(`Content-Type`(MediaType.text.html)))
    }
    Router("/" -> routes)

  val shutdown =
    BlazeServerBuilder[IO].bindHttp(8080, "localhost").
      withHttpApp(hello.orNotFound).
      withIdleTimeout(Duration.Inf).
      withResponseHeaderTimeout(Duration.Inf).
      //withServiceErrorHandler(r => t => stackError(t)).
      allocated.unsafeRunSync()._2 // feRunSync()._2


  println("Press any key to stop")
  System.in.read()
  shutdown.unsafeRunSync()