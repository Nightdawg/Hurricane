package haven.automated;

import haven.*;
import haven.Window;
import java.awt.*;
import java.util.*;

public class Autoshoot implements Runnable {
    private GameUI gui;
	WBotUtils wBotUtils;

    public Autoshoot(GameUI gui) {
        this.gui=gui;
		this.wBotUtils = new WBotUtils(gui);
    }

	Gob closestGob;

	@Override
    public void run() {
		wBotUtils.sysMsg("Shoot!");
		closestGob = wBotUtils.findClosestTarget(350);
		if(closestGob!=null){
			wBotUtils.shoot(closestGob);
		}
    }
}
