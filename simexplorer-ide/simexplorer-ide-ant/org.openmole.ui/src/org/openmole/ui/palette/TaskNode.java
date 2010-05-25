/*
 *  Copyright (C) 2010 Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.ui.palette;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.util.datatransfer.ExTransferable;

import org.openide.util.lookup.Lookups;
import org.openmole.core.workflow.model.task.IGenericTask;
import org.openmole.ui.commons.ApplicationCustomize;
import org.openmole.ui.workflow.implementation.Preferences;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class TaskNode extends AbstractNode {

    private DataFlavor dataFlavor;
   // private Class<? extends IGenericTaskModelUI> model;
    private Class<? extends IGenericTask> coreTask;

    public TaskNode(DataFlavor key,
                  //  Class<? extends IGenericTaskModelUI> taskModel) {
                    Class<? extends IGenericTask> coreTask) {
        super(Children.LEAF, Lookups.fixed(new Object[]{key}));
        this.dataFlavor = key;
       // this.model = taskModel;
        this.coreTask = coreTask;

        setIconBaseWithExtension(Preferences.getInstance().getModelSettings(Preferences.getInstance().getModelClass(coreTask)).getThumbImagePath());
    }

     //DND start
    @Override
    public Transferable drag() throws IOException {
        ExTransferable retValue = ExTransferable.create( super.drag() );
        //add the 'data' into the Transferable
        retValue.put( new ExTransferable.Single(ApplicationCustomize.TASK_DATA_FLAVOR) {
            @Override
            protected Object getData() throws IOException, UnsupportedFlavorException 
            {return coreTask;}
            
        });
        return retValue;
    }
    //DND end
}
