package haven;

import haven.render.Homo3D;
import haven.render.Pipe;
import haven.render.RenderTree;

public abstract class GobInfo extends GAttrib implements RenderTree.Node, PView.Render2D {
    protected Tex tex;
    public Coord3f pos = new Coord3f(0, 0, 1);
    protected final Object texLock = new Object();
    protected Pair<Double, Double> center = new Pair<>(0.5, 1.0);
    protected boolean dirty = true;

    public GobInfo(Gob owner) {
        super(owner);
    }

    protected abstract boolean enabled();

    protected void up(float up) {
        pos = new Coord3f(0, 0, up);
    }

    @Override
    public void ctick(double dt) {
        synchronized (texLock) {
            if(enabled() && dirty && tex == null) {
                tex = render();
                dirty = false;
            }
        }
    }

    @Override
    public void draw(GOut g, Pipe state) {
        if (!GameUI.showUI)
            return;
        synchronized (texLock) {
            if(enabled() && tex != null) {
                Coord sc = Homo3D.obj2sc(pos, state, Area.sized(g.sz()));
                if(sc == null) {return;}
                sc.y = sc.y + UI.scale(17);
                if(sc.isect(Coord.z, g.sz())) {
                    g.aimage(tex, sc, center.a, center.b);
                }
            }
        }
    }

    protected abstract Tex render();

    public void clear() {
        synchronized(texLock) {
            if(tex != null) {
                tex.dispose();
                tex = null;
            }
        }
        dirty = true;
    }

    public void dispose() {
        clear();
    }
}
