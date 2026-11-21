package haven;

import java.awt.Color;

public class GobFoodTroughInfo extends GobInfo {
	static final String RES_NAME = "gfx/terobjs/trough";
	private static final Color LOW_SWILL_COLOR = new Color(255, 85, 85);
	private static final Color PARTIAL_SWILL_COLOR = new Color(255, 170, 170);

	protected GobFoodTroughInfo(Gob owner) {
		super(owner);
		up(6);
	}

	@Override
	protected boolean enabled() {
		CheckBox setting = OptWnd.showFoodTroughFillLevelTextCheckBox;
		return setting != null && setting.a && !gob.isHidden;
	}

	@Override
	protected Tex render() {
		if (gob.getattr(Drawable.class) instanceof ResDrawable drawable && drawable.getres().name.equals(RES_NAME)) {
			return switch (drawable.sdt.checkrbuf(0)) {
				case 0 -> swillLabel("Swill 0%", Color.RED);
				case 1 -> swillLabel("Swill <50%", LOW_SWILL_COLOR);
				case 3 -> swillLabel("Swill <100%", PARTIAL_SWILL_COLOR);
				case 7 -> swillLabel("Swill 100%", Color.WHITE);
				default -> null;
			};
		}
		return null;
	}

	private static Tex swillLabel(String label, Color color) {
		return new TexI(GobHealthInfo.renderLabel(label, color));
	}
}
