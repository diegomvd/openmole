/*
 * Copyright (C) 2012 mathieu
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

package org.openmole.ide.plugin.domain.file

import java.io.File
import org.openmole.core.model.data.Prototype
import org.openmole.ide.core.model.data.IDomainDataUI
import org.openmole.misc.tools.io.FromString._
import org.openmole.plugin.domain.file.ListFilesDomain

class ListFilesDomainDataUI(val name: String = "",
                            val directoryPath: String = "",
                            val regexp: String = ".*") extends IDomainDataUI {

  def coreObject(prototypeObject: Prototype[_]) = new ListFilesDomain(new File(directoryPath), regexp)

  def coreClass = classOf[ListFilesDomain]

  def buildPanelUI = new ListFilesDomainPanelUI(this)
}
