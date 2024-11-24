package haven.automated;

import haven.Label;
import haven.Scrollbar;
import haven.Window;
import haven.*;
import haven.automated.helpers.TileStatic;

import java.awt.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static haven.OCache.posres;

public class OreAndStoneCounter extends Window implements Runnable {
    private final GameUI gui;
    private boolean stop;
    private OreList oreList;

    public OreAndStoneCounter(GameUI gui) {
        super(UI.scale(200, 35), "Ore & Stone Counter");
        this.gui = gui;
        this.stop = false;
        this.oreList = new OreList(250, 20);
        add(oreList, UI.scale(0, 0));
    }

    @Override
    public void run(){
        while(!stop){
            Map<String, Integer> ores = new TreeMap<>();
            Map<String, Integer> rocks = new TreeMap<>();
            for (int x = -44; x < 44; x++) {
                for (int y = -44; y < 44; y++) {
                    try {
                        if(gui.map.player() == null){
                            continue;
                        }
                        int t = gui.ui.sess.glob.map.gettile(gui.map.player().rc.floor().div(11).add(x, y));
                        Resource res =  gui.ui.sess.glob.map.tilesetr(t);
                        if(res.name.contains("gfx/tiles/rocks/")){
                            String name = res.basename();
                            if(TileStatic.ORE_NAMES.get(name) != null){
                                name = TileStatic.ORE_NAMES.get(name);
                                ores.put(name, ores.getOrDefault(name, 1) + 1);
                            } else {
                                rocks.put(name, rocks.getOrDefault(name, 1) + 1);
                            }
                        }
                    } catch (Loading ignored) {}
                }
            }
            oreList.removeAll();
            for(Map.Entry<String, Integer> finalOres : ores.entrySet()){
                oreList.addItem(new Ore(finalOres.getKey().toUpperCase(), finalOres.getValue(), true));
            }
            for(Map.Entry<String, Integer> finalRocks : rocks.entrySet()){
                if(oreList.listOres() < 21){
                    oreList.addItem(new Ore(finalRocks.getKey().toUpperCase(), finalRocks.getValue(), false));
                }
            }
            if(oreList.listOres() == 0){
            this.resize(UI.scale(200), UI.scale(35));
            } else  {
                pack();
            }
            sleep(5000);
        }
    }

    private void sleep(int duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException ignored) {}
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if ((sender == this) && (Objects.equals(msg, "close"))) {
            stop = true;
            stop();
            reqdestroy();
            gui.oreAndStoneCounter = null;
            gui.oreAndStoneCounterThread = null;
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
    
    public static class OreList extends Widget {
        ArrayList<Ore> ores = new ArrayList<>();
        Scrollbar sb;
        int rowHeight = UI.scale(20);
        int rows, w;
        
        public OreList(int w, int rows) {
            this.rows = rows;
            this.w = w;
            this.sz = new Coord(UI.scale(w), rowHeight * rows);
            sb = new Scrollbar(rowHeight * rows, 0, 100);
        }

        public void addItem(Ore item) {
            add(item);
            ores.add(item);
        }

        public void deleteItem(Ore item) {
            item.dispose();
            ores.remove(item);
            if (sb.max - 1 < sb.min) {
                sb.val = 0;
            } else if (sb.max - 1 < sb.val) {
                sb.val = sb.max - 1;
            }
        }

        public void removeAll() {
            for (Ore item : ores) {
                item.dispose();
            }
            ores.clear();
        }

        public int listOres() {
            return ores.size();
        }

        @Override
        public void draw(GOut g) {
            sb.max = ores.size() - rows;
            for (int i = 0; i < rows; i++) {
                if (i + sb.val >= ores.size())
                    break;
                GOut ig = g.reclip(new Coord(UI.scale(15), i * rowHeight), UI.scale(w - UI.scale(15), rowHeight));
                ores.get(i + sb.val).draw(ig);
            }
            super.draw(g);
        }

        @Override
        public void wdgmsg(Widget sender, String msg, Object... args) {
            if (msg.equals("delete") && sender instanceof Ore) {
                deleteItem((Ore) sender);
            } else {
                super.wdgmsg(sender, msg, args);
            }
        }
    }

    public static class Ore extends Widget {
        private Label nameLbl;
        private Label countLbl;

        public Ore(String name, int count, boolean ore) {
            nameLbl = new Label(name);
            if(ore){
                nameLbl.setcolor(Color.YELLOW);
            }
            countLbl = new Label(String.valueOf(count));
            if(count>50 && ore){
                countLbl.setcolor(Color.RED);
            }

            add(nameLbl, UI.scale(10, 4));
            add(countLbl, UI.scale(120, 4));
        }
    }

    @Override
    public void reqdestroy() {
        Utils.setprefc("wndc-oreAndStoneCounterWindow", this.c);
        super.reqdestroy();
    }

}
