/*
 Copyright 2008-2011 Gephi
 Authors : Mathieu Bastian <mathieu.bastian@gephi.org>
 Website : http://www.gephi.org

 This file is part of Gephi.

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright 2011 Gephi Consortium. All rights reserved.

 The contents of this file are subject to the terms of either the GNU
 General Public License Version 3 only ("GPL") or the Common
 Development and Distribution License("CDDL") (collectively, the
 "License"). You may not use this file except in compliance with the
 License. You can obtain a copy of the License at
 http://gephi.org/about/legal/license-notice/
 or /cddl-1.0.txt and /gpl-3.0.txt. See the License for the
 specific language governing permissions and limitations under the
 License.  When distributing the software, include this License Header
 Notice in each file and include the License files at
 /cddl-1.0.txt and /gpl-3.0.txt. If applicable, add the following below the
 License Header, with the fields enclosed by brackets [] replaced by
 your own identifying information:
 "Portions Copyrighted [year] [name of copyright owner]"

 If you wish your version of this file to be governed by only the CDDL
 or only the GPL Version 3, indicate your decision by adding
 "[Contributor] elects to include this software in this distribution
 under the [CDDL or GPL Version 3] license." If you do not indicate a
 single choice of license, a recipient has the option to distribute
 your version of this file under either the CDDL, the GPL Version 3 or
 to extend the choice of license to its licensees as provided above.
 However, if you add GPL Version 3 code and therefore, elected the GPL
 Version 3 license, then the option applies only if the new code is
 made subject to such option by the copyright holder.

 Contributor(s):

 Portions Copyrighted 2011 Gephi Consortium.
 */
package ubco;

import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.io.generator.spi.Generator;
import org.gephi.io.generator.spi.GeneratorUI;
import org.gephi.io.importer.api.ContainerLoader;
import org.gephi.io.importer.api.EdgeDirectionDefault;
import org.gephi.utils.progress.ProgressTicket;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;
import ubco.algorithm.QuasiThresholdMover;
import ubco.utility.GraphTranslator;

import javax.swing.*;

/**
 * Generates a quasi-threshold graph from the graph in the current workspace.
 * There are two options: the first is to show the complete edited graph,
 * and the second is to show only the tree skeleton as a directed graph
 * that implicitly defines the qt graph.
 *
 * @author Zach Holland
 */
@ServiceProvider(service = Generator.class)
public class QtCommunitiesGenerator implements Generator {

    protected ProgressTicket progress;
    protected boolean cancel = false;

    private boolean showTransitiveClosures;

    @Override
    public void generate(ContainerLoader container) {
        // Get the graph from the current workspace.
        GraphController graphController = Lookup.getDefault().lookup(GraphController.class);
        GraphModel graphModel = graphController.getGraphModel();
        Graph graph = graphModel.getGraph();

        graph.readLock();

        // Edit the graph using the qtm algorithm.
        QuasiThresholdMover<Integer> qtm = new QuasiThresholdMover<>(GraphTranslator.gephiToJung(graph), Integer.MAX_VALUE);
        edu.uci.ics.jung.graph.Graph<Integer, String> resultGraph = qtm.doQuasiThresholdMover(showTransitiveClosures, false);

        // Edges are directed if only showing the tree skeleton.
        container.setEdgeDefault(showTransitiveClosures ? EdgeDirectionDefault.UNDIRECTED : EdgeDirectionDefault.DIRECTED);

        // Build the graph in Gephi.
        GraphTranslator.jungToGephi(container, graph, resultGraph, showTransitiveClosures);

        graph.readUnlock();
    }

    @Override
    public String getName() {
        return "Generate QT Communities";
    }

    public void setShowTransitiveClosures(boolean showTransitiveClosures) {
        this.showTransitiveClosures = showTransitiveClosures;
    }

    @Override
    public GeneratorUI getUI() {
        // Creates the panel that gives the choice between the two graph display options.
        return new GeneratorUI() {
            private QtCommunitiesGeneratorPanel panel;
            private QtCommunitiesGenerator qtGenerator;

            @Override
            public JPanel getPanel() {
                if (panel == null) {
                    panel = new QtCommunitiesGeneratorPanel();
                }
                return panel;
            }

            @Override
            public void setup(Generator generator) {
                qtGenerator = (QtCommunitiesGenerator) generator;
                if (panel == null) {
                    panel = new QtCommunitiesGeneratorPanel();
                }
            }

            @Override
            public void unsetup() {
                ButtonModel buttonModel = panel.getButtonGroup().getSelection();
                qtGenerator.setShowTransitiveClosures(QtCommunitiesGeneratorPanel.SHOW_COMPLETE_GRAPH.equals(buttonModel.getActionCommand()));
                panel = null;
            }
        };
    }

    @Override
    public boolean cancel() {
        cancel = true;
        return true;
    }

    @Override
    public void setProgressTicket(ProgressTicket progressTicket) {
        this.progress = progressTicket;
    }
}
