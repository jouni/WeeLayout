package org.vaadin.weelayout.client.ui;

import java.util.ArrayList;
import java.util.Set;

import org.vaadin.csstools.client.ComputedStyle;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.event.dom.client.DomEvent.Type;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.Container;
import com.vaadin.terminal.gwt.client.EventId;
import com.vaadin.terminal.gwt.client.Paintable;
import com.vaadin.terminal.gwt.client.RenderSpace;
import com.vaadin.terminal.gwt.client.UIDL;
import com.vaadin.terminal.gwt.client.Util;
import com.vaadin.terminal.gwt.client.VConsole;
import com.vaadin.terminal.gwt.client.ValueMap;
import com.vaadin.terminal.gwt.client.ui.LayoutClickEventHandler;

public class VWeeLayout extends ComplexPanel implements Container {

	/** Set the CSS class name to allow styling. */
	public static final String CLASSNAME = "v-weelayout";

	private LayoutClickEventHandler clickEventHandler = new LayoutClickEventHandler(
			this, EventId.LAYOUT_CLICK) {

		@Override
		protected <H extends EventHandler> HandlerRegistration registerHandler(
				H handler, Type<H> type) {
			return addDomHandler(handler, type);
		}

		@Override
		protected Paintable getChildComponent(
				com.google.gwt.user.client.Element element) {
			return getComponent(element);
		}
	};

	private Paintable getComponent(Element element) {
		return Util.getChildPaintableForElement(client, VWeeLayout.this,
				(com.google.gwt.user.client.Element) element.cast());
	}

	/** The client side widget identifier */
	protected String paintableId;

	/** Reference to the server connection object. */
	ApplicationConnection client;

	/** Direction of the layout */
	private boolean vertical = true;

	/** Are overflowing widgets clipped */
	private boolean clip = false;

	/** Should the layout calculate relative sizes inside undefined sized layout */
	private boolean smart = false;

	/** Current margin values */
	protected int[] margin;

	/** Current border sizes */
	protected int[] border;

	/** Current padding values */
	protected int[] padding;

	// Current inner size (excluding margins, border and padding) in pixels
	protected int width = -1;
	protected int height = -1;

	private String lastStyleName;

	// Is the layout size undefined, i.e. defined by the contained components
	protected boolean undefWidth = false;
	protected boolean undefHeight = false;

	private Element horizontalAligner;

	private boolean isRendering = false;

	// Information to use after all fixed size widgets are rendered
	private int usedSpace = 0;
	private final ArrayList<Cell> relativeSizedWidgets = new ArrayList<Cell>();

	public VWeeLayout() {
		setElement(Document.get().createDivElement());
		setStyleName(CLASSNAME);
	}

	/**
	 * Called whenever an update is received from the server
	 */
	public void updateFromUIDL(UIDL uidl, ApplicationConnection client) {
		isRendering = true;
		getElement().getStyle().setOverflow(Overflow.HIDDEN);

		updateExtraSizeInfo();

		if (client.updateComponent(this, uidl, true)) {
			isRendering = false;
			return;
		}

		clickEventHandler.handleEventHandlerRegistration(client);

		this.client = client;
		paintableId = uidl.getId();
		vertical = uidl.hasAttribute("vertical");
		clip = uidl.hasAttribute("clip");
		smart = uidl.hasAttribute("smart");

		if (vertical) {
			addStyleDependentName("vertical");
			removeStyleDependentName("horizontal");
		} else {
			addStyleDependentName("horizontal");
			removeStyleDependentName("vertical");
		}

		updateDynamicSizeInfo(uidl);

		updateActualSize();

		// Vertical-align needs one element to base the alignment onto
		// This is done only once, before any components are painted
		if (!vertical && horizontalAligner == null) {
			horizontalAligner = Document.get().createSpanElement();
			horizontalAligner.setClassName(CLASSNAME + "-aligner");
			getElement().appendChild(horizontalAligner);
		}

		// Iterate through Paintables in UIDL, add new ones and remove any
		// old ones.
		final int uidlCount = uidl.getChildCount();
		int uidlPos = 0;

		// Additional info that needs to be passed to components
		final ValueMap alignments = uidl.getMapAttribute("alignments");

		relativeSizedWidgets.clear();

		for (; uidlPos < uidlCount; uidlPos++) {

			final UIDL childUIDL = (uidlPos < uidlCount) ? uidl
					.getChildUIDL(uidlPos) : null;

			final Widget uidlWidget = childUIDL != null ? (Widget) client
					.getPaintable(childUIDL) : null;

			final Cell cell = getCellForWidget(uidlWidget, true);

			if (cell.getParent() == null || getChildren().get(uidlPos) != cell) {
				/*
				 * Widget is either new or has changed place
				 */
				cell.removeFromParent();

				// Logical attach
				getChildren().insert(cell, uidlPos);

				// Physical attach
				DOM.insertChild(getElement(), cell.getElement(),
						vertical ? uidlPos * 2 : uidlPos + 1);

				// Adopt.
				adopt(cell);
			}

			if (alignments.containsKey(childUIDL.getId())) {
				cell.setAlignment(alignments.getInt(childUIDL.getId()));
			}

			((Paintable) cell.getChildWidget()).updateFromUIDL(childUIDL,
					client);

			// Only when size is specified, we calculate cell dimensions (for
			// expansions)
			if (smart
					|| ((vertical && !undefHeight) || (!vertical && !undefWidth))) {
				cell.updateRelativeSize(childUIDL);
				if (cell.hasRelativeSizeInParentDirection()) {
					relativeSizedWidgets.add(cell);
				}
			}

		} // All UIDL widgets painted

		// All remaining widgets are removed
		removeChildrenAfter(uidlPos);

		// Don't keep track of size for undefined sized layout and don't
		// calculate expansions
		if (smart || ((vertical && !undefHeight) || (!vertical && !undefWidth))) {
			if (smart) {
				clearComponentSizesInNonParentDirection();
			}
			updateActualSize();
			updateUsedSpace();
			updateRelativeSizedWidgets();
		}

		isRendering = false;
		if (!clip) {
			getElement().getStyle().clearOverflow();
		}
	}

