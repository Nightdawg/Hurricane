package haven;

import java.awt.*;
import java.awt.image.BufferedImage;

public class GobHealthInfo extends GobInfo {
    private static final Text.Foundry gobhpf = new Text.Foundry(Text.sans.deriveFont(Font.BOLD), 12);

    private GobHealth health;

    public GobHealthInfo(Gob owner) {
        super(owner);
    }

    @Override
    protected boolean enabled() {
        return OptWnd.displayObjectDurabilityPercentageCheckBox.a;
    }

    @Override
    protected Tex render() {
        if(gob == null || gob.getres() == null) { return null;}

        BufferedImage health = health();

        if(health == null) {
            return null;
        }

        return new TexI(ItemInfo.catimgsh(3, 0, null, health));
    }

    @Override
    public void dispose() {
        health = null;
        super.dispose();
    }

    private BufferedImage health() {
        health = gob.getattr(GobHealth.class);
        if(health != null) {
            return text();
        }

        return null;
    }

    @Override
    public String toString() {
        Resource res = gob.getres();
        return String.format("GobInfo<%s>", res != null ? res.name : "<loading>");
    }

    public BufferedImage text() {
        if(health.hp < 1) {
            int c = 15 + (int) Math.floor(health.hp * 240);
            return renderLabel(String.format("%d%%", Math.round(100 * health.hp)), new Color(255, c, c));
        }
        return null;
    }

    static BufferedImage renderLabel(String label, Color color) {
        return Utils.outline2(Text.renderstroked(label, color, Color.BLACK, gobhpf).img, Color.BLACK, true);
    }
}
