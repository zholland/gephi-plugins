package ubco.utility;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseGraph;
import edu.uci.ics.jung.graph.util.Pair;
import org.gephi.graph.api.Node;
import org.gephi.io.importer.api.ContainerLoader;
import org.gephi.io.importer.api.EdgeDraft;
import org.gephi.io.importer.api.NodeDraft;
import ubco.structure.Edge;
import ubco.structure.Vertex;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class that translates a graph from the gephi format to jung and vice versa.
 *
 * @author Zach Holland
 */
public class GraphTranslator {
    /**
     * Translates a graph in the Gephi format to a JUNG graph.
     *
     * @param gephiGraph The Gephi graph to transform.
     * @return The JUNG graph.
     */
    public static Graph<Vertex<Integer>, Edge<String>> gephiToJung(org.gephi.graph.api.Graph gephiGraph) {
        Graph<Vertex<Integer>, Edge<String>> graph = new SparseGraph<>();

        // Create nodes
        Map<Node, Vertex<Integer>> nodes = new HashMap<>();
        for (Node n : gephiGraph.getNodes()) {
            Vertex<Integer> v = new Vertex<>(Integer.valueOf((String) n.getId()));
            nodes.put(n, v);
            graph.addVertex(v);
        }

        // Create edges
        for (org.gephi.graph.api.Edge e : gephiGraph.getEdges()) {
            Node n1 = e.getSource();
            Node n2 = e.getTarget();
            Vertex<Integer> v1 = nodes.get(n1);
            Vertex<Integer> v2 = nodes.get(n2);

            graph.addEdge(new Edge<>(v1.getId() + "-" + v2.getId()), v1, v2);
        }

        return graph;
    }

    /**
     * Translates a graph in the JUNG format to a Gephi graph.
     *
     * @param container              The Gephi container to place the graph.
     * @param gephiGraph             The Gephi graph.
     * @param jungGraph              The JUNG graph to build in Gephi.
     * @param showTransitiveClosures true builds the entire edited graph, false builds only the tree skeleton.
     */
    public static void jungToGephi(ContainerLoader container,
                                   org.gephi.graph.api.Graph gephiGraph,
                                   Graph<Integer, String> jungGraph,
                                   boolean showTransitiveClosures) {
        // Create nodes
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

        // Create edges
        // Choice depends on if entire edited graph is to be displayed or just the tree skeleton
        if (showTransitiveClosures) {
            jungGraph.getEdges().forEach(e -> {
                EdgeDraft ed = container.factory().newEdgeDraft();
                Pair<Integer> endPoints = jungGraph.getEndpoints(e);
                if (endPoints.getFirst() != Integer.MAX_VALUE && endPoints.getSecond() != Integer.MAX_VALUE) {
                    ed.setSource(nodes.get(endPoints.getFirst()));
                    ed.setTarget(nodes.get(endPoints.getSecond()));
                    container.addEdge(ed);
                }
            });
        } else {
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
    }
}
