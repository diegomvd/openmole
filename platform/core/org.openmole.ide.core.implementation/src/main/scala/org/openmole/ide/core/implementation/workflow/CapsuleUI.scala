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

package org.openmole.ide.core.implementation.workflow

import java.awt.Color
import org.netbeans.api.visual.action.ActionFactory
import org.netbeans.api.visual.widget.ComponentWidget
import org.netbeans.api.visual.widget.Widget
import scala.swing.Component
import org.openmole.ide.core.implementation.provider.CapsuleMenuProvider
import org.openmole.ide.core.model.commons.Constants._
import org.openmole.ide.core.model.commons.CapsuleType._
import org.openmole.ide.core.model.workflow.IInputSlotWidget
import org.openmole.ide.core.model.dataproxy.IEnvironmentDataProxyUI
import org.openmole.ide.core.model.workflow._
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.misc.widget.LinkLabel
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.model.panel.PanelMode._
import org.openmole.ide.misc.widget.MigPanel
import scala.collection.mutable.HashMap
import scala.swing.Action
import scala.swing.Label
import scala.collection.mutable.ListBuffer

class CapsuleUI(val scene: IMoleScene, 
                var dataProxy: Option[ITaskDataProxyUI],
                var capsuleType:CapsuleType,
                var startingCapsule: Boolean = false,
                var environment: Option[IEnvironmentDataProxyUI] = None) extends Widget(scene.graphScene) with ICapsuleUI{
  def this(sc: IMoleScene) = this (sc,None,CAPSULE,sc.manager.capsules.size == 0,None)
  
  val taskComponentWidget = new TaskComponentWidget(scene,new TaskWidget(scene,this))
  addChild(taskComponentWidget)
  createActions(MOVE).addAction (ActionFactory.createMoveAction)
  
  
  var islots= ListBuffer.empty[IInputSlotWidget]
  val oslot= new OutputSlotWidget(scene,this)
  var samplingWidget: Option[SamplingWidget] = None
  var nbInputSlots = 0
//  val connectableWidget= new ConnectableWidget(scene,this)
  //val connectableWidget= new TaskComponentWidget(scene,new TaskWidget(scene,this))
//  addChild(connectableWidget)
  val capsuleMenuProvider= new CapsuleMenuProvider(scene, this)
  
  scene.refresh
        
  getActions.addAction(ActionFactory.createPopupMenuAction(capsuleMenuProvider))
    
  def widget = this
  
  def copy(sc: IMoleScene) = {
    var slotMapping = new HashMap[IInputSlotWidget,IInputSlotWidget]
    val c = new CapsuleUI(sc,dataProxy,capsuleType,startingCapsule,environment)
    islots.foreach(i=>slotMapping+=i->c.addInputSlot(false))
    if (dataProxy.isDefined) {
      c.setDataProxy(dataProxy.get)
    } 
    else capsuleType = BASIC_TASK
    (c,slotMapping)
  }
  
  def defineAsStartingCapsule(b : Boolean) = {
    startingCapsule = b
    islots.foreach{ isw=>
      isw.setStartingSlot(b)}
    scene.validate
    scene.refresh
  }
  
  def encapsule(dpu: ITaskDataProxyUI)= {
    setDataProxy(dpu)
    capsuleMenuProvider.addTaskMenus
//    titleLabel = new LinkLabel(dpu.dataUI.name,new Action(""){
//        def apply = scene.displayPropertyPanel(dpu, EDIT)})
  }
  
  
  def addInputSlot(on: Boolean): IInputSlotWidget =  {
    if (on) startingCapsule = on
    
    nbInputSlots+= 1
    val im = new InputSlotWidget(scene,this,nbInputSlots,on)
   // addInputSlot(im)
    islots += im
    taskComponentWidget.addChild(im)
    scene.validate
    scene.refresh
    im
  }

  def removeInputSlot= {
    nbInputSlots-= 1
    removeFirstInputSlot
  }
  
  def setDataProxy(dpu: ITaskDataProxyUI)={
    dataProxy= Some(dpu)
    if (Proxys.isExplorationTaskData(dpu.dataUI)) {
      capsuleType = EXPLORATION_TASK 
      addSampling
    } else capsuleType = BASIC_TASK
  }
  
  
  def x = convertLocalToScene(getLocation).getX
  
  def y = convertLocalToScene(getLocation).getY
  
  def addInputSlot(iw: InputSlotWidget) {}
    
  def removeFirstInputSlot = {}
  
  def addSampling = {}
}
