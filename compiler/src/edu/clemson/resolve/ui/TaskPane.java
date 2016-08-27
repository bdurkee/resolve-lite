package edu.clemson.resolve.ui;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class TaskPane extends JPanel {

    /** Used when generating PropertyChangeEvents for the "scrollOnExpand" property */
    public static final String SCROLL_ON_EXPAND_CHANGED_KEY = "scrollOnExpand";

    /** Used when generating PropertyChangeEvents for the "title" property */
    public static final String TITLE_CHANGED_KEY = "title";

    /** Used when generating PropertyChangeEvents for the "icon" property */
    public static final String ICON_CHANGED_KEY = "icon";

    /** Used when generating PropertyChangeEvents for the "special" property */
    public static final String SPECIAL_CHANGED_KEY = "special";

    /** Used when generating PropertyChangeEvents for the "animated" property */
    public static final String ANIMATED_CHANGED_KEY = "animated";

    private String title;
    private Icon icon;
    private boolean special;
    private boolean scrollOnExpand;

    public TaskPane() {
        this((String) null);
    }

    public TaskPane(String title) {
        this(title, null);
    }

    public TaskPane(Icon icon) {
        this(null, icon);
    }

    public TaskPane(String title, Icon icon) {
        collapsePane = new JXCollapsiblePane();
        collapsePane.setOpaque(false);
        super.setLayout(new BorderLayout(0, 0));
        super.addImpl(collapsePane, BorderLayout.CENTER, -1);

        setTitle(title);
        setIcon(icon);

        updateUI();
        setFocusable(true);

        // disable animation if specified in UIManager
        setAnimated(!Boolean.FALSE.equals(UIManager.get("TaskPane.animate")));

        // listen for animation events and forward them to registered listeners
        collapsePane.addPropertyChangeListener("collapsed", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                JXTaskPane.this.firePropertyChange(evt.getPropertyName(), evt.getOldValue(),
                        evt.getNewValue());
            }
        });
    }
}
