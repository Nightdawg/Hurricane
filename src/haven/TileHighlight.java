package haven;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.List;
import java.util.*;

import static haven.MCache.cmaps;

public class TileHighlight {
    private static final Set<String> highlight = new HashSet<>();
    private static final Map<String, List<TileItem>> tiles = new HashMap<>();
    private static final List<String> categories = new ArrayList<>();
    public static final String TAG = "tileHighlight";
    private static boolean initialized = false;
    private static final String ALL = "All";
    
    private static String lastCategory = ALL;
    public static volatile long seq = 0;
    
    public static final Text.Foundry elf = CharWnd.attrf;
    public static final Color every = new Color(255, 255, 255, 16);
    public static final Color other = new Color(255, 255, 255, 32);
    public static final int elh = elf.height() + UI.scale(2);



	public static final Map<String, List<String>> tileHighlightNames = new HashMap<String, List<String>>(){{
		put("Ground Tiles", new ArrayList<String>(){{
			add("gfx/tiles/ashland");
            add("gfx/tiles/badlands");
			add("gfx/tiles/beach");
			add("gfx/tiles/beechgrove");
            add("gfx/tiles/blackwood");
			add("gfx/tiles/bluesod");
			add("gfx/tiles/boards");
			add("gfx/tiles/bog");
			add("gfx/tiles/bogwater");
			add("gfx/tiles/bountyacre");
			add("gfx/tiles/cave");
			add("gfx/tiles/cloudrange");
			add("gfx/tiles/deep");
			add("gfx/tiles/deeptangle");
			add("gfx/tiles/dirt");
			add("gfx/tiles/dryflat");
			add("gfx/tiles/dryweald");
			add("gfx/tiles/fen");
			add("gfx/tiles/fenwater");
			add("gfx/tiles/field");
			add("gfx/tiles/flowermeadow");
			add("gfx/tiles/gleamgrotto");
			add("gfx/tiles/grass");
			add("gfx/tiles/greenbrake");
			add("gfx/tiles/greensward");
            add("gfx/tiles/grove");
			add("gfx/tiles/hardsteppe");
			add("gfx/tiles/heath");
			add("gfx/tiles/highground");
			add("gfx/tiles/leaf");
			add("gfx/tiles/leafpatch");
			add("gfx/tiles/leafdetritus");
			add("gfx/tiles/lichenwold");
			add("gfx/tiles/lushcave");
			add("gfx/tiles/lushfield");
			add("gfx/tiles/mine");
			add("gfx/tiles/moor");
			add("gfx/tiles/mossbrush");
			add("gfx/tiles/mountain");
			add("gfx/tiles/mountainsnow");
			add("gfx/tiles/nil");
			add("gfx/tiles/oakwilds");
			add("gfx/tiles/odeep");
			add("gfx/tiles/odeeper");
			add("gfx/tiles/owater");
			add("gfx/tiles/oxpasture");
            add("gfx/tiles/peatmoss");
			add("gfx/tiles/pinebarren");
			add("gfx/tiles/redplain");
            add("gfx/tiles/riverbed");
			add("gfx/tiles/rockbeach");
			add("gfx/tiles/rootbosk");
			add("gfx/tiles/sandcliff");
			add("gfx/tiles/scrubveld");
			add("gfx/tiles/seabed");
            add("gfx/tiles/seabirdrookery");
			add("gfx/tiles/shadehollow");
			add("gfx/tiles/shadycopse");
			add("gfx/tiles/skargard");
			add("gfx/tiles/sombrebramble");
			add("gfx/tiles/sourtimber");
			add("gfx/tiles/spave");
			add("gfx/tiles/swamp");
			add("gfx/tiles/swampwater");
			add("gfx/tiles/timberland");
			add("gfx/tiles/thicket");
			add("gfx/tiles/wald");
			add("gfx/tiles/warmdepth");
			add("gfx/tiles/water");
			add("gfx/tiles/wildmoor");
			add("gfx/tiles/wildturf");
		}});
		put("Ores (Cave Walls)", new ArrayList<String>(){{
			add("gfx/tiles/rocks/argentite");
			add("gfx/tiles/rocks/blackcoal");
			add("gfx/tiles/rocks/cassiterite");
			add("gfx/tiles/rocks/chalcopyrite");
			add("gfx/tiles/rocks/cinnabar");
			add("gfx/tiles/rocks/cuprite");
			add("gfx/tiles/rocks/galena");
			add("gfx/tiles/rocks/hematite");
			add("gfx/tiles/rocks/hornsilver");
			add("gfx/tiles/rocks/ilmenite");
			add("gfx/tiles/rocks/leadglance");
			add("gfx/tiles/rocks/limonite");
			add("gfx/tiles/rocks/magnetite");
			add("gfx/tiles/rocks/malachite");
			add("gfx/tiles/rocks/nagyagite");
			add("gfx/tiles/rocks/petzite");
			add("gfx/tiles/rocks/peacockore");
			add("gfx/tiles/rocks/sylvanite");
		}});
		put("Rocks (Cave Walls)", new ArrayList<String>(){{
			add("gfx/tiles/rocks/alabaster");
			add("gfx/tiles/rocks/apatite");
			add("gfx/tiles/rocks/arkose");
			add("gfx/tiles/rocks/basalt");
			add("gfx/tiles/rocks/breccia");
			add("gfx/tiles/rocks/chert");
			add("gfx/tiles/rocks/corund");
			add("gfx/tiles/rocks/diabase");
			add("gfx/tiles/rocks/diorite");
			add("gfx/tiles/rocks/dolomite");
			add("gfx/tiles/rocks/eclogite");
			add("gfx/tiles/rocks/feldspar");
			add("gfx/tiles/rocks/flint");
			add("gfx/tiles/rocks/fluorospar");
			add("gfx/tiles/rocks/gabbro");
			add("gfx/tiles/rocks/gneiss");
			add("gfx/tiles/rocks/granite");
			add("gfx/tiles/rocks/graywacke");
			add("gfx/tiles/rocks/greenschist");
			add("gfx/tiles/rocks/hornblende");
			add("gfx/tiles/rocks/jasper");
			add("gfx/tiles/rocks/kyanite");
			add("gfx/tiles/rocks/limestone");
			add("gfx/tiles/rocks/marble");
			add("gfx/tiles/rocks/mica");
			add("gfx/tiles/rocks/microlite");
			add("gfx/tiles/rocks/olivine");
			add("gfx/tiles/rocks/orthoclase");
			add("gfx/tiles/rocks/pegmatite");
			add("gfx/tiles/rocks/porphyry");
			add("gfx/tiles/rocks/pumice");
			add("gfx/tiles/rocks/quartz");
			add("gfx/tiles/rocks/rhyolite");
			add("gfx/tiles/rocks/sandstone");
			add("gfx/tiles/rocks/schist");
			add("gfx/tiles/rocks/serpentine");
			add("gfx/tiles/rocks/slate");
			add("gfx/tiles/rocks/soapstone");
			add("gfx/tiles/rocks/sodalite");
			add("gfx/tiles/rocks/sunstone");
			add("gfx/tiles/rocks/zincspar");
			add("gfx/tiles/rocks/halite"); // rock salt
		}});
	}};
	public static List<String> savedHighlightedMapTiles = new ArrayList<String>(Arrays.asList(Utils.getprefsa("savedHighlightedMapTiles_2", new String[0])));
    
