/* Preprocessed source code */
package haven.res.gfx.tiles.flavor.ridge_edge;

import java.util.*;
import java.util.function.*;
import haven.*;
import haven.render.*;
import haven.resutil.*;
import haven.resutil.Ridges.*;

/* >flavor: RidgeEdge$Fac */
@haven.FromResource(name = "gfx/tiles/flavor/ridge-edge", version = 2)
public class RidgeEdge implements Tileset.Flavor {
    public NodeWrap mat;
    public float r = 1.25f, zoff = -0.12f, xoff = -0.12f;

    public RidgeEdge(Resource sres, Object[][] argsv) {
	for(Object[] args : argsv) {
	    int a = 0;
	    if((a < args.length) && (args[a] instanceof String))
		this.mat = Resource.classres(RidgeEdge.class).pool.load((String)args[a++], Utils.iv(args[a++])).get().flayer(Material.Res.class).get();
	    while(a < args.length) {
		Object[] parg = (Object[])args[a++];
		switch((String)parg[0]) {
		case "r":
		    this.r = Utils.fv(parg[1]);
		    break;
		case "z":
		    this.zoff = Utils.fv(parg[1]);
		    break;
		case "x":
		    this.xoff = Utils.fv(parg[1]);
		    break;
		}
	    }
	}
	if(this.mat == null)
	    this.mat = sres.flayer(Material.Res.class).get();
    }

    public static RPart getdesc(Terrain trn, Coord gtc) {
	if(!(trn.grid instanceof MCache.Grid))
	    return(null);
	MCache.Grid grid = (MCache.Grid)trn.grid;
	Coord ltc = gtc.sub(grid.ul);
	Coord cc = ltc.div(MCache.cutsz);
	MapMesh mesh = grid.getcut(cc);
	Coord mtc = gtc.sub(mesh.ul);
	Ridges r = mesh.data(Ridges.id);
	return(r.getrdesc(mtc));
    }

    public class Point {
	public final Coord3f p;
	public final List<Point> t = new ArrayList<>(1), f = new ArrayList<>(1);
	public final Surface.Vertex[] v = new Surface.Vertex[8];

	public Point(Coord3f p) {
	    this.p = p;
	}

	public void fin(Surface surf) {
	    Coord3f s = Coord3f.o;
	    for(Point f : this.f)
		s = s.add(p.sub(f.p).norm());
	    for(Point t : this.t)
		s = s.add(t.p.sub(p).norm());
	    Coord3f d = s.norm(), o = d.cmul(Coord3f.zu), u = Coord3f.zu;
	    for(int i = 0; i < v.length; i++) {
		double a = (i * 2 * Math.PI) / v.length;
		v[i] = surf.new Vertex(p.add(o.mul((r * (float)Math.cos(a)) + xoff)).add(u.mul((r * (float)-Math.sin(a)) + zoff)));
	    }
	}

	public void connect() {
	    Surface surf = v[0].s();
	    for(Point t : this.t) {
		for(int i = 0; i < v.length; i++) {
		    int o = (i + 1) % v.length;
		    surf.new Face(v[i], v[o], t.v[o]);
		    surf.new Face(v[i], t.v[o], t.v[i]);
		}
	    }
	}

	public void build(MeshBuf mbuf) {
	    MeshBuf.Tex tex = mbuf.layer(MeshBuf.tex);
	    for(Point t : this.t) {
		MeshBuf.Vertex[] fv = new MeshBuf.Vertex[v.length];
		MeshBuf.Vertex[] tv = new MeshBuf.Vertex[v.length];
		float il = 1f / v.length;
		for(int i = 0; i < v.length; i++) {
		    fv[i] = new Surface.MeshVertex(mbuf, v[i]);
		    tex.set(fv[i], Coord3f.of(0, i * il, 0));
		    tv[i] = new Surface.MeshVertex(mbuf, t.v[i]);
		    tex.set(tv[i], Coord3f.of(1, i * il, 0));
		}
		for(int i = 0; i < v.length; i++) {
		    int o = (i + 1) % v.length;
		    mbuf.new Face(fv[i], fv[o], tv[o]);
		    mbuf.new Face(fv[i], tv[o], tv[i]);
		}
	    }
	}
    }

    private static final Map<NodeWrap, Function<Buffer, MeshBuf>> ids = new WeakHashMap<>();
    public MeshBuf getbuf(Buffer cbuf) {
	Function<Buffer, MeshBuf> id;
	synchronized(ids) {
	    id = ids.computeIfAbsent(mat, mat -> buf -> {
		    MeshBuf mbuf = new MeshBuf();
		    buf.finish(new Runnable() {
			    public void run() {
				if(!mbuf.emptyp()) {
				    FastMesh mesh = mbuf.mkmesh();
				    Gob ob = new Tileset.Flavor.GridObj(buf);
				    ob.setattr(new SprDrawable(ob, owner -> new StaticSprite(owner, Resource.classres(RidgeEdge.class), mat.apply(mesh))));
				    buf.add(ob);
				}
			    }
			});
		    return(mbuf);
		});
	}
	return(cbuf.datum(id));
    }

    public void flavor2(Buffer buf, Terrain trn, Random seed) {
	float oz = OptWnd.flatWorldCheckBox.a ? 0 : (float)trn.map.getfz(trn.area.ul);
	Map<Coord3f, Point> points = new IdentityHashMap<>();
	for(Coord tc : trn.tiles()) {
	    RPart desc = getdesc(trn, tc);
	    if(desc == null)
		continue;
	    for(int i = 0; i < desc.uedge.length; i++) {
		for(int o = 0; o < desc.uedge[i].length - 1; o++) {
		    Surface.Vertex v1 = desc.v[desc.uedge[i][o]], v2 = desc.v[desc.uedge[i][o + 1]];
		    Point p1 = points.computeIfAbsent(v1, v -> new Point(v.sub(0, 0, oz)));
		    Point p2 = points.computeIfAbsent(v2, v -> new Point(v.sub(0, 0, oz)));
		    p1.t.add(p2);
		    p2.f.add(p1);
		}
	    }
	}
	Surface surf = new Surface();
	points.values().forEach(p -> p.fin(surf));
	points.values().forEach(Point::connect);
	surf.fin();
	MeshBuf mbuf = getbuf(buf);
	points.values().forEach(p -> p.build(mbuf));
    }

    public void flavor(Buffer buf, Terrain trn, Random seed) {
	try {
	    flavor2(buf, trn, seed);
	} catch(NoSuchMethodError e) {
	    Warning.warn("client lacks ridge-edge support");
	    return;
	}
    }

    public static class Fac implements Factory {
	public final Resource sres;
	public final Object[] fargs;

	public Fac(Resource sres, Object... fargs) {
	    this.sres = sres;
	    this.fargs = fargs;
	}

	public RidgeEdge make(Tileset set, Object... iargs) {
	    return(new RidgeEdge(sres, new Object[][] {fargs, iargs}));
	}
    }
}
