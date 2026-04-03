package haven;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.awt.*;
import java.io.File;
import java.util.Arrays;

public class StudyInventory extends Inventory {
    private Tex[] histtex = null;
    private static final Color histclr = new Color(238, 238, 238, 160);

    private double lastCurioAlertPlayed;

    @RName("inv-study")
    public static class $_ implements Factory {
        public Widget create(UI ui, Object[] args) {
            return new StudyInventory((Coord) args[0]);
        }
    }

    public StudyInventory(Coord sz) {
        super(sz);
    }

    @Override
    public void draw(GOut g) {
        if (OptWnd.showStudyReportHistoryCheckBox.a) {
            if (histtex == null) {
                histtex = new Tex[16];
                String chrid = ui.gui.chrid;
                if (chrid != "") {
                    String[] hist = Utils.getprefsa("studyhist_" + chrid, null);
                    if (hist != null) {
                        for (int i = 0; i < 16; i++) {
                            if (!hist[i].equals("null")) {
                                final int _i = i;
                                Defer.later(new Defer.Callable<Void>() {
                                    public Void call() {
                                        try {
                                            Resource res = Resource.remote().load(hist[_i]).get();
                                            histtex[_i] = res.layer(Resource.imgc).tex();
                                        } catch (Loading le) {
                                            Defer.later(this);
                                        }
                                        return null;
                                    }
                                });
                            }
                        }
                    }
                }
            }
            g.chcolor(histclr);
            for (int i = 0; i < 16; i++) {
                Tex tex = histtex[i];
                if (tex != null) {
                    try {
                        int y = i / 4 * Inventory.sqsz.y + 1;
                        int x = i % 4 * Inventory.sqsz.x + 1;
                        g.image(tex, new Coord(x, y));
                    } catch (Resource.LoadException e) {
                    }
                }
            }
            g.chcolor();
        }

        super.draw(g);
    }

    @Override
    public void addchild(Widget child, Object... args) {
        super.addchild(child, args);

        if (OptWnd.showStudyReportHistoryCheckBox.a) {
            String chrid = ui.gui.chrid;
            if (chrid != "") {
                String[] hist = Utils.getprefsa("studyhist_" + chrid, new String[16]);
                if (histtex == null) {
                    histtex = new Tex[16];
                    if (hist != null) {
                        for (int i = 0; i < 16; i++) {
                            final String resname = hist[i];
                            if (resname != null && !resname.equals("null")) {
                                final int _i = i;
                                Defer.later(new Defer.Callable<Void>() {
                                    public Void call() {
                                        try {
                                            Resource res = Resource.remote().load(resname).get();
                                            histtex[_i] = res.layer(Resource.imgc).tex();
                                        } catch (Loading le) {
                                            Defer.later(this);
                                        }
                                        return null;
                                    }
                                });
                            }
                        }
                    }
                }

                for (WItem itm : wmap.values()) {
                    int x = itm.c.x / Inventory.sqsz.x;
                    int y = itm.c.y / Inventory.sqsz.y;
                    int i = y * 4 + x;
                    try {
                        Resource res = itm.item.getres();
                        Resource.Image layer = res.layer(Resource.imgc);
                        if (layer == null)
                            continue;
                        Coord dim = layer.tex().sz();

                        int clearx = dim.x > 32 ? dim.x / 32: 1;
                        int cleary = dim.y > 32 ? dim.y / 32: 1;
                        for (int cx = x; cx < x + clearx; cx++) {
                            for (int cy = y; cy < y + cleary; cy++) {
                                int ci = cy * 4 + cx;
                                try {
                                hist[ci] = null;
                                histtex[ci] = null;
                                } catch (ArrayIndexOutOfBoundsException e) {
                                }
                            }
                        }

                        hist[i] = res.name;
                        histtex[i] = res.layer(Resource.imgc).tex();
                    } catch (Loading e) {
                    }
                }
                Utils.setprefsa("studyhist_" + chrid, hist);
            }
        }
    }

    @Override
    public void cdestroy(Widget w) {
        super.cdestroy(w);
        if (!(w instanceof WItem))
            return;
        GItem item = ((WItem) w).item;
        try {
            haven.resutil.Curiosity ci = ItemInfo.find(haven.resutil.Curiosity.class, item.info());
            if (ci != null && ((WItem) w).itemmeter.get() > 0.99) {
                Resource.Tooltip tt = item.resource().layer(Resource.Tooltip.class);
                if (tt != null)
                    ui.gui.syslog.append("Gained " + ci.exp + " LP (" + tt.t + ")", Color.LIGHT_GRAY);

                if (OptWnd.soundAlertForFinishedCuriositiesCheckBox.a){
                    double now = System.currentTimeMillis();
                    if ((now - lastCurioAlertPlayed) > 200){ // ND: Hopefully this will prevent ear rape if multiple curios finish at the same time, lol.
                        try {
                            File file = new File(haven.MainFrame.gameDir + "res/customclient/sfx/CurioFinished.wav");
                            if (file.exists()) {
                                AudioInputStream in = AudioSystem.getAudioInputStream(file);
                                AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
                                AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
                                Audio.CS klippi = new Audio.PCMClip(pcmStream, 2, 2);
                                ((Audio.Mixer) Audio.player.stream).add(new Audio.VolAdjust(klippi, 0.8));
                                lastCurioAlertPlayed = System.currentTimeMillis();
                            }
                        } catch (Exception e) {
                        }
                    }
                }

                if (OptWnd.autoReloadCuriositiesFromInventoryCheckBox.a) {
                    Window invwnd = ui.gui.getwnd("Inventory");
                    Window cupboard = ui.gui.getwnd("Cupboard");
                    Resource res = item.resource();
                    if (res != null) {
                        if (!replacecurio(invwnd, res, ((WItem) w).c) && cupboard != null)
                            replacecurio(cupboard, res, ((WItem) w).c);
                    }
                }
            }
        } catch (Loading e) {
        }

    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (OptWnd.lockStudyReportCheckBox.a && (msg.equals("invxf") || msg.equals("take") || msg.equals("transfer") || (msg.equals("drop") && args.length > 1))) {
            return;
        } else if (OptWnd.lockStudyReportCheckBox.a && msg.equals("drop")) {
            Coord c = (Coord) args[0];
            for (WItem itm : wmap.values()) {
                for (int x = itm.c.x; x < itm.c.x + itm.sz.x; x += Inventory.sqsz.x) {
                    for (int y = itm.c.y; y < itm.c.y + itm.sz.y; y += Inventory.sqsz.y) {
                        if (x / Inventory.sqsz.x == c.x && y / Inventory.sqsz.y == c.y)
                            return;
                    }
                }
            }
        }
        super.wdgmsg(sender, msg, args);
    }

    private boolean replacecurio(Window wnd, Resource res, Coord c) {
        try {
            for (Widget invwdg = wnd.lchild; invwdg != null; invwdg = invwdg.prev) {
                Inventory inv = Inventory.fromWidget(invwdg);
                if (inv != null) {
                    for (WItem itm : inv.wmap.values()) {
                        GItem ngitm = itm.item;
                        Resource nres = ngitm.resource();
                        if (nres != null && nres.name.equals(res.name)) {
                            ngitm.wdgmsg("take", itm.c);
                            wdgmsg("drop", c.add(sqsz.div(2)).div(invsq.sz()));
                            return true;
                        }
                    }
                    return false;
                }
            }
        } catch (Exception e) { // ignored
        }
        return false;
    }
}
