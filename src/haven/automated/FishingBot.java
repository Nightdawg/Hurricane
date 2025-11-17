package haven.automated;

import haven.*;
import haven.Button;
import haven.Label;
import haven.Window;
import haven.automated.helpers.FishingAtlas;
import haven.widgets.MultiSelectList;
import haven.widgets.TwoOptionSwitch;

import java.awt.*;
import java.util.*;
import java.util.List;

import static haven.OCache.posres;

public class FishingBot extends Window implements Runnable {
    private final GameUI gui;

    private boolean stop;
    private boolean active;

    private final Button startButton;
    private final CheckBox startCheckBox;

    private final TwoOptionSwitch<String> fishingPoleChoice;
    private final MultiSelectList<String> hookChoice;
    private final MultiSelectList<String> fishLineChoice;
    private final MultiSelectList<String> baitChoice;
    private final MultiSelectList<String> lureChoice;

    public FishingBot(GameUI gui) {
        super(UI.scale(415, 190), "Fishing Bot");
        this.gui = gui;
        this.stop = false;
        this.active = false;

        Label fishingLabel = new Label("Choose Fishing Pole:") {
            {
                setstroked(Color.BLACK);
                setcolor(Color.LIGHT_GRAY);
            }
        };
        add(fishingLabel, UI.scale(20, 0));

        Label hookLabel = new Label("Choose Hook:") {
            {
                setstroked(Color.BLACK);
                setcolor(Color.LIGHT_GRAY);
            }
        };
        add(hookLabel, UI.scale(30, 73));

        Label fishLineLabel = new Label("Choose Fishline:") {
            {
                setstroked(Color.BLACK);
                setcolor(Color.LIGHT_GRAY);
            }
        };
        add(fishLineLabel, UI.scale(155, 0));

        Label baitLabel = new Label("Choose Bait:") {
            {
                setstroked(Color.BLACK);
                setcolor(Color.LIGHT_GRAY);
            }
        };
        add(baitLabel, UI.scale(295, 0));


        fishingPoleChoice = add(new TwoOptionSwitch<String>(UI.scale(120, 36), 18, FishingAtlas.fishingPoles, s -> s) {
            @Override
            protected void changed(String sel, int idx) {
                if (idx == 0) {
                    baitChoice.show();
                    lureChoice.hide();
                    baitLabel.settext("Choose Bait:");
                    active = false;
                    startButton.change("Start");
                } else {
                    baitChoice.hide();
                    lureChoice.show();
                    baitLabel.settext("Choose Lure:");
                    active = false;
                    startButton.change("Start");
                }
            }
        }, UI.scale(10, 20));

        hookChoice = add(
                new MultiSelectList<>(UI.scale(130, 72), 18, FishingAtlas.fishingHooks, s -> s),
                UI.scale(10, 92)
        );

        fishLineChoice = add(
                new MultiSelectList<>(UI.scale(130, 144), 18, FishingAtlas.fishingLines, s -> s),
                UI.scale(135, 20)
        );

        baitChoice = add(
                new MultiSelectList<>(UI.scale(140, 144), 18, FishingAtlas.fishingBaits, s -> s),
                UI.scale(260, 20)
        );

        lureChoice = add(
                new MultiSelectList<>(UI.scale(140, 144), 18, FishingAtlas.fishingLures, s -> s),
                UI.scale(260, 20)
        );
        lureChoice.hide();

        startCheckBox = add(new CheckBox("Fishing"){
            {
                a = true;
            }
        }, UI.scale(100, 173));

        startButton = add(new Button(UI.scale(50), "Start") {
            @Override
            public void click() {
                active = !active;
                if (active) {
                    this.change("Stop");
                } else {
                    this.change("Start");
                }
            }
        }, UI.scale(210, 175));
    }

    private int contentAnalysis(WItem item) {
        int count = 0;
        try {
            for (ItemInfo info : item.item.info()) {
                if (info instanceof ItemInfo.Contents) {
                    count++;
                }
            }
        } catch (Loading ignored) {
        }
        return count;
    }


    private void prepareFishingPole() {
        String poleSel = fishingPoleChoice.getSelected();

        Equipory eq = gui.getequipory();
        int hand = -1;
        for (int slot : new int[]{6, 7}) {
            WItem it = eq.slots[slot];
            if (it != null && it.item != null) {
                String name = it.item.getname();
                if (poleSel.equals(name)) {
                    hand = slot;
                    break;
                }
            }
        }
        if (hand == -1) {
            deactivate("Fishbot: You need to wear " + poleSel + ". Stopping..");
            return;
        }

        WItem pole = eq.slots[hand];
        int state = contentAnalysis(pole);

        List<WItem> items = (state < 3)
                ? AUtils.getAllItemsFromAllInventoriesAndStacksExcludeBeltAndKeyring(gui)
                : Collections.emptyList();

        if (state == 0) {
            putOnFishingLine(items, hand);
        } else if (state == 1) {
            putOnHook(items, hand);
        } else if (state == 2) {
            if ("Primitive Casting-Rod".equals(poleSel)) {
                putOnLure(items, hand);
            } else {
                putOnBait(items, hand);
            }
        } else if (state == 3) {
            if (startCheckBox.a) {
                beginFishing();
            }
        }
    }


