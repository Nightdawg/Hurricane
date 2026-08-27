/* Preprocessed source code */
/* $use: lib/bollar */

package haven.res.gfx.fx.cavewarn;

import haven.*;
import haven.render.*;
import haven.res.lib.bollar.*;
import java.util.*;
import java.nio.*;
import java.awt.Color;

/* >spr: Cavein */
@haven.FromResource(name = "gfx/fx/cavewarn", version = 9)
public class Cavein extends Sprite implements Sprite.CDel, PView.Render2D {
    static final Pipe.Op mat = new Light.PhongLight(false,
						    new Color(255, 255, 255), new Color(255, 255, 255),
						    new Color(128, 128, 128), new Color(0, 0, 0),
						    1);
    final List<Boll> bollar = new LinkedList<Boll>();
    final BollData data = new BollData(new VertexArray.Layout(new VertexArray.Layout.Input(Homo3D.vertex,     new VectorFormat(3, NumberFormat.FLOAT32), 0,  0, 24),
							      new VertexArray.Layout.Input(Homo3D.normal,     new VectorFormat(3, NumberFormat.FLOAT32), 0, 12, 24)));
    Random rnd = new Random();
    boolean spawn = true;
    float de = 0;
    float str;
    float life;
    Coord3f off;
    Coord3f sz;
	public Gob gobowner = null;

	final Tex numberTex;
	Integer number;
	Map colorMap = new LinkedHashMap<Integer, Color>(){{
		put(1, new Color(0, 102, 255));
		put(2, new Color(2, 194, 0));
		put(3, new Color(251, 4, 0));
		put(4, new Color(0, 21, 255));
		put(5, new Color(162, 0, 2));
		put(6, new Color(0, 175, 169));
		put(7, new Color(0, 0, 0));
		put(8, new Color(210, 210, 210));
	}};
	Color numberColor;

    public Cavein(Owner owner, Resource res, Message sdt) {
	super(owner, res);
	str = sdt.uint8();
//	System.out.println(str);
	sz = new Coord3f(sdt.float8() * 11f, sdt.float8() * 11f, 0f);
	off = new Coord3f(-sz.x / 2f, -sz.y / 2f, sdt.float8() * 11f);
        sdt.uint8();
	life = 60 * OptWnd.sweeperDurations.get(OptWnd.sweeperSetDuration);
		number = (int) Math.round(str / 30.0);
		numberColor = (Color) colorMap.get(number);
		if (number != 7)
			numberTex = new TexI(Utils.outline2(Text.num20boldFnd.renderstroked(String.valueOf(number), numberColor, Color.BLACK).img, Color.BLACK, true));
		else
			numberTex = new TexI(Utils.outline2(Text.num20boldFnd.renderstroked(String.valueOf(number), numberColor, Color.WHITE).img, Color.BLACK, true));
	if (owner instanceof Gob)
		gobowner = (Gob) owner;
	else if (owner instanceof Gob.Overlay)
		gobowner = ((Gob.Overlay) owner).gob;
	}

    class Boll {
	Coord3f p, v, n;
	float sz;
	float t;

	Boll(Coord3f pos, float sz) {
	    this.p = new Coord3f(pos.x, pos.y, pos.z);
	    this.v = new Coord3f(0, 0, 0);
	    this.n = new Coord3f(rnd.nextFloat() - 0.5f, rnd.nextFloat() - 0.5f, rnd.nextFloat() - 0.5f).norm();
	    this.sz = sz;
	    this.t = -1;
	}

	boolean tick(float dt) {
	    v.z -= dt;
	    v.z = Math.min(0, v.z + (dt * 5f * v.z * v.z / sz));
	    v.x += dt * (float)rnd.nextGaussian() * 0.1f;
	    v.y += dt * (float)rnd.nextGaussian() * 0.1f;
	    p.x += v.x;
	    p.y += v.y;
	    p.z += v.z;
	    if(p.z < 0) {
		p.z = 0;
		v.z *= -0.7f;
		v.x = v.z * (rnd.nextFloat() - 0.5f);
		v.y = v.z * (rnd.nextFloat() - 0.5f);
		if(t < 0)
		    t = 0;
	    }
	    if(t >= 0) {
		t += dt;
	    }
	    return(t > 1.5f);
	}
    }

    public boolean tick(double ddt) {
	float dt = (float)ddt;
	de += dt * str;
	if(spawn && (de > 1)) {
	    de -= 1;
	    bollar.add(new Boll(off.add(rnd.nextFloat() * sz.x, rnd.nextFloat() * sz.y, rnd.nextFloat() * sz.x), 0.5f + (rnd.nextFloat() * 1.5f)));
	}
	for(Iterator<Boll> i = bollar.iterator(); i.hasNext();) {
	    Boll boll = i.next();
	    if(boll.tick(dt))
		i.remove();
	}
	if(life > 0 && ((life -= dt) <= 0))
	    spawn = false;
	return(!spawn && bollar.isEmpty());
    }

    public void gtick(Render g) {
	data.update(g, bollar.size(), this::fill);
    }

    private FillBuffer fill(DataBuffer dst, Environment env) {
	FillBuffer ret = env.fillbuf(dst);
	ByteBuffer buf = ret.push();
	for(Boll boll : bollar) {
	    buf.putFloat(boll.p.x).putFloat(boll.p.y).putFloat(boll.p.z);
	    buf.putFloat(boll.n.x).putFloat(boll.n.y).putFloat(boll.n.z);
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

    public void delete() {
	spawn = false;
    }

	@Override
	public void draw(GOut g, Pipe state) {
		if (OptWnd.enableMineSweeperCheckBox.a) {
			try {
				Coord2d sc2d = gobowner.rc;
				double z = gobowner.glob.map.getcz(sc2d);
				if (z == 0) {
					Coord3f sc3f = gobowner.glob.sess.ui.gui.map.screenxf(sc2d);
					Coord sc = sc3f.round2();
					g.aimage(numberTex, sc, 0.5, 0.5);
				}
			} catch (Exception ignored) {
			}
		}
	}
}
