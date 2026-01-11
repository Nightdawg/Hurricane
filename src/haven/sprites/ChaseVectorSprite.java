package haven.sprites;

import haven.*;
import haven.render.Pipe;

import java.awt.*;
import java.util.Arrays;

public class ChaseVectorSprite extends Sprite implements PView.Render2D {


    public static Color YOURCOLOR = OptWnd.yourselfVectorColorOptionWidget.currentColor;
    public static Color FRIENDCOLOR = OptWnd.friendVectorColorOptionWidget.currentColor;
    public static Color ENEMYCOLOR = OptWnd.enemyVectorColorOptionWidget.currentColor;
    private final Gob gob;
    private final UI ui;

    public ChaseVectorSprite(Gob gob) {
        super(gob, null);
        this.gob = gob;
        this.ui = gob.glob.sess.ui;
    }

    private static final String[] IGNOREDCHASEVECTORS = {
            "gfx/terobjs/vehicle/cart",
            "gfx/terobjs/vehicle/wagon",
            "gfx/terobjs/vehicle/wheelbarrow",
    };

    public void draw(GOut g, Pipe state) {
        if (OptWnd.drawChaseVectorsCheckBox.a) {
            try {
                if (gob != null && ui != null) {
                    MapView mv = ui.gui.map;
                    if (mv != null) {
                        Moving mov = gob.getattr(Moving.class);
                        if (mov instanceof Homing) {
                            Gob target = ((Homing) mov).targetGob();
                            if (target != null) {
                                Resource targetRes = target.getres();
                                boolean drawMyColor = false;
			                    if (Arrays.stream(IGNOREDCHASEVECTORS).noneMatch(gob.getres().name::contains) && Arrays.stream(IGNOREDCHASEVECTORS).noneMatch(targetRes.name::contains)) {
                                    if (!gob.occupants.isEmpty()){
                                        if (gob.getres().name.equals("gfx/terobjs/vehicle/rowboat")) {
                                            for (Gob occupant : gob.occupants) {
                                                if (occupant.getPoses().contains("rowboat-d") || occupant.getPoses().contains("rowing")) {
                                                    if (occupant.isMe) {
                                                        drawMyColor = true;
                                                        break;
                                                    }
                                                }
                                            }
                                        } else if (gob.getres().name.equals("gfx/terobjs/vehicle/knarr")) {
                                            for (Gob occupant : gob.occupants) {
                                                if (occupant.getPoses().contains("knarrman9")) {
                                                    if (occupant.isMe) {
                                                        drawMyColor = true;
                                                        break;
                                                    }
                                                }
                                            }
                                        } else if (gob.getres().name.equals("gfx/terobjs/vehicle/spark")) {
                                            for (Gob occupant : gob.occupants) {
                                                if (occupant.getPoses().contains("sparkan-idle") || occupant.getPoses().contains("sparkan-sparkan")) {
                                                    if (occupant.isMe) {
                                                        drawMyColor = true;
                                                        break;
                                                    }
                                                }
                                            }
                                        } else if (gob.getres().name.equals("gfx/terobjs/vehicle/snekkja")) {
                                            for (Gob occupant : gob.occupants) {
                                                if (occupant.getPoses().contains("snekkjaman0")) {
                                                    if (occupant.isMe) {
                                                        drawMyColor = true;
                                                        break;
                                                    }
                                                }
                                            }
                                        } else {
                                            for (Gob occupant : gob.occupants) {
                                                if (occupant.isMe) {
                                                    drawMyColor = true;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                    Color chaserColor;
                                    if (gob.isMe || drawMyColor) {
                                        chaserColor = YOURCOLOR;
                                    } else if (gob.isPartyMember() && !gob.isMe) {
                                        chaserColor = FRIENDCOLOR;
                                    } else {
                                        chaserColor = ENEMYCOLOR;
                                    }
                                    Coord ChaserCoord = mv.screenxf(gob.getc()).round2();
                                    Coord TargetCoord = mv.screenxf(target.getc()).round2();
                                    g.chcolor(Color.BLACK);
                                    g.line(ChaserCoord, TargetCoord, 4);
                                    g.chcolor(chaserColor);
                                    g.line(ChaserCoord, TargetCoord, 2);
                                    g.chcolor();
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }
    }
}
