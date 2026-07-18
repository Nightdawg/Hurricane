/* Preprocessed source code */
/* $use: lib/bollar */

package haven.res.gfx.tiles.flavor.wspray;

import haven.*;
import haven.render.*;
import haven.resutil.*;
import haven.res.lib.bollar.*;
import java.util.*;
import java.util.function.*;
import java.nio.*;
import static haven.MCache.tilesz;

@haven.FromResource(name = "gfx/tiles/flavor/wspray", version = 2)
public class Effect extends Sprite {
    public static final VertexArray.Layout fmt =
	new VertexArray.Layout(new VertexArray.Layout.Input(Homo3D.vertex,     new VectorFormat(3, NumberFormat.FLOAT32), 0,  0, 20),
			       new VertexArray.Layout.Input(Homo3D.normal,     new VectorFormat(3, NumberFormat.SNORM8),  0, 12, 20),
			       new VertexArray.Layout.Input(VertexColor.color, new VectorFormat(4, NumberFormat.UNORM8),  0, 16, 20));
    public static final Pipe.Op draw = Pipe.Op.compose(ScreenPointSize.fixed(2),
						       States.maskdepth, new Rendered.Order.Default(15000),
						       // States.Depthtest.none,
						       VertexColor.instance, ShadowMap.maskshadow);
    public static final float tpb = 0.01f;
    public final Collection<Line> lines = new ArrayList<>();
    public final Pipe.Op mat;
    private final Random rnd = new Random();
    private final Collection<Boll> bollar = new FastArrayList<Boll>();
    private final BollData data = new BollData(fmt);
    private final Coord3f cc;

    public Effect(Owner owner, Resource res) {
	super(owner, res);
	cc = owner.context(Gob.class).getrc();
	mat = Pipe.Op.compose(res.flayer(Material.Res.class, 0).get(), draw);
    }

    public static class Line {
	public final Coord3f a, b, nd;
	public final float ai, bi;
	public float de;

	public Line(Coord3f a, Coord3f b, float ai, float bi) {
	    this.a = a;
	    this.b = b;
	    this.nd = Coord3f.of(b.x, b.y, 0).sub(a.x, a.y, 0).norm();
	    this.ai = ai;
	    this.bi = bi;
	}
    }

    public class Boll {
	public static final float life = 1;
	public float x, y, z;
	public float xv, yv, zv;
	public float t = 0;
	public float intns = 0.1f + (rnd.nextFloat() * 0.1f);

	public Boll(Coord3f pos, Coord3f vel) {
	    x = pos.x;
	    y = pos.y;
	    z = pos.z;
	    xv = vel.x;
	    yv = vel.y;
	    zv = vel.z;
	}

	public boolean tick(float dt) {
	    x += xv * dt;
	    y += yv * dt;
	    z += zv * dt;
	    t += dt;
	    return(t >= life);
	}

	public float alpha() {
	    if(t < 0.1f)
		return((t / 0.1f) * intns);
	    else
		return((1 - ((t - 0.1f) / (life - 0.1f))) * intns);
	}
    }

    public void addline(Coord2d ap, Coord2d bp, double az, double bz, double ai, double bi) {
	lines.add(new Line(Coord3f.of((float)ap.x, (float)ap.y, (float)az).sub(cc).invy(),
			   Coord3f.of((float)bp.x, (float)bp.y, (float)bz).sub(cc).invy(),
			   (float)ai, (float)bi));
    }

    public static boolean enabled = true;
    public boolean tick(double ddt) {
	float dt = (float)ddt;
	if(enabled) {
	    for(Line l : lines) {
		l.de += dt;
		float tpb = this.tpb / ((l.ai + l.bi) * 0.5f * 0.1f);
		int n = (int)(l.de / tpb);
		l.de -= n * tpb;
		for(int i = 0; i < n; i++) {
		    Coord3f d = l.b.sub(l.a);
		    Coord3f td = Coord3f.of(-l.nd.y, l.nd.x, rnd.nextFloat());
		    Coord3f p = l.a.add(d.mul(rnd.nextFloat())).add(td.mul(rnd.nextFloat() * 11));
		    bollar.add(new Boll(p, td.mul(10)));
		}
	    }
	}
	for(Iterator<Boll> i = bollar.iterator(); i.hasNext();) {
	    Boll boll = i.next();
	    if(boll.tick(dt))
		i.remove();
	}
	// Debug.statprint(String.format("Vattenbollar: %,d", bollar.size()), true);
	return(false);
    }

    static {
	Console.setscmd("wspray", new Console.Command() {
		public void run(Console cons, String[] args) {
		    enabled = Utils.parsebool(args[1]);
		}
	    });
    }

    public void gtick(Render g) {
	data.update(g, bollar.size(), this::fill);
    }

    private FillBuffer fill(DataBuffer dst, Environment env) {
	FillBuffer ret= env.fillbuf(dst);
	ByteBuffer buf = ret.push();
	for(Boll boll : bollar) {
	    buf.putFloat(boll.x).putFloat(boll.y).putFloat(boll.z);
	    buf.put((byte)0).put((byte)0).put((byte)127).put((byte)0);
	    buf.put((byte)-1).put((byte)-1).put((byte)-1).put((byte)(255 * boll.alpha()));
	}
	return(ret);
    }

    public void dispose() {
	data.dispose();
    }

    public void added(RenderTree.Slot slot) {
	slot.ostate(mat);
	slot.add(data);
    }

    public static final Function<Tileset.Flavor.Buffer, Effect> id = buf -> {
	Gob ob = new Tileset.Flavor.Obj(buf, buf.area.ul.mul(tilesz), 0) {
		public Placer placer() {
		    return(glob.map.mapplace);
		}

		protected Pipe.Op getmapstate(Coord3f pc) {
		    return(null);
		}
	    };
	/* XXX: Use SprDrawable.apply() */
	SprDrawable d = new SprDrawable(ob, owner -> new Effect(owner, Resource.classres(Effect.class)));
	Effect fx = (Effect)d.spr;
	ob.setattr(d);
	buf.finish(() -> {
		if(!fx.lines.isEmpty())
		    buf.add(ob);
	    });
	return(fx);
    };
}

/* >flavor: WaterSpray */
