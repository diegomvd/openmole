/*
 * Copyright (C) 2010 reuillon
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
 */

package org.openmole.core.batchservicecontrol

import org.openmole.core.batchservicecontrol.internal.Activator
import org.openmole.core.model.execution.batch.BatchServiceDescription
import org.openmole.core.model.execution.batch.IAccessToken
import java.util.concurrent.TimeUnit

object IUsageControl {
  final val ResourceReleased = "ResourceReleased"
  
 def withToken[B]( desc: BatchServiceDescription, f: (IAccessToken => B)): B = {
    val usageControl = Activator.getRessourceControl.usageControl(desc)
    val token = usageControl.waitAToken
    try {
      f(token)
    } finally {
      usageControl.releaseToken(token)
    }
  }
}

trait IUsageControl {

    def waitAToken(time: Long, unit: TimeUnit): IAccessToken

    def waitAToken: IAccessToken

    def releaseToken(token: IAccessToken)

    def tryGetToken: Option[IAccessToken]

    def load: Int
}
