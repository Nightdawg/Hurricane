/* Preprocessed source code */
package haven.sprites;

import haven.*;
import haven.render.Homo3D;
import haven.render.Pipe;

public class PartyMarkSprite extends Sprite implements PView.Render2D {
    protected Tex tex;
    public Coord3f pos = new Coord3f(0, 0, 1);
    public Coord3f posu = new Coord3f(0, 0, 2);
    public final float normsz = -8f;

    public PartyMarkSprite(Owner owner, Tex tex) {
	super(owner, null);
    this.tex = tex;
    }
    
    public void draw(GOut g, Pipe state) {
    Coord3f fsc = Homo3D.obj2view(pos, state, Area.sized(Coord.z, g.sz()));
    Coord3f sczu = Homo3D.obj2view(posu, state, Area.sized(Coord.z, g.sz()));
    Coord sc = fsc.round2();
    if(sc == null)
        return;
    float scale = (sczu.y - fsc.y) / normsz;
    Coord ul = new Coord(Math.round(tex.sz().inv().x * scale)/2, Math.round(tex.sz().inv().y * scale));
    g.image(tex, sc.add(ul), tex.sz().mul(scale));

    }


}
