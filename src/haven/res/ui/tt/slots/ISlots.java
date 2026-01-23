/* Preprocessed source code */
package haven.res.ui.tt.slots;

import haven.*;
import static haven.PUtils.*;
import java.awt.image.*;
import java.awt.Graphics;
import java.awt.Font;
import java.awt.Color;
import java.util.*;
import haven.res.ui.tt.attrmod.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/* >tt: Fac */
@haven.FromResource(name = "ui/tt/slots", version = 33)
public class ISlots extends ItemInfo.Tip implements GItem.NumberInfo {
    public static final Text ch = Text.render("Gilding list:");
    public static final Text.Foundry progf = new Text.Foundry(Text.dfont.deriveFont(Font.ITALIC), 10, new Color(0, 169, 224));
    public final Collection<SItem> s = new ArrayList<SItem>();
    public final int left;
    public final double pmin, pmax;
    public final Resource[] attrs;
    public final boolean ignol;
    private UI ui = null;
    static final Pattern integerStatPattern = Pattern.compile("\\{([+-]?\\d+)\\}");
    static final Pattern percentageStatPattern = Pattern.compile("\\{([+-]?\\d*(\\.\\d+)?|\\d+)%\\}");

    public ISlots(Owner owner, int left, double pmin, double pmax, Resource[] attrs) {
	super(owner);
	this.left = left;
	this.pmin = pmin;
	this.pmax = pmax;
	this.attrs = attrs;
	// XXX? Should the format be changed instead?
	ignol = owner.fcontext(MenuGrid.class, false) != null;
    if (owner instanceof GItem)
        this.ui = ((GItem) owner).ui;
    }

    public static final String chc = "192,192,255";
    public void layout(Layout l) {
    boolean extendedView = ui == null || (ui != null && ui.modshift); // ND: There's a weird bug with barterstands. The UI from the Shopbox class doesn't detect ui.modshift or some crap.
	l.cmp.add(ch.img, new Coord(UI.scale(2), l.cmp.sz.y + UI.scale(4)));
	if(attrs.length > 0) {
	    BufferedImage head = RichText.render(String.format("Chance: $col[%s]{%d%%} to $col[%s]{%d%%}", chc, Math.round(100 * pmin), chc, Math.round(100 * pmax)), 0).img;
	    int h = head.getHeight();
	    int x = 10, y = l.cmp.sz.y;
	    l.cmp.add(head, new Coord(x, y));
	    x += head.getWidth() + 10;
	    for(int i = 0; i < attrs.length; i++) {
		BufferedImage icon = convolvedown(attrs[i].layer(Resource.imgc).img, new Coord(h, h), CharWnd.iconfilter);
		l.cmp.add(icon, new Coord(x, y));
		x += icon.getWidth() + 2;
	    }
	} else {
	    BufferedImage head = RichText.render(String.format("Chance: $col[%s]{%d%%}", chc, (int)Math.round(100 * pmin)), 0).img;
	    l.cmp.add(head, new Coord(10, l.cmp.sz.y));
	}
	Map<Entry, String> totalAttr = new HashMap<>();
	for(SItem si : s) {
		if (extendedView)
			si.layout(l);
		for (ItemInfo ii : si.info) {
			if (ii instanceof AttrMod) {
				AttrMod attrMod = (AttrMod) ii;
				for (Entry attrmodEntry : attrMod.tab) {
					boolean exist = false;
					for (Map.Entry<Entry, String> entry : totalAttr.entrySet()) {
						if (entry.getKey().attr.name().equals(attrmodEntry.attr.name())) {
							Matcher integerMatcher1 = integerStatPattern.matcher(attrmodEntry.fmtvalue());
							Matcher integerMatcher2 = integerStatPattern.matcher(entry.getValue());
							Matcher percentageMatcher1 = percentageStatPattern.matcher(attrmodEntry.fmtvalue());
							Matcher percentageMatcher2 = percentageStatPattern.matcher(entry.getValue());
							if (integerMatcher1.find() && integerMatcher2.find()) {
								int sum = Integer.parseInt(integerMatcher2.group(1)) + Integer.parseInt(integerMatcher1.group(1));
								entry.setValue(String.format("%s{%s%d}", RichText.Parser.col2a((sum < 0) ? haven.res.ui.tt.attrmod.Attribute.debuff : haven.res.ui.tt.attrmod.Attribute.buff), sum < 0 ? "-" : "+", Math.abs(sum)));
								exist = true;
								break;
							}
							if (percentageMatcher1.find() && percentageMatcher2.find()) {
								double sum = Double.parseDouble(percentageMatcher1.group(1)) + Double.parseDouble(percentageMatcher2.group(1));
								entry.setValue(String.format("%s{%s%s%%}",
										RichText.Parser.col2a((sum < 0) ? haven.res.ui.tt.attrmod.Attribute.debuff : haven.res.ui.tt.attrmod.Attribute.buff),
										(sum < 0) ? "-" : "+", Utils.odformat2(sum, 1)));
								exist = true;
								break;
							}
						}
					}
					if (!exist) totalAttr.put(attrmodEntry, attrmodEntry.fmtvalue());
				}
			}
		}
	}
	if (!extendedView) {
		if (totalAttr.size() > 0) {
			List<Entry> lmods = new ArrayList<>();
			List<Map.Entry<Entry, String>> sortAttr = totalAttr.entrySet().stream().sorted(this::BY_PRIORITY).collect(Collectors.toList());
			for (Map.Entry<Entry, String> entry : sortAttr) {
				lmods.add(new StringEntry(entry.getKey().attr, entry.getValue()));
			}
			Entry[] tab = lmods.toArray(new Entry[0]);
			BufferedImage[] icons = new BufferedImage[tab.length];
			BufferedImage[] names = new BufferedImage[tab.length];
			BufferedImage[] values = new BufferedImage[tab.length];
			int w = 0;
			for(int i = 0; i < tab.length; i++) {
				Entry row = tab[i];
				names[i] = Text.render(row.attr.name()).img;
				icons[i] = row.attr.icon();
				if(icons[i] != null)
					icons[i] = convolvedown(icons[i], Coord.of(names[i].getHeight()), CharWnd.iconfilter);
				values[i] = RichText.render(row.fmtvalue(), 0).img;
				w = Math.max(w, names[i].getWidth());
			}
			for(int i = 0; i < tab.length; i++) {
				int y = l.cmp.sz.y;
				if(icons[i] != null)
					l.cmp.add(icons[i], Coord.of(0, y));
				int nx = names[i].getHeight() + (int)UI.scale(0.75);
				l.cmp.add(names[i], Coord.of(nx, y));
				l.cmp.add(values[i], Coord.of(nx + w + UI.scale(5), y));
			}
		}
	}
	if(left > 0)
	    l.cmp.add(progf.render((left > 1)?String.format("Gildable \u00d7%d", left):"Gildable").img, new Coord(10, l.cmp.sz.y));
    if (ui != null)
		l.cmp.add(RichText.render(extendedView ? "$col[218,163,0]{<Showing expanded Gilding List>}" : "$col[185,185,185]{<Hold Shift to expand Gilding List>}", 0).img, new Coord(0, l.cmp.sz.y));
    }

