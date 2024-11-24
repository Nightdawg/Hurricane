package haven.automated;


import haven.*;

import static haven.OCache.posres;
import static java.lang.Thread.sleep;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;
import java.lang.Math;

// TODO:
//  Check for left storage
//    Announce if only one storage space is left
//    Cancel action if storage is full


public class WagonNearestPickup implements Runnable {
    private GameUI gui;

    private final double max_distance = 12 * 5;
    private final HashSet<String> liftables_knocked = new HashSet<String>(Arrays.asList(
        "gfx/kritter/badger/badger",
        "gfx/kritter/cattle/cattle",
        "gfx/kritter/badger/badger",
        "gfx/kritter/bear/bear",
        "gfx/kritter/beaver/beaver",
        "gfx/kritter/boar/boar",
        "gfx/kritter/caveangler/caveangler",
        "gfx/kritter/cavelouse/cavelouse",
        "gfx/kritter/chasmconch/chasmconch",
        "gfx/kritter/eagleowl/eagleowl",
        "gfx/kritter/fox/fox",
        "gfx/kritter/goat/wildgoat",
        "gfx/kritter/goldeneagle/goldeneagle",
        "gfx/kritter/greyseal/greyseal",
        "gfx/kritter/horse/horse",
        "gfx/kritter/lynx/lynx",
        "gfx/kritter/moose/moose",
        "gfx/kritter/sheep/sheep",
        "gfx/kritter/ooze/greenooze",
        "gfx/kritter/otter/otter",
        "gfx/kritter/pelican/pelican",
        "gfx/kritter/reddeer/reddeer",
        "gfx/kritter/reindeer/reindeer",
        "gfx/kritter/roedeer/roedeer",
        "gfx/kritter/stoat/stoat",
        "gfx/kritter/swan/swan",
        "gfx/kritter/troll/troll",
        "gfx/kritter/walrus/walrus",
        "gfx/kritter/wolf/wolf",
        "gfx/kritter/wolverine/wolverine",
        "gfx/kritter/woodgrouse/woodgrouse-m"
    ));
    private final HashSet<String> liftables_generic = new HashSet<String>(Arrays.asList(
        "gfx/terobjs/crate",
        "gfx/terobjs/chest",
        "gfx/terobjs/barrel"  
    ));
    //gfx/terobjs/trees/alderlog
    private boolean waitPose(Gob gob, String pose, boolean invert, int delay, int timeout) throws InterruptedException{
        int counter = 0;
        while(gob.getPoses().contains(pose) ^ invert){
            if(counter >= timeout){
                return true;
            }
            counter += delay;
            Thread.sleep(delay);
        }
        return false;
    }

    private boolean TryExitWagonAtAngle(Coord2d target, double degree) throws InterruptedException{
        Gob player = gui.map.player();

        Set<String> poses = player.getPoses();
        String startPose = null;
        if(poses.contains("wagondrivan")){
            startPose = "wagondrivan";
        }else if(poses.contains("wagonsittan")){
            startPose = "wagonsittan";
        }else{
            throw new InterruptedException("Exiting Vehicle failed, unknown pose: (" + String.join(" / ", poses) + ")");
        }

        double angle = player.rc.angle(target) + Math.toRadians(degree);
        double distFromPlayer = target.dist(player.rc);

        Coord2d exitCoords = player.rc.add(Math.cos(angle) * distFromPlayer, Math.sin(angle) * distFromPlayer);
        gui.map.wdgmsg ( "click", Coord.z, exitCoords.floor ( posres ), 1, UI.MOD_CTRL, 0);

        return waitPose(player, startPose, false, 25, 150);
    }

    public WagonNearestPickup(GameUI gui) {
        this.gui = gui;
        Integer ping = GameUI.getPingValue();
    }

    @Override
    public void run() {
        
        try {
            Gob vehicle = null;
            Gob target = null;
            boolean isOnWagon = false;
            
            Gob player = gui.map.player();
            if (player == null)
                return;

            Coord3f raw = player.placed.getc();
            if(raw != null)
                return;
            for (Gob gob : Utils.getAllGobs(gui)) {
                Resource res = null;
                try {
                    res = gob.getres();
                } catch (Loading l) {
                }
                if (res != null) {
                    double distFromPlayer = gob.rc.dist(player.rc);

                    //Is a Wagon, check that, continue after that
                    if (res.name.equals("gfx/terobjs/vehicle/wagon")){
                        if (distFromPlayer <= 3 && (vehicle == null || distFromPlayer < vehicle.rc.dist(player.rc))) {
                            isOnWagon = true;
                            vehicle = gob;
                        }
                        continue;
                    }

                    if(distFromPlayer > max_distance){
                        continue;
                    }

                    if( (liftables_knocked.contains(res.name) && gob.getPoses().contains("knock")) ||
                        (liftables_generic.contains(res.name))){
                        if((target == null || distFromPlayer < target.rc.dist(player.rc))){
                            target = gob;
                        }  
                    }
                    
                }
            }

            if (isOnWagon) {
                
                if (target == null){
                    gui.error("No liftable found.");
                    return;
                }

                //Exit Wagon
                if(TryExitWagonAtAngle(target.rc, 0) && 
                   TryExitWagonAtAngle(target.rc, 45) && 
                   TryExitWagonAtAngle(target.rc, -45)){
                    throw new InterruptedException("Exiting Wagon failed, path is blocked");
                }
                
                //Lift object
                gui.wdgmsg("act", "carry");
                gui.map.wdgmsg("click", Coord.z, target.rc.floor(posres), 1, 0, 0, (int) target.id, target.rc.floor(posres), 0, -1);
                if(waitPose(player, "banzai", true, 30, 3000)){
                    throw new InterruptedException("Lifting object took to long");
                }

                //Store in wagon
                gui.map.wdgmsg("click", Coord.z, vehicle.rc.floor(posres), 3, 0, 0, (int) vehicle.id, vehicle.rc.floor(posres), 0, -1);
                if(waitPose(player, "banzai", false, 30, 5000)){
                    throw new InterruptedException("Storing in wagon took to long");
                }

                //Enter Wagon
                FlowerMenu.setNextSelection("Ride");
                gui.map.wdgmsg("click", Coord.z, vehicle.rc.floor(posres), 3, 0, 0, (int) vehicle.id, vehicle.rc.floor(posres), 0, -1);
                return;
            }
        
        } catch (InterruptedException e) {
            gui.error(e.getMessage());
        }

        if (gui.wagonNearestPickupThread != null) {
            gui.wagonNearestPickupThread.interrupt();
            gui.wagonNearestPickupThread = null;
        }
    }
}
