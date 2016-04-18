package ubco.structure;

/**
 * Represents an edge in a graph.
 *
 * @param <T> The type of the id of the edge.
 * @author Zach Holland
 */
public class Edge<T> {
    private T _id;
    private int _numTriangles;

    /**
     * Creates an edge with the given id.
     *
     * @param id The id of the edge.
     */
    public Edge(T id) {
        _id = id;
        _numTriangles = 0;
    }

    /**
     * Creates an edge with the given id and whose triangle counter is set to the given number.
     * Note: setting the triangle counter does not affect how many triangles the edge participates in;
     * it is used to simple keep track of the number.
     *
     * @param id           The id of
     * @param numTriangles The number of triangles that this edge participates in.
     */
    public Edge(T id, int numTriangles) {
        _id = id;
        _numTriangles = numTriangles;
    }

    public T getId() {
        return _id;
    }

    public int getNumTriangles() {
        return _numTriangles;
    }

    public void setNumTriangles(int numTriangles) {
        _numTriangles = numTriangles;
    }

    @Override
    public String toString() {
        return _id.toString();
    }
}
