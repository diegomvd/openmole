/*
 *  Copyright © 2008, Cemagref
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston,
 *  MA  02110-1301  USA
 */
package org.simexplorer.ui.ide.structure;

import org.openmole.core.structuregenerator.ComplexNode;
import java.io.Serializable;
import java.util.logging.Logger;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.explorer.view.NodeTableModel;
import org.openide.explorer.view.TreeTableView;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.simexplorer.ui.ide.workflow.model.ApplicationsExplorerService;
//import org.openide.util.Utilities;

/**
 * Top component which displays something.
 */
public final class InputStructureTopComponent extends TopComponent implements ExplorerManager.Provider {

    private static InputStructureTopComponent instance;
    /** path to the icon used by the component and its open action */
//    static final String ICON_PATH = "SET/PATH/TO/ICON/HERE";
    private static final String PREFERRED_ID = "InputStructureTopComponent";
    private NodeTableModel nodeTableModel;
    private final ExplorerManager explorerManager = new ExplorerManager();
    private ComplexNode inputStructure;

    private InputStructureTopComponent() {
        nodeTableModel = new NodeTableModel();
        nodeTableModel.setProperties(new Node.Property[]{new StructureNodeNode.TypeProperty()});
        initComponents();
        ((TreeTableView) jScrollPane1).setRootVisible(true);
        setName(NbBundle.getMessage(InputStructureTopComponent.class, "CTL_InputStructureTopComponent"));
        setToolTipText(NbBundle.getMessage(InputStructureTopComponent.class, "HINT_InputStructureTopComponent"));
        associateLookup(ExplorerUtils.createLookup(explorerManager, getActionMap()));

//        setIcon(Utilities.loadImage(ICON_PATH, true));
    }

    // TODO maybe use the lookup
    public void applicationUpdated() {
        ApplicationsExplorerService applicationsExplorer = Lookup.getDefault().lookup(ApplicationsExplorerService.class);
        inputStructure = applicationsExplorer.getExplorationApplication().getInputStructure();
        if (inputStructure != null) {
            ComplexNodeNode rootNode = new ComplexNodeNode(inputStructure, null);
            explorerManager.setRootContext(rootNode);
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new TreeTableView(nodeTableModel);

        setLayout(new java.awt.BorderLayout());
        add(jScrollPane1, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables

    /**
     * Gets default instance. Do not use directly: reserved for *.settings files only,
     * i.e. deserialization routines; otherwise you could get a non-deserialized instance.
     * To obtain the singleton instance, use {@link findInstance}.
     */
    public static synchronized InputStructureTopComponent getDefault() {
        if (instance == null) {
            instance = new InputStructureTopComponent();
        }
        return instance;
    }

    /**
     * Obtain the InputStructureTopComponent instance. Never call {@link #getDefault} directly!
     */
    public static synchronized InputStructureTopComponent findInstance() {
        TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        if (win == null) {
            Logger.getLogger(InputStructureTopComponent.class.getName()).warning(
                    "Cannot find " + PREFERRED_ID + " component. It will not be located properly in the window system.");
            return getDefault();
        }
        if (win instanceof InputStructureTopComponent) {
            return (InputStructureTopComponent) win;
        }
        Logger.getLogger(InputStructureTopComponent.class.getName()).warning(
                "There seem to be multiple components with the '" + PREFERRED_ID +
                "' ID. That is a potential source of errors and unexpected behavior.");
        return getDefault();
    }

    @Override
    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_ALWAYS;
    }

    @Override
    public void componentOpened() {
        // TODO add custom code on component opening
    }

    @Override
    public void componentClosed() {
        // TODO add custom code on component closing
    }

    /** replaces this in object stream */
    @Override
    public Object writeReplace() {
        return new ResolvableHelper();
    }

    @Override
    protected String preferredID() {
        return PREFERRED_ID;
    }

    final static class ResolvableHelper implements Serializable {

        private static final long serialVersionUID = 1L;

        public Object readResolve() {
            return InputStructureTopComponent.getDefault();
        }
    }

    @Override
    public ExplorerManager getExplorerManager() {
        return explorerManager;
    }
}