    public static final Object[] defn = {Loading.waitfor(Resource.classres(ISlots.class).pool.load("ui/tt/defn", 7))};
    public class SItem {
	public final Resource res;
	public final GSprite spr;
	public final List<ItemInfo> info;
	public final String name;

	public SItem(ResData sdt, Object[] raw) {
	    this.res = sdt.res.get();
	    ItemSpec spec1 = new ItemSpec(owner, sdt, Utils.extend(new Object[] {defn}, raw));
	    this.spr = spec1.spr();
	    this.name = spec1.name();
	    ItemSpec spec2 = new ItemSpec(owner, sdt, raw);
	    this.info = spec2.info();
	}

	private BufferedImage img() {
	    if(spr instanceof GSprite.ImageSprite)
		return(((GSprite.ImageSprite)spr).image());
	    return(res.layer(Resource.imgc).img);
	}

	public void layout(Layout l) {
	    BufferedImage icon = PUtils.convolvedown(img(), new Coord(16, 16), CharWnd.iconfilter);
	    BufferedImage lbl = Text.render(name).img;
	    BufferedImage sub = longtip(info);
	    int x = 10, y = l.cmp.sz.y;
	    l.cmp.add(icon, new Coord(x, y));
	    l.cmp.add(lbl, new Coord(x + 16 + 3, y + ((16 - lbl.getHeight()) / 2)));
	    if(sub != null)
		l.cmp.add(sub, new Coord(x + 16, y + 16));
	}
    }

    public int order() {
	return(200);
    }

    public int itemnum() {
	return(s.size());
    }

    public static final Color avail = new Color(0, 169, 224);
    public Color numcolor() {
	return((left > 0) ? avail : Color.WHITE);
    }

    public void drawoverlay(GOut g, Tex tex) {
	if(!ignol)
	    GItem.NumberInfo.super.drawoverlay(g, tex);
    }

    	public static BufferedImage longtip(List<ItemInfo> info) { // ND: Added this here to overwrite method from ItemInfo and prevent an extra text stroke on contents tooltip
		if(info.isEmpty())
			return(null);
		Layout l = new Layout(info.get(0).owner);
		for(ItemInfo ii : info) {
			if(ii instanceof Tip) {
				Tip tip = (Tip)ii;
				l.add(tip);
			}
		}
		if(l.tips.size() < 1)
			return(null);
		return(l.render());
	}

	private int BY_PRIORITY(Map.Entry<Entry, String> o1, Map.Entry<Entry, String> o2) {
		String a1 =  o1.getKey().attr.name();
		String a2 =  o2.getKey().attr.name();
		return Integer.compare(Config.statsAndAttributesOrder.indexOf(a2), Config.statsAndAttributesOrder.indexOf(a1));
	}

}
