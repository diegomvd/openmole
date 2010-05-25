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
package org.openmole.ui.workflow.implementation;

import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;
import org.openmole.ui.workflow.model.ICapsuleModelUI;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class MoleSceneManager {
    private BidiMap<String,ICapsuleModelUI> taskCapsuleModels = new DualHashBidiMap<String,ICapsuleModelUI>();

    private int nodeCounter = 0;
    private int nodeID = 0;

    public void incrementNodeName(){
        nodeCounter++;
    }

    public String getNodeName(){
        return "task"+nodeCounter;
    }

    public String getNodeID(){
        return "node"+nodeID;
    }

    public void registerTaskCapsuleModel(ICapsuleModelUI cm) {
        taskCapsuleModels.put(getNodeID(), cm);
        nodeID++;
    }

    public String getTaskCapsuleModel(ICapsuleModelUI cm){
        return taskCapsuleModels.getKey(cm);
    }

    public ICapsuleModelUI getTaskCapsuleModel(String name){
        return taskCapsuleModels.get(name);
    }

    public void removeTaskCapsuleModel(ICapsuleModelUI tc){
        taskCapsuleModels.remove(tc);
    }

    public void setTransition(String start,
                              String end) {
        taskCapsuleModels.get(start).setTransitionTo(taskCapsuleModels.get(end).getTaskCapsule());
    }

    public void printTaskC(){
        for(String t: taskCapsuleModels.keySet()){
            System.out.println("TASKC :: " + t);
        }
    }
}
