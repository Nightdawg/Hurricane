/* Preprocessed source code */
package haven.res.ui.expwnd;

import haven.*;

/* >wdg: ExpWnd */
@haven.FromResource(name = "ui/expwnd", version = 23)
public class ExpWnd extends Window {
    public static Resource sfx = Loading.waitfor(Resource.classres(ExpWnd.class).pool.load("sfx/exp", 1));
    public static final RichText.Foundry fnd = new RichText.Foundry();
    public final Indir<Resource> exp;
    public final int ep;
    private Button close;
    private Img img, text;

    public static Widget mkwidget(UI ui, Object... args) {
	Indir<Resource> res = ui.sess.getresv(args[0]);
	int ep = (args.length > 1) ? Utils.iv(args[1]) : 0;
	return(new ExpWnd(res, ep));
    }

    public ExpWnd(Indir<Resource> exp, int ep) {
	super(UI.scale(300, 50), "Hey, listen!", true);
	this.exp = exp;
	this.ep = ep;
    }

    protected void added() {
	if(c.equals(0, 0))
	    c = new Coord((parent.sz.x - sz.x) / 2, ((parent.sz.y / 2) - sz.y) / 2);
	Audio.play(sfx);
	super.added();
    }

    public void tick(double dt) {
	if(img == null) {
	    Tex img;
	    String cap, text;
	    try {
		img = exp.get().layer(Resource.imgc).tex();
		Resource.Tooltip tt = exp.get().layer(Resource.tooltip);
		cap = (tt == null)?null:(tt.t);
		text = exp.get().layer(Resource.pagina).text;
	    } catch(Loading e) {
		return;
	    }
	    if(cap != null)
		chcap(cap);
	    this.img = add(new Img(img), 0, UI.scale(10));
	    this.text = add(new Img(fnd.render(text, UI.scale(300)).tex()), img.sz().x + UI.scale(5), 10);
	    if(ep > 0)
		add(new Label("Experience points gained: " + ep), this.text.c.x, this.text.c.y + this.text.sz.y + UI.scale(10));
	    Coord csz = contentsz();
	    this.close = adda(new Button(UI.scale(100), "Okay!"), csz.x / 2, csz.y + UI.scale(25), 0.5, 0);
	    resize(contentsz());
	    this.c = new Coord((parent.sz.x - sz.x) / 2, ((parent.sz.y / 2) - sz.y) / 2);
	}
	super.tick(dt);
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
	if(sender == close)
	    wdgmsg("close");
	else
	    super.wdgmsg(sender, msg, args);
    }
}
