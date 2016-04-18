package ubco.utility;

import edu.uci.ics.jung.graph.Graph;
import ubco.structure.Edge;
import ubco.structure.Vertex;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Provides the ability to count the triangles in a graph either by creating a TriangleCounter object,
 * or using the static countAllTriangles method.
 *
 * @param <T> The id type of the Vertex object used in the Graph.
 * @author Zach Holland
 */
public class TriangleCounter<T extends Comparable<T>> {

    private Graph<Vertex<T>, Edge<String>> _graph;

    /**
     * Creates a new TriangleCounter object.
     *
     * @param graph The graph whose triangles are to be counted.
     */
    public TriangleCounter(Graph<Vertex<T>, Edge<String>> graph) {
        _graph = graph;
    }

    /**
     * Gets the set of neighboring vertices, excluding the 'exclude' vertex.
     *
     * @param v       The vertex to get the neighbors of.
     * @param exclude The vertex to exclude from the neighborhood.
     * @return A set containing the neighbors of 'v', excluding 'exclude'
     */
    private HashSet<Vertex<T>> getNeighborSet(Vertex<T> v, Vertex<T> exclude) {
        return _graph.getNeighbors(v)
                       .stream()
                       .filter(id -> !id.equals(exclude))
                       .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * Counts the number of triangles that the edge 'v1 v2' is involved in.
     *
     * @param v1 The first vertex of the edge.
     * @param v2 The second vertex of the edge.
     * @return The number of triangles that the edge 'v1 v2' is involved in.
     */
    public int countTriangles(Vertex<T> v1, Vertex<T> v2) {
        HashSet<Vertex<T>> vertexSet = getNeighborSet(v1, v2);
        vertexSet.retainAll(getNeighborSet(v2, v1));

        return vertexSet.size();
    }

    /**
     * Efficiently counts all of the triangles in the given graph and stores the number of triangles that each edge
     * participates in, in the Edge object.
     * <p>
     * Uses the Fast-Forward triangle counting algorithm. See T. Schank's dissertation "Algorithmic Aspects of
     * Triangle-Based Network Analysis"
     * URL: digbib.ubka.uni-karlsruhe.de/valltexte/documents/4541
     *
     * @param graph The graph to count all of the triangles in.
     * @param <V>   The id type of the vertices.
     */
    public static <V extends Comparable<V>> void countAllTriangles(Graph<Vertex<V>, Edge<String>> graph) {
        // Get the set of vertices sorted by descending degree.
        TreeSet<Vertex<V>> vertices = graph.getVertices().stream()
                                              .collect(Collectors.toCollection(TreeSet::new));

        // Create a map to store a set of vertices associated with each vertex.
        // This is A(v_i) in the psudocode.
        HashMap<Vertex<V>, Set<Vertex<V>>> vertexMap = new HashMap<>(graph.getVertexCount());
        graph.getVertices().forEach(v -> vertexMap.put(v, new TreeSet<>()));

        vertices.forEach(s -> graph.getNeighbors(s)
                                      .forEach(t -> {
                                          int sDegree = graph.degree(s);
                                          int tDegree = graph.degree(t);

                                          // Proceed only if the degree of s is greater than the degree of t OR if the
                                          // degrees are equal and s comes before t in the natural ordering of the
                                          // vertex type. This is required to maintain an absolute ordering of the
                                          // vertices.
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
