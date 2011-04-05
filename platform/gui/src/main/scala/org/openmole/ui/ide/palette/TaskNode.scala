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

import java.awt.datatransfer.DataFlavor
import org.openmole.ui.ide.workflow.implementation.Preferences
import org.openmole.ui.ide.workflow.model.IEntityUI
import org.openmole.ui.ide.workflow.implementation.PropertyManager

class TaskNode(key: DataFlavor, task: IEntityUI) extends GenericNode(key,Preferences.properties(MoleConcepts.TASK_INSTANCE, task.entityType).getProperty(PropertyManager.THUMB_IMG),task.name){

}
//package org.openmole.ui.ide.palette;
//
//import java.awt.datatransfer.DataFlavor;
//import java.awt.datatransfer.Transferable;
//import java.awt.datatransfer.UnsupportedFlavorException;
//import java.io.IOException;
//import org.openide.util.datatransfer.ExTransferable;
//import org.openmole.commons.exception.UserBadDataError;
//
//import org.openmole.core.model.task.IGenericTask;
//import org.openmole.ui.ide.commons.ApplicationCustomize;
//import org.openmole.ui.ide.palette.Category.CategoryName;
//import org.openmole.ui.ide.workflow.implementation.IEntityUI;
//import org.openmole.ui.ide.workflow.implementation.Preferences;
//import org.openmole.ui.ide.workflow.implementation.PropertyManager;
//
///**
// *
// * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
// */
//public class TaskNode extends GenericNode {
//
//    private IEntityUI task;
//
//    public TaskNode(DataFlavor key,
//                    IEntityUI task) throws UserBadDataError {
//        super(key,
//                Preferences.getInstance().getProperties(CategoryName.TASK_INSTANCE, task.getType()).getProperty(PropertyManager.THUMB_IMG),
//                task.getName());
//        this.task = task;
//    }
//
//    @Override
//    public Transferable drag() throws IOException {
//        ExTransferable retValue = ExTransferable.create( super.drag() );
//        retValue.put( new ExTransferable.Single(ApplicationCustomize.TASK_DATA_FLAVOR) {
//            @Override
//            protected Object getData() throws IOException, UnsupportedFlavorException 
//            {return task;}
//            
//        });
//        return retValue;
//    }
//}