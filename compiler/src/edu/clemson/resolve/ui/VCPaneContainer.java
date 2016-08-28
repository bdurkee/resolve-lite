package edu.clemson.resolve.ui;

import com.sun.istack.internal.NotNull;

import javax.swing.*;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;

public class VCPaneContainer extends JXPanel {

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

}
