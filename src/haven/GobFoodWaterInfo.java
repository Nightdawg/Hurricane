package haven;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GobFoodWaterInfo extends GobInfo {

    private static final BufferedImage lowFoodImage = PUtils.convolvedown(PUtils.rasterimg(PUtils.blurmask2(Resource.local().loadwait("customclient/lowFood").layer(Resource.imgc).img.getRaster(), 4, 1, Color.BLACK)), UI.scale(34, 34), CharWnd.iconfilter);
    private static final BufferedImage lowWaterImage = PUtils.convolvedown(PUtils.rasterimg(PUtils.blurmask2(Resource.local().loadwait("customclient/lowWater").layer(Resource.imgc).img.getRaster(), 4, 1, Color.BLACK)), UI.scale(34, 34), CharWnd.iconfilter);
	private static final Map<String, Tex> contentTexCache = new HashMap<>();

    protected GobFoodWaterInfo(Gob owner) {
	super(owner);
    }

    @Override
	protected boolean enabled() {
		return OptWnd.showLowFoodWaterIconsCheckBox.a && !gob.isHidden;
	}

	@Override
	protected Tex render() {
	up(6);
	if(gob == null || gob.getres() == null) { return null;}
		if (icons() != null)
			return icons();
		return null;
	}

	@Override
    public void dispose() {
	super.dispose();
    }

	private Tex icons() {
		Drawable dr = gob.getattr(Drawable.class);
		ResDrawable d = (dr instanceof ResDrawable) ? (ResDrawable) dr : null;
		String resName = gob.getres().name;
		if (d != null) {
			int rbuf = d.sdt.checkrbuf(0);
			String key = null;
			if (resName.equals("gfx/terobjs/chickencoop")) {
                boolean hasWater = (rbuf & 1) != 0;
                boolean hasFood  = (rbuf & 2) != 0;
				if (!hasFood && !hasWater) {
					key = "both";
				} else if (!hasFood) {
					key = "food";
				} else if (!hasWater) {
					key = "water";
				} else {
					return null;
				}
			} else if (resName.equals("gfx/terobjs/rabbithutch")) {
                boolean hasWater = (rbuf & 4) != 0;
                boolean hasFood  = (rbuf & 16) != 0;
				if (!hasWater && !hasFood) {
					key = "both";
				} else if (!hasFood) {
					key = "food";
				} else if (!hasWater) {
					key = "water";
				} else {
					return null;
				}
			} else {
				return null;
			}
			// Check cache first
			Tex cachedTex = contentTexCache.get(key);
			if (cachedTex != null) {
				return cachedTex;
			}
			// Build parts
			BufferedImage[] parts = null;
			switch (key) {
				case "both":
					parts = new BufferedImage[]{lowFoodImage, lowWaterImage};
					break;
				case "food":
					parts = new BufferedImage[]{lowFoodImage};
					break;
				case "water":
					parts = new BufferedImage[]{lowWaterImage};
					break;
			}
			// Validate parts
			for (BufferedImage part : parts) {
				if (part == null) return null;
			}
			Tex contentTex = new TexI(ItemInfo.catimgs(1, parts));
			contentTexCache.put(key, contentTex);
			return contentTex;
		}
		return null;
	}

}