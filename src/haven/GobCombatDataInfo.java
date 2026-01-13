package haven;

import haven.render.Homo3D;
import haven.render.Pipe;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static haven.Fightsess.*;

public class GobCombatDataInfo extends GobInfo {

    public Gob gob;
    Fightview.Relation rel;
    int combatMedColorShift = 0;
    private boolean combatMedAlphaShiftUp = true;

    public GobCombatDataInfo(Gob owner, Fightview.Relation rel) {
        super(owner);
        gob = owner;
        this.rel = rel;
    }

    @Override
    public void draw(GOut g, Pipe state) {
        up(15);
        combatMedAlphaShift();
        if (!GameUI.showUI)
            return;
        if (gob != null && gob.glob != null && gob.glob.sess != null && gob.glob.sess.ui != null && gob.glob.sess.ui.gui != null && gob.glob.sess.ui.gui.fs != null) {
            Fightsess fs = gob.glob.sess.ui.gui.fs;
            if (OptWnd.drawFloatingCombatDataCheckBox.a) {
                if (OptWnd.drawFloatingCombatDataOnCurrentTargetCheckBox.a) {
                    if (rel == fs.fv.current) {
                        Coord sc = Homo3D.obj2sc(pos, state, Area.sized(g.sz()));
                        if (sc != null)
                            drawCombatData(g, rel, sc, OptWnd.includeCurrentTargetShowOpeningsAbovePercentageCombatInfoCheckBox.a, true);
                    }
                }
                if (OptWnd.drawFloatingCombatDataOnOthersCheckBox.a) {
                    if (rel != fs.fv.current) {
                        Coord sc = Homo3D.obj2sc(pos, state, Area.sized(g.sz()));
                        if (sc != null)
                            drawCombatData(g, rel, sc, !OptWnd.onlyShowOpeningsAbovePercentageCombatInfoCheckBox.a, !OptWnd.onlyShowCoinsAbove4CombatInfoCheckBox.a);
                    }
                }
            }

        }
    }

    @Override
    protected boolean enabled() {
        return false;
    }

    @Override
    protected Tex render() {
        return null;
    }

