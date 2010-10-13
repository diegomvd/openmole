/*
 *  Copyright © 2009, Cemagref
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
package org.simexplorer.ide.ui.domain;

import java.awt.CardLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultComboBoxModel;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;
import org.openmole.core.model.domain.IDomain;
import org.simexplorer.ide.ui.PanelFactory;
import org.simexplorer.core.workflow.methods.DomainEditorPanel;
import org.simexplorer.core.workflow.methods.EditorPanel;
import org.simexplorer.core.workflow.model.metada.MetadataLoader;

/**
 *
 * @author thierry
 */
@ServiceProvider(service=EditorPanel.class)
public class QuantitativeDomainPanel extends DomainEditorPanel<IDomain> {

    private Map<Class, Integer> position = new HashMap<Class, Integer>();
    private Class domain;
    // to be able to retrieve the current displayed editor
    private Map<Class<? extends IDomain>, EditorPanel<IDomain>> domainEditors;
    // current displayed editor
    private EditorPanel<IDomain> domainEditor;

    public QuantitativeDomainPanel() {
        super(IDomain.class);
        domainEditors = new HashMap<Class<? extends IDomain>, EditorPanel<IDomain>>();
        initComponents();
        this.factorTypeComboBoxActionPerformed(null);
        int i = 0;
        for (Class cl : QuantitativeDomain.getFactorDomains().keySet()) {
            for (Class<? extends IDomain> domain0 : QuantitativeDomain.getFactorDomains().get(cl)) {
                EditorPanel<IDomain> ed = (EditorPanel<IDomain>) PanelFactory.getEditor(domain0);
                if (ed.getObjectEdited() == null) {
                    try {
                        ed.setObjectEdited((IDomain) domain0.newInstance());
                    } catch (InstantiationException ex) {
                        Exceptions.printStackTrace(ex);
                    } catch (IllegalAccessException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
                domainConfigurationPanel.add(ed, domain0.getCanonicalName());
                domainEditors.put(domain0, ed);
                position.put(domain0, i++);
            }
        }
    }

    @Override
    public void setObjectEdited(IDomain range) {
        super.setObjectEdited(range);
        if ((range != null) && !(range instanceof QuantitativeDomain)) {
            ((EditorPanel<IDomain>) domainConfigurationPanel.getComponent(position.get(range.getClass()))).setObjectEdited(range);
            Class c = QuantitativeDomain.getDomainObjectType(range.getClass());
            factorTypeComboBox.setSelectedItem(c);
            domainComboBox.setSelectedItem(range.getClass());
        }
    }

    @Override
    public void applyChanges() {
        if ((domainConfigurationPanel.getComponentCount() > 0) && (domain != null)) {
            ((EditorPanel<IDomain>) domainConfigurationPanel.getComponent(position.get(domain))).applyChanges();
        }
    }

    @Override
    public String isInputValid() {
        return domainEditor != null ? domainEditor.isInputValid() : super.isInputValid();
    }


    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        quantitativePanel = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        factorTypeComboBox = new javax.swing.JComboBox();
        domainComboBox = new javax.swing.JComboBox();
        domainConfigurationPanel = new javax.swing.JPanel();

        jLabel6.setText(org.openide.util.NbBundle.getMessage(QuantitativeDomainPanel.class, "QuantitativeDomainPanel.jLabel6.text")); // NOI18N

        jLabel7.setText(org.openide.util.NbBundle.getMessage(QuantitativeDomainPanel.class, "QuantitativeDomainPanel.jLabel7.text")); // NOI18N

        factorTypeComboBox.setModel(new DefaultComboBoxModel(QuantitativeDomain.getFactorDomains().keySet().toArray()));
        factorTypeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                factorTypeComboBoxActionPerformed(evt);
            }
        });

        domainComboBox.setEnabled(false);
        domainComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                domainComboBoxActionPerformed(evt);
            }
        });

        domainConfigurationPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        domainConfigurationPanel.setLayout(new java.awt.CardLayout());

        javax.swing.GroupLayout quantitativePanelLayout = new javax.swing.GroupLayout(quantitativePanel);
        quantitativePanel.setLayout(quantitativePanelLayout);
        quantitativePanelLayout.setHorizontalGroup(
            quantitativePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(quantitativePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(quantitativePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(domainConfigurationPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 365, Short.MAX_VALUE)
                    .addGroup(quantitativePanelLayout.createSequentialGroup()
                        .addGroup(quantitativePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel6)
                            .addComponent(jLabel7))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 166, Short.MAX_VALUE)
                        .addGroup(quantitativePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(domainComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(factorTypeComboBox, 0, 150, Short.MAX_VALUE))))
                .addContainerGap())
        );
        quantitativePanelLayout.setVerticalGroup(
            quantitativePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(quantitativePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(quantitativePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(factorTypeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(quantitativePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel7)
                    .addComponent(domainComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(domainConfigurationPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 208, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 389, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(quantitativePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 298, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(quantitativePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void factorTypeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_factorTypeComboBoxActionPerformed
        if (factorTypeComboBox.getSelectedItem() != null) {
            domainComboBox.setEnabled(true);
            List<Class<? extends IDomain>> domains = QuantitativeDomain.getFactorDomains().get(factorTypeComboBox.getSelectedItem());
            ArrayList<String> domainsArray = new ArrayList<String>();
            for (Class<? extends IDomain> type : domains) {
                domainsArray.add(MetadataLoader.loadMetadata(type).get("name"));
            }
            domainComboBox.setModel(new DefaultComboBoxModel(domainsArray.toArray()));
            this.domainComboBoxActionPerformed(null);
        } else {
            domainComboBox.setEnabled(false);
        }

        applyChanges();
}//GEN-LAST:event_factorTypeComboBoxActionPerformed

    private void domainComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_domainComboBoxActionPerformed
        if (domainComboBox.getSelectedItem() != null) {
            domain = (Class) QuantitativeDomain.getFactorDomains().get(factorTypeComboBox.getSelectedItem()).get(domainComboBox.getSelectedIndex());
            ((CardLayout) domainConfigurationPanel.getLayout()).show(domainConfigurationPanel, domain.getCanonicalName());
            domainEditor = domainEditors.get(domain);
            this.revalidate();
            applyChanges();
        }
}//GEN-LAST:event_domainComboBoxActionPerformed

    @Override
    public IDomain getObjectEdited() {
        return ((EditorPanel<IDomain>) domainConfigurationPanel.getComponent(position.get(domain))).getObjectEdited();
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox domainComboBox;
    private javax.swing.JPanel domainConfigurationPanel;
    private javax.swing.JComboBox factorTypeComboBox;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel quantitativePanel;
    // End of variables declaration//GEN-END:variables

    @Override
    public Class getType() {
        return (Class) factorTypeComboBox.getSelectedItem();
    }
}
