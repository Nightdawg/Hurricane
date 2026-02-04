package haven.sprites;

import haven.*;
import haven.render.Homo3D;
import haven.render.Pipe;

import java.awt.*;
import java.awt.image.BufferedImage;

public class LeaderPingArrowSprite extends Sprite implements PView.Render2D {
    private static final int SIZE = 48;
    private static final float HEIGHT_OFFSET = 7f;
    private static final Color TARGET_COLOR = new Color(255, 0, 0); // Red
    private static final Color OUTLINE_COLOR = new Color(255, 255, 255); // White outline
    private static final int CIRCLE_THICKNESS = 3;
    private static final int CROSSHAIR_THICKNESS = 3;

    public Coord3f pos = new Coord3f(0, 0, HEIGHT_OFFSET);
    private static Tex cachedFrameTex = null;

    public LeaderPingArrowSprite(Owner owner) {
        super(owner, null);
        if (cachedFrameTex == null) {
            cachedFrameTex = createFrameTexture();
        }
    }

    private static Tex createFrameTexture() {
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int centerX = SIZE / 2;
        int centerY = SIZE / 2;
        int outerRadius = SIZE / 2 - 4;
        int innerRadius = SIZE / 4;
        int crosshairLength = SIZE / 2 - 8;
        int crosshairGap = SIZE / 6;

        g2d.setColor(OUTLINE_COLOR);
        g2d.setStroke(new BasicStroke(CIRCLE_THICKNESS + 2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawOval(centerX - outerRadius, centerY - outerRadius, outerRadius * 2, outerRadius * 2);
        g2d.drawOval(centerX - innerRadius, centerY - innerRadius, innerRadius * 2, innerRadius * 2);

        g2d.setColor(TARGET_COLOR);
        g2d.setStroke(new BasicStroke(CIRCLE_THICKNESS, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawOval(centerX - outerRadius, centerY - outerRadius, outerRadius * 2, outerRadius * 2);
        g2d.drawOval(centerX - innerRadius, centerY - innerRadius, innerRadius * 2, innerRadius * 2);

        g2d.setColor(OUTLINE_COLOR);
        g2d.setStroke(new BasicStroke(CROSSHAIR_THICKNESS + 2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(centerX - crosshairLength, centerY, centerX - crosshairGap, centerY);
        g2d.drawLine(centerX + crosshairGap, centerY, centerX + crosshairLength, centerY);
        g2d.drawLine(centerX, centerY - crosshairLength, centerX, centerY - crosshairGap);
        g2d.drawLine(centerX, centerY + crosshairGap, centerX, centerY + crosshairLength);

        g2d.setColor(TARGET_COLOR);
        g2d.setStroke(new BasicStroke(CROSSHAIR_THICKNESS, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(centerX - crosshairLength, centerY, centerX - crosshairGap, centerY);
        g2d.drawLine(centerX + crosshairGap, centerY, centerX + crosshairLength, centerY);
        g2d.drawLine(centerX, centerY - crosshairLength, centerX, centerY - crosshairGap);
        g2d.drawLine(centerX, centerY + crosshairGap, centerX, centerY + crosshairLength);

        g2d.dispose();
        return new TexI(img);
    }

    @Override
    public void draw(GOut g, Pipe state) {
        Coord3f fsc = Homo3D.obj2view(pos, state, Area.sized(Coord.z, g.sz()));
        Coord sc = fsc.round2();
        if (sc == null)
            return;

        float distanceScale = Math.max(0.01f, Math.min(1.0f, 1.0f / (float)Math.pow(fsc.z * 0.8f, 0.7)));
        int scaledW = (int)(cachedFrameTex.sz().x * 1.0 * distanceScale);
        int scaledH = (int)(cachedFrameTex.sz().y * 1.0 * distanceScale);
        Coord sz = new Coord(scaledW, scaledH);
        g.aimage(cachedFrameTex, sc, 0.5, 0.5, sz);
    }
}