    private void putOnFishingLine(List<WItem> items, int hand) {
        List<String> selected = fishLineChoice.getSelected();
        Set<String> allowedNames = (selected != null && !selected.isEmpty())
                ? new HashSet<>(selected) : FishingAtlas.fishingLines;

        List<WItem> candidates = new ArrayList<>();
        for (WItem it : items) {
            if (it == null || it.item == null) continue;
            String name = it.item.getname();
            if (name != null && allowedNames.contains(name)) {
                candidates.add(it);
            }
        }

        if (candidates.isEmpty()) {
            deactivate("Fishbot: No matching fishlines found in inventory. Stopping..");
            return;
        }

        Collections.shuffle(candidates);
        WItem chosen = candidates.getFirst();

        chosen.item.wdgmsg("take", Coord.z);

        WItem handItem = gui.getequipory().slots[hand];
        if (handItem != null && handItem.item != null) {
            handItem.item.wdgmsg("itemact", 0);
            sleep(500);
        } else {
            deactivate("Fishbot: No fishing pole in hand slot: " + hand + " to attach the line to. Stopping..");
        }
    }

    private void putOnHook(List<WItem> items, int hand) {
        List<String> selected = hookChoice.getSelected();
        Set<String> allowedNames = (selected != null && !selected.isEmpty())
                ? new HashSet<>(selected)
                : FishingAtlas.fishingHooks;

        List<WItem> candidates = new ArrayList<>();
        for (WItem it : items) {
            if (it == null || it.item == null) continue;
            String name = it.item.getname();
            if (name != null && allowedNames.contains(name)) {
                candidates.add(it);
            }
        }

        if (candidates.isEmpty()) {
            deactivate("Fishbot: No matching hooks found in inventory. Stopping..");
            return;
        }

        Collections.shuffle(candidates);
        WItem chosen = candidates.getFirst();

        chosen.item.wdgmsg("take", Coord.z);

        WItem handItem = gui.getequipory().slots[hand];
        if (handItem != null && handItem.item != null) {
            handItem.item.wdgmsg("itemact", 0);
            sleep(500);
        } else {
            deactivate("Fishbot: No fishing pole in hand slot: " + hand + " to attach the hook to. Stopping..");
        }
    }


    private void putOnBait(List<WItem> items, int hand) {
        List<String> selected = baitChoice.getSelected();
        Set<String> allowedNames = (selected != null && !selected.isEmpty())
                ? new HashSet<>(selected)
                : FishingAtlas.fishingBaits;

        List<WItem> candidates = new ArrayList<>();
        for (WItem it : items) {
            if (it == null || it.item == null) continue;
            String name = it.item.getname();
            if (name != null && allowedNames.contains(name)) {
                candidates.add(it);
            }
        }

        if (candidates.isEmpty()) {
            deactivate("Fishbot: No matching baits found in inventory. Stopping..");
            return;
        }

        Collections.shuffle(candidates);
        WItem chosen = candidates.getFirst();

        chosen.item.wdgmsg("take", Coord.z);

        WItem handItem = gui.getequipory().slots[hand];
        if (handItem != null && handItem.item != null) {
            handItem.item.wdgmsg("itemact", 0);
            sleep(500);
        } else {
            deactivate("Fishbot: No fishing pole in hand slot: " + hand + " to attach the bait to. Stopping..");
        }
    }

    private void putOnLure(List<WItem> items, int hand) {
        List<String> selected = lureChoice.getSelected();
        Set<String> allowedNames = (selected != null && !selected.isEmpty())
                ? new HashSet<>(selected)
                : FishingAtlas.fishingLures;

        List<WItem> candidates = new ArrayList<>();
        for (WItem it : items) {
            if (it == null || it.item == null) continue;
            String name = it.item.getname();
            if (name != null && allowedNames.contains(name)) {
                candidates.add(it);
            }
        }

        if (candidates.isEmpty()) {
            deactivate("Fishbot: No matching lures found in inventory. Stopping..");
            return;
        }

        Collections.shuffle(candidates);
        WItem chosen = candidates.getFirst();

        chosen.item.wdgmsg("take", Coord.z);

        WItem handItem = gui.getequipory().slots[hand];
        if (handItem != null && handItem.item != null) {
            handItem.item.wdgmsg("itemact", 0);
            sleep(500);
        } else {
            deactivate("Fishbot: No fishing pole in hand slot: " + hand + " to attach the lure to. Stopping..");
        }
    }


    @Override
    public void run() {
        while (!stop) {
            if (!checkVitals()) {
                sleep(1000);
                continue;
            }
            if (active) {
                if (gui.maininv.getFreeSpace() < 3) {
                    deactivate("Fishing Bot: Full inventory! Stopping..");
                }
                prepareFishingPole();
            }
            sleep(500);
        }
    }

