package org.vaadin.weelayout;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import com.vaadin.event.LayoutEvents.LayoutClickEvent;
import com.vaadin.event.LayoutEvents.LayoutClickListener;
import com.vaadin.event.LayoutEvents.LayoutClickNotifier;
import com.vaadin.terminal.PaintException;
import com.vaadin.terminal.PaintTarget;
import com.vaadin.terminal.gwt.client.EventId;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;

/**
 * Server side component for the VWeeLayout widget.
 */
@SuppressWarnings("serial")
@com.vaadin.ui.ClientWidget(org.vaadin.weelayout.client.ui.VWeeLayout.class)
public class WeeLayout extends AbstractLayout implements LayoutClickNotifier {

    public enum Direction {
        VERTICAL, HORIZONTAL;
    }

    private Direction direction;

    private static final String CLICK_EVENT = EventId.LAYOUT_CLICK;

    /**
     * Custom layout slots containing the components.
     */
    protected LinkedList<Component> components = new LinkedList<Component>();

    /**
     * Mapping from components to alignments (horizontal + vertical).
     */
    private final Map<Component, Alignment> componentToAlignment = new HashMap<Component, Alignment>();

    /**
     * Should the layout clip any overflowing components outside the layout
     * dimensions.
     */
    private boolean clip = false;

    private boolean smartRelatives;

    @SuppressWarnings("unused")
    private WeeLayout() {
        // Force the user to specify the direction
    }

    /**
     * Create a new layout. The direction of the child components must be
     * specified. The direction can only be set once.
     * 
     * @param direction
     *            The direction in which the child components will flow, either
     *            {@link Direction}.VERTICAL or {@link Direction} .HORIZONTAL
     */
    public WeeLayout(Direction direction) {
        this.direction = direction;
    }

    /**
     * Add a component into this container. The component is added after the
     * previous component.
     * 
     * @param c
     *            the component to be added.
     */
    @Override
    public void addComponent(Component c) {
        components.add(c);
        try {
            super.addComponent(c);
            requestRepaint();
        } catch (IllegalArgumentException e) {
            components.remove(c);
            throw e;
        }
    }

    /**
     * Add a component into this container. The component is added after the
     * previous component.
     * 
     * @param c
     *            the component to be added.
     * @param alignment
     *            the alignment for the component.
     */
    public void addComponent(Component c, Alignment alignment) {
        addComponent(c);
        if (alignment != null) {
            setComponentAlignment(c, alignment);
        }
    }

    /**
     * Add a component into this container. The component is added after the
     * previous component.
     * 
     * @param c
     *            the component to be added.
     * @param width
     *            set the width of the component. Use <code>null</code> to leave
     *            untouched.
     * @param height
     *            set the height of the component. Use <code>null</code> to
     *            leave untouched.
     * @param alignment
     *            the alignment for the component.
     */
    public void addComponent(Component c, String width, String height,
            Alignment alignment) {
        addComponent(c);
        if (width != null) {
            c.setWidth(width);
        }
        if (height != null) {
            c.setHeight(height);
        }
        if (alignment != null) {
            setComponentAlignment(c, alignment);
        }
    }

    /**
     * Adds a component into indexed position in this container.
     * 
     * @param c
     *            the component to be added.
     * @param index
     *            the Index of the component position. The components currently
     *            in and after the position are shifted forwards.
     */
    public void addComponent(Component c, int index) {
        components.add(index, c);
        try {
            super.addComponent(c);
            requestRepaint();
        } catch (IllegalArgumentException e) {
            components.remove(c);
            throw e;
        }
    }

    /**
     * Adds a component into indexed position in this container.
     * 
     * @param c
     *            the component to be added.
     * @param index
     *            the Index of the component position. The components currently
     *            in and after the position are shifted forwards.
     * @param alignment
     *            the alignment for the component.
     */
    public void addComponent(Component c, int index, Alignment alignment) {
        components.add(index, c);
        try {
            super.addComponent(c);
            setComponentAlignment(c, alignment);
            requestRepaint();
        } catch (IllegalArgumentException e) {
            components.remove(c);
            componentToAlignment.remove(c);
            throw e;
        }
    }

    /**
     * Removes the component from this container.
     * 
     * @param c
     *            the component to be removed.
     */
    @Override
    public void removeComponent(Component c) {
        components.remove(c);
        componentToAlignment.remove(c);
        super.removeComponent(c);
        requestRepaint();
    }

