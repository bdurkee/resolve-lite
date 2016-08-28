package edu.clemson.resolve.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class CollapsiblePane extends JXPanel {

    /**
     * JXCollapsible has a built-in toggle action which can be bound to buttons. Accesses the action through
     * {@code collapsiblePane.getActionMap().get(JXCollapsiblePane.TOGGLE_ACTION}.
     */
    public final static String TOGGLE_ACTION = "toggle";

    /**
     * The direction defines how the collapsible pane will collapse. The onstant names were designed by choosing a fixed point and then
     * determining the collapsing direction from that fixed point. This means {@code RIGHT} expands to the right and this is probably
     * the best expansion for a component in {@link BorderLayout#EAST}.
     */
    public enum Direction {
        /**
         * Collapses left. Suitable for {@link BorderLayout#WEST}.
         */
        LEFT(false),

        /**
         * Collapses right. Suitable for {@link BorderLayout#EAST}.
         */
        RIGHT(false),

        /**
         * Collapses up. Suitable for {@link BorderLayout#NORTH}.
         */
        UP(true),

        /**
         * Collapses down. Suitable for {@link BorderLayout#SOUTH}.
         */
        DOWN(true),

        /**
         * Collapses toward the leading edge. Suitable for {@link BorderLayout#LINE_START}.
         */
        LEADING(false) {
            @Override
            Direction getFixedDirection(ComponentOrientation co) {
                return co.isLeftToRight() ? LEFT : RIGHT;
            }
        },

        /**
         * Collapses toward the trailing edge. Suitable for {@link BorderLayout#LINE_END}.
         */
        TRAILING(false) {
            @Override
            Direction getFixedDirection(ComponentOrientation co) {
                return co.isLeftToRight() ? RIGHT : LEFT;
            }
        },

        /**
         * Collapses toward the starting edge. Suitable for {@link BorderLayout#PAGE_START}.
         */
        START(true) {
            @Override
            Direction getFixedDirection(ComponentOrientation co) {
                return UP;
            }
        },

        /**
         * Collapses toward the ending edge. Suitable for {@link BorderLayout#PAGE_END}.
         */
        END(true) {
            @Override
            Direction getFixedDirection(ComponentOrientation co) {
                return DOWN;
            }
        },
        ;

        private final boolean vertical;

        private Direction(boolean vertical) {
            this.vertical = vertical;
        }

        /**
         * Gets the orientation for this direction.
         *
         * @return {@code true} if the direction is vertical, {@code false}
         *         otherwise
         */
        public boolean isVertical() {
            return vertical;
        }

        /**
         * Gets the fixed direction equivalent to this direction for the specified orientation.
         *
         * @param co
         *            the component's orientation
         * @return the fixed direction corresponding to the component's orietnation
         */
        Direction getFixedDirection(ComponentOrientation co) {
            return this;
        }
    }

    /**
     * Toggles the JXCollapsiblePane state and updates its icon based on the
     * JXCollapsiblePane "collapsed" status.
     */
    private class ToggleAction extends AbstractAction implements PropertyChangeListener {
        public ToggleAction() {
            super(TOGGLE_ACTION);
            // the action must track the collapsed status of the pane to update its icon
            CollapsiblePane.this.addPropertyChangeListener("collapsed", this);
        }

        @Override
        public void putValue(String key, Object newValue) {
            super.putValue(key, newValue);
            //if (EXPAND_ICON.equals(key) || COLLAPSE_ICON.equals(key)) {
            //    updateIcon();
           // }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setCollapsed(!isCollapsed());
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            //updateIcon();
        }

        void updateIcon() {
           // if (isCollapsed()) {
           //     putValue(SMALL_ICON, getValue(EXPAND_ICON));
           // } else {
           //     putValue(SMALL_ICON, getValue(COLLAPSE_ICON));
          //  }
        }
    }
    private Direction direction = Direction.UP;
    private WrapperContainer wrapper;
    /**
     * Indicates whether the component is collapsed or expanded
     */
    private boolean collapsed = false;

    public CollapsiblePane() {
        this(Direction.UP);
    }

    public CollapsiblePane(Direction direction) {
        super.setLayout(new BorderLayout());
        this.direction = direction;
        //animator = new AnimationListener();
        //setAnimationParams(new AnimationParams(30, 8, 0.01f, 1.0f));

        setContentPane(createContentPane());

        setDirection(direction);

        // add an action to automatically toggle the state of the pane
        getActionMap().put(TOGGLE_ACTION, new ToggleAction());
    }

    public void setCollapsed(boolean val) {
        boolean oldValue = isCollapsed();
        this.collapsed = val;


            wrapper.collapsedState = isCollapsed();
            wrapper.getView().setVisible(!isCollapsed());
            revalidate();
            repaint();
            firePropertyChange("collapsed", oldValue, isCollapsed());

    }

    public void setDirection(Direction direction) {


        Direction oldValue = getDirection();
        this.direction = direction;

        if (direction.isVertical()) {
            getContentPane().setLayout(new VerticalLayout(2));
        } else {
            getContentPane().setLayout(new HorizontalLayout(2));
        }

        firePropertyChange("direction", oldValue, getDirection());
    }

    public Direction getDirection() {
        return direction;
    }

    /**
     * Sets the content pane of this JXCollapsiblePane. The {@code contentPanel}
     * <i>should</i> implement {@code Scrollable} and return {@code true} from
     * {@link Scrollable#getScrollableTracksViewportHeight()} and
     * {@link Scrollable#getScrollableTracksViewportWidth()}. If the content
     * pane fails to do so and a {@code JScrollPane} is added as a child, it is
     * likely that the scroll pane will never correctly size. While it is not
     * strictly necessary to implement {@code Scrollable} in this way, the
     * default content pane does so.
     *
     * @param contentPanel
     *                the container delegate used to hold all of the contents
     *                for this collapsible pane
     * @throws IllegalArgumentException
     *                 if contentPanel is null
     */
    public void setContentPane(Container contentPanel) {
        if (contentPanel == null) {
            throw new IllegalArgumentException("Content pane can't be null");
        }

        if (wrapper != null) {
            //these next two lines are as they are because if I try to remove
            //the "wrapper" component directly, then super.remove(comp) ends up
            //calling remove(int), which is overridden in this class, leading to
            //improper behavior.
            assert super.getComponent(0) == wrapper;
            super.remove(0);
        }

        wrapper = new WrapperContainer(contentPanel);
        wrapper.collapsedState = isCollapsed();
        wrapper.getView().setVisible(!wrapper.collapsedState);
        super.addImpl(wrapper, BorderLayout.CENTER, -1);
    }

    /**
     * Forwards to the content pane.
     *
     * @param minimumSize
     *            the size to set on the content pane
     */
    @Override
    public void setMinimumSize(Dimension minimumSize) {
        getContentPane().setMinimumSize(minimumSize);
    }

    /**
     * @return true if the pane is collapsed, false if expanded
     */
    public boolean isCollapsed() {
        return collapsed;
    }

    /**
     * @return the content pane
     */
    public Container getContentPane() {
        if (wrapper == null) {
            return null;
        }

        return (Container) wrapper.getView();
    }
    /**
     * Tagging interface for containers in a JXCollapsiblePane hierarchy who needs
     * to be revalidated (invalidate/validate/repaint) when the pane is expanding
     * or collapsing. Usually validating only the parent of the JXCollapsiblePane
     * is enough but there might be cases where the parent's parent must be
     * validated.
     */
    public static interface CollapsiblePaneContainer {
        Container getValidatingContainer();
    }
    /**
     * Creates the content pane used by this collapsible pane.
     *
     * @return the content pane
     */
    protected Container createContentPane() {
        return new JPanel();
    }

    private final class WrapperContainer extends JViewport {
        boolean collapsedState;
        private volatile float alpha;
        private boolean oldOpaque;

        public WrapperContainer(Container c) {
            alpha = 1.0f;
            collapsedState = false;
            setView(c);

            // we must ensure the container is opaque. It is not opaque it introduces
            // painting glitches specially on Linux with JDK 1.5 and GTK look and feel.
            // GTK look and feel calls setOpaque(false)
            if (c instanceof JComponent && !c.isOpaque()) {
                ((JComponent) c).setOpaque(true);
            }
        }

        /**
         * {@inheritDoc} <p>
         *
         * Overridden to not have JViewPort behaviour (that is scroll the view)
         * but delegate to parent scrollRectToVisible just a JComponent does.<p>
         */
        @Override
        public void scrollRectToVisible(Rectangle aRect) {
            //avoids JViewport's implementation
            //by using JXCollapsiblePane's it will delegate upward
            //getting any core fixes, by avoiding c&p
            CollapsiblePane.this.scrollRectToVisible(aRect);
        }

    }
}
