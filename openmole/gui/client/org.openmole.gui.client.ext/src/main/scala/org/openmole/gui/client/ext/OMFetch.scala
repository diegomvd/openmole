package org.openmole.gui.client.ext

/*
 * Copyright (C) 2022 Romain Reuillon
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

import endpoints4s.xhr.EndpointsSettings

import scala.concurrent.duration.*
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.timers
import org.openmole.gui.shared.api.*
import org.openmole.gui.shared.data.ErrorData

object OMFetch:
  def apply[API](api: EndpointsSettings => API) = new OMFetch(api)
  case class ServerError(data: ErrorData) extends Throwable

class OMFetch[API](api: EndpointsSettings => API) {

  def future[O](
    f: API => scala.concurrent.Future[O],
    timeout: Option[FiniteDuration] = Some(60 seconds),
    warningTimeout: Option[FiniteDuration] = Some(10 seconds),
    onTimeout: () ⇒ Unit = () ⇒ {},
    onWarningTimeout: () ⇒ Unit = () ⇒ {},
    onFailed: Throwable => Unit = _ => {})(using baseURI: BasePath): scala.concurrent.Future[O] =
    val timeoutSet = warningTimeout.map(t => timers.setTimeout(t.toMillis) { onWarningTimeout() })

    def stopTimeout = timeoutSet.foreach(timers.clearTimeout)

    val future = f(api(EndpointsSettings().withTimeout(timeout).withBaseUri(BasePath.value(baseURI))))
    future.andThen {
      case f@Failure(_: scala.concurrent.TimeoutException) ⇒
        stopTimeout
        onTimeout()
        f
      case f@Failure(t) =>
        stopTimeout
        onFailed(t)
        f
      case f =>
        stopTimeout
        f
    }

  def futureError[O](
    f: API => scala.concurrent.Future[Either[ErrorData, O]],
    timeout: Option[FiniteDuration] = Some(60 seconds),
    warningTimeout: Option[FiniteDuration] = Some(10 seconds),
    onTimeout: () ⇒ Unit = () ⇒ {},
    onWarningTimeout: () ⇒ Unit = () ⇒ {},
    onFailed: Throwable => Unit = _ => {})(using baseURI: BasePath): scala.concurrent.Future[O] =

    val timeoutSet = warningTimeout.map(t => timers.setTimeout(t.toMillis) {
      onWarningTimeout()
    })

    def stopTimeout = timeoutSet.foreach(timers.clearTimeout)

    val future = f(api(EndpointsSettings().withTimeout(timeout).withBaseUri(BasePath.value(baseURI))))
    future.transform {
      case Success(Right(r)) =>
        stopTimeout
        Success(r)
      case Success(Left(e)) =>
        stopTimeout
        val throwable = OMFetch.ServerError(e)
        onFailed(throwable)
        Failure(throwable)
      case Failure(t: scala.concurrent.TimeoutException) ⇒
        stopTimeout
        onTimeout()
        Failure(t)
      case Failure(t) =>
        stopTimeout
        onFailed(t)
        Failure(t)
    }



  //  def apply[O, R](
//    r: API => scala.concurrent.Future[O],
//    timeout: FiniteDuration = 60 seconds,
//    warningTimeout: FiniteDuration = 10 seconds,
//    onTimeout: () ⇒ Unit = () ⇒ {},
//    onWarningTimeout: () ⇒ Unit = () ⇒ {},
//    onFailed: Throwable => Unit = _ => {})(action: O => R) = {
//    //import scala.concurrent.ExecutionContext.Implicits.global
//    //    org.openmole.gui.client.core.APIClient.uuid(()).future.onComplete { i => println("uuid " + i.get.uuid) }
//    val f = future(
//      f = r,
//      timeout = timeout,
//      warningTimeout = warningTimeout,
//      onTimeout = onTimeout,
//      onWarningTimeout = onWarningTimeout,
//      onFailed = onFailed)
//
//    f.onComplete {
//      case Success(r) ⇒ action(r)
//      case _ =>
//    }
//  }


}