    private boolean checkVitals() {
        try {
            double hp = gui.getmeters("hp").get(1).a;
            if (hp < 0.02) {
                gui.act("travel", "hearth");
                try {
                    Thread.sleep(8000);
                } catch (InterruptedException ignored) {
                }
                deactivate("Fishing Bot: HP IS " + hp + " .. PORTING HOME!");
                return false;
            }
            double nrj = ui.gui.getmeter("nrj", 0).a;
            if (nrj < 0.25) {
                deactivate("Fishing Bot: Low on energy! Stopping..");
                return false;
            }
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    private Coord2d getFishingCoord() {
        Gob player = gui.map.player();
        if (player == null) {
            return null;
        }
        return new Coord2d(player.rc.x + 22 * Math.cos(player.a), player.rc.y + 22 * Math.sin(player.a));
    }

    private void beginFishing() {
        Gob player = gui.map.player();
        if (player == null) {
            return;
        }
        Set<String> poses = player.getPoses();
        if (poses.contains("fishidle")) {
            return;
        }

        Coord2d fishCoord = getFishingCoord();
        if (fishCoord != null) {
            gui.menu.wdgmsg("use", 7,0);
            sleep(500);
            gui.map.wdgmsg("click", Coord.z, fishCoord.floor(posres), 1, 0);
            sleep(500);
            gui.map.wdgmsg("click", Coord.z, fishCoord.floor(posres), 3, 0);
        }
    }

    private void sleep(int duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException ignored) {
        }
    }

    public void deactivate(String message) {
        gui.msg(message, Color.WHITE);
        System.out.println(message);
        active = false;
        startButton.change("Start");
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if ((sender == this) && (Objects.equals(msg, "close"))) {
            stop = true;
            stop();
            reqdestroy();
            gui.fishingBot = null;
            gui.fishingThread = null;
        } else
            super.wdgmsg(sender, msg, args);
    }

    public void stop() {
        gui.map.wdgmsg("click", Coord.z, gui.map.player().rc.floor(posres), 1, 0);
        if (gui.map.pfthread != null) {
            gui.map.pfthread.interrupt();
        }
        this.destroy();
    }

    private List<FishWindowRow> returnFishWindow() {
        List<FishWindowRow> rows;

        Optional<Window> test = returnFishingWindow();

        if (test.isPresent()) {
            sleep(1000);

            rows = new ArrayList<>();

            boolean sawButton = false;
            Button currentBtn;
            FishWindowRow currentRow = null;
            int labelIndex = 0;

            for (Widget w : test.get().children()) {
                if (!sawButton) {
                    if (w instanceof Button) {
                        sawButton = true;
                        currentBtn = (Button) w;              // start first line
                        currentRow = new FishWindowRow(currentBtn);
                    }
                } else {
                    if (w instanceof Button) {
                        rows.add(currentRow);

                        currentBtn = (Button) w;
                        currentRow = new FishWindowRow(currentBtn);
                        labelIndex = 0;
                    } else if (w instanceof Label) {
                        String t = ((Label) w).texts;
                        t = (t == null) ? "" : t.trim();
                        switch (labelIndex) {
                            case 0:
                                if (!t.isEmpty()) currentRow.fishName = t;
                                break;
                            case 1:
                                if (!t.isEmpty()) {
                                    String digits = t.replaceAll("\\D+", "");
                                    if (!digits.isEmpty()) currentRow.gearPct = Integer.parseInt(digits);
                                }
                                break;
                            case 3:
                                if (!t.isEmpty()) {
                                    String digits = t.replaceAll("\\D+", "");
                                    if (!digits.isEmpty()) currentRow.lurePct = Integer.parseInt(digits);
                                }
                                break;
                            case 5:
                                if (!t.isEmpty()) {
                                    String digits = t.replaceAll("\\D+", "");
                                    if (!digits.isEmpty()) currentRow.finalPct = Integer.parseInt(digits);
                                }
                                break;
                            default:
                                break;
                        }
                        labelIndex++;
                    }
                }
            }

            for (FishWindowRow r : rows) {
                System.out.println(
                        r.button.wdgid() + " - " +
                                (r.fishName == null ? "" : r.fishName) +
                                " | gear=" + (r.gearPct == null ? "" : (r.gearPct + "%")) +
                                " | lure=" + (r.lurePct == null ? "" : (r.lurePct + "%")) +
                                " | final=" + (r.finalPct == null ? "" : (r.finalPct + "%"))
                );
            }

        } else {
            rows = Collections.emptyList();
        }
        return rows;
    }

    private Optional<Window> returnFishingWindow() {
        return ui.getAllWidgets().stream()
                .filter(Window.class::isInstance)
                .map(Window.class::cast)
                .filter(win -> "This is bait".equals(win.cap))
                .findFirst();
    }
}

class FishWindowRow {
    Button button;
    String fishName;
    Integer gearPct;
    Integer lurePct;
    Integer finalPct;

    FishWindowRow(Button button) {
        this.button = button;
    }
}