    private void drawCombatData(GOut g, Fightview.Relation rel, Coord sc, boolean showAllOpenings, boolean alwaysShowCoins) {
        int scaledY = sc.y - UI.scale(86);
        Coord topLeft = new Coord(sc.x - UI.scale(32), scaledY);
        boolean openings;
        boolean cleaveUsed = false;
        long cleaveDuration = 4300;
        //Check if cleave indicator is needed
        if (rel.lastActCleave != null) {
            // ND: Figure out the *MINIMUM* cooldown someone can have after cleaving.
            if (gob.currentWeapon.equals("b12axe"))
                cleaveDuration = 5400;
            else if (gob.currentWeapon.equals("cutblade"))
                cleaveDuration = 5200;
            cleaveUsed = System.currentTimeMillis() - rel.lastActCleave < cleaveDuration;
        }
        boolean defenseUsed = false;
        if (rel.lastActDefence != null) {
            defenseUsed = System.currentTimeMillis() - rel.lastActDefence < rel.lastDefenceDuration;
        }

        // ND: Check for openings depending on whether it's a player or not
        if (gob != null && gob.getres() != null) {
            if (gob.getres().name.equals("gfx/borka/body")) { // ND: If it's a player, the first buff is the maneuver, always, so skip it.
                openings = rel.buffs.children(Buff.class).size() > 1;
            } else { // ND: Everything else doesn't have a maneuver.
                openings = rel.buffs.children(Buff.class).size() > 0;
            }
        } else {
            openings = rel.buffs.children(Buff.class).size() > 1;
        }

        topLeft.x -= UI.scale(2) * rel.buffs.children(Buff.class).size();

        // IP / OIP Text
        boolean showCoins = true;
        if (!alwaysShowCoins)
            if (rel.ip < 4 && rel.oip < 4)
                showCoins = false;
        if (showCoins) {
            g.chcolor(0, 0, 0, 120);
            g.frect(new Coord(topLeft.x + UI.scale(3), topLeft.y + UI.scale(9)), UI.scale(new Coord(39, 20)));
            g.chcolor(255, 255, 255, 255);
            int oipOffset = rel.oip < 10 ? 35 : 40;
            g.aimage(Text.renderstroked(Integer.toString(rel.ip), OptWnd.myIPCombatColorOptionWidget.currentColor, Color.BLACK, ipAdditionalFont).tex(), new Coord(topLeft.x + UI.scale(20), topLeft.y + UI.scale(19)), 1, 0.5);
            g.aimage(Text.renderstroked("-", Color.WHITE, Color.BLACK, ipAdditionalFont).tex(), new Coord(topLeft.x + UI.scale(26), topLeft.y + UI.scale(18)), 1, 0.5);
            g.aimage(Text.renderstroked(Integer.toString(rel.oip), OptWnd.enemyIPCombatColorOptionWidget.currentColor, Color.BLACK, ipAdditionalFont).tex(), new Coord(topLeft.x + UI.scale(oipOffset), topLeft.y + UI.scale(19)), 1, 0.5);
        }

        // Maneuver
        if (OptWnd.showCombatManeuverCombatInfoCheckBox.a) {
            for (Buff buff : rel.buffs.children(Buff.class)) {
                try {
                    if (buff.res != null && buff.res.get() != null) {
                        String name = buff.res.get().name;
                        if (Config.maneuvers.contains(name)) {
                            g.chcolor(0, 0, 0, 255);
                            g.frect(new Coord(topLeft.x + UI.scale(41), topLeft.y + UI.scale(9)), UI.scale(new Coord(20, 20)));
                            g.chcolor(255, 255, 255, 255);
                            int meterValue = getOpeningValue(buff);
                            Resource.Image img = buff.res.get().flayer(Resource.imgc);
                            Tex maneuverTexture = new TexI(PUtils.uiscale(img.scaled, UI.scale(new Coord(18, 18))));
                            if (name.equals("paginae/atk/combmed")) {
                                if (meterValue > 70) {
                                    g.chcolor(255, 255 - combatMedColorShift, 255 - combatMedColorShift, 255);
                                }
                            }
                            g.image(maneuverTexture, new Coord(topLeft.x + UI.scale(42), topLeft.y + UI.scale(10)));

                            // Meter
                            if (meterValue > 1) {
                                g.chcolor(0, 0, 0, 255);
                                g.frect(new Coord(topLeft.x + UI.scale(61), topLeft.y + UI.scale(9)), UI.scale(new Coord(5, 20)));
                                if (meterValue < 30) {
                                    g.chcolor(255, 255, 255, 255);
                                } else {
                                    g.chcolor(255, (255 - (255 * meterValue) / 100), (255 - (255 * meterValue) / 100), 255);
                                }
                                g.frect(new Coord(topLeft.x + UI.scale(62), topLeft.y + UI.scale(28) - ((18 * meterValue) / 100)), UI.scale(new Coord(3, (18 * meterValue) / 100)));
                            }
                        }
                    }
                } catch (Loading ignored) {
                }
            }
        }


        // openings, only if it has any
        if (openings) {
            List<Fightsess.TemporaryOpening> openingList = new ArrayList<>();
            for (Buff buff : rel.buffs.children(Buff.class)) {
                try {
                    if (buff.res != null && buff.res.get() != null) {
                        Tex img = buff.res.get().flayer(Resource.imgc).tex();
                        String name = buff.res.get().name;
                        if (OptWnd.improvedOpeningsImageColor.containsKey(name)) {
                            int meterValue = getOpeningValue(buff);
                            openingList.add(new Fightsess.TemporaryOpening(meterValue, name, OptWnd.improvedOpeningsImageColor.get(name), img));
                        }
                    }
                } catch (Loading ignored) {
                }
            }
            openingList.sort((o1, o2) -> Integer.compare(o2.value, o1.value));
            boolean showOpenings = true;
            if (!showAllOpenings){
                if (!openingList.isEmpty() && !OptWnd.minimumOpeningTextEntry.text().isEmpty()){
                    if (openingList.get(0).value < Integer.parseInt(OptWnd.minimumOpeningTextEntry.text())){
                        showOpenings = false;
                    }
                }
            }
            if (showOpenings) {
                int openingOffsetX = 4;
                for (Fightsess.TemporaryOpening opening : openingList) {
                    g.chcolor(0, 0, 0, 255);
                    g.frect(new Coord(topLeft.x + UI.scale(openingOffsetX) - UI.scale(1), topLeft.y + UI.scale(30) - UI.scale(1)), UI.scale(new Coord(20, 20)));
                    g.chcolor(opening.color);
                    if (OptWnd.showCombatOpeningsAsLettersCheckBox.a)
                        g.image(opening.img, new Coord(topLeft.x + UI.scale(openingOffsetX), topLeft.y + UI.scale(30)), UI.scale(new Coord(18, 18)));
                    else
                        g.frect(new Coord(topLeft.x + UI.scale(openingOffsetX), topLeft.y + UI.scale(30)), UI.scale(new Coord(18, 18)));
                    g.chcolor(255, 255, 255, 255);

                    int valueOffset = opening.value < 10 ? 15 : opening.value< 100 ? 18 : 20;
                    g.aimage(Text.renderstroked(String.valueOf(opening.value), openingAdditionalFont).tex(), new Coord(topLeft.x + UI.scale(openingOffsetX) + UI.scale(valueOffset) - UI.scale(1), topLeft.y + UI.scale(39)), 1, 0.5);
                    openingOffsetX += 19;
                }
            }
        }

        //add cleave cooldown indicator
        if (cleaveUsed) {
            long timer = ((cleaveDuration - (System.currentTimeMillis() - rel.lastActCleave)));
            g.chcolor(new Color(0, 0, 0, 255));
            g.frect(new Coord(topLeft.x + UI.scale(3), topLeft.y - UI.scale(4)), UI.scale(new Coord((int) ((76 * timer)/cleaveDuration)+2, 13)));
            g.chcolor(new Color(213, 0, 0, 255));
            g.frect(new Coord(topLeft.x + UI.scale(4), topLeft.y - UI.scale(3)), UI.scale(new Coord((int) ((76 * timer)/cleaveDuration), 11)));
            g.chcolor(new Color(255, 255, 255, 255));
            g.aimage(Text.renderstroked(getCooldownTime(timer), cleaveAdditionalFont).tex(), new Coord(topLeft.x + UI.scale(52), topLeft.y + UI.scale(2)), 1, 0.5);
        }

        //add defense cooldown indicator, just like cleave
        if (defenseUsed) {
            long timer = ((rel.lastDefenceDuration - (System.currentTimeMillis() - rel.lastActDefence)));
            g.chcolor(new Color(0, 0, 0, 255));
            g.frect(new Coord(topLeft.x + UI.scale(3), topLeft.y - UI.scale(4)), UI.scale(new Coord((int) ((76 * timer)/rel.lastDefenceDuration)+2, 13)));
            g.chcolor(new Color(227, 136, 0, 255));
            g.frect(new Coord(topLeft.x + UI.scale(4), topLeft.y - UI.scale(3)), UI.scale(new Coord((int) ((76 * timer)/rel.lastDefenceDuration), 11)));
            g.chcolor(new Color(255, 255, 255, 255));
            g.aimage(Text.renderstroked(getCooldownTime(timer), cleaveAdditionalFont).tex(), new Coord(topLeft.x + UI.scale(52), topLeft.y + UI.scale(2)), 1, 0.5);
        }
        g.chcolor(255, 255, 255, 255);
    }

    private int getOpeningValue(Buff buff) {
        Double meterDouble = buff.ameteri.get();
        if (meterDouble != null) {
            return (int) (100 * meterDouble);
        }
        return 0;
    }

    public String getCooldownTime(long time) {
        double convertedTime = time / 1000.0;
        return String.format("%.1f", convertedTime);
    }

    private void combatMedAlphaShift(){
        int fps = GLPanel.Loop.fps > 0 ? GLPanel.Loop.fps : 1;
        int alphaShiftSpeed = 2400/fps;
        if (combatMedAlphaShiftUp) {
            if (combatMedColorShift + alphaShiftSpeed <= 255) {
                combatMedColorShift += alphaShiftSpeed;
            } else {
                combatMedAlphaShiftUp = false;
                combatMedColorShift = 255;
            }
        } else {
            if (combatMedColorShift - alphaShiftSpeed >= 0){
                combatMedColorShift -= alphaShiftSpeed;
            } else {
                combatMedAlphaShiftUp = true;
                combatMedColorShift = 0;
            }
        }
    }


}