    public static boolean isHighlighted(String name) {
	synchronized (highlight) {
	    return highlight.contains(name);
	}
    }
    
    public static void toggle(String name) {
	synchronized (highlight) {
	    if(highlight.contains(name)) {
		unhighlight(name);
		savedHighlightedMapTiles.remove(name);
	    } else {
		highlight(name);
		savedHighlightedMapTiles.add(name);
	    }
	}
		Utils.setprefsa("savedHighlightedMapTiles_2", savedHighlightedMapTiles.toArray(new String[0]));
    }
    
    public static void highlight(String name) {
	synchronized (highlight) {
	    if(highlight.add(name)) {
		seq++;
	    }
	}
    }
    
    public static void unhighlight(String name) {
	synchronized (highlight) {
	    if(highlight.remove(name)) {
		seq++;
	    }
	}
    }
    
    public static BufferedImage olrender(MapFile.DataGrid grid) {
	TileHighlightOverlay ol = new TileHighlightOverlay(grid);
	WritableRaster buf = PUtils.imgraster(cmaps);
	Color col = ol.color();
	if(col != null) {
	    Coord c = new Coord();
	    for (c.y = 0; c.y < cmaps.y; c.y++) {
		for (c.x = 0; c.x < cmaps.x; c.x++) {
		    if(ol.get(c)) {
			buf.setSample(c.x, c.y, 0, ((col.getRed() * col.getAlpha()) + (buf.getSample(c.x, c.y, 1) * (255 - col.getAlpha()))) / 255);
			buf.setSample(c.x, c.y, 1, ((col.getGreen() * col.getAlpha()) + (buf.getSample(c.x, c.y, 1) * (255 - col.getAlpha()))) / 255);
			buf.setSample(c.x, c.y, 2, ((col.getBlue() * col.getAlpha()) + (buf.getSample(c.x, c.y, 2) * (255 - col.getAlpha()))) / 255);
			buf.setSample(c.x, c.y, 3, Math.max(buf.getSample(c.x, c.y, 3), col.getAlpha()));
		    }
		}
	    }
	}
	return (PUtils.rasterimg(buf));
    }
    
