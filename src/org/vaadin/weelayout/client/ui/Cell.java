package org.vaadin.weelayout.client.ui;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.BrowserInfo;
import com.vaadin.terminal.gwt.client.UIDL;
import com.vaadin.terminal.gwt.client.Util;
import com.vaadin.terminal.gwt.client.VCaption;
import com.vaadin.terminal.gwt.client.RenderInformation.FloatSize;
import com.vaadin.terminal.gwt.client.ui.AlignmentInfo;
import com.vaadin.terminal.gwt.client.ui.Icon;

public class Cell extends SimplePanel {

	private Widget childWidget;
	private FloatSize relSize;
	private Element lineBreak;
	private boolean vertical;
	private AlignmentInfo alignment;
	private Element caption;

	public Cell(Widget w, boolean vertical) {
		/*
		 * SPAN for IE6 & 7, DIV for all others (only pure inline elements can
		 * be "inline-block" in IE6/7)
		 */
		super(BrowserInfo.get().isIE6() || BrowserInfo.get().isIE7() ? Document
				.get().createSpanElement() : Document.get().createDivElement());

		setStyleName(VWeeLayout.CLASSNAME + "-cell");

		this.vertical = vertical;

		childWidget = w;
		add(w);

		setAlignment(1);
	}

	@Override
	protected void onAttach() {
		super.onAttach();

		// For vertical alignment, add a line break
		if (vertical) {
			lineBreak = Document.get().createDivElement();
			lineBreak.setClassName(VWeeLayout.CLASSNAME + "-linebreak");
			getElement().getParentElement()
					.insertAfter(lineBreak, getElement());
		}
	}

	@Override
	protected void onDetach() {
		// Cleanup possible line-break element
		if (vertical) {
			getElement().getParentElement().removeChild(lineBreak);
		}

		super.onDetach();
	}

	public Widget getChildWidget() {
		return childWidget;
	}

	void updateRelativeSize(FloatSize size) {
		if (size == null) {
			relSize = new FloatSize(-1, -1);
		} else {
			relSize = size;
		}
	}

	void updateRelativeSize(UIDL uidl) {
		if (uidl.hasAttribute("cached")) {
			return;
		}
		float relativeWidth = -1;
		if (uidl.hasAttribute("width")) {
			final String w = uidl.getStringAttribute("width");
			if (w.endsWith("%")) {
				relativeWidth = Float
						.parseFloat(w.substring(0, w.length() - 1));
			}
		}
		float relativeHeight = -1;
		if (uidl.hasAttribute("height")) {
			final String h = uidl.getStringAttribute("height");
			if (h.endsWith("%")) {
				relativeHeight = Float
						.parseFloat(h.substring(0, h.length() - 1));
			}
		}
		relSize = new FloatSize(relativeWidth, relativeHeight);
	}

	float getRelativeSizeInParentDirection() {
		return vertical ? relSize.getHeight() : relSize.getWidth();
	}

	boolean hasRelativeSizeInParentDirection() {
		if (relSize == null) {
			return false;
		}
		if (vertical) {
			return relSize.getHeight() != -1;
		} else {
			return relSize.getWidth() != -1;
		}
	}

	boolean hasRelativeSizeInNonParentDirection() {
		if (relSize == null) {
			return false;
		}
		if (!vertical) {
			return relSize.getHeight() != -1;
		} else {
			return relSize.getWidth() != -1;
		}
	}

	void setAlignment(int align) {
		alignment = new AlignmentInfo(align);
		if (vertical) {
			if (alignment.isLeft()) {
				addStyleDependentName("left");
				removeStyleDependentName("right");
			} else if (alignment.isRight()) {
				removeStyleDependentName("left");
				addStyleDependentName("right");
			} else if (alignment.isHorizontalCenter()) {
				removeStyleDependentName("left");
				removeStyleDependentName("right");
			}
		} else {
			if (alignment.isTop()) {
				removeStyleDependentName("middle");
				removeStyleDependentName("bottom");
			} else if (alignment.isBottom()) {
				removeStyleDependentName("middle");
				addStyleDependentName("bottom");
			} else if (alignment.isVerticalCenter()) {
				addStyleDependentName("middle");
				removeStyleDependentName("bottom");
			}
		}
	}

	void updateCaption(UIDL uidl, ApplicationConnection client) {
		boolean hasIcon = uidl.hasAttribute("icon");
		boolean hasText = uidl.hasAttribute("caption");
		boolean hasDescription = uidl.hasAttribute("description");
		boolean showRequired = uidl.getBooleanAttribute("required");
		boolean showError = uidl.hasAttribute("error")
				&& !uidl.getBooleanAttribute("hideErrors");
		if (VCaption.isNeeded(uidl)) {
			String html = "";
			if (caption == null) {
				caption = Document.get().createSpanElement();
				String styleNames = getStylePrimaryName()
						+ "-caption v-caption";
				if (uidl.hasAttribute("style")) {
					for (String style : uidl.getStringAttribute("style").split(
							" ")) {
						styleNames += " v-caption-" + style;
					}
				}
				caption.setClassName(styleNames);
				getElement().insertFirst(caption);
			}
			if(hasIcon) {
				Icon icon = new Icon(client, uidl.getStringAttribute("icon"));
				html += icon.toString();
			}
			if(hasText) {
				html += Util.escapeHTML(uidl.getStringAttribute("caption"));
			}
			caption.setInnerHTML(html);
		} else if (caption != null) {
			getElement().removeChild(caption);
			caption = null;
		}
	}

	boolean hasCaption() {
		return caption != null;
	}
	
	Element getCaptionElement() {
		return caption;
	}

	/**
	 * For relative sized widget, returns the caption size. For others returns
	 * the complete size (caption + widget).
	 * 
	 * @return
	 */
	int getRequiredSizeInParentDirection() {
		if (hasRelativeSizeInParentDirection()) {
			if (vertical) {
				// TODO calculate caption margins
				return caption == null ? 0 : Util.getRequiredHeight(caption);
			} else {
				// TODO handle case when caption is on the left side
				return 0;
			}
		}

		return getSizeInParentDirection();
	}

	/**
	 * Returns the complete size (caption + widget).
	 * 
	 * @return the size of this cell, containing the possible caption and widget
	 */
	int getSizeInParentDirection() {
		// TODO calculate margins
		if (vertical) {
			return Util.getRequiredHeight(getElement());
		} else {
			return Util.getRequiredWidth(getElement());
		}
	}
}
