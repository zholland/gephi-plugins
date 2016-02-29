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

@ServiceProvider(service = Generator.class)
public class QtCommunitiesGenerator implements Generator {

    protected ProgressTicket progress;
    protected boolean cancel = false;

    @Override
    public void generate(ContainerLoader container) {
        GraphController graphController = Lookup.getDefault().lookup(GraphController.class);
        GraphModel graphModel = graphController.getGraphModel();
        Graph graph = graphModel.getGraph();

        graph.readLock();

        QuasiThresholdMover<Integer> qtm = new QuasiThresholdMover<>(GraphTranslator.gephiToJung(graph), Integer.MAX_VALUE);
        edu.uci.ics.jung.graph.Graph<Integer, String> resultGraph = qtm.doQuasiThresholdMover(true);

        container.setEdgeDefault(EdgeDirectionDefault.UNDIRECTED);
        GraphTranslator.jungToGephi(container, graph, resultGraph);

        graph.readUnlock();
    }

    @Override
    public String getName() {
        return "Generate QT Communities";
    }

    @Override
    public GeneratorUI getUI() {
        return null;
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
