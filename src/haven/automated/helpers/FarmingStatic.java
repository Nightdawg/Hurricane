package haven.automated.helpers;


import haven.res.ui.grainslot.Grainslot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class FarmingStatic {
    public static final List<Grainslot> grainSlots = new CopyOnWriteArrayList<>();

    public static volatile boolean turnipDrop = false;
}
