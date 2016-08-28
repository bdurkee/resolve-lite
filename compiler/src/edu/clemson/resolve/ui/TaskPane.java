package edu.clemson.resolve.ui;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class TaskPane extends JPanel implements CollapsiblePane.CollapsiblePaneContainer {

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
    private CollapsiblePane collapsePane;

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
        collapsePane = new CollapsiblePane();
        collapsePane.setOpaque(false);
        super.setLayout(new BorderLayout(0, 0));
        super.addImpl(collapsePane, BorderLayout.CENTER, -1);

        this.title = title;
        this.icon = icon;

        updateUI();
        setFocusable(true);

        // disable animation if specified in UIManager
        //setAnimated(!Boolean.FALSE.equals(UIManager.get("TaskPane.animate")));

        // listen for animation events and forward them to registered listeners
        collapsePane.addPropertyChangeListener("collapsed", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                TaskPane.this.firePropertyChange(evt.getPropertyName(), evt.getOldValue(),
                        evt.getNewValue());
            }
        });
    }
    public Container getValidatingContainer() {
        return getParent();
    }


    public Container getContentPane() {
        return collapsePane.getContentPane();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    public void setScrollOnExpand(boolean scrollOnExpand) {
        this.scrollOnExpand = scrollOnExpand;
    }

    /**
     * Should this group scroll to be visible after
     * this group was expanded.
     *
     * @return true if we should scroll false if nothing
     * should be done.
     */
    public boolean isScrollOnExpand() {
        return scrollOnExpand;
    }

    public void setCollapsed(boolean collapsed) {
        collapsePane.setCollapsed(collapsed);
    }

    public boolean isCollapsed() {
        return collapsePane.isCollapsed();
    }

    @Override
    protected void addImpl(Component comp, Object constraints, int index) {
        getContentPane().add(comp, constraints, index);
        //Fixes SwingX #364; adding to internal component we need to revalidate ourself
        revalidate();
    }

    @Override
    public void setLayout(LayoutManager mgr) {
        if (collapsePane != null) {
            getContentPane().setLayout(mgr);
        }
    }

    public Component add(Action action) {
        Component c = new JButton(action);
        add(c);
        return c;
    }

    @Override
    public void remove(Component comp) {
        getContentPane().remove(comp);
    }

    @Override
    public void remove(int index) {
        getContentPane().remove(index);
    }

    @Override
    public void removeAll() {
        getContentPane().removeAll();
    }

    /** @see JComponent#paramString() */
    @Override
    protected String paramString() {
        return super.paramString()
                + ",title="
                + getTitle()
                + ",icon="
                + getIcon()
                + ",collapsed="
                + String.valueOf(isCollapsed())
                + ",scrollOnExpand="
                + String.valueOf(isScrollOnExpand())
                + ",ui=" + getUI();
    }
}
