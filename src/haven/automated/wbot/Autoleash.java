package haven.automated;

import haven.*;
import java.awt.*;
import java.util.*;

public class Autoleash implements Runnable {
    private GameUI gui;
	WBotUtils wBotUtils;

    public Autoleash(GameUI gui) {
        this.gui=gui;
		this.wBotUtils = new WBotUtils(gui);
    }

	Gob closestGob;

	@Override
    public void run() {
		wBotUtils.sysMsg("Leash it!");
		closestGob = wBotUtils.findClosestTamedAnimal(350);
		if(closestGob!=null){
			wBotUtils.leash(gui.autoleashThread, closestGob);
		}
    }
}
