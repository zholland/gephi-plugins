package ubco.utility;

import edu.uci.ics.jung.graph.Graph;
import ubco.structure.Edge;
import ubco.structure.Vertex;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class TriangleCounter<T extends Comparable<T>> {
    private Graph<Vertex<T>, Edge<String>> _graph;

    public TriangleCounter(Graph<Vertex<T>, Edge<String>> graph) {
        _graph = graph;
    }

    private HashSet<Vertex<T>> getNeighborSet(Vertex<T> v, Vertex<T> exclude) {
        return _graph.getNeighbors(v)
                .stream()
                .filter(id -> !id.equals(exclude))
                .collect(Collectors.toCollection(HashSet::new));
    }

    public int countTriangles(Vertex<T> v1, Vertex<T> v2) {
        HashSet<Vertex<T>> vertexSet = getNeighborSet(v1, v2);
        vertexSet.retainAll(getNeighborSet(v2, v1));

        return vertexSet.size();
    }

    public static <V extends Comparable<V>> void countAllTriangles(Graph<Vertex<V>, Edge<String>> graph) {
        TreeSet<Vertex<V>> vertices = graph.getVertices().stream()
                .collect(Collectors.toCollection(TreeSet::new));

        HashMap<Vertex<V>, Set<Vertex<V>>> vertexMap = new HashMap<>(graph.getVertexCount());
        graph.getVertices().forEach(v -> vertexMap.put(v, new TreeSet<>()));

        vertices.forEach(s -> graph.getNeighbors(s)
                .forEach(t -> {
                    int sDegree = graph.degree(s);
                    int tDegree = graph.degree(t);
                    if (sDegree > tDegree || (sDegree == tDegree && s.compareTo(t) < 0)) {
                        Set<Vertex<V>> sSet = vertexMap.get(s);
                        Set<Vertex<V>> tSet = vertexMap.get(t);
                        Set<Vertex<V>> intersection = new TreeSet<>(sSet);
                        intersection.retainAll(tSet);
                        intersection.forEach(v -> {
                            Edge<String> edgeST = graph.findEdge(s, t);
                            edgeST.setNumTriangles(edgeST.getNumTriangles() + 1);
                            Edge<String> edgeSV = graph.findEdge(s, v);
                            edgeSV.setNumTriangles(edgeSV.getNumTriangles() + 1);
                            Edge<String> edgeVT = graph.findEdge(v, t);
                            edgeVT.setNumTriangles(edgeVT.getNumTriangles() + 1);
                        });
                        vertexMap.get(t).add(s);
                    }
                }));
    }
}