    public static void toggle(GameUI gui){
	tryInit(gui);
	if(gui.tileHighlight == null) {
	    gui.tileHighlight = gui.add(new TileHighlightCFG(), Utils.getprefc("tileHighlightWnd", gui.mapfile.c));
	} else {
	    gui.tileHighlight.show(!gui.tileHighlight.visible());
		if (gui.tileHighlight.visible()) {
			gui.tileHighlight.raisedAndFocused = false;
		}
	}
    }
    
    private static void tryInit(GameUI gui) {
	if(initialized) {return;}
	categories.add(ALL);
	ArrayList<TileItem> all = new ArrayList<>();
	tiles.put(ALL, all);
		for (Map.Entry<String, List<String>> entry : tileHighlightNames.entrySet()) {
	    String category = entry.getKey();
	    categories.add(category);
	    List<String> tiles = entry.getValue();
	    List<TileItem> items = new ArrayList<>(tiles.size());
	    for (String tile : tiles) {
		items.add(new TileItem(tile));
	    }
	    items.sort(Comparator.comparing(item -> item.name));
	    TileHighlight.tiles.put(category, items);
	    all.addAll(items);
	}
	all.sort(Comparator.comparing(item -> item.name));
	gui.mapfile.toggleol(TileHighlight.TAG, true);
	initialized = true;
    }
    
    private static class TileItem {
	private final String name, res;
	private final Tex tex;
	
	private TileItem(String res) {
	    this.res = res;
		String key = Utils.prettyResName(res).toLowerCase();
		if (Config.ORE_FULL_NAMES.containsKey(key)) {
			this.name = Config.ORE_FULL_NAMES.get(key);
		} else if (Config.STONE_FULL_NAMES.containsKey(key)) {
			this.name = Config.STONE_FULL_NAMES.get(key);
		} else {
			this.name = Utils.prettyResName(res);
		}
	    this.tex = elf.renderstroked(this.name, Color.WHITE, Color.BLACK).tex();
	}
    }
    
    public static class TileHighlightCFG extends Window {
	public static final String FILTER_DEFAULT = "Start typing to filter";

	private final TileList list;
	private final Label filter;
	private String category = lastCategory;

	public boolean raisedAndFocused = false;

	private final CheckBox toggleAll;
	
