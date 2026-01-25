/* Preprocessed source code */
package haven.res.ui.barterbox;

import haven.*;
import haven.res.ui.tt.q.qbuff.QBuff;

import static haven.Inventory.invsq;
import static haven.Inventory.sqsz;
import java.awt.image.BufferedImage;
import java.util.*;
import java.awt.Color;

/* >wdg: haven.res.ui.barterbox.Shopbox */
@haven.FromResource(name = "ui/barterbox", version = 76)
public class Shopbox extends Widget implements ItemInfo.SpriteOwner, GSprite.Owner {
    public static final Text qlbl = Text.render("Quality:");
    public static final Text any = Text.render("Any");
    public static final Tex bg = Resource.classres(Shopbox.class).layer(Resource.imgc, 0).tex();
    public static final Coord itemc = UI.scale(5, 5),
	buyc = UI.scale(5, 66),
	pricec = UI.scale(200, 5),
	qualc = UI.scale(220 + 40, 5).add(invsq.sz()),
	cbtnc = UI.scale(220, 66),
	spipec = UI.scale(85, 40),
	bulkTextEntryc = UI.scale(87, 68),
	bpipec = UI.scale(300, 66);
    public ResData res;
    public ItemSpec price;
    public Text num;
    public int pnum, pq;
    private Text pnumt, pqt;
    private GSprite spr;
    private Object[] info = {};
    private Button spipe, bpipe, bbtn, cbtn;
    private TextEntry pnume, pqe, bulkTextEntry;
    public final boolean admin;
	private Text quality;

    public static Widget mkwidget(UI ui, Object... args) {
	boolean adm = (Integer)args[0] != 0;
	return(new Shopbox(adm));
    }

    public Shopbox(boolean admin) {
	super(bg.sz());
	if(this.admin = admin) {
	    spipe = add(new Button(UI.scale(75), "Connect"), spipec);
	    bpipe = add(new Button(UI.scale(75), "Connect"), bpipec);
	    cbtn = add(new Button(UI.scale(75), "Change"), cbtnc);
	    pnume = adda(new TextEntry(UI.scale(50), ""), pricec.add(invsq.sz()).add(UI.scale(5, 0)), 0.0, 1.0);
	    pnume.canactivate = true; pnume.dshow = true;
	    adda(new Label("Quality:"), qualc.add(0, 0), 0.0, 1.0);
	    pqe = adda(new TextEntry(UI.scale(40), ""), qualc.add(UI.scale(40, 0)), 0.0, 1.0);
	    pqe.canactivate = true; pqe.dshow = true;
	}
    }

    public abstract class AttrCache<T> {
	private List<ItemInfo> forinfo = null;
	private T save = null;

	public T get() {
	    try {
		List<ItemInfo> info = info();
		if(info != forinfo) {
		    save = find(info);
		    forinfo = info;
		}
	    } catch(Loading e) {
		return(null);
	    }
	    return(save);
	}

	protected abstract T find(List<ItemInfo> info);
    }

    public final AttrCache<Tex> itemnum = new AttrCache<Tex>() {
	protected Tex find(List<ItemInfo> info) {
	    GItem.NumberInfo ninf = ItemInfo.find(GItem.NumberInfo.class, info);
	    if(ninf == null) return(null);
	    return(PUtils.strokeTex(WItem.quantityFoundry.renderstroked2(Integer.toString(ninf.itemnum()), Color.WHITE, Color.BLACK)));
	}
    };

