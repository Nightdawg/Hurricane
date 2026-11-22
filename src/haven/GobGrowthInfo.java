package haven;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GobGrowthInfo extends GobInfo {
    public static final int TREE_START = 10;
	public static final int BUSH_START = 30;
	public static final double TREE_MULT = 100.0 / (100.0 - TREE_START);
	public static final double BUSH_MULT = 100.0 / (100.0 - BUSH_START);
	private static final Map<String, Tex> stageTextCache = new HashMap<>();
	public static final BufferedImage SEEDS_STAGE_DOT = drawDot(new Color(0, 102, 255,255));
	public static final BufferedImage FINAL_STAGE_DOT = drawDot(new Color(189, 0, 0,255));
	public static final Tex SEEDS_STAGE_DOT_TEX = new TexI(ItemInfo.catimgsh(3, 0, null, SEEDS_STAGE_DOT));
	public static final Tex FINAL_STAGE_DOT_TEX = new TexI(ItemInfo.catimgsh(3, 0, null, FINAL_STAGE_DOT));


	public static Tex getStageTex(int stage, int maxStage) {
		String key = String.valueOf(stage);
		if (!stageTextCache.containsKey(key)) {
			BufferedImage stageImage = renderStageText(key);
			stageTextCache.put(key, new TexI(ItemInfo.catimgsh(3, 0, null, stageImage)));
		}
		return stageTextCache.get(key);
	}



	private static BufferedImage renderStageText(String stage) {
		return Text.std.renderstroked(stage, new Color(255, 243, 180,255), Color.BLACK).img;
	}


    private final boolean crop;

    protected GobGrowthInfo(Gob owner, boolean crop) {
	super(owner);
	this.crop = crop;
    }

    @Override
	protected boolean enabled() {
		return !gob.isHidden && (crop ? OptWnd.displayCropGrowthInfoCheckBox.a : OptWnd.displayTreeBushGrowthInfoCheckBox.a);
	}

    @Override
    protected Tex render() {
	if(gob == null || gob.getres() == null) { return null;}

	Tex growth = growth();

	if(growth == null) {
	    return null;
	}

	return growth;
    }
    
    @Override
    public void dispose() {
	super.dispose();
    }

    private Tex growth() {
	Text.Line line = null;
	Resource res = gob.getres();
	if(crop && Utils.isSpriteKind(gob, "GrowingPlant", "TrellisPlant") && !(OptWnd.toggleGobHidingCheckBox.a && OptWnd.hideCropsCheckbox.a)) {
	    int maxStage = 0;
	    for (FastMesh.MeshRes layer : gob.getres().layers(FastMesh.MeshRes.class)) {
		if(layer.id / 10 > maxStage) {
		    maxStage = layer.id / 10;
		}
	    }
	    Message data = getDrawableData(gob);
	    if(data != null) {
		int stage = data.uint8();
		if(stage > maxStage) {stage = maxStage;}
		if(res != null && (res.name.contains("carrot"))) {
			if (stage == maxStage - 1) {
				return SEEDS_STAGE_DOT_TEX;
			} else if (stage == maxStage) {
				return FINAL_STAGE_DOT_TEX;
			} else {
				return getStageTex(stage, maxStage);
			}
		} else if (res != null && (res.name.contains("turnip") || res.name.contains("leek"))){
			if (stage == maxStage - 2) {
				return SEEDS_STAGE_DOT_TEX;
			} else if (stage == maxStage) {
				return FINAL_STAGE_DOT_TEX;
			} else {
				return getStageTex(stage, maxStage);
			}
		} else {
			if (stage == maxStage){
				return FINAL_STAGE_DOT_TEX;
			} else {
				return getStageTex(stage, maxStage);
			}

		}
		}
	} else if(!crop && Utils.isSpriteKind(gob, "Tree")) {
		boolean isHidden = true;
	    Message data = getDrawableData(gob);
	    if(data != null && !data.eom()) {
		data.skip(1);
		int growth = data.eom() ? -1 : data.uint8();
		if(growth >= 0) {
			if(res.name.contains("gfx/terobjs/trees") && !res.name.endsWith("log") && !res.name.endsWith("oldtrunk") && !(OptWnd.toggleGobHidingCheckBox.a && OptWnd.hideTreesCheckbox.a)) {
			growth = (int) (TREE_MULT * (growth - TREE_START));
			if (growth <= 100)
				isHidden = false;
			int oversizedTreesPercentage = OptWnd.oversizedTreesPercentageTextEntry.text().isEmpty() ? 1 : Integer.parseInt(OptWnd.oversizedTreesPercentageTextEntry.text());
			if (OptWnd.alsoShowOversizedTreesAbovePercentageCheckBox.a && growth >= oversizedTreesPercentage)
				isHidden = false;
		    } else if(res.name.startsWith("gfx/terobjs/bushes") && !(OptWnd.toggleGobHidingCheckBox.a && OptWnd.hideBushesCheckbox.a)) {
			growth = (int) (BUSH_MULT * (growth - BUSH_START));
			isHidden = false;
		    }
			if (!isHidden) {
				Color c = Utils.blendcol(growth / 100.0, Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN);
				line = Text.std.renderstroked(String.format("%d%%", growth), c, Color.BLACK);
			}
		}
	    }
	}

	if(line != null) {
	    return new TexI(ItemInfo.catimgsh(3, 0, null, line.img));
	}
	return null;
    }


	private static BufferedImage drawDot(Color c) {
		int diameter = 11;
		BufferedImage img = new BufferedImage(diameter, diameter * 2, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(c);

		g.fillOval(0, 1, diameter, diameter);

		g.setColor(Color.BLACK);
		g.setStroke(new BasicStroke(1));
		g.drawOval(0, 1, diameter, diameter);

		g.dispose();
		return img;
	}


	private static Message getDrawableData(Gob gob) {
	Drawable dr = gob.getattr(Drawable.class);
	ResDrawable d = (dr instanceof ResDrawable) ? (ResDrawable) dr : null;
	if(d != null)
	    return d.sdt.clone();
	else
	    return null;
    }


    @Override
    public String toString() {
	Resource res = gob.getres();
	return String.format("GobInfo<%s>", res != null ? res.name : "<loading>");
    }
}