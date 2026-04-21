package haven;

import haven.res.ui.stackinv.ItemStack;
import haven.res.ui.tt.q.qbuff.QBuff;
import haven.res.ui.tt.q.quality.Quality;

import java.awt.Color;
import java.util.*;

import static haven.Inventory.sqsz;

public class ExtInventory extends Widget {
    public enum TransferMode {
        GLOBAL,
        LOCAL
    }

    private static final int MARGIN = UI.scale(5);
    private static final int PANEL_W = UI.scale(180);
    private static final int HEADER_H = UI.scale(20);
    private static final int ROW_H = UI.scale(20);
    private static final int MIN_ROWS = 8;

    private static final Color BG = new Color(16, 16, 16, 128);
    private static final Color ROW_EVEN = new Color(255, 255, 255, 16);
    private static final Color ROW_ODD = new Color(255, 255, 255, 32);
    private static final Color HEADER = new Color(255, 255, 255, 64);

    private static final boolean DEBUG_EXTINV = false;

    public final Inventory inv;
    public final TransferMode transferMode;
    private final QualityPanel panel;

    private Coord lastLayoutInvSz = null;
    private boolean lastLayoutExpanded = false;

    private boolean expanded = false;
    private volatile boolean pendingUnresolvedItems = false;

    public ExtInventory(Inventory inv) {
        this(inv, TransferMode.GLOBAL);
    }

    public ExtInventory(Inventory inv, TransferMode transferMode) {
        super(Coord.z);
        this.inv = inv;
        this.transferMode = transferMode;
        add(inv, Coord.z);
        this.panel = add(new QualityPanel(inv), new Coord(inv.sz.x + MARGIN, 0));
        setExpanded(false);
        relayout();
    }

    private static void dbg(String fmt, Object... args) {
        if (DEBUG_EXTINV) {
            System.out.println("[ExtInventory] " + String.format(fmt, args));
        }
    }

    private static String qstr(WItem w) {
        Double q = qualityOf(w);
        return (q == null) ? "q?" : String.format("q%.1f", q);
    }

    private static String itemdbg(WItem w) {
        String name;
        try {
            name = w.item.getname();
        } catch (Loading l) {
            name = "???";
        }

        String res;
        try {
            res = w.item.res.get().name;
        } catch (Loading l) {
            res = "???";
        }

        return String.format("%s [%s] %s id=%d infoseq=%d",
                name, res, qstr(w), w.item.wdgid(), w.item.infoseq);
    }

    public void togglePanel() {
        setExpanded(!expanded);
    }

    public void setExpanded(boolean v) {
        expanded = v;
        panel.visible = v;
        if (v) {
            panel.refresh();
        }
        relayout();
    }

    private void relayout() {
        if (!expanded) {
            panel.hide();
            resize(inv.sz);

            Window wnd = getparent(Window.class);
            if (wnd != null) {
                wnd.pack();
            }
            lastLayoutInvSz = new Coord(inv.sz);
            lastLayoutExpanded = expanded;
            return;
        }

        panel.show();
        int panelH = Math.max(inv.sz.y, HEADER_H + (ROW_H * MIN_ROWS));
        panel.resize(new Coord(PANEL_W, panelH));
        panel.move(new Coord(inv.sz.x + MARGIN, 0));
        resize(new Coord(inv.sz.x + MARGIN + panel.sz.x, Math.max(inv.sz.y, panel.sz.y)));

        Window wnd = getparent(Window.class);
        if (wnd != null) {
            wnd.pack();
        }

        lastLayoutInvSz = new Coord(inv.sz);
        lastLayoutExpanded = expanded;
    }

    @Override
    public void tick(double dt) {
        super.tick(dt);

        boolean invSizeChanged = (lastLayoutInvSz == null) || !lastLayoutInvSz.equals(inv.sz);
        boolean expandedChanged = (lastLayoutExpanded != expanded);

        boolean panelGeometryChanged = expanded &&
                ((panel.c.x != inv.sz.x + MARGIN) ||
                        (panel.sz.y != Math.max(inv.sz.y, HEADER_H + (ROW_H * MIN_ROWS))));

        if (invSizeChanged || expandedChanged || panelGeometryChanged) {
            relayout();
        }
    }

