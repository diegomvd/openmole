///**
// * Created by Mathieu Leclaire on 19/04/18.
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU Affero General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// *
// */
//package org.openmole.gui.plugin.wizard.jar
//
//import org.openmole.core.workspace.Workspace
//import org.openmole.gui.shared.data.*
//
//trait JarWizardAPI {
//  def toTask(
//    target:         SafePath,
//    executableName: String,
//    command:        String,
//    inputs:         Seq[PrototypePair],
//    outputs:        Seq[PrototypePair],
//    libraries:      Option[String],
//    resources:      Resources,
//    data:           JarWizardData): WizardToTask
//
//  def parse(safePath: SafePath): Option[LaunchingCommand]
//
//  def jarClasses(jarPath: SafePath): Seq[FullClass]
//
//  def jarMethods(jarPath: SafePath, classString: String): Seq[JarMethod]
//}