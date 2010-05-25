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
package org.openmole.ui.workflow.provider;

import java.util.ArrayList;
import java.util.Collection;
import javax.swing.JMenuItem;
import org.openmole.core.workflow.model.task.IGenericTask;
import org.openmole.ui.workflow.action.AddTaskAction;
import org.openmole.ui.workflow.action.AddTaskCapsuleAction;
import org.openmole.ui.workflow.implementation.MoleScene;
import org.openmole.ui.workflow.implementation.Preferences;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class MoleSceneMenuProvider extends GenericMenuProvider {

    Collection<Class<? extends IGenericTask>> tasks = new ArrayList<Class<? extends IGenericTask>>();

    public MoleSceneMenuProvider(MoleScene moleScene) {
        super();

        JMenuItem itemTCapsule = new JMenuItem("Add a Task Capsule");
        itemTCapsule.addActionListener(new AddTaskCapsuleAction(moleScene));


        items.add(itemTCapsule);
    }
}
