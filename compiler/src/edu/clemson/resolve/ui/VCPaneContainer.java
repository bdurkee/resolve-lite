package edu.clemson.resolve.ui;

import com.sun.istack.internal.NotNull;

import javax.swing.*;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;

public class VCPaneContainer extends JPanel {

    private ScrollableSizeHint scrollableWidthHint = ScrollableSizeHint.FIT;
    private ScrollableSizeHint scrollableHeightHint = ScrollableSizeHint.FIT;

    public VCPaneContainer() {
        super(null);

        addContainerListener(new ContainerAdapter() {
            @Override
            public void componentRemoved(ContainerEvent e) {
                repaint();
            }
        });
        setScrollableHeightHint(ScrollableSizeHint.PREFERRED_STRETCH);
    }

    protected ScrollableSizeHint getScrollableHeightHint() {
        return scrollableHeightHint;
    }

    /**
     * Sets the vertical sizing hint. The hint is used by the Scrollable implementation
     * to service the getScrollableTracksHeight.
     *
     * @param hint the vertical sizing hint, must not be null
     *   and must be vertical.
     *
     * @throws NullPointerException if null
     *
     * @see ScrollableSizeHint
     */
    public final void setScrollableHeightHint(@NotNull ScrollableSizeHint hint) {
        ScrollableSizeHint oldValue = getScrollableHeightHint();
        if (oldValue == hint) return;
        this.scrollableHeightHint = hint;
        revalidate();
        firePropertyChange("scrollableHeightHint", oldValue, getScrollableHeightHint());
    }
}
