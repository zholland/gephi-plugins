package ubco.algorithm;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;
import ubco.structure.Edge;
import ubco.structure.Vertex;
import ubco.utility.PseudoC4P4Counter;
import ubco.utility.TriangleCounter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Represents an instance of a QuasiThresholdMover algorithm. The algorithm can be started by first creating a
 * QuasiThresholdMover object, then evoking the method doQuasiThresholdMover.
 *
 * @param <V> The id type of the vertex object.
 * @author Zach Holland
 */
public class QuasiThresholdMover<V extends Comparable<V>> {

    // The total number of iterations to run the algorithm.
    public static final int ITERATIONS = 5;

    // If simulated annealing is enabled, this determines the number of iterations that have a probability of
    // making a sub-optimal choice.
    public static final int ANNEALING_ITERATIONS = 0;

    // The initial probability of making a sub-optimal choice if simulated annealing is enabled.
    public static final double INITIAL_SUB_OPTIMAL_CHOICE_PROBABILITY = 0.1;

    // The working graph
    private Graph<Vertex<V>, Edge<String>> _graph;

    // The original input graph
    private Graph<Vertex<V>, Edge<String>> _originalGraph;

    // The universal root vertex
    private Vertex<V> _root;

    // Random generator.
    private Random _random;

    // For comparing the depth of two vertices. (Decreasing depth)
    private Comparator<Vertex<V>> _depthComparator = (v1, v2) -> v2.getDepth() - v1.getDepth(); // I don't think there will be an overflow.

    // Stores the close children of a given vertex.
    private Map<Vertex<V>, Set<Vertex<V>>> _closeChildren;

    // Stores the best parent of the sub-tree rooted at the key.
    // The best parent in the entire tree will be at the root.
    private Map<Vertex<V>, Vertex<V>> _bestParentMap;

    /**
     * Creates a new instance of a QuasiThresholdMover from the given input graph and root vertex.
     *
     * @param inputGraph The graph to run through the algorithm.
     * @param root       The vertex to use as the universal root.
     */
    public QuasiThresholdMover(Graph<Vertex<V>, Edge<String>> inputGraph, V root) {
        _random = new Random();

        // Save the input graph for comparison later.
        _originalGraph = inputGraph;

        // Create the universal vertex
        _root = new Vertex<>(root, inputGraph.getVertexCount(), null, 0);

        // Create a copy of the input graph for the algorithm to work on.
        _graph = new SparseGraph<>();
        inputGraph.getVertices().stream().forEach(v -> _graph.addVertex(v));
        inputGraph.getEdges().stream().forEach(e -> {
            Pair<Vertex<V>> endpoints = inputGraph.getEndpoints(e);
            _graph.addEdge(new Edge<>(endpoints.getFirst().getId() + "-" + endpoints.getSecond()), endpoints.getFirst(), endpoints.getSecond());
        });
    }

    /**
     * Perform the initialization step. Makes the first 'best-guess' at the optimal edited qt graph.
     */
    private void initialize() {
        long startTime = System.nanoTime();

        // Sort the vertices by descending degree.
        PriorityQueue<Vertex<V>> vertexQueue = new PriorityQueue<>((v1, v2) -> v2.getDegree() - v1.getDegree());

        // Add the universal root to every vertex and add it to the queue
        _root.setParent(_root);
        _graph.addVertex(_root);
        _graph.getVertices()
                .stream()
                .filter(v -> !v.equals(_root))
                .forEach(v -> {
                    v.setParent(_root);
                    v.setDepth(1);
                    _root.addChild(v);
                    _graph.addEdge(new Edge<>(_root.getId() + "-" + v.getId()), _root, v);
                    vertexQueue.add(v);
                });
        vertexQueue.add(_root);

        // Count all the triangles that each edge participates in.
        TriangleCounter.countAllTriangles(_graph);

        // Track all the nodes that have been processed.
        HashSet<Vertex<V>> processed = new HashSet<>(vertexQueue.size());

        PseudoC4P4Counter<V> pc = new PseudoC4P4Counter<>(_graph);
        while (!vertexQueue.isEmpty()) {
            Vertex<V> current = vertexQueue.poll();
            processed.add(current);

            // Get all the neighbors of the current vertex that are not processed
            // AND (that share the same parent as the current vertex
            //        OR (that have a PseudoC4P4 score between the current and the neighbor that is <= to the score between the neighbor and its parent
            //              AND that have a depth <= to the number of triangles that the edge between the current and neighbor are involved in + 1)
            TreeSet<Vertex<V>> neighbors = _graph.getNeighbors(current)
                                                   .stream()
                                                   .filter(v -> !processed.contains(v) && (Objects.equals(current.getParent(), v.getParent())
                                                                                                   || (pc.score(current, v) <= pc.score(v, v.getParent())
                                                                                                               && v.getDepth() <= _graph.findEdge(v, current).getNumTriangles() + 1)))
                                                   .collect(Collectors.toCollection(TreeSet::new));

            // Get the most frequent parent that appears in the set of neighbors.
            Map<Vertex<V>, Integer> parentOccurrences = new HashMap<>();
            neighbors.stream().map(Vertex::getParent).forEach(p -> {
                Integer count = parentOccurrences.get(p);
                if (count == null) {
                    parentOccurrences.put(p, 1);
                } else {
                    parentOccurrences.put(p, count + 1);
                }
            });
            Optional<Map.Entry<Vertex<V>, Integer>> optionalEntry = parentOccurrences.entrySet().stream().max((e1, e2) -> e1.getValue().compareTo(e2.getValue()));
            Vertex<V> tempParent = optionalEntry.isPresent() ? optionalEntry.get().getKey() : null;

            // If the most frequent parent is not equal to the current parent, change the parent to the most frequent parent.
            if (tempParent != null && !tempParent.equals(current.getParent())) {
                changeParent(current, tempParent);
                current.setDepth(0);
                pc.setToInfinity(current, tempParent);
            }

            // For each of the neighbors that have not been processed and meet the 'criteria', set their parent
            // to be the current vertex and increase their depth.
            _graph.getNeighbors(current)
                    .stream()
                    .filter(v -> !processed.contains(v))
                    .filter(v -> Objects.equals(current.getParent(), v.getParent())
                                         || (pc.score(current, v) < pc.score(v, v.getParent())
                                                     && v.getDepth() < _graph.findEdge(current, v).getNumTriangles() + 1))
                    .forEach(v -> {
                        changeParent(v, current);
                        v.setDepth(v.getDepth() + 1);
                    });
        }
        long endTime = System.nanoTime();
//        System.out.println((double)(endTime - startTime) / 1000000d);
    }

    /**
     * Changes the parent vertex of the given child vertex to the new parent.
     *
     * @param child     The vertex to change the parent of.
     * @param newParent The new parent vertex.
     */
    private void changeParent(Vertex<V> child, Vertex<V> newParent) {
        Vertex<V> oldParent = child.getParent();
        if (oldParent != null) {
            oldParent.removeChild(child);
        }
        child.setParent(newParent);
        if (newParent != null) {
            newParent.addChild(child);
        }
    }

    /**
     * Finds a new locally better position for the given vertex in the qt graph, and finds the children it should adopt.
     *
     * @param vm The vertex to find a new locally better position.
     */
    private void core(Vertex<V> vm) {
        // Queue of all vertices in T ordered by decreasing tree depth.
        Queue<Vertex<V>> queue = new PriorityQueue<>(_graph.getVertexCount(), _depthComparator);
        queue.addAll(_graph.getVertices());


        // Initialize the maps for child closeness, score_max, best parent, and the set of close children.
        Map<Vertex<V>, Integer> childCloseMap = new HashMap<>();
        Map<Vertex<V>, Integer> scoreMaxMap = new HashMap<>();
        _bestParentMap = new HashMap<>();
        _closeChildren = new HashMap<>();

        while (!queue.isEmpty()) {
            Vertex<V> v = queue.poll();

            int childCloseSum = 0;
            int childCloseSumOverCloseChildren = 0;

            Vertex<V> potentialBestChild = null;
            int potentialScoreMax = -1;

            for (Vertex<V> c : v.getChildren()) {
                int childCloseC = childCloseMap.get(c);

                // If the child closeness of c is greater than 0, add it to the set of close children for v.
                if (childCloseC > 0) {
                    Set<Vertex<V>> closeChildren = _closeChildren.get(v);
                    if (closeChildren == null) {
                        closeChildren = new TreeSet<>();
                    }
                    closeChildren.add(c);
                    _closeChildren.put(v, closeChildren);
                    // Increment the sum over all close children
                    childCloseSumOverCloseChildren += childCloseC;
                }
                // Increment the sum over all children
                childCloseSum += childCloseC;

                // if score_max(c) is better than the current potential score_max,
                // save the child and the score_max.
                int scoreMaxC = scoreMaxMap.get(c);
                if (scoreMaxC > potentialScoreMax) {
                    potentialScoreMax = scoreMaxC;
                    potentialBestChild = c;
                }
            }

            // Save the child closeness score of v.
            childCloseMap.put(v, childCloseSum + diff(vm, v));

            // If the score_max(potentialBestChild) is greater than the sum of the child closeness of all the close
            // children then save it as the best parent in the subtree rooted at v. Otherwise, the best parent
            // in the subtree rooted at v is v.
            if (potentialScoreMax > childCloseSumOverCloseChildren) {
                scoreMaxMap.put(v, potentialScoreMax + diff(vm, v));
                _bestParentMap.put(v, _bestParentMap.get(potentialBestChild));
            } else {
                scoreMaxMap.put(v, childCloseSumOverCloseChildren + diff(vm, v));
                _bestParentMap.put(v, v);
            }
        }
    }

