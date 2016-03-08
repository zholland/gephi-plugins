package ubco.utility;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseGraph;
import org.gephi.graph.api.Node;
import org.gephi.io.importer.api.ContainerLoader;
import org.gephi.io.importer.api.EdgeDraft;
import org.gephi.io.importer.api.NodeDraft;
import ubco.structure.Edge;
import ubco.structure.Vertex;

import java.util.HashMap;
import java.util.Map;

public class GraphTranslator {
    public static Graph<Vertex<Integer>, Edge<String>> gephiToJung(org.gephi.graph.api.Graph gephiGraph) {
        Graph<Vertex<Integer>, Edge<String>> graph = new SparseGraph<>();

        Map<Node, Vertex<Integer>> nodes = new HashMap<>();
        for (Node n : gephiGraph.getNodes()) {
            Vertex<Integer> v = new Vertex<>(Integer.valueOf((String) n.getId()));
            nodes.put(n, v);
            graph.addVertex(v);
        }

        for (org.gephi.graph.api.Edge e : gephiGraph.getEdges()) {
            Node n1 = e.getSource();
            Node n2 = e.getTarget();
            Vertex<Integer> v1 = nodes.get(n1);
            Vertex<Integer> v2 = nodes.get(n2);

            graph.addEdge(new Edge<>(v1.getId() + "-" + v2.getId()), v1, v2);
        }

        return graph;
    }

    public static void jungToGephi(ContainerLoader container,
                                   org.gephi.graph.api.Graph gephiGraph,
                                   Graph<Integer, String> jungGraph,
                                   boolean showTransitiveClosures) {
        // create nodes
        Map<Integer, NodeDraft> nodes = new HashMap<>();
        jungGraph.getVertices()
                .stream()
                .filter(v -> v != Integer.MAX_VALUE)
                .forEach(v -> {
                    NodeDraft nd = container.getNode(v.toString());
                    nd.setLabel(v.toString());
                    nodes.put(v, nd);
                    container.addNode(nd);
                });

//        if (!showTransitiveClosures) {
//            computePositions(nodes, gephiGraph);
//        }

        // create edges
        jungGraph.getEdges()
                .stream()
                .filter(e -> jungGraph.getSource(e) != Integer.MAX_VALUE)
                .forEach(e -> {
                    EdgeDraft ed = container.factory().newEdgeDraft();
                    ed.setSource(nodes.get(jungGraph.getSource(e)));
                    ed.setTarget(nodes.get(jungGraph.getDest(e)));
                    container.addEdge(ed);
                });
    }

//    private static void computePositions(Map<Integer, NodeDraft> nodes, org.gephi.graph.api.Graph gephiGraph) {
//
//    }
}