	public TileHighlightCFG() {
	    super(Coord.z, "Tile Highlight");
	    
	    int h = add(new Label("Categories: "), Coord.z).sz.y;
	    add(toggleAll = new CheckBox("Select All") {
		@Override
		public void changed(boolean val) {
		    list.filtered.forEach(item -> {
				synchronized (highlight) {
					if (val) {
						highlight(item.res);
						savedHighlightedMapTiles.add(item.res);
					} else {
						unhighlight(item.res);
						savedHighlightedMapTiles.remove(item.res);
					}
				}
		    });
			Utils.setprefsa("savedHighlightedMapTiles_2", savedHighlightedMapTiles.toArray(new String[0]));
		}
	    }, UI.scale(135, 0));
	    h += UI.scale(5);
	    
	    add(new CategoryList(UI.scale(125), 4, elh), 0, h).sel = category;
	    
	    list = add(new TileList(UI.scale(220), UI.unscale(12)), UI.scale(135), h);
	    filter = adda(new Label(FILTER_DEFAULT), list.pos("ur").y(0), 1, 0);
	    pack();
	    setfocus(list);
	    list.setItems(tiles.get(category));

		list.filtered.forEach(item -> {
			if(savedHighlightedMapTiles != null && savedHighlightedMapTiles.size() > 0 && savedHighlightedMapTiles.contains(item.res)) {
				highlight(item.res);
			} else {
				unhighlight(item.res);
			}
		});
		updateAllCheckbox();
	}
	
	private void updateFilter(String text) {
	    filter.settext((text == null || text.isEmpty()) ? FILTER_DEFAULT : text);
	    filter.c = list.pos("ur").y(0).adds(-filter.sz.x, 0);
	}
	
	@Override
	public void tick(double dt) {
	    super.tick(dt);
		if (!raisedAndFocused) {
			parent.setfocus(this);
			raise();
			raisedAndFocused = true;
		}
	}
	
	@Override
	public void destroy() {
	    super.destroy();
	    ui.gui.tileHighlight = null;
	}

	private class TileList extends FilteredListBox<TileItem> {
	    private Coord showc;
	    
	    public TileList(int w, int h) {
		super(w, h, elh);
		this.showc = showc();
		showFilterText = false;
	    }
	    
	    private Coord showc() {
		return (new Coord(sz.x - (sb.vis() ? sb.sz.x : 0) - ((elh - CheckBox.sbox.sz().y) / 2) - CheckBox.sbox.sz().x,
		    ((elh - CheckBox.sbox.sz().y) / 2)));
	    }
	    
	    public void draw(GOut g) {
		this.showc = showc();
		super.draw(g);
	    }
	    
	    @Override
	    protected void filter() {
		super.filter();
		updateFilter(this.filter.line());
	    }
	    
	    @Override
	    protected boolean match(TileItem item, String filter) {
		if(filter.isEmpty()) {
		    return true;
		}
		if(item.name == null)
		    return (false);
		return (item.name.toLowerCase().contains(filter.toLowerCase()));
	    }
	    
	    public boolean keydown(KeyDownEvent ev) {
		if(ev.awt.getKeyCode() == java.awt.event.KeyEvent.VK_SPACE) {
		    if(sel != null) {
			toggle(sel.res);
			Utils.setprefsa("savedHighlightedMapTiles_2", savedHighlightedMapTiles.toArray(new String[0]));
			updateAllCheckbox();
		    }
		    return (true);
		}
		return (super.keydown(ev));
	    }
	    
	    @Override
	    protected void drawitem(GOut g, TileItem item, int idx) {
		g.chcolor(((idx % 2) == 0) ? every : other);
		g.frect(Coord.z, g.sz());
		g.chcolor();
		g.aimage(item.tex, new Coord(UI.scale(3), elh / 2), 0.0, 0.5);
		g.image(CheckBox.sbox, showc);
		if(isHighlighted(item.res))
		    g.image(CheckBox.smark, showc);
	    }
	    
