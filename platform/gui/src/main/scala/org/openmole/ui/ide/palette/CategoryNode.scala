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

package org.openmole.ui.ide.palette

import org.openide.nodes.AbstractNode
import org.openide.util.lookup.Lookups

class CategoryNode(category: ICategory) extends AbstractNode(category.children,Lookups.singleton(category)){
  setDisplayName(category.name)
}
//package org.openmole.ui.ide.palette;
//
//import org.openide.nodes.AbstractNode;
//import org.openide.util.lookup.Lookups;
//
///**
// *
// * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
// */
//public class CategoryNode  extends AbstractNode {
//
//    /** Creates a new instance of CategoryNode */
//    public CategoryNode( ICategory category ) {
//        super(category.getChildren(), Lookups.singleton(category) );
//        setDisplayName(category.getName());
//    }
//}