    /**
     * Compute the diff function for the given vertices. It is 1 if they are neighbors, and -1 otherwise.
     *
     * @param vm The first vertex.
     * @param v  The second vertex.
     * @return 1 if they are neighbors, and -1 otherwise.
     */
    private int diff(Vertex<V> vm, Vertex<V> v) {
        return _graph.isNeighbor(vm, v) ? 1 : -1;
    }

    /**
     * Recursively compute all the depths of the children of the given vertex in the tree.
     *
     * @param v     The vertex to start at.
     * @param depth The starting depth.
     */
    private void computeDepths(Vertex<V> v, int depth) {
        v.setDepth(depth);
        v.getChildren().forEach(c -> computeDepths(c, depth + 1));
    }

    /**
     * Performs the Quasi-threshold Mover algorithm and returns an edited qt graph.
     * <p>
     * There are two options that affect the behaviour of the algorithm:
     * <ol>
     * <li>Show Transitive Closures
     * <ul>
     * <li>Show the entire edited graph. For this choose showTransitiveClosures to be true</li>
     * <li>Show only the skeleton tree structure that implies the qt graph. For this choose showTransitiveClosures
     * to be false.</li>
     * </ul>
     * </li>
     * <li>Simulated Annealing
     * <ul>
     * <li>Allow the algorithm to make the occasional sub-optimal choice. The probability of making a sub-optimal
     * choice decreases as the iteration. This can keep the algorithm from getting stuck at a local minimum.
     * To enable this choose simulatedAnnealing to be true.</li>
     * <li>Only allow the algorithm to make the locally optimal choice. This is the standard behaviour. For this
     * choose simulatedAnnealing to be false.</li>
     * </ul></li>
     * </ol>
     *
     * @param showTransitiveClosures true returns the actual edited graph,
     *                               and false returns only the skeleton tree structure.
     * @param simulatedAnnealing     true allows the algorithm to make a sub-optimal choice, and false only makes
     *                               locally optimal choices.
     * @return A quasi-threshold graph, or a tree which implies a qt graph.
     */
    public Graph<V, String> doQuasiThresholdMover(boolean showTransitiveClosures, boolean simulatedAnnealing) {
        // Run the initialize algorithm
        initialize();

        // Compute the depths of the resulting tree.
        computeDepths(_root, 0);

        ArrayList<Vertex<V>> vertices = _graph.getVertices()
                                                .stream()
                                                .filter(vm -> vm != _root)
                                                .collect(Collectors.toCollection(ArrayList::new));

        // Do a number of iterations equal to ITERATIONS
        for (int i = 0; i < ITERATIONS; i++) {
            long startTime = System.nanoTime();

            // Shuffle the order that the vertices are examined in.
            Collections.shuffle(vertices);

            final int finalI = i;

            // Run the core algorithm on each vertex and pick the best parent.
            vertices.forEach(vm -> {
                Vertex<V> oldParent = vm.getParent();
                ArrayList<Vertex<V>> oldChildren = vm.getChildren();

                // Remove the vertex vm from the graph
                changeParent(vm, _root);
                vm.setDepth(1);

                // Move children to the vm parent
                vm.getChildren().forEach(c -> adjustChildrenDepth(c, -1));
                vm.setChildren(new ArrayList<>());
                oldParent.getChildren().addAll(oldChildren);
                oldChildren.forEach(c -> c.setParent(oldParent));

                // Find the new best parent by running the core algorithm.
                core(vm);

                // If simulatedAnnealing is enabled, there is a chance that a random vertex is selected to be the
                // new parent. Otherwise select the new parent as returned by the core algorithm.
                Vertex<V> newParent;
                if (simulatedAnnealing) {
                    if (_random.nextDouble() > ((double) finalI / ANNEALING_ITERATIONS)
                                                       * INITIAL_SUB_OPTIMAL_CHOICE_PROBABILITY
                                                       + (1d - INITIAL_SUB_OPTIMAL_CHOICE_PROBABILITY)) {
                        ArrayList<Vertex<V>> vertexList = _graph.getVertices()
                                                                  .stream()
                                                                  .filter(v -> v != vm)
                                                                  .collect(Collectors.toCollection(ArrayList::new));
                        newParent = vertexList.get(_random.nextInt(vertexList.size()));
                    } else {
                        newParent = _bestParentMap.get(_root);
                    }
                } else {
                    newParent = _bestParentMap.get(_root);
                }
                // Change to the new parent and update the depth.
                changeParent(vm, newParent);
                vm.setDepth(newParent.getDepth() + 1);

                // Adopt all the children of the new parent that are close to vm.
                Set<Vertex<V>> childrenToAdopt = _closeChildren.get(newParent);
                if (childrenToAdopt != null) {
                    newParent.getChildren().removeAll(childrenToAdopt);
                    vm.setChildren(new ArrayList<>(childrenToAdopt));
                    childrenToAdopt.forEach(c -> adjustChildrenDepth(c, 1));
                    childrenToAdopt.forEach(c -> c.setParent(vm));
                }
            });
            long endTime = System.nanoTime();

            // ************************************
            // Uncomment this to track the number of edits after each iteration.
            //*************************************
//            System.out.println("Edits (iteration " + (i + 1) + "): "
//                                       + GraphEditCounter.numberOfEditsAfterFinished(_originalGraph, buildQtGraph(true)));
        }
        return buildQtGraph(showTransitiveClosures);
    }

