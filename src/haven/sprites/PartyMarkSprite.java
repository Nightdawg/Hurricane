/* Preprocessed source code */
package haven.sprites;

import haven.*;
import haven.render.Homo3D;
import haven.render.Pipe;

public class PartyMarkSprite extends Sprite implements PView.Render2D {
    protected Tex tex;
    public Coord3f pos = new Coord3f(0, 0, 10); // Raised to belt height

    public PartyMarkSprite(Owner owner, Tex tex) {
	super(owner, null);
    this.tex = tex;
    }
    
    public void draw(GOut g, Pipe state) {
    Coord3f fsc = Homo3D.obj2view(pos, state, Area.sized(Coord.z, g.sz()));
    Coord sc = fsc.round2();
    if(sc == null)
        return;

    float distanceScale = Math.max(0.01f, Math.min(1.0f, 1.0f / (float)Math.pow(fsc.z * 0.8f, 0.7)));
    int scaledW = (int)(tex.sz().x * 0.4 * distanceScale);
    int scaledH = (int)(tex.sz().y * 0.4 * distanceScale);
    Coord sz = new Coord(scaledW, scaledH);
    g.aimage(tex, sc, 0.5, 0.5, sz);
    }
}
