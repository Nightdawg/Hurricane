package haven.widgets;

import haven.Coord;
import haven.GOut;
import haven.UI;
import haven.Widget;

import java.util.*;
import java.util.function.Function;

public class TwoOptionSwitch<T> extends Widget {
    private final List<T> items = new ArrayList<>(2);
    private int selectedIndex = 0;

    private final int rowHeight;
    private final int textPaddingX;
    private final int separatorThickness;
    private final Function<T, String> textRenderer;

    private int hoverIndex = -1;

    public TwoOptionSwitch(Coord size, int rowHeight, T first, T second, Function<T, String> renderer) {
        this(size, rowHeight, Arrays.asList(first, second), renderer);
    }

    public TwoOptionSwitch(Coord size, int rowHeight, Collection<T> data, Function<T, String> renderer) {
        this.rowHeight = UI.scale(rowHeight);
        this.textPaddingX = UI.scale(8);
        this.separatorThickness = Math.max(1, UI.scale(1));
        this.textRenderer = (renderer != null) ? renderer : String::valueOf;

        setItemsInternal(data);
        Coord scaled = UI.scale(size);
        int h = Math.max(scaled.y, this.rowHeight * 2);
        this.sz = new Coord(scaled.x, h);
    }

    private void setItemsInternal(Collection<T> data) {
        items.clear();
        if (data != null) items.addAll(data);
        if (items.size() != 2)
            throw new IllegalArgumentException("TwoOptionSwitch requires exactly two items");
        selectedIndex = Math.min(Math.max(selectedIndex, 0), 1);
    }

    public void toggle() {
        selectedIndex = 1 - selectedIndex;
    }

    public String getSelected() {
        return textRenderer.apply(items.get(selectedIndex));
    }

    @Override
    public void resize(Coord newSize) {
        Coord scaled = UI.scale(newSize);
        int h = Math.max(scaled.y, rowHeight * 2);
        super.resize(new Coord(scaled.x, h));
    }

    @Override
    public boolean mousedown(MouseDownEvent ev) {
        if (ev.b != 1) return false;
        int index = ev.c.y / rowHeight;
        if (index < 0 || index >= 2) return false;
        toggle();
        return true;
    }

    @Override
    public void mousemove(MouseMoveEvent ev) {
        int index = ev.c.y / rowHeight;
        hoverIndex = (index >= 0 && index < 2) ? index : -1;
    }

    @Override
    public boolean mousehover(MouseHoverEvent ev, boolean hovering) {
        if (!hovering) hoverIndex = -1;
        return false;
    }

    @Override
    public void draw(GOut g) {
        g.chcolor(16, 16, 16, 160);
        g.frect(Coord.z, this.sz);
        g.chcolor();

        int innerWidth = this.sz.x;

        for (int i = 0; i < 2; i++) {
            int y = i * rowHeight;

            if (i == selectedIndex) {
                g.chcolor(60, 120, 200, 160);
                g.frect(Coord.of(0, y), Coord.of(innerWidth, rowHeight));
                g.chcolor();
            } else if ((i & 1) == 0) {
                g.chcolor(0, 0, 0, 40);
                g.frect(Coord.of(0, y), Coord.of(innerWidth, rowHeight));
                g.chcolor();
            }

            if (i == hoverIndex && i != selectedIndex) {
                g.chcolor(255, 255, 255, 30);
                g.frect(Coord.of(0, y), Coord.of(innerWidth, rowHeight));
                g.chcolor();
            }

            String text = textRenderer.apply(items.get(i));
            g.atext(text, Coord.of(textPaddingX, y + (rowHeight / 2)), 0, 0.5);

            g.chcolor(255, 255, 255, 40);
            g.frect(Coord.of(0, y + rowHeight - separatorThickness),
                    Coord.of(innerWidth, separatorThickness));
            g.chcolor();
        }
        super.draw(g);
    }

    @Override public void wdgmsg(String msg, Object... args) {}
    @Override public void wdgmsg(Widget sender, String msg, Object... args) {}
}
