package edu.clemson.resolve.ui;

import javafx.application.Application;

import javax.swing.*;
import javax.swing.text.html.HTMLDocument;
import java.awt.*;

/**
 * Created by danielwelch on 8/27/16.
 */
public class TaskPaneDemo extends JPanel {

    private TaskPane systemGroup;
    private TaskPane officeGroup;
    private TaskPane seeAlsoGroup;
    private TaskPane detailsGroup;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame();

                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.getContentPane().add(new TaskPaneDemo());
                frame.setPreferredSize(new Dimension(800, 600));
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }

    public TaskPaneDemo() {
        super(new BorderLayout());
        createTaskPaneDemo();
        //Application.getInstance().getContext().getResourceMap(getClass()).injectComponents(this);
        //bind();
    }

    private void createTaskPaneDemo() {
        VCPaneContainer tpc = new VCPaneContainer();

        // "System" GROUP
        systemGroup = new TaskPane();
        systemGroup.setName("systemGroup");
        tpc.add(systemGroup);

        // "Office" GROUP
        officeGroup = new TaskPane();
        officeGroup.setName("officeGroup");
        tpc.add(officeGroup);

        // "SEE ALSO" GROUP and ACTIONS
        seeAlsoGroup = new TaskPane();
        seeAlsoGroup.setName("seeAlsoGroup");
        tpc.add(seeAlsoGroup);

        // "Details" GROUP
        detailsGroup = new TaskPane();
        detailsGroup.setName("detailsGroup");

        //TODO better injection for editor area
        JEditorPane area = new JEditorPane("text/html", "<html>");
        area.setName("detailsArea");

        area.setFont(UIManager.getFont("Label.font"));

        Font defaultFont = UIManager.getFont("Button.font");

        String stylesheet = "body { margin-top: 0; margin-bottom: 0; margin-left: 0; margin-right: 0; font-family: "
                + defaultFont.getName()
                + "; font-size: "
                + defaultFont.getSize()
                + "pt;  }"
                + "a, p, li { margin-top: 0; margin-bottom: 0; margin-left: 0; margin-right: 0; font-family: "
                + defaultFont.getName()
                + "; font-size: "
                + defaultFont.getSize()
                + "pt;  }";

        detailsGroup.add(area);

        tpc.add(detailsGroup);

        add(new JScrollPane(tpc));
    }
}
