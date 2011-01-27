package com.example.weelayout;

import org.vaadin.weelayout.WeeLayout;
import org.vaadin.weelayout.WeeLayout.Direction;

import com.vaadin.Application;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.NativeButton;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Button.ClickEvent;

@SuppressWarnings("serial")
public class WeelayoutApplication extends Application {
    @Override
    public void init() {
        Window mainWindow = new Window("Weelayout Application");
        setMainWindow(mainWindow);
        mainWindow.setContent(undefinedWithRelativeSizes());
    }

    boolean core = false;
    boolean vertical = false;

    Layout splitRecursive(int deep) {
        Layout l = null;
        if (core) {
            l = vertical ? new VerticalLayout() : new HorizontalLayout();
        } else {
            l = new WeeLayout(vertical ? Direction.VERTICAL
                    : Direction.HORIZONTAL);
        }
        l.setSizeFull();
        if (core) {
            AbstractOrderedLayout c = (AbstractOrderedLayout) l;
            NativeButton b = new NativeButton("One");
            b.setSizeFull();
            c.addComponent(b);
            c.setExpandRatio(b, 1);
            if (deep > 0) {
                Layout c2 = splitRecursive(--deep);
                c.addComponent(c2);
                c.setExpandRatio(c2, 9);
            }
        } else {
            WeeLayout wl = (WeeLayout) l;
            wl.setClipping(true);
            NativeButton b = new NativeButton("One");
            b.setSizeFull();
            if (vertical) {
                b.setHeight("10%");
            } else {
                b.setWidth("10%");
            }
            l.addComponent(b);
            if (deep > 0) {
                Layout w = splitRecursive(--deep);
                if (vertical) {
                    w.setHeight("90%");
                } else {
                    w.setWidth("90%");
                }
                l.addComponent(w);
            } else {
                b.setSizeFull();
            }
        }
        return l;
    }

    WeeLayout undefinedWithRelativeSizes() {
        WeeLayout wl = new WeeLayout(Direction.HORIZONTAL);
        // wl.setHeight("100%");
        wl.addComponent(new NativeButton("With long caption",
                new Button.ClickListener() {
                    @Override
                    public void buttonClick(ClickEvent event) {
                        event
                                .getButton()
                                .setCaption(
                                        event.getButton().getCaption() == null ? "Long caption"
                                                : null);
                    }
                }), "100%", "30px", Alignment.TOP_LEFT);
        wl.addComponent(new NativeButton("Two"), "100%", "100%",
                Alignment.TOP_LEFT);
        wl.setSmartRelativeSizes(true);
        return wl;
    }

    WeeLayout splitView() {
        WeeLayout wl = new WeeLayout(Direction.HORIZONTAL);
        wl.setSizeFull();
        wl.addComponent(new NativeButton("One", new Button.ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                event.getButton().setWidth("300px");
            }
        }), "100px", "30px", Alignment.TOP_RIGHT);
        wl.addComponent(new Label(""), "14px", "14px", Alignment.TOP_CENTER);
        wl.addComponent(new NativeButton("Two"), "100%", "100%",
                Alignment.TOP_CENTER);
        // wl.setClipping(true);
        return wl;
    }

    WeeLayout createVertical(int recurse) {
        WeeLayout wl = new WeeLayout(Direction.VERTICAL);
        wl.setSizeFull();
        // wl.setWidth("100%");
        // wl.setHeight("50%");
        wl.addComponent(new TextField("Left"), Alignment.TOP_LEFT);
        wl.addComponent(new TextField("Center"), Alignment.TOP_CENTER);
        TextField tf = new TextField("Right");
        tf.setWidth("50%");
        wl.addComponent(tf, Alignment.TOP_RIGHT);
        if (recurse > 0) {
            wl.addComponent(createHorizontal(--recurse));

        }
        return wl;
    }

    WeeLayout createHorizontal(int recurse) {
        WeeLayout wl = new WeeLayout(Direction.HORIZONTAL);
        wl.setSizeFull();
        // wl.setHeight("100%");
        wl.addComponent(new TextField("Top"), Alignment.TOP_LEFT);
        wl.addComponent(new TextField("Middle"), Alignment.MIDDLE_LEFT);
        TextField tf = new TextField("Bottom");
        tf.setHeight("50%");
        wl.addComponent(tf, Alignment.BOTTOM_LEFT);
        if (recurse > 0) {
            wl.addComponent(createVertical(--recurse));

        }
        return wl;
    }

    /** Same with core layouts */

    VerticalLayout createCoreVertical(int recurse) {
        VerticalLayout l = new VerticalLayout();
        l.setSizeFull();
        TextField tf = new TextField("Left");
        l.addComponent(tf);
        l.setComponentAlignment(tf, Alignment.TOP_LEFT);
        tf = new TextField("Center");
        l.addComponent(tf);
        l.setComponentAlignment(tf, Alignment.TOP_CENTER);
        tf = new TextField("Right");
        l.addComponent(tf);
        tf.setWidth("50%");
        l.setComponentAlignment(tf, Alignment.TOP_RIGHT);
        if (recurse > 0) {
            HorizontalLayout createCoreHorizontal = createCoreHorizontal(--recurse);
            l.addComponent(createCoreHorizontal);
            l.setExpandRatio(createCoreHorizontal, 1);
        }
        return l;
    }

    HorizontalLayout createCoreHorizontal(int recurse) {
        HorizontalLayout l = new HorizontalLayout();
        l.setSizeFull();
        TextField tf = new TextField("Top");
        l.addComponent(tf);
        l.setComponentAlignment(tf, Alignment.TOP_LEFT);
        tf = new TextField("Middle");
        l.addComponent(tf);
        l.setComponentAlignment(tf, Alignment.MIDDLE_LEFT);
        tf = new TextField("Bottom");
        l.addComponent(tf);
        tf.setWidth("50%");
        l.setComponentAlignment(tf, Alignment.BOTTOM_LEFT);
        if (recurse > 0) {
            VerticalLayout createCoreVertical = createCoreVertical(--recurse);
            l.addComponent(createCoreVertical);
            l.setExpandRatio(createCoreVertical, 1);
        }
        return l;
    }

}