    private static Inventory inventory(Widget w) {
        return Inventory.fromWidget(w);
    }

    private static boolean isInPlayerInventory(WItem item) {
        Window window = item.getparent(Window.class);
        return window != null && Objects.equals("Inventory", window.cap);
    }

    private static List<Integer> getExternalInventoryIds(UI ui) {
        List<Integer> externalInventoryIds = ui.gui.getAllWindows()
                .stream()
                .flatMap(w -> w.children().stream())
                .map(ExtInventory::inventory)
                .filter(Objects::nonNull)
                .filter(i -> {
                    Window window = i.getparent(Window.class);
                    return window != null && !Inventory.PLAYER_INVENTORY_NAMES.contains(window.cap);
                })
                .map(Widget::wdgid)
                .collect(java.util.stream.Collectors.toList());

        List<Integer> stockpileIds = ui.gui.getAllWindows()
                .stream()
                .map(w -> w.getchild(ISBox.class))
                .filter(Objects::nonNull)
                .map(Widget::wdgid)
                .collect(java.util.stream.Collectors.toList());

        externalInventoryIds.addAll(stockpileIds);
        return externalInventoryIds;
    }

    private long inventoryStateStamp() {
        long sig = 1469598103934665603L;

        for (WItem w : inv.getAllItems()) {
            sig ^= w.item.wdgid();
            sig *= 1099511628211L;

            sig ^= w.item.infoseq;
            sig *= 1099511628211L;

            Double q = qualityOf(w);
            sig ^= (q == null) ? 0L : Double.doubleToLongBits(q);
            sig *= 1099511628211L;

            Widget contents = w.item.contents;
            if (contents instanceof ItemStack) {
                ItemStack stack = (ItemStack) contents;
                sig ^= stack.order.size();
                sig *= 1099511628211L;

                for (GItem gi : stack.order) {
                    sig ^= gi.wdgid();
                    sig *= 1099511628211L;
                }
            }
        }

        return sig;
    }

