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
    public HashMap<String, Coord2d> questGiverLocations = new HashMap<>();
    public static CheckBox ignoreBeginningOfQuestsCheckBox;

    public QuestHelper() {
        super(UI.scale(300, 380), "Quest Helper", false);
        add(new PButton(160, "Refresh List", questList), UI.scale(20, 10));
        add(ignoreBeginningOfQuestsCheckBox = new CheckBox("Ignore \"Beginning of...\" Quests"){
            {a = (Utils.getprefb("ignoreBeginningOfQuests", true));}
            public void changed(boolean val) {
                Utils.setprefb("ignoreBeginningOfQuests", val);
                refresh();
            }
        }, UI.scale(20, 50));
        questList = new QuestList(UI.scale(290), 13,this);
        add(questList, UI.scale(16, 75));
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if ((sender == this) && (Objects.equals(msg, "close"))) {
            questList.resetLocation();
            hide();
            disable();
            Utils.setprefc("wndc-questHelperWindow", this.c);
        } else
            super.wdgmsg(sender, msg, args);
    }

    @Override
    public boolean globtype(GlobKeyEvent ev) {
        if (ev.awt.getKeyCode() == 27) {
            hide();
            disable();
            return true;
        }
        return super.globtype(ev);
    }

    public void addConds(List<QuestWnd.Quest.Condition> ncond, int id) {
        if (!active)
            return;
        boolean alltrue = true;
        for (int i = 0; i < ncond.size(); i++) {
            QuestListItem qitem = new QuestListItem(ncond.get(i).desc, ncond.get(i).done, id);
            if (alltrue && i == ncond.size() - 1) {
                qitem = new QuestListItem("\u2605 " + ncond.get(i).desc, 2, id);
            } else if (ncond.get(i).done == 1) {
                qitem = new QuestListItem("\u2713 " + ncond.get(i).desc, ncond.get(i).done, id);
            } else {
                alltrue = false;
            }
            boolean dontadd = false;

            for (QuestListItem item : questList.quests) {
                if (qitem.name.equals(item.name) && qitem.parentid == item.parentid) {
                    dontadd = true;
                }
            }
            if (dontadd == false) {
                qitem.coord = questGiverLocations.get(qitem.questGiver);
                questList.quests.add(qitem);
            }
        }
        questList.quests.sort(questList.comp);
    }

    private void disable() {
        active = false;
    }

    public void refresh() {
        if (!active)
            return;
        questList.quests.clear();
        questList.refresh = true;
        questList.quests.sort(questList.comp);
    }

    private class PButton extends Button {
        public final QuestList tgt;

        public PButton(int w, String title, QuestList tgt) {
            super(w, title);
            this.tgt = tgt;
        }

        @Override
        public void click() {
            refresh();
        }
    }

    private static class QuestList extends OldListBox<QuestListItem> {
        private static final Coord nameoff = new Coord(0, 5);
        public List<QuestListItem> quests = new ArrayList<>(50);
        public boolean refresh = true;
        private long lastUpdateTime = System.currentTimeMillis();
        private final Comparator<QuestListItem> comp = Comparator.comparing(a -> a.name);
        private Coord2d playerPos = null;
        public Coord2d knownLocation = null;
        public Coord2d knownLocationRc = null;
        private QuestHelper questHelper;

        public QuestList(int w, int h, QuestHelper questHelper) {
            super(w, h, UI.scale(24));
            this.questHelper = questHelper;
        }

        @Override
        public void tick(double dt) {
            GameUI gui = ui.gui;
            if (gui == null || gui.menu == null)
                return;

            if (questHelper.active) {
                long timesincelastupdate = System.currentTimeMillis() - lastUpdateTime;
                if (timesincelastupdate < 1000) {
                    refresh = false;
                }

                if (ui != null && (refresh)) {
                    refresh = false;
                    lastUpdateTime = System.currentTimeMillis();

                    quests.clear();
                    try {
                        for (QuestWnd.Quest quest : ui.gui.chrwdg.quest.cqst.quests) {
                            if (quest.id != ui.gui.chrwdg.skill.credos.pqid) {
                                if (!quest.title().startsWith("Beginning of") || !ignoreBeginningOfQuestsCheckBox.a)
                                    ui.gui.chrwdg.wdgmsg("qsel", quest.id);
                            }
                        }
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    } catch (Loading e) {
                        refresh = true;
                    }

                    Collections.sort(quests);
                    if (quests.size() > 0)
                        change(quests.get(0));
                }
            }
        }

        private void resetLocation() {
            knownLocation = null;
            knownLocationRc = null;
            playerPos = null;
        }

        @Override
        protected QuestListItem listitem(int idx) {
            return quests.get(idx);
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
            try {
                if (item.status == 2) {
                    g.chcolor(new Color(0, 255, 0));
                } else if (item.status == 1) {
                    g.chcolor(new Color(0, 255, 255));
                } else {
                    g.chcolor(new Color(255, 255, 255));
                }
                if (item.coord != null && playerPos != null) {
                    g.text(item.name + " d: (" + (int) (item.coord.dist(playerPos) * 100) + ")", nameoff);
                } else {
                    g.text(item.name, nameoff);
                }

                g.chcolor();
            } catch (Loading e) {
            }
        }

        @Override
        public void change(QuestListItem item) {
            if (item != null) {
                super.change(item);
                ui.gui.chrwdg.wdgmsg("qsel", item.parentid);
            }
        }
    }

    public static class QuestListItem implements Comparable<QuestListItem> {
        public String name;
        public int status;
        public int parentid;
        public Coord2d coord;
        public String questGiver;

        public QuestListItem(final String name, final int status, final int parentid) {
            this.coord = null;
            this.questGiver = "";
            this.name = name;
            this.status = status;
            this.parentid = parentid;
            final Pattern p = Pattern.compile("[A-Z].*?([A-Z].*?)\\b.*?");
            final Matcher m = p.matcher(name);
            if (m.find()) {
                this.questGiver = m.group(1);
            }
        }

        @Override
        public int compareTo(final QuestListItem o) {
            return this.name.compareTo(o.name);
        }
    }
}