    @Override
    public void paintContent(PaintTarget target) throws PaintException {
        super.paintContent(target);

        // Specify direction
        if (direction.equals(Direction.VERTICAL)) {
            target.addAttribute("vertical", true);
        }

        // Specify clipping
        if (clip) {
            target.addAttribute("clip", true);
        }

        // Specify smart relative size handling
        if (smartRelatives) {
            target.addAttribute("smart", true);
        }

        // Adds all items in all the locations
        for (Component c : components) {
            // Paint child component UIDL
            c.paint(target);
        }

        // Add child component alignment info to layout tag
        target.addAttribute("alignments", componentToAlignment);
    }

    public Iterator<Component> getComponentIterator() {
        return components.iterator();
    }

    public void replaceComponent(Component oldComponent, Component newComponent) {
        // Gets the locations
        int oldLocation = -1;
        int newLocation = -1;
        int location = 0;
        for (final Iterator<Component> i = components.iterator(); i.hasNext();) {
            final Component component = i.next();

            if (component == oldComponent) {
                oldLocation = location;
            }
            if (component == newComponent) {
                newLocation = location;
            }

            location++;
        }

        if (oldLocation == -1) {
            addComponent(newComponent);
        } else if (newLocation == -1) {
            removeComponent(oldComponent);
            addComponent(newComponent, oldLocation);
        } else {
            if (oldLocation > newLocation) {
                components.remove(oldComponent);
                components.add(newLocation, oldComponent);
                components.remove(newComponent);
                componentToAlignment.remove(newComponent);
                components.add(oldLocation, newComponent);
            } else {
                components.remove(newComponent);
                components.add(oldLocation, newComponent);
                components.remove(oldComponent);
                componentToAlignment.remove(oldComponent);
                components.add(newLocation, oldComponent);
            }

            requestRepaint();
        }
    }

    /**
     * Set the alignment of component in this layout. Only one direction is
     * affected, depending on the layout direction, i.e. only vertical alignment
     * is considered when the direction is horizontal.
     * 
     * @param childComponent
     * @param alignment
     */
    public void setComponentAlignment(Component childComponent,
            Alignment alignment) {
        if (components.contains(childComponent) && alignment != null) {
            componentToAlignment.put(childComponent, alignment);
            requestRepaint();
        } else {
            throw new IllegalArgumentException(
                    "Component must be added to layout before using setComponentAlignment()");
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.ui.Layout.AlignmentHandler#getComponentAlignment(com
     * .vaadin.ui.Component)
     */
    public Alignment getComponentAlignment(Component childComponent) {
        Alignment alignment = componentToAlignment.get(childComponent);
        if (alignment == null) {
            return Alignment.TOP_LEFT;
        } else {
            return alignment;
        }
    }

    /**
     * Returns the index of the given component.
     * 
     * @param component
     *            The component to look up.
     * @return The index of the component or -1 if the component is not a child.
     */
    public int getComponentIndex(Component component) {
        return components.indexOf(component);
    }

    /**
     * Returns the component at the given position.
     * 
     * @param index
     *            The position of the component.
     * @return The component at the given index.
     * @throws IndexOutOfBoundsException
     *             If the index is out of range.
     */
    public Component getComponent(int index) throws IndexOutOfBoundsException {
        return components.get(index);
    }

    /**
     * Returns the number of components in the layout.
     * 
     * @return Component amount
     */
    public int size() {
        return components.size();
    }

    /**
     * Set the clipping value for this layout. If clipping is <code>true</code>,
     * components overflowing outside the layout boundaries will be clipped.
     * Otherwise overflowing components are visible.
     * 
     * @param clip
     *            the new clipping value.
     */
    public void setClipping(boolean clip) {
        this.clip = clip;
        requestRepaint();
    }

    /**
     * When the layout size is undefined, relative sizes are calculated as
     * zeros. Set this flag to <code>true</code> if you wish for the layout to
     * calculate relative sizes inside undefined sized layouts as well (the
     * largest component will determine the size).
     * 
     * @param smartRelatives
     */
    public void setSmartRelativeSizes(boolean smartRelatives) {
        this.smartRelatives = smartRelatives;
    }

    public void addListener(LayoutClickListener listener) {
        addListener(CLICK_EVENT, LayoutClickEvent.class, listener,
                LayoutClickListener.clickMethod);
    }

    public void removeListener(LayoutClickListener listener) {
        removeListener(CLICK_EVENT, LayoutClickEvent.class, listener);
    }

}