    private static String rawName(WItem w) {
        try {
            String n = w.item.getname();
            if (n == null)
                return null;
            n = n.trim();
            if (n.isEmpty())
                return null;
            if ("it's null".equals(n) || "exception".equals(n))
                return null;
            return n;
        } catch (Loading l) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isStackWrapperName(String name) {
        return name != null && name.endsWith(", stack of");
    }

    private static Double qualityOf(WItem w) {
        QBuff qb = w.item.getQBuff();
        if (qb != null)
            return qb.q;

        try {
            Quality q = ItemInfo.find(Quality.class, w.item.info());
            if (q != null)
                return q.q;
        } catch (Loading ignored) {
        }

        return null;
    }

    private static int byQuality(WItem a, WItem b) {
        Double qa = qualityOf(a);
        Double qb = qualityOf(b);

        if (Objects.equals(qa, qb))
            return 0;
        if (qa == null)
            return 1;
        if (qb == null)
            return -1;

        return Double.compare(qb, qa);
    }

    private static int byReverseQuality(WItem a, WItem b) {
        return byQuality(b, a);
    }

    private static void sortByQuality(List<WItem> items, boolean reverse) {
        if (reverse) {
            items.sort(ExtInventory::byReverseQuality);
        } else {
            items.sort(ExtInventory::byQuality);
        }
    }

    private static boolean isResolvedForList(WItem w) {
        String name = rawName(w);
        if (name == null)
            return false;
        if (isStackWrapperName(name))
            return false;
        if (w.item.infoseq <= 0)
            return false;
        return qualityOf(w) != null;
    }

    private Map<WItem, WItem> buildTopLevelMap() {
        Map<WItem, WItem> map = new IdentityHashMap<>();

        for (WItem top : inv.getAllItems()) {
            map.put(top, top);

            Widget contents = top.item.contents;
            if (contents instanceof ItemStack) {
                ItemStack stack = (ItemStack) contents;
                for (GItem gi : stack.order) {
                    WItem child = stack.wmap.get(gi);
                    if (child != null) {
                        map.put(child, top);
                    }
                }
            }
        }

        return map;
    }

    private static void processGroup(List<WItem> items, boolean reverse, String action, Object... args) {
        dbg("processGroup action=%s count=%d reverse=%s", action, items.size(), reverse);

        sortByQuality(items, reverse);

        for (WItem item : items) {
            if (item.parent != null) {
                dbg("  send %s -> %s", action, itemdbg(item));
                item.item.wdgmsg(action, args);
            }
        }
    }

    private static void sendInvxf2(GItem item, int amount, int externalInventoryId) {
        Object[] invxf2Args = new Object[3];
        invxf2Args[0] = 0;
        invxf2Args[1] = amount;
        invxf2Args[2] = externalInventoryId;
        item.wdgmsg("invxf2", invxf2Args);
    }

    private void transferGroupSmart(List<WItem> items, boolean reverse) {
        if (transferMode == TransferMode.LOCAL) {
            List<WItem> ordered = new ArrayList<>(items);
            processGroup(ordered, reverse, "transfer", sqsz.div(2));
            return;
        }

        List<Integer> externalInventoryIds = getExternalInventoryIds(ui);

        if (externalInventoryIds.isEmpty()) {
            processGroup(items, reverse, "transfer", sqsz.div(2));
            return;
        }

        List<WItem> ordered = new ArrayList<>(items);
        sortByQuality(ordered, reverse);

        Map<WItem, WItem> topLevelMap = buildTopLevelMap();
        LinkedHashMap<WItem, List<WItem>> grouped = new LinkedHashMap<>();

        for (WItem item : ordered) {
            WItem top = topLevelMap.getOrDefault(item, item);
            grouped.computeIfAbsent(top, k -> new ArrayList<>()).add(item);
        }

        dbg("transferGroupSmart groups=%d reverse=%s", grouped.size(), reverse);

        for (Map.Entry<WItem, List<WItem>> e : grouped.entrySet()) {
            WItem top = e.getKey();
            List<WItem> members = e.getValue();

            Widget contents = top.item.contents;

            if (contents instanceof ItemStack && isInPlayerInventory(top)) {
                ItemStack stack = (ItemStack) contents;
                int amount = Math.min(members.size(), stack.order.size());
                boolean wholeStackSelected = (amount == stack.order.size());

                dbg("  stack transfer top=%s members=%d stack.order=%d amount=%d whole=%s",
                        itemdbg(top), members.size(), stack.order.size(), amount, wholeStackSelected);

                if (amount > 0 && !stack.order.isEmpty()) {
                    if (wholeStackSelected) {
                        for (Integer externalInventoryId : externalInventoryIds) {
                            dbg("    send whole-stack invxf2 via first child id=%d target=%d amount=%d",
                                    stack.order.get(0).wdgid(), externalInventoryId, amount);
                            sendInvxf2(stack.order.get(0), amount, externalInventoryId);
                        }
                    } else {
                        List<WItem> exact = new ArrayList<>(members);
                        exact.sort((a, b) -> {
                            int ia = stack.order.indexOf(a.item);
                            int ib = stack.order.indexOf(b.item);
                            return Integer.compare(ib, ia);
                        });

                        dbg("    mixed stack exact ids ordered=%s",
                                exact.stream().map(w -> w.item.wdgid()).collect(java.util.stream.Collectors.toList()));

                        for (WItem child : exact) {
                            if (child.parent != null) {
                                for (Integer externalInventoryId : externalInventoryIds) {
                                    dbg("    send mixed-stack exact invxf2 -> %s target=%d",
                                            itemdbg(child), externalInventoryId);
                                    sendInvxf2(child.item, 1, externalInventoryId);
                                }
                            }
                        }
                    }
                }
            } else {
                dbg("  loose transfer top=%s members=%d", itemdbg(top), members.size());

                for (WItem item : members) {
                    if (item.parent != null) {
                        dbg("    send loose transfer -> %s", itemdbg(item));
                        item.item.wdgmsg("transfer", sqsz.div(2));
                    }
                }
            }
        }
    }

    private List<WItem> snapshotMatchingItems(String name, String resname, Double q, Grouping grouping) {
        List<WItem> flat = new ArrayList<>();
        for (WItem w : inv.getAllItems()) {
            collectItemsForSnapshot(flat, w);
        }

        List<WItem> out = new ArrayList<>();
        for (WItem w : flat) {
            String wn = safeName(w);
            String wr = safeResname(w);
            Double wq = quantizeQ(qualityOf(w), grouping);

            if (Objects.equals(name, wn) &&
                    Objects.equals(resname, wr) &&
                    Objects.equals(q, wq)) {
                out.add(w);
            }
        }
        return out;
    }

    private void collectItemsForSnapshot(List<WItem> out, WItem w) {
        String name = rawName(w);
        Widget contents = w.item.contents;

        if ((contents instanceof ItemStack) || isStackWrapperName(name)) {
            if (contents instanceof ItemStack) {
                ItemStack stack = (ItemStack) contents;
                if (!stack.order.isEmpty()) {
                    for (GItem gi : stack.order) {
                        WItem sw = stack.wmap.get(gi);
                        if (sw == null) {
                            pendingUnresolvedItems = true;
                            continue;
                        }
                        if (!isResolvedForList(sw)) {
                            pendingUnresolvedItems = true;
                            continue;
                        }
                        out.add(sw);
                    }
                } else {
                    pendingUnresolvedItems = true;
                }
            }
            return;
        }

        if (!isResolvedForList(w)) {
            pendingUnresolvedItems = true;
            return;
        }

        out.add(w);
    }

    private static String safeName(WItem w) {
        String n = rawName(w);
        return (n == null) ? "???" : n;
    }

    private static String safeResname(WItem w) {
        try {
            return w.item.res.get().name;
        } catch (Loading l) {
            return "???";
        }
    }

    private static Double quantizeQ(Double q, Grouping g) {
        if (q == null || g == Grouping.NONE) return q;
        if (g == Grouping.Q) return q;

        q = Math.floor(q);
        if (g == Grouping.Q1) return q;
        if (g == Grouping.Q5) return q - (q % 5.0);
        if (g == Grouping.Q10) return q - (q % 10.0);
        return q;
    }

    private void transferRowWithRetries(String name, String resname, Double q, Grouping grouping, boolean reverse) {
        for (int pass = 0; pass < 5; pass++) {
            if (pass > 0) {
                try {
                    Thread.sleep(120);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            pendingUnresolvedItems = false;
            List<WItem> matching = snapshotMatchingItems(name, resname, q, grouping);

            dbg("retry pass=%d row=%s/%s q=%s matching=%d pending=%s",
                    pass, name, resname, q, matching.size(), pendingUnresolvedItems);

            if (matching.isEmpty()) {
                return;
            }

            transferGroupSmart(matching, reverse);

            if (!pendingUnresolvedItems) {
                return;
            }
        }
    }

    private enum Grouping {
        NONE("Type"),
        Q("Quality"),
        Q1("Q1"),
        Q5("Q5"),
        Q10("Q10");

        public final String label;

        Grouping(String label) {
            this.label = label;
        }

        public Grouping next() {
            Grouping[] vals = values();
            return vals[(ordinal() + 1) % vals.length];
        }

        public Grouping previous() {
            Grouping[] vals = values();
            return vals[(ordinal() - 1 + vals.length) % vals.length];
        }

    }

    private static final class GroupKey {
        final String name;
        final String resname;
        final Double q;

        GroupKey(String name, String resname, Double q) {
            this.name = name;
            this.resname = resname;
            this.q = q;
        }
    }

    private static final class GroupRow {
        final GroupKey key;
        final List<WItem> items;
        final WItem highSample;
        final WItem lowSample;
        final Double avgQ;
        private String text;
        private Tex tex;

        GroupRow(GroupKey key, List<WItem> items) {
            this.key = key;
            this.items = items;
            this.highSample = highestQ(items);
            this.lowSample = lowestQ(items);
            this.avgQ = averageQ(items);
        }

        private static WItem highestQ(List<WItem> items) {
            WItem best = items.get(0);
            for (WItem w : items) {
                if (byQuality(w, best) < 0) {
                    best = w;
                }
            }
            return best;
        }

        private static WItem lowestQ(List<WItem> items) {
            WItem worst = items.get(0);
            for (WItem w : items) {
                if (byQuality(w, worst) > 0) {
                    worst = w;
                }
            }
            return worst;
        }

        private static Double averageQ(List<WItem> items) {
            double sum = 0.0;
            int n = 0;
            for (WItem w : items) {
                Double q = qualityOf(w);
                if (q != null) {
                    sum += q;
                    n++;
                }
            }
            return (n == 0) ? null : (sum / n);
        }

        String text() {
            if (text == null) {
                String qtxt = (avgQ == null) ? "q?" : String.format("q%.1f", avgQ);
                text = String.format("x%d %s  %s", items.size(), key.name, qtxt);
            }
            return text;
        }

        Tex tex() {
            if (tex == null) {
                tex = Text.render(text()).tex();
            }
            return tex;
        }
    }

    private static final class GroupRowComparator implements Comparator<GroupRow> {
        @Override
        public int compare(GroupRow a, GroupRow b) {
            int cmp = a.key.name.compareToIgnoreCase(b.key.name);
            if (cmp != 0) return cmp;

            if (a.key.q == null && b.key.q == null) return a.key.resname.compareTo(b.key.resname);
            if (a.key.q == null) return 1;
            if (b.key.q == null) return -1;

            cmp = -Double.compare(a.key.q, b.key.q);
            if (cmp != 0) return cmp;

            return a.key.resname.compareTo(b.key.resname);
        }
    }

    private final class QualityPanel extends Widget implements DTarget {
        private final Inventory inv;
        private final List<GroupRow> rows = new ArrayList<>();
        public final Scrollbar sb;

        private Grouping grouping = Grouping.Q1;
        private int scroll = 0;
        private Tex headerTex = null;
        private long lastStamp = Long.MIN_VALUE;

        QualityPanel(Inventory inv) {
            super(new Coord(PANEL_W, Math.max(inv.sz.y, HEADER_H + (ROW_H * MIN_ROWS))));
            this.inv = inv;
            this.sb = adda(new Scrollbar(sz.y-ROW_H, 0, 0){
                @Override
                public void changed() {
                    scroll = val;
                    super.changed();
                }
            }, sz.x, ROW_H, 1, 0);
        }

        private void rebuildHeader() {
            headerTex = Text.render("Group: " + grouping.label + "  (click to cycle)").tex();
        }

        private void rebuild() {
            rows.clear();
            pendingUnresolvedItems = false;

            Map<String, List<WItem>> grouped = new LinkedHashMap<>();
            Map<String, GroupKey> keys = new LinkedHashMap<>();

            List<WItem> flat = new ArrayList<>();
            for (WItem w : inv.getAllItems()) {
                collectItems(flat, w);
            }

            for (WItem w : flat) {
                String name = ExtInventory.safeName(w);
                String resname = ExtInventory.safeResname(w);
                Double q = ExtInventory.quantizeQ(qualityOf(w), grouping);

                String bucket;
                if (grouping == Grouping.NONE) {
                    bucket = name + "\u0000" + resname;
                } else {
                    bucket = name + "\u0000" + resname + "\u0000" + ((q == null) ? "null" : String.format("%.4f", q));
                }

                keys.put(bucket, new GroupKey(name, resname, q));
                grouped.computeIfAbsent(bucket, k -> new ArrayList<>()).add(w);
            }

            for (Map.Entry<String, List<WItem>> e : grouped.entrySet()) {
                rows.add(new GroupRow(keys.get(e.getKey()), e.getValue()));
            }

            Collections.sort(rows, new GroupRowComparator());
            sb.val = scroll = Math.max(0, Math.min(scroll, maxScroll()));
            rebuildHeader();
        }

        private void refresh() {
            rebuild();
            lastStamp = ExtInventory.this.inventoryStateStamp();
        }

        private void collectItems(List<WItem> out, WItem w) {
            String name = rawName(w);
            Widget contents = w.item.contents;

            if ((contents instanceof ItemStack) || isStackWrapperName(name)) {
                if (contents instanceof ItemStack) {
                    ItemStack stack = (ItemStack) contents;

                    dbg("collect stack wrapper: %s order=%d", itemdbg(w), stack.order.size());

                    if (!stack.order.isEmpty()) {
                        for (GItem gi : stack.order) {
                            WItem sw = stack.wmap.get(gi);
                            if (sw == null) {
                                dbg("  missing child widget for stack item id=%d", gi.wdgid());
                                continue;
                            }

                            if (!isResolvedForList(sw)) {
                                pendingUnresolvedItems = true;
                                dbg("  skip unresolved child: %s", itemdbg(sw));
                                continue;
                            }

                            dbg("  collect child: %s", itemdbg(sw));
                            out.add(sw);
                        }
                    } else {
                        dbg("  stack empty for now, skipping outer wrapper");
                    }
                } else {
                    dbg("skip wrapper without contents: %s", itemdbg(w));
                }
                return;
            }

            if (!isResolvedForList(w)) {
                pendingUnresolvedItems = true;
                dbg("skip unresolved item: %s", itemdbg(w));
                return;
            }

            dbg("collect item: %s", itemdbg(w));
            out.add(w);
        }

        private int visibleRows() {
            return Math.max(1, (sz.y - HEADER_H) / ROW_H);
        }

        private int maxScroll() {
            sb.max = rows.size() - visibleRows();
            return Math.max(0, rows.size() - visibleRows());
        }

        private GroupRow rowAt(Coord c) {
            if (c.y < HEADER_H) return null;
            int idx = (c.y - HEADER_H) / ROW_H + scroll;
            if (idx < 0 || idx >= rows.size()) return null;
            return rows.get(idx);
        }

        @Override
        public void tick(double dt) {
            super.tick(dt);
            if (!expanded) {
                return;
            }

            long stamp = ExtInventory.this.inventoryStateStamp();
            if (stamp != lastStamp) {
                dbg("rebuild panel: stamp changed old=%d new=%d", lastStamp, stamp);
                rebuild();
                lastStamp = stamp;
            }
        }

        @Override
        public void draw(GOut g) {
            g.chcolor(BG);
            g.frect(Coord.z, sz);
            g.chcolor();

            g.chcolor(HEADER);
            g.frect(Coord.z, new Coord(sz.x, HEADER_H));
            g.chcolor();

            if (headerTex == null) {
                rebuildHeader();
            }
            g.aimage(headerTex, new Coord(4, HEADER_H / 2), 0.0, 0.5);

            int y = HEADER_H;
            int end = Math.min(rows.size(), scroll + visibleRows());
            for (int i = scroll; i < end; i++) {
                GroupRow row = rows.get(i);

                g.chcolor(((i & 1) == 0) ? ROW_EVEN : ROW_ODD);
                g.frect(new Coord(0, y), new Coord(sz.x, ROW_H));
                g.chcolor();

                g.aimage(row.tex(), new Coord(4, y + (ROW_H / 2)), 0.0, 0.5);

                y += ROW_H;
            }

            super.draw(g);
        }

        @Override
        public boolean mousewheel(MouseWheelEvent ev) {
            int ns = scroll + ((ev.a > 0) ? 1 : -1);
            sb.val = scroll = Math.max(0, Math.min(ns, maxScroll()));
            return true;
        }

        @Override
        public boolean mousedown(MouseDownEvent ev) {
            if (ev.b != 1 && ev.b != 3) {
                return false;
            }

            if (ev.c.y < HEADER_H) {
                if (ev.b == 1) {
                    grouping = grouping.next();
                    rebuild();
                    return true;
                } else if (ev.b == 3) {
                    grouping = grouping.previous();
                    rebuild();
                    return true;
                }

            }

            GroupRow row = rowAt(ev.c);
            if (row == null || row.items.isEmpty()) {
                return false;
            }
            if (sb.max <= 0 || (ev.c.x < sb.c.x)) {
                if (ui.modshift && !ui.modmeta && !ui.modctrl) { // ND: Transfer only one item of the clicked quality
                    // Transfer only one item (smart about external inventories)
                    WItem sample = (ev.b == 3) ? row.lowSample : row.highSample;
                    if (sample != null && sample.parent != null) {
                        // If LOCAL mode, just send a normal transfer
                        if (ExtInventory.this.transferMode == TransferMode.LOCAL) {
                            sample.item.wdgmsg("transfer", sqsz.div(2));
                        } else {
                            // GLOBAL mode: try to send to external inventories if any, otherwise fallback to transfer
                            List<Integer> externalInventoryIds = ExtInventory.getExternalInventoryIds(ui);
                            if (externalInventoryIds.isEmpty()) {
                                sample.item.wdgmsg("transfer", sqsz.div(2));
                            } else {
                                // Send invxf2 for the single item to all external inventories
                                for (Integer externalInventoryId : externalInventoryIds) {
                                    sendInvxf2(sample.item, 1, externalInventoryId);
                                }
                            }
                        }
                    }
                    return true;
                }

                if (ui.modmeta && !ui.modctrl) { // ND: Transfer all items of the clicked quality
                    final String rowName = row.key.name;
                    final String rowResname = row.key.resname;
                    final Double rowQ = row.key.q;
                    final Grouping rowGrouping = grouping;
                    final boolean reverse = (ev.b == 3);

                    dbg("alt-click row: %s", row.text());
                    dbg("  row item count=%d pending=%s", row.items.size(), pendingUnresolvedItems);

                    new Thread(() -> ExtInventory.this.transferRowWithRetries(
                            rowName, rowResname, rowQ, rowGrouping, reverse
                    )).start();

                    return true;
                }

                if (ui.modctrl && !ui.modmeta && !ui.modshift) { // ND: Drop only one item of the clicked quality
                    // Drop only one item of the clicked quality
                    WItem sample = (ev.b == 3) ? row.lowSample : row.highSample;
                    if (sample != null && sample.parent != null) {
                        List<WItem> single = new ArrayList<>();
                        single.add(sample);
                        processGroup(single, ev.b == 3, "drop", sqsz.div(2));
                    }
                    return true;
                }

                if (ui.modctrl && ui.modmeta && !ui.modshift) { // ND: Drop all items of the clicked quality
                    List<WItem> ordered = new ArrayList<>(row.items);
                    processGroup(ordered, ev.b == 3, "drop", sqsz.div(2));
                    return true;
                }

                WItem sample = (ev.b == 3) ? row.lowSample : row.highSample;
                if (sample != null && sample.parent != null) {
                    sample.item.wdgmsg("take", sqsz.div(2));
                }
                return true;
            }
            return false;
        }

        @Override
        public Object tooltip(Coord c, Widget prev) {
            GroupRow row = rowAt(c);
            if (row == null || row.highSample == null) {
                return null;
            }
            return row.highSample.tooltip(Coord.z, (prev == this) ? row.highSample : prev);
        }

        @Override
        public boolean drop(DTarget.Drop ev) {
            return false;
        }

        @Override
        public boolean iteminteract(DTarget.Interact ev) {
            return false;
        }
    }
}