	    @Override
	    public boolean mousedown(MouseDownEvent ev) {
		int idx = idxat(ev.c);
		if((idx >= 0) && (idx < listitems())) {
		    Coord ic = ev.c.sub(idxc(idx));
		    TileItem item = listitem(idx);
		    if(ic.x < showc.x + CheckBox.sbox.sz().x) {
			toggle(item.res);
			Utils.setprefsa("savedHighlightedMapTiles_2", savedHighlightedMapTiles.toArray(new String[0]));
			updateAllCheckbox();
			return (true);
		    }
		}
		return (super.mousedown(ev));
	    }
	}
	
	private class CategoryList extends OldListBox<String> {
	    public CategoryList(int w, int h, int itemh) {
		super(w, h, itemh);
	    }
	    
	    @Override
	    public void change(String item) {
		super.change(item);
		list.setItems(tiles.getOrDefault(item, Collections.emptyList()));
		list.sb.val = 0;
		category = lastCategory = item;
		updateAllCheckbox();
	    }
	    
	    @Override
	    protected String listitem(int i) {
		return categories.get(i);
	    }
	    
	    @Override
	    protected int listitems() {
		return categories.size();
	    }
	    
	    @Override
	    protected void drawitem(GOut g, String item, int i) {
		g.chcolor(((i % 2) == 0) ? every : other);
		g.frect(Coord.z, g.sz());
		g.chcolor();
		g.aimage(Text.renderstroked(item).tex(), new Coord(UI.scale(3), elh / 2), 0, 0.5);
	    }
	}
		@Override
		public void wdgmsg(Widget sender, String msg, Object... args) {
			if((sender == this) && (msg == "close")) {
				hide();
			} else {
				super.wdgmsg(sender, msg, args);
			}
		}

		private void updateAllCheckbox() {
			if(toggleAll == null) {
				return;
			}
			List<TileItem> items = list != null ? list.filtered : null;
			toggleAll.a = items != null
					&& !items.isEmpty()
					&& items.stream().allMatch(icon -> isHighlighted(icon.res));
		}
		@Override
		public boolean mouseup(MouseUpEvent ev) {
			if(dm != null) {
				dm.remove();
				dm = null;
				preventDraggingOutside();
				Utils.setprefc("tileHighlightWnd", ui.gui.tileHighlight.c);
			} else {
				super.mouseup(ev);
			}
			return(true);
		}
    }
    
    public static class TileHighlightOverlay {
	private final boolean[] ol;
	
	public TileHighlightOverlay(MapFile.DataGrid g) {
	    this.ol = new boolean[cmaps.x * cmaps.y];
	    fill(g);
	}
	
	private void fill(MapFile.DataGrid grid) {
	    if(grid == null) {return;}
	    Coord c = new Coord(0, 0);
	    for (c.x = 0; c.x < cmaps.x; c.x++) {
		for (c.y = 0; c.y < cmaps.y; c.y++) {
		    int tile = grid.gettile(c);
		    MapFile.TileInfo tileset = grid.tilesets[tile];
		    boolean v = isHighlighted(tileset.res.name);
		    set(c, v);
		    if(v) { setn(c, true); } //make 1 tile border around actual tiles
		}
	    }
	}
	
	public boolean get(Coord c) {
	    return (ol[c.x + (c.y * cmaps.x)]);
	}
	
	public void set(Coord c, boolean v) {
	    ol[c.x + (c.y * cmaps.x)] = v;
	}
	
	public void set(int x, int y, boolean v) {
	    if(x >= 0 && y >= 0 && x < cmaps.x && y < cmaps.y) {
		ol[x + (y * cmaps.x)] = v;
	    }
	}
	
	public void setn(Coord c, boolean v) {
	    set(c.x - 1, c.y - 1, v);
	    set(c.x - 1, c.y + 1, v);
	    set(c.x + 1, c.y - 1, v);
	    set(c.x + 1, c.y + 1, v);
	    set(c.x, c.y - 1, v);
	    set(c.x, c.y + 1, v);
	    set(c.x - 1, c.y, v);
	    set(c.x + 1, c.y, v);
	}
	
	public Color color() {
	    return Color.GREEN;
	}
    }
    
}
