package ubco;

import org.gephi.filters.api.FilterLibrary;
import org.gephi.filters.spi.Category;
import org.gephi.filters.spi.Filter;
import org.gephi.filters.spi.FilterBuilder;
import org.gephi.project.api.Workspace;
import org.openide.util.lookup.ServiceProvider;

import javax.swing.*;

@ServiceProvider(service = FilterBuilder.class)
public class HideEdgesFilterBuilder implements FilterBuilder {

    @Override
    public Category getCategory() {
        return FilterLibrary.EDGE;
    }

    @Override
    public String getName() {
        return "Hide all edges";
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Hides all edges in the graph";
    }

    public Filter getFilter() {
        return new HideEdgesFilter();
    }

    @Override
    public JPanel getPanel(Filter filter) {
        return null;
    }

    @Override
    public void destroy(Filter filter) {
    }

    public Filter getFilter(Workspace wrkspc) {
        return new HideEdgesFilter(); //To change body of generated methods, choose Tools | Templates.
    }
}