	protected void updateExtraSizeInfo() {
		ComputedStyle cs = new ComputedStyle(getElement());
		margin = cs.getMargin();
		border = cs.getBorder();
		padding = cs.getPadding();
	}

	public void updateActualSize() {
		ComputedStyle cs = new ComputedStyle(getElement());
		width = cs.getIntProperty("width");
		height = cs.getIntProperty("height");
	}

	private int cumulativeSize = 0;

	private void updateRelativeSizedWidgets() {
		// TODO handle Firefox sub-pixel errors somehow
		/*
		 * ApplicationConnection handles the relative sizes for us
		 * automatically, but we need to take care of possible rounding errors
		 * (unwanted one pixel gaps)
		 * 
		 * This is only needed when there are more than one relatively sized
		 * widget, and their added percentages equals 100.
		 * 
		 * This needs to be fired lazily in order to make it happen after
		 * ApplicationConnections automatic layout calculations.
		 */
		if (relativeSizedWidgets.size() > 1) {
			int totalPercentage = 0;
			cumulativeSize = usedSpace;
			for (Cell cell : relativeSizedWidgets) {
				totalPercentage += cell.getRelativeSizeInParentDirection();
			}
			if (totalPercentage == 100.0) {
				Timer delay = new Timer() {
					@Override
					public void run() {
						getElement().getStyle().setOverflow(Overflow.HIDDEN);
						for (Cell cell : relativeSizedWidgets) {
							cumulativeSize += cell.getSizeInParentDirection();
						}

						Cell lastCell = relativeSizedWidgets
								.get(relativeSizedWidgets.size() - 1);
						Widget w = lastCell.getChildWidget();
						if (vertical) {
							String sHeight = w.getElement().getStyle()
									.getHeight();
							int oldHeight = Integer.parseInt(sHeight.substring(
									0, sHeight.length() - 2));
							if (height > cumulativeSize) {
								w.setHeight(oldHeight
										+ (height - cumulativeSize) + "px");
								if (w instanceof HasWidgets) {
									client.runDescendentsLayout((HasWidgets) w);
								}
							}
						} else {
							String sWidth = w.getElement().getStyle()
									.getWidth();
							int oldWidth = Integer.parseInt(sWidth.substring(0,
									sWidth.length() - 2));
							if (width > cumulativeSize) {
								w.setWidth(oldWidth + (width - cumulativeSize)
										+ "px");
								if (w instanceof HasWidgets) {
									client.runDescendentsLayout((HasWidgets) w);
								}
							}
						}
						if (!clip) {
							getElement().getStyle().clearOverflow();
						}
					}
				};
				delay.schedule(1);
			}
		}
		if (smart) {
			for (Widget w : getChildren()) {
				Cell cell = (Cell) w;
				client.handleComponentRelativeSize(cell.getChildWidget());
			}
		}
	}

	private void updateUsedSpace() {
		usedSpace = 0;
		for (Widget w : getChildren()) {
			Cell cell = (Cell) w;
			usedSpace += cell.getRequiredSizeInParentDirection();
		}
	}

	private Cell getCellForWidget(Widget w, boolean createNew) {
		for (Widget child : getChildren()) {
			Cell cell = (Cell) child;
			if (cell.getChildWidget() == w) {
				return cell;
			}
		}
		if (createNew) {
			return new Cell(w, vertical);
		} else {
			return null;
		}
	}

