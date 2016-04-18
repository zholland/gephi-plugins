package ubco.utility;

import edu.uci.ics.jung.graph.Graph;
import ubco.structure.Edge;
import ubco.structure.Vertex;

import java.util.HashSet;
import java.util.Set;

/**
 * Provides the ability to estimate the number of C4s and P4s that an edge participates in.
 * Used in conjunction with Triangle counter. If an edge has a high score, it is likely that
 * it should be deleted in the initialization portion of the algorithm.
 *
 * @param <V> The id type of the vertices.
 * @author Zach Holland
 */
public class PseudoC4P4Counter<V extends Comparable<V>> {
    private Graph<Vertex<V>, Edge<String>> _graph;
    private Set<Edge<String>> _infinities;

    /**
     * Creates a new PseudoC4P4Counter object.
     *
     * @param graph The graph on which to estimate the C4s and P4s.
     */
    public PseudoC4P4Counter(Graph<Vertex<V>, Edge<String>> graph) {
        _graph = graph;
        _infinities = new HashSet<>();
    }

    /**
     * Returns the sum of the number of C4s in which 'v1 v2' participates and the number of P4 in
     * which 'v1 v2' participates as central edge.
     *
     * @param v1 The first vertex.
     * @param v2 The second vertex.
     * @return The sum of the number of C4s in which 'v1 v2' participates and the number of P4 in
     * which 'v1 v2' participates as central edge.
     */
    public int score(Vertex<V> v1, Vertex<V> v2) {
        Edge<String> edge = _graph.findEdge(v1, v2);
        if (_infinities.contains(edge)) {
            return Integer.MAX_VALUE;
        } else {
            return (_graph.degree(v1) - 1 - edge.getNumTriangles()) * (_graph.degree(v2) - 1 - edge.getNumTriangles());
        }
    }

    /**
     * Set the score of the given edge to infinity.
     *
     * @param v1 The first vertex.
     * @param v2 The second vertex.
     */
    public void setToInfinity(Vertex<V> v1, Vertex<V> v2) {
        Edge<String> edge = _graph.findEdge(v1, v2);
        _infinities.add(edge);
    }
}
