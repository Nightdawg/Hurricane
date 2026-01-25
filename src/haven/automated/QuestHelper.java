package haven.automated;

import haven.Button;
import haven.Window;
import haven.*;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuestHelper extends Window {
    public boolean active = false;
    public QuestList questList;
    public static HashMap<String, Coord2d> questGiverLocations = new HashMap<>();
    public static CheckBox ignoreBeginningOfQuestsCheckBox;

    private static final Set<String> TUTORIAL_QUESTS = new HashSet<>(Arrays.asList(
            "something to eat & drink",
            "shifting gear",
            "ways in the wild"
    ));

    public QuestHelper() {
        super(UI.scale(300, 380), "Quest Helper", false);
        questList = new QuestList(UI.scale(280), 12, this);
        add(new PButton(160, "Refresh List", this), UI.scale(20, 10));
        add(ignoreBeginningOfQuestsCheckBox = new CheckBox("Ignore Tutorial Quests"){
            {a = Utils.getprefb("ignoreBeginningOfQuests", true);}
            public void changed(boolean val) {
                Utils.setprefb("ignoreBeginningOfQuests", val);
                refresh();
            }
        }, UI.scale(20, 50));
        add(questList, UI.scale(10, 80));
    }

    public boolean shouldIgnore(String title) {
        if (ignoreBeginningOfQuestsCheckBox == null || !ignoreBeginningOfQuestsCheckBox.a)
            return false;
        String lowerTitle = title.toLowerCase();
        return lowerTitle.contains("beginning") || TUTORIAL_QUESTS.contains(lowerTitle);
    }

    @Override
    public void show() {
        super.show();
        this.active = true;
        refresh();
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if ((sender == this) && (Objects.equals(msg, "close"))) {
            this.active = false;
            questList.reset();
            hide();
            Utils.setprefc("wndc-questHelperWindow", this.c);
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    public void addConds(List<QuestWnd.Quest.Condition> ncond, int id) {
        if (!active) return;

        GameUI gui = ui.gui;
        if (gui != null && gui.chrwdg != null && gui.chrwdg.quest != null) {
            for (QuestWnd.Quest q : gui.chrwdg.quest.cqst.quests) {
                if (q.id == id) {
                    try {
                        if (shouldIgnore(q.title())) return;
                    } catch (Exception e) {
                        return;
                    }
                    break;
                }
            }
        }

        synchronized (questList.quests) {
            boolean changed = false;
            boolean allOthersDone = true;
            for (QuestWnd.Quest.Condition c : ncond) {
                if (!c.desc.toLowerCase().contains("tell") && c.done == 0) {
                    allOthersDone = false;
                    break;
                }
            }

            for (QuestWnd.Quest.Condition c : ncond) {
                String baseDesc = c.desc.replace("\u2605 ", "").replace("\u2713 ", "");
                String prefix = (c.done == 1) ? "\u2713 " : "";
                int status = (c.done == 0 && baseDesc.toLowerCase().contains("tell") && allOthersDone) ? 2 : c.done;

                boolean exists = false;
                for (QuestListItem item : questList.quests) {
                    if (item.parentid == id && item.name.contains(baseDesc)) {
                        if (item.status != status || !item.name.equals(prefix + baseDesc)) {
                            item.status = status;
                            item.name = prefix + baseDesc;
                            changed = true;
                        }
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    QuestListItem newItem = new QuestListItem(prefix + baseDesc, status, id);
                    newItem.coord = questGiverLocations.get(newItem.questGiver);
                    questList.quests.add(newItem);
                    changed = true;
                }
            }

            if (changed) {
                questList.quests.sort(questList.comp);
            }
        }
    }

    public void refresh() {
        this.active = true;
        questList.startRefresh();
    }

    private class PButton extends Button {
        private final QuestHelper parent;

        public PButton(int w, String title, QuestHelper parent) {
            super(w, title);
            this.parent = parent;
        }

        @Override
        public void click() {
            parent.refresh();
        }
    }

    private static class QuestList extends OldListBox<QuestListItem> {
        private static final Coord nameoff = new Coord(0, 5);
        public final List<QuestListItem> quests = Collections.synchronizedList(new ArrayList<>());
        public boolean refreshPending = false;
        private long lastReq = 0;
        private final QuestHelper qh;
        public final Comparator<QuestListItem> comp = Comparator.comparing(a -> a.name);

        public QuestList(int w, int h, QuestHelper qh) {
            super(w, h, UI.scale(24));
            this.qh = qh;
        }

        public void startRefresh() {
            synchronized (quests) {
                quests.clear();
            }
            this.refreshPending = true;
        }

        public void reset() {
            this.refreshPending = false;
            synchronized (quests) {
                quests.clear();
            }
        }

        @Override
        public void tick(double dt) {
            if (!qh.active) return;
            long now = System.currentTimeMillis();

            GameUI gui = ui.gui;
            if (gui == null || gui.chrwdg == null || gui.chrwdg.quest == null) return;

            List<QuestWnd.Quest> gameQuests = gui.chrwdg.quest.cqst.quests;

            synchronized (quests) {
                Iterator<QuestListItem> it = quests.iterator();
                while (it.hasNext()) {
                    QuestListItem item = it.next();
                    boolean stillExists = false;
                    for (QuestWnd.Quest q : gameQuests) {
                        if (q.id == item.parentid) {
                            stillExists = true;
                            break;
                        }
                    }
                    if (!stillExists) it.remove();
                }
            }

            for (Widget w : gui.chrwdg.quest.questbox.children()) {
                if (w instanceof QuestWnd.Quest.Box) {
                    QuestWnd.Quest.Box box = (QuestWnd.Quest.Box) w;

                    try {
                        if (qh.shouldIgnore(box.title())) {
                            continue;
                        }

                        boolean alreadyInList = false;
                        synchronized (quests) {
                            for (QuestListItem item : quests) {
                                if (item.parentid == box.id) {
                                    alreadyInList = true;
                                    break;
                                }
                            }
                        }

                        if (!alreadyInList && box.cond != null && box.cond.length > 0) {
                            qh.addConds(Arrays.asList(box.cond), box.id);
                        }
                    } catch (Exception ignored) {}
                }
            }

            if (refreshPending) {
                if (now - lastReq > 1000) {
                    lastReq = now;
                    boolean allFound = true;

                    int credoId = (gui.chrwdg.skill != null && gui.chrwdg.skill.credos != null)
                            ? gui.chrwdg.skill.credos.pqid : -1;

                    for (QuestWnd.Quest q : gameQuests) {
                        if (q.id == credoId) continue;

                        try {
                            if (qh.shouldIgnore(q.title())) continue;

                            boolean hasData = false;
                            synchronized (quests) {
                                for (QuestListItem item : quests) {
                                    if (item.parentid == q.id) {
                                        hasData = true;
                                        break;
                                    }
                                }
                            }

                            if (!hasData) {
                                gui.chrwdg.wdgmsg("qsel", q.id);
                                allFound = false;
                            }
                        } catch (Exception e) {
                            allFound = false;
                        }
                    }

                    if (allFound) {
                        refreshPending = false;
                    }
                }
            }
        }

        @Override
        protected QuestListItem listitem(int idx) {
            synchronized (quests) {
                return (idx >= 0 && idx < quests.size()) ? quests.get(idx) : null;
            }
        }

        @Override
        protected int listitems() {
            return quests.size();
        }

        @Override
        protected void drawbg(GOut g) {
            g.chcolor(0, 0, 0, 120);
            g.frect(Coord.z, sz);
            g.chcolor();
        }

        @Override
        protected void drawitem(GOut g, QuestListItem item, int idx) {
            if (item == null) return;

            try {
                if (item.status == 2) {
                    g.chcolor(Color.YELLOW);
                } else if (item.status == 1) {
                    g.chcolor(Color.CYAN);
                } else {
                    g.chcolor(Color.WHITE);
                }

                String text = item.name;
                GameUI gui = ui.gui;

                if (item.coord != null && gui != null && gui.map != null && gui.map.player() != null) {
                    int distInTiles = (int) Math.ceil(item.coord.dist(gui.map.player().rc) / 11.0);
                    text += " (" + distInTiles + "t)";
                }

                g.text(text, nameoff);
                g.chcolor();
            } catch (Loading e) {
            }
        }

        @Override
        public void change(QuestListItem item) {
            if (item != null) {
                super.change(item);
                if (ui.gui != null) {
                    ui.gui.chrwdg.wdgmsg("qsel", item.parentid);
                }
            }
        }
    }

    public static class QuestListItem implements Comparable<QuestListItem> {
        public String name;
        public String questGiver = "";
        public int status;
        public int parentid;
        public Coord2d coord;

        public QuestListItem(String name, int status, int parentid) {
            this.name = name;
            this.status = status;
            this.parentid = parentid;

            Matcher m = Pattern.compile("([A-Z][a-z]+)").matcher(name);
            if (m.find()) {
                this.questGiver = m.group(1);
            }
        }

        @Override
        public int compareTo(QuestListItem o) {
            return this.name.compareTo(o.name);
        }
    }
}