	private void removeChildrenAfter(int pos) {
		int toRemove = getChildren().size() - pos;
		while (toRemove-- > 0) {
			Cell child = (Cell) getChildren().get(pos);
			remove(child);
			client.unregisterPaintable((Paintable) child.getChildWidget());
		}
	}

	public RenderSpace getAllocatedSpace(Widget child) {
		Cell c = getCellForWidget(child, false);
		int captionHeight = 0;
		if (c.hasCaption() && !vertical)
			captionHeight = c.getCaptionElement().getOffsetHeight();
		if (child instanceof VWeeLayout) {
			return new RenderSpace(vertical ? width : width - usedSpace,
					(vertical ? height - usedSpace : height) - captionHeight);
		}
		ComputedStyle cs = new ComputedStyle(child.getElement());
		int[] margin = cs.getMargin();
		int width = this.width - margin[1] - margin[3];
		int height = this.height - margin[0] - margin[2];
		return new RenderSpace(vertical ? width : width - usedSpace,
				(vertical ? height - usedSpace : height) - captionHeight);
	}

	public boolean hasChildComponent(Widget component) {
		for (Widget w : getChildren()) {
			Cell cell = (Cell) w;
			if (cell.getChildWidget() == component) {
				return true;
			}
		}
		return false;
	}

	public void replaceChildComponent(Widget oldComponent, Widget newComponent) {
		// TODO Auto-generated method stub

	}

	public boolean requestLayout(Set<Paintable> children) {
		for (Paintable p : children) {
			Cell cell = getCellForWidget((Widget) p, false);
			cell.updateRelativeSize(client.getRelativeSize((Widget) p));
			if (!cell.hasRelativeSizeInParentDirection()) {
				relativeSizedWidgets.remove(cell);
			}
		}
		if (smart) {
			clearComponentSizesInNonParentDirection();
		}
		updateUsedSpace();
		if (!smart) {
			for (Cell cell : relativeSizedWidgets) {
				client.handleComponentRelativeSize(cell.getChildWidget());
			}
		}
		// For other than undefined size, we need to calculate a bit
		if (smart || ((vertical && !undefHeight) || (!vertical && !undefWidth))) {
			int oldWidth = width;
			int oldHeight = height;
			updateActualSize();
			updateRelativeSizedWidgets();
			return (width == oldWidth && height == oldHeight);
		} else {
			return false;
		}
	}

	public void updateCaption(Paintable component, UIDL uidl) {
		getCellForWidget((Widget) component, false).updateCaption(uidl, client);
	}

	private void updateDynamicSizeInfo(UIDL uidl) {
		String w = uidl.hasAttribute("width") ? uidl
				.getStringAttribute("width") : "";
		undefWidth = w.equals("");
		String h = uidl.hasAttribute("height") ? uidl
				.getStringAttribute("height") : "";
		undefHeight = h.equals("");
	}

	@Override
	public void setWidth(String w) {
		String toBeWidth = "";
		if (w != null && !"".equals(w)) {
			// Assume pixel values are always passed from ApplicationConnection
			int newWidth = ComputedStyle.parseInt(w) - margin[1] - margin[3]
					- border[1] - border[3] - padding[1] - padding[3];
			if (newWidth < 0) {
				newWidth = 0;
			}
			toBeWidth = newWidth + "px";
			super.setWidth(toBeWidth);
		} else {
			super.setWidth("");
		}

		if (!isRendering) {
			updateActualSize();
			updateRelativeSizedWidgets();
		}
	}

	@Override
	public void setHeight(String h) {
		String toBeHeight = "";

		if (h != null && !"".equals(h)) {
			// Assume pixel values are always passed from ApplicationConnection
			int newHeight = ComputedStyle.parseInt(h) - margin[0] - margin[2]
					- border[0] - border[2] - padding[0] - padding[2];
			if (newHeight < 0) {
				newHeight = 0;
			}
			toBeHeight = newHeight + "px";
			super.setHeight(toBeHeight);
		} else {
			super.setHeight("");
		}

		if (!isRendering) {
			updateActualSize();
			updateRelativeSizedWidgets();
			Util.runWebkitOverflowAutoFix(getElement());
		}
	}

	private void clearComponentSizesInNonParentDirection() {
		for (Widget w : getChildren()) {
			Cell cell = (Cell) w;
			if (cell.hasRelativeSizeInNonParentDirection()) {
				if (vertical) {
					cell.getChildWidget().setWidth("");
				} else {
					cell.getChildWidget().setHeight("");
				}
			}
		}
	}
}
