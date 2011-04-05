/*
 * Copyright (C) 2011 Mathieu leclaire <mathieu.leclaire at openmole.org>
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

package org.openmole.ui.ide.dialog

import org.openmole.ui.ide.workflow.model.IEntityUI
import org.openmole.ui.ide.workflow.model.IContainerUI

trait IManager {
  def entityInstance(name: String, t: Class[_]): IEntityUI

  def container: IContainerUI
  
  def classTypes: Set[Class[_]]
}

//import org.openmole.ui.ide.workflow.model.IContainerUI;
//import scala.collection.Set;
//import org.openmole.ui.ide.workflow.model.IEntityUI;
//
///**
// *
// * @author Mathieu Leclaire <mathieu.leclaire@openmole.org>
// */
//public interface IManager {
//    IEntityUI getEntityInstance(String name,Class type);
//    IContainerUI getContainer();
//    Set<Class<?>> getClassTypes();
//}