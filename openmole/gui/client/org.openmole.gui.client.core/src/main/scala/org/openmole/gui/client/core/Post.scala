//package org.openmole.gui.client.core
//
//import org.openmole.gui.client.core.alert.{ BannerLevel }
//import org.openmole.gui.client.ext.OMPost
//
//import scala.concurrent.duration.Duration
//import scala.concurrent.duration._
///*
// * Copyright (C) 04/01/17 // mathieu.leclaire@openmole.org
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU Affero General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU Affero General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//
//case class PostContext(
//  timeout:        Duration                     = 60 seconds,
//  warningTimeout: Duration                     = 10 seconds,
//  alert:          (String, BannerLevel) ⇒ Unit)
//
//object Post {
//
//  def apply(
//    timeout:        Duration                     = 60 seconds,
//    warningTimeout: Duration                     = 10 seconds,
//    alert:          (String, BannerLevel) ⇒ Unit = panels.bannerAlert.register) = {
//    OMPost(
//      timeout,
//      warningTimeout,
//      (request: String) ⇒ alert(s"The request ${request} failed.", BannerLevel.Critical),
//      () ⇒ alert("The request is very long. Please check your connection.", BannerLevel.Regular)
//    )
//  }
//
//}
//
