package ubco;

import javax.swing.*;
import java.awt.*;

/**
 * Panel that displays the options for the QtCommunitiesGenerator.
 *
 * @author Zach Holland
 */
public class QtCommunitiesGeneratorPanel extends JPanel {
    public static final String SHOW_COMPLETE_GRAPH = "Show the complete edited graph";
    public static final String SHOW_TREE_ONLY = "Show the tree which highlights the hierarchical structure";

    private ButtonGroup buttonGroup;

    public QtCommunitiesGeneratorPanel() {
        JRadioButton editedGraph = new JRadioButton(SHOW_COMPLETE_GRAPH);
        editedGraph.setActionCommand(SHOW_COMPLETE_GRAPH);
        JRadioButton tree = new JRadioButton(SHOW_TREE_ONLY);
        tree.setActionCommand(SHOW_TREE_ONLY);
        buttonGroup = new ButtonGroup();
        buttonGroup.add(editedGraph);
        buttonGroup.add(tree);
        this.setSize(200,300);
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.add(editedGraph);
        this.add(tree);
        editedGraph.setSelected(true);
    }

    public ButtonGroup getButtonGroup() {
        return buttonGroup;
    }
}
