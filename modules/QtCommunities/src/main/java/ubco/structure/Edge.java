package ubco.structure;

public class Edge<T> {
    private T _id;
    private int _numTriangles;

    public Edge(T id) {
        _id = id;
        _numTriangles = 0;
    }

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