    public void draw(GOut g) {
	g.image(bg, Coord.z);
	sprite: {
	    ResData res = this.res;
	    if(res != null) {
		GOut sg = g.reclip(itemc, invsq.sz());
		sg.image(invsq, Coord.z);
		GSprite spr = this.spr;
		if(spr == null) {
		    try {
			spr = this.spr = GSprite.create(this, res.res.get(), res.sdt.clone());
		    } catch(Loading l) {
			sg.image(WItem.missing.layer(Resource.imgc).tex(), Coord.z, sqsz);
			break sprite;
		    }
		}
		spr.draw(sg);
		if(itemnum.get() != null)
		    sg.aimage(itemnum.get(), sqsz, 0.9, 0.8);
		if(num != null)
		    g.aimage(PUtils.strokeTex(num), itemc.add(invsq.sz()).add(UI.scale(5, 0)), 0.0, 1.0);
		if (quality != null) {
			g.aimage(PUtils.strokeTex(quality), itemc.add(invsq.sz()).add(UI.scale(4, -18)), 0.0, 1.0);
		}
	    }
	}

	ItemSpec price = this.price;
	if(price != null) {
	    GOut sg = g.reclip(pricec, invsq.sz());
	    sg.image(invsq, Coord.z);
	    try {
		price.spr().draw(sg);
	    } catch(Loading l) {
		sg.image(WItem.missing.layer(Resource.imgc).tex(), Coord.z, sqsz);
	    }
	    if(!admin && (pnumt != null))
		g.aimage(PUtils.strokeTex(pnumt), pricec.add(invsq.sz()), 0.0, 1.0);
	    if(!admin) {
		if(pqt != null) {
		    g.aimage(PUtils.strokeTex(qlbl), qualc, 0, 1);
		    g.aimage(PUtils.strokeTex(pqt), qualc.add(UI.scale(40, 0)), 0, 1);
		}
	    }
	}
	super.draw(g);
    }

    private List<ItemInfo> cinfo;
    public List<ItemInfo> info() {
	if(cinfo == null) {
		cinfo = ItemInfo.buildinfo(this, info);
		QBuff qb = quality();
		if (qb != null)
			quality = Text.render("Quality: " + (int) qb.q);
	}
	return(cinfo);
    }

    public class IconTip implements Indir<Tex>, ItemInfo.InfoTip {
	private final Tex tex;

	private IconTip(BufferedImage img) {
	    this.tex = new TexI(img);
	}

	public List<ItemInfo> info() {return(Shopbox.this.info());}
	public Tex get() {return(this.tex);}
    }

    private Object longtip = null;
    private Tex pricetip = null;
    public Object tooltip(Coord c, Widget prev) {
	ResData res = this.res;
	if(c.isect(itemc, sqsz) && (res != null)) {
	    try {
		if(longtip == null) {
		    BufferedImage ti = ItemInfo.longtip(info());
		    Resource.Pagina pg = res.res.get().layer(Resource.pagina);
		    if(pg != null)
			ti = ItemInfo.catimgs(0, ti, RichText.render("\n" + pg.text, UI.scale(200)).img);
		    try {
			longtip = new IconTip(ti);
		    } catch(NoClassDefFoundError e) {
			/* XXX: Only here waiting for clients to update with
			 * ItemInfo.InfoTip. Remove in due time. */
			longtip = new TexI(ti);
		    }
		}
		return(longtip);
	    } catch(Loading l) {
		return("...");
	    }
	}
	if(c.isect(pricec, sqsz) && (price != null)) {
	    try {
		if(pricetip == null)
		    pricetip = new TexI(ItemInfo.longtip(price.info()));
		return(pricetip);
	    } catch(Loading l) {
		return("...");
	    }
	}
	return(super.tooltip(c, prev));
    }

    public <C> C context(Class<C> cl) {return(OwnerContext.uictx.context(cl, ui));}
    @Deprecated
    public Glob glob() {return(ui.sess.glob);}
    public Resource resource() {return(res.res.get());}
    public GSprite sprite() {
	if(spr == null)
		this.spr = GSprite.create(this, res.res.get(), res.sdt.clone());
	return(spr);
    }
    public Resource getres() {return(res.res.get());}
    private Random rnd = null;
    public Random mkrandoom() {
	if(rnd == null)
	    rnd = new Random();
	return(rnd);
    }

    private static Integer parsenum(TextEntry e) {
	try {
	    if(e.text().equals(""))
		return(0);
	    return(Integer.parseInt(e.text()));
	} catch(NumberFormatException exc) {
	    return(null);
	}
    }

