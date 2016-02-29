package ubco;

import org.gephi.filters.spi.ComplexFilter;
import org.gephi.filters.spi.FilterProperty;
import org.gephi.graph.api.Graph;

public class HideEdgesFilter implements ComplexFilter {

    @Override
    public Graph filter(Graph graph) {
        graph.clearEdges();
        return graph;
    }

    @Override
    public String getName() {
        return "Hide all edges";
    }

    @Override
    public FilterProperty[] getProperties() {
        return new FilterProperty[0];
    }
}
