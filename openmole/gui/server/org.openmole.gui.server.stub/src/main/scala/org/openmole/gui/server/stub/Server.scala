package org.openmole.gui.server.stub

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


import org.openmole.gui.server.core.GUIServlet
import java.io.File
import cats.effect.IO
import org.http4s.*
import org.http4s.blaze.server.*
import org.http4s.server.*
import org.http4s.dsl.io.*
import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.openmole.gui.server.core.{ApiImpl, GUIServerServices}
import org.openmole.tool.crypto.Cypher

import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.duration.Duration


@main def server(args: String*) =
  val webapp = args.head

  implicit val runtime = cats.effect.unsafe.IORuntime.global

  def css = Seq("css/bootstrap.css", "css/extrafont.css", "css/style.css")
  def application = GUIServlet.html(/*s"${GUIServlet.webpackLibrary}.run();"*/"openmole_stub_client.run();", css, "")

  OutputManager.uninstall
  val services =  GUIServerServices(Workspace(new File("/tmp/openmole")), None, None, None)
  val serviceProvider = GUIServerServices.ServicesProvider(services, new AtomicReference(Cypher("password")))
  val apiImpl = new ApiImpl(serviceProvider, None)
  //apiImpl.activatePlugins

  def stackError(t: Throwable) =
    import org.openmole.core.tools.io.Prettifier.*
    import io.circe.*
    import io.circe.syntax.*
    import io.circe.generic.auto.*
    import org.openmole.gui.shared.data.*
    import org.http4s.headers.`Content-Type`
    InternalServerError { Left(ErrorData(t)).asJson.noSpaces }.map(_.withContentType(`Content-Type`(MediaType.application.json)))


  val apiServer = new StubAPIServer(apiImpl, stackError)

  def hello =
    import org.http4s.headers.{`Content-Type`}
    val routes: HttpRoutes[IO] = HttpRoutes.of {
      case request@GET -> Root / "js" / "snippets" / path =>
        StaticFile.fromFile(new File(webapp, s"js/$path"), Some(request)).getOrElseF(NotFound())
      case request@GET -> Root / "js" / path =>
        StaticFile.fromFile(new File(webapp, s"js/$path"), Some(request)).getOrElseF(NotFound())
      case request@GET -> Root / "css" / path =>
        StaticFile.fromFile(new File(webapp, s"css/$path"), Some(request)).getOrElseF(NotFound())
      case request@GET -> Root / "img" / path =>
        StaticFile.fromFile(new File(webapp, s"img/$path"), Some(request)).getOrElseF(NotFound())
      case request@GET -> Root / "fonts" / path =>
        StaticFile.fromFile(new File(webapp, s"fonts/$path"), Some(request)).getOrElseF(NotFound())
      case request@GET -> Root => Ok(application.render).map(_.withContentType(`Content-Type`(MediaType.text.html)))
    }
    Router(Seq("/" -> routes, "/" -> apiServer.routes, "/" -> apiServer.endpointRoutes)*).orNotFound

  val shutdown =
    BlazeServerBuilder[IO].bindHttp(8080, "localhost").
      withHttpApp(hello).
      withIdleTimeout(Duration.Inf).
      withResponseHeaderTimeout(Duration.Inf).
      //withServiceErrorHandler(r => t => stackError(t)).
      allocated.unsafeRunSync()._2 // feRunSync()._2


  println("Press any key to stop")
  System.in.read()
  shutdown.unsafeRunSync()