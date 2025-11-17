package haven.automated;

import haven.Coord;
import haven.GameUI;

public class GrubGrubBot implements Runnable{
    public static boolean transferTicksInstead = false;
    private final GameUI gui;
    private boolean stop = false;

    public GrubGrubBot(GameUI gui) {
        this.gui = gui;
    }

    @Override
    public void run() {
        while (!stop) {
            int totalTicks = gui.maininv.getItemsPartial("Tick").size();
            if (totalTicks >= 2 ) {
                if(gui.makewnd != null && gui.makewnd.makeWidget != null){
                    gui.makewnd.makeWidget.wdgmsg("make",0);
                }
            }
            sleep(200);

            if(gui.prog != null) {
                sleep(2000);
            }

            for (haven.WItem witem : gui.maininv.getItemsPartial("Grub-Grub")) {
                witem.item.wdgmsg("transfer", Coord.z);
            }
            sleep(200);

            GrubGrubBot.transferTicksInstead = gui.maininv.getFreeSpace() > 1;

            sleep(1000);
        }
    }

    public void stop() {
        stop = true;
    }

    private void sleep(int duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException ignored) {
        }
    }
}
