package ubco.utility;

import edu.uci.ics.jung.graph.Graph;
import ubco.structure.Edge;
import ubco.structure.Vertex;

import java.util.HashSet;
import java.util.Set;

public class PseudoC4P4Counter<V extends Comparable<V>> {
    private Graph<Vertex<V>, Edge<String>> _graph;
    private Set<Edge<String>> _infinities;

    public PseudoC4P4Counter(Graph<Vertex<V>, Edge<String>> graph) {
        _graph = graph;
        _infinities = new HashSet<>();
    }

    public int score(Vertex<V> v1, Vertex<V> v2) {
        Edge<String> edge = _graph.findEdge(v1, v2);
        if (_infinities.contains(edge)) {
            return Integer.MAX_VALUE;
        } else {
            return (_graph.degree(v1) - 1 - edge.getNumTriangles()) * (_graph.degree(v2) - 1 - edge.getNumTriangles());
        }
    }

    public void setToInfinity(Vertex<V> v1, Vertex<V> v2) {
        Edge<String> edge = _graph.findEdge(v1, v2);
        _infinities.add(edge);
    }
}