    /**
     * Adjusts the depth counter of the given vertex.
     *
     * @param v          The vertex whose depth needs to be adjusted.
     * @param adjustment The amount by which to adjust the depth.
     */
    private void adjustChildrenDepth(Vertex<V> v, int adjustment) {
        v.setDepth(v.getDepth() + adjustment);
        v.getChildren().forEach(c -> adjustChildrenDepth(c, adjustment));
    }

    /**
     * Builds a graph from the tree structure output from the core algorithm.
     * <p>
     * There are two options:
     * <ol>
     * <li>Show the entire edited graph. For this choose showTransitiveClosures to be true</li>
     * <li>Show only the skeleton tree structure that implies the qt graph. For this choose showTransitiveClosures
     * to be false.</li>
     * </ol>
     *
     * @param showTransitiveClosures true returns the actual edited graph,
     *                               and false returns only the skeleton tree structure.
     * @return A quasi-threshold graph, or a tree which implies a qt graph.
     */
    private Graph<V, String> buildQtGraph(boolean showTransitiveClosures) {
        Graph<V, String> returnGraph = new SparseGraph<>();

        if (showTransitiveClosures) {
            _root.getChildren().stream().forEach(v -> {
                Set<Vertex<V>> ancestors = new HashSet<>();
                addEdges(v, ancestors, returnGraph);
            });
        } else {
            bfsBuildGraph(returnGraph, _root);
        }
        _graph.removeVertex(_root);
        return returnGraph;
    }

    private void bfsBuildGraph(Graph<V, String> returnGraph, Vertex<V> current) {
        if (current.getParent() != _root) {
            returnGraph.addVertex(current.getId());
            returnGraph.addEdge(current.getParent().getId() + "-" + current.getId(),
                    current.getParent().getId(),
                    current.getId(),
                    EdgeType.DIRECTED);
        }
        current.getChildren().forEach(c -> bfsBuildGraph(returnGraph, c));
    }

    /**
     * Recursively adds the edges to the quasi-threshold graph skeleton.
     *
     * @param v           The current vertex.
     * @param ancestors   A set of all the ancestor of the current vertex.
     * @param returnGraph The graph to be returned.
     */
    private void addEdges(Vertex<V> v, Set<Vertex<V>> ancestors, Graph<V, String> returnGraph) {
        // Add the current vertex to the return graph.
        returnGraph.addVertex(v.getId());

        // Add edges from all the ancestor of v to v.
        ancestors.stream().forEach(a -> returnGraph.addEdge(a.getId() + "-" + v.getId(), a.getId(), v.getId()));

        // Recursively apply addEdges to the children.
        if (!v.getChildren().isEmpty()) {
            Set<Vertex<V>> newAncestors = new HashSet<>(ancestors);
            newAncestors.add(v);
            v.getChildren().stream().forEach(c -> addEdges(c, newAncestors, returnGraph));
        }
    }
}
