package haven;

import java.awt.Color;
import java.util.*;

public class SimplifiedMapColors {
    public static boolean enabled;
    private static final Map<String, Color> tileColors = new HashMap<>();

    public static Color SprintLands;
    public static Color ThirdSpeedLands;
    public static Color Swamps;
    public static Color Thicket;

    static {
        enabled = Utils.getprefb("simplifiedMapColorsEnabled", false);

        String[] sprintLandsColor = Utils.getprefsa("simplifiedMapColors_sprintLands_colorSetting", new String[]{"0", "255", "0", "255"});
        SprintLands = new Color(Integer.parseInt(sprintLandsColor[0]), Integer.parseInt(sprintLandsColor[1]), Integer.parseInt(sprintLandsColor[2]), Integer.parseInt(sprintLandsColor[3]));

        String[] thirdSpeedLandsColor = Utils.getprefsa("simplifiedMapColors_thirdSpeedLands_colorSetting", new String[]{"0", "128", "0", "255"});
        ThirdSpeedLands = new Color(Integer.parseInt(thirdSpeedLandsColor[0]), Integer.parseInt(thirdSpeedLandsColor[1]), Integer.parseInt(thirdSpeedLandsColor[2]), Integer.parseInt(thirdSpeedLandsColor[3]));

        String[] swampsColor = Utils.getprefsa("simplifiedMapColors_swamps_colorSetting", new String[]{"0", "128", "128", "255"});
        Swamps = new Color(Integer.parseInt(swampsColor[0]), Integer.parseInt(swampsColor[1]), Integer.parseInt(swampsColor[2]), Integer.parseInt(swampsColor[3]));

        String[] thicketColor = Utils.getprefsa("simplifiedMapColors_thicket_colorSetting", new String[]{"255", "255", "0", "255"});
        Thicket = new Color(Integer.parseInt(thicketColor[0]), Integer.parseInt(thicketColor[1]), Integer.parseInt(thicketColor[2]), Integer.parseInt(thicketColor[3]));

        updateColorMappings();
    }

    public static void updateColorMappings() {
        tileColors.clear();
        updateSprintLandsMapping();
        updateThirdSpeedLandsMapping();
        updateSwampsMapping();
        updateThicketMapping();
    }

    public static void updateSprintLandsMapping() {
        tileColors.put("gfx/tiles/acreclaypit", SprintLands);
        tileColors.put("gfx/tiles/badlands", SprintLands);
        tileColors.put("gfx/tiles/beach", SprintLands);
        tileColors.put("gfx/tiles/bluesod", SprintLands);
        tileColors.put("gfx/tiles/bountyacre", SprintLands);
        tileColors.put("gfx/tiles/cave", SprintLands);
        tileColors.put("gfx/tiles/cloudrange", SprintLands);
        tileColors.put("gfx/tiles/dirt", SprintLands);
        tileColors.put("gfx/tiles/dryflat", SprintLands);
        tileColors.put("gfx/tiles/field", SprintLands);
        tileColors.put("gfx/tiles/flowermeadow", SprintLands);
        tileColors.put("gfx/tiles/gleamgrotto", SprintLands);
        tileColors.put("gfx/tiles/grass", SprintLands);
        tileColors.put("gfx/tiles/greensward", SprintLands);
        tileColors.put("gfx/tiles/hardsteppe", SprintLands);
        tileColors.put("gfx/tiles/heath", SprintLands);
        tileColors.put("gfx/tiles/highground", SprintLands);
        tileColors.put("gfx/tiles/lushcave", SprintLands);
        tileColors.put("gfx/tiles/lushfield", SprintLands);
        tileColors.put("gfx/tiles/moor", SprintLands);
        tileColors.put("gfx/tiles/oxpasture", SprintLands);
        tileColors.put("gfx/tiles/peatmoss", SprintLands);
        tileColors.put("gfx/tiles/ploweddirt", SprintLands);
        tileColors.put("gfx/tiles/redplain", SprintLands);
        tileColors.put("gfx/tiles/scrubveld", SprintLands);
        tileColors.put("gfx/tiles/seabirdrookery", SprintLands);
        tileColors.put("gfx/tiles/shadehollow", SprintLands);
        tileColors.put("gfx/tiles/skargard", SprintLands);
        tileColors.put("gfx/tiles/warmdepth", SprintLands);
        tileColors.put("gfx/tiles/wildcavern", SprintLands);
        tileColors.put("gfx/tiles/wildmoor", SprintLands);
        tileColors.put("gfx/tiles/wildturf", SprintLands);
    }

    public static void updateThirdSpeedLandsMapping() {
        tileColors.put("gfx/tiles/ashland", ThirdSpeedLands);
        tileColors.put("gfx/tiles/beechgrove", ThirdSpeedLands);
        tileColors.put("gfx/tiles/blackwood", ThirdSpeedLands);
        tileColors.put("gfx/tiles/deeptangle", ThirdSpeedLands);
        tileColors.put("gfx/tiles/dryweald", ThirdSpeedLands);
        tileColors.put("gfx/tiles/greenbrake", ThirdSpeedLands);
        tileColors.put("gfx/tiles/grove", ThirdSpeedLands);
        tileColors.put("gfx/tiles/leaf", ThirdSpeedLands);
        tileColors.put("gfx/tiles/leafpatch", ThirdSpeedLands);
        tileColors.put("gfx/tiles/lichenwold", ThirdSpeedLands);
        tileColors.put("gfx/tiles/mossbrush", ThirdSpeedLands);
        tileColors.put("gfx/tiles/oakwilds", ThirdSpeedLands);
        tileColors.put("gfx/tiles/pinebarren", ThirdSpeedLands);
        tileColors.put("gfx/tiles/rootbosk", ThirdSpeedLands);
        tileColors.put("gfx/tiles/shadycopse", ThirdSpeedLands);
        tileColors.put("gfx/tiles/sombrebramble", ThirdSpeedLands);
        tileColors.put("gfx/tiles/sourtimber", ThirdSpeedLands);
        tileColors.put("gfx/tiles/timberland", ThirdSpeedLands);
        tileColors.put("gfx/tiles/wald", ThirdSpeedLands);
    }

    public static void updateSwampsMapping() {
        tileColors.put("gfx/tiles/bog", Swamps);
        tileColors.put("gfx/tiles/bogwater", Swamps);
        tileColors.put("gfx/tiles/fen", Swamps);
        tileColors.put("gfx/tiles/fenwater", Swamps);
        tileColors.put("gfx/tiles/swamp", Swamps);
        tileColors.put("gfx/tiles/swampwater", Swamps);
        tileColors.put("gfx/tiles/shallowwater", Swamps);
    }

    public static void updateThicketMapping() {
        tileColors.put("gfx/tiles/thicket", Thicket);
    }

    public static int applyColor(String tileName, int originalRgb) {
        if (!enabled) {
            return originalRgb;
        }
        Color customColor = tileColors.get(tileName);
        if(customColor != null) {
            return customColor.getRGB();
        }
        return originalRgb;
    }
}