    public boolean mousedown(MouseDownEvent ev) {
	if((ev.b == 3) && ev.c.isect(pricec, sqsz) && (price != null)) {
	    wdgmsg("pclear");
	    return(true);
	}
	return(super.mousedown(ev));
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
	Integer n;
	if(sender == bbtn) {
		if (!bulkTextEntry.text().isEmpty()) {
			int count = Integer.parseInt(bulkTextEntry.text());
			for (int i = 0; i < count; ++i) {
				this.wdgmsg("buy");
			}
		} else {
			this.wdgmsg("buy");
		}
	} else if(sender == spipe) {
	    wdgmsg("spipe");
	} else if(sender == bpipe) {
	    wdgmsg("bpipe");
	} else if(sender == cbtn) {
	    wdgmsg("change");
	} else if((sender == pnume) || (sender == pqe)) {
	    wdgmsg("price", parsenum(pnume), parsenum(pqe));
	} else {
	    super.wdgmsg(sender, msg, args);
	}
    }

    private void updbtn() {
	boolean canbuy = (res != null) && (price != null) && (pnum > 0);
	if(canbuy && (bbtn == null)) {
	    bbtn = add(new Button(UI.scale(75), "Buy:"), buyc);
		bulkTextEntry = add(new TextEntry(UI.scale(71), ""){
			protected void changed() {
				this.settext(this.text().replaceAll("[^\\d]", "")); // Only numbers
				this.settext(this.text().replaceAll("(?<=^.{2}).*", "")); // No more than 2 digits
				super.changed();
			}
		}, bulkTextEntryc);
	} else if(!canbuy && (bbtn != null)) {
	    bbtn.reqdestroy();
	    bbtn = null;
	}
    }

    private static Text rnum(String fmt, int n) {
	if(n < 1)
	    return(null);
	return(Text.render(String.format(fmt, n)));
    }

    public void uimsg(String name, Object... args) {
	if(name == "res") {
	    this.res = null;
	    this.spr = null;
	    if(args.length > 0) {
		ResData res = new ResData(ui.sess.getresv((Integer)args[0]), Message.nil);
		if(args.length > 1)
		    res.sdt = new MessageBuf((byte[])args[1]);
		this.res = res;
	    }
	    updbtn();
	} else if(name == "tt") {
	    info = args;
	    cinfo = null;
	    longtip = null;
	} else if(name == "n") {
	    int num = Utils.iv(args[0]);
	    this.num = Text.render(String.format("%d left", num));
	} else if(name == "price") {
	    int a = 0;
	    if(args[a] == null) {
		a++;
		price = null;
	    } else {
		Indir<Resource> res = ui.sess.getresv(args[a++]);
		Message sdt = Message.nil;
		if(args[a] instanceof byte[])
		    sdt = new MessageBuf((byte[])args[a++]);
		Object[] info = null;
		if(args[a] instanceof Object[]) {
		    info = new Object[0][];
		    while(args[a] instanceof Object[])
			info = Utils.extend(info, args[a++]);
		}
		price = new ItemSpec(uictx.curry(ui), new ResData(res, sdt), info);
	    }
	    pricetip = null;
	    pnum = Utils.iv(args[a++]);
	    pq = Utils.iv(args[a++]);
	    if(!admin) {
		pnumt = rnum("\u00d7%d", pnum);
		pqt = (pq > 0)?rnum("%d+", pq):any;
	    } else {
		pnume.settext((pnum > 0)?Integer.toString(pnum):""); pnume.commit();
		pqe.settext((pq > 0)?Integer.toString(pq):""); pqe.commit();
	    }
	    updbtn();
	} else {
	    super.uimsg(name, args);
	}
    }

	private QBuff quality() {
		try {
			for (ItemInfo info : info()) {
				if (info instanceof ItemInfo.Contents)
					return getQBuff(((ItemInfo.Contents) info).sub);
			}
			return getQBuff(info());
		} catch (Exception ignored) {
		}
		return null;
	}

	private QBuff getQBuff(List<ItemInfo> infolist) {
		for (ItemInfo info : infolist) {
			if (info instanceof QBuff)
				return (QBuff) info;
		}
		return null;
	}
}
