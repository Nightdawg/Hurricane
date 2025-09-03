package haven.widgets;

import haven.Coord;
import haven.GOut;
import haven.Scrollbar;
import haven.UI;
import haven.Widget;

import java.util.*;
import java.util.function.Function;

public class SingleSelectList<T> extends Widget {
    private final List<T> items = new ArrayList<>();
    private int selectedIndex = -1;

    private final int rowHeight;
    private final int textPaddingX;
    private final int separatorThickness;
    private int scrollOffsetY = 0;
    private final Scrollbar verticalScrollbar;
    private final Function<T, String> textRenderer;

    private int hoverIndex = -1;

    public SingleSelectList(Coord size, int rowHeight, Collection<T> data, Function<T, String> renderer) {
        this.sz = size;
        this.rowHeight = UI.scale(rowHeight);
        this.textPaddingX = UI.scale(8);
        this.separatorThickness = Math.max(1, UI.scale(1));
        this.textRenderer = (renderer != null) ? renderer : String::valueOf;

        if (data != null) items.addAll(data);

        this.verticalScrollbar = add(new Scrollbar(this.sz.y, 0,
                Math.max(0, items.size() * this.rowHeight - this.sz.y)) {
            public void changed() { scrollOffsetY = this.val; }
        }, new Coord(this.sz.x - Scrollbar.width, 0));

        clampScroll();
    }

    public void setItems(Collection<T> data) {
        items.clear();
        if (data != null) items.addAll(data);
        selectedIndex = -1;
        clampScroll();
        resize(this.sz);
    }

    public String getSelected() {
        if (selectedIndex < 0 || selectedIndex >= items.size())
            return null;
        return textRenderer.apply(items.get(selectedIndex));
    }

    @Override
    public void resize(Coord newSize) {
        super.resize(UI.scale(newSize));
        verticalScrollbar.c = new Coord(this.sz.x - Scrollbar.width, 0);
        verticalScrollbar.resize(this.sz.y);
        verticalScrollbar.max = Math.max(0, items.size() * rowHeight - this.sz.y);
        clampScroll();
    }

    private void clampScroll() {
        if (scrollOffsetY < 0) scrollOffsetY = 0;
        int max = Math.max(0, items.size() * rowHeight - Math.max(0, this.sz.y));
        if (scrollOffsetY > max) scrollOffsetY = max;
        verticalScrollbar.val = scrollOffsetY;
    }

    public void scroll(int lines) {
        if (lines == 0) return;
        int delta = lines * rowHeight;
        scrollOffsetY = Math.max(0, Math.min(scrollOffsetY + delta, verticalScrollbar.max));
        verticalScrollbar.val = scrollOffsetY;
    }

    @Override
    public boolean mousewheel(MouseWheelEvent ev) {
        scroll(ev.a);
        return true;
    }

    @Override
    public boolean mousedown(MouseDownEvent ev) {
        if (ev.b != 1) return false;
        if (ev.c.x >= this.sz.x - Scrollbar.width) return false;

        int index = (ev.c.y + scrollOffsetY) / rowHeight;
        if (index < 0 || index >= items.size()) return false;

        if (selectedIndex == index) {
            selectedIndex = -1;
        } else {
            selectedIndex = index;
        }
        return true;
    }

    @Override
    public void mousemove(MouseMoveEvent ev) {
        if (ev.c.x >= this.sz.x - Scrollbar.width) { hoverIndex = -1; return; }
        int index = (ev.c.y + scrollOffsetY) / rowHeight;
        hoverIndex = (index >= 0 && index < items.size()) ? index : -1;
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

        int first = Math.max(0, scrollOffsetY / rowHeight);
        int visibleCount = Math.max(1, (this.sz.y + rowHeight - 1) / rowHeight);
        int last = Math.min(items.size(), first + visibleCount + 1);
        int innerWidth = this.sz.x - Scrollbar.width;

        for (int i = first; i < last; i++) {
            int y = i * rowHeight - scrollOffsetY;

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
