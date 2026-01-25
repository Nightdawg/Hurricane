package haven;

import haven.render.Location;
import haven.render.Pipe;

public class GobCustomSizeAndRotation implements Gob.SetupMod {
    Gob gob;
    
    private Pipe.Op op = null;
    
    public void update(Gob gob) {
    this.gob = gob;
	if(gob.getres() == null || gob.getres().name == null) {
	    op = null;
	    return;
	}
    update();
    }
    
    private void update() {

	    op = makeScale();

    }
    
    private Pipe.Op makeScale() {
    if (gob.getres() != null) {
        String resName = gob.getres().name;
        if(OptWnd.flatCupboardsCheckBox.a && resName.equals("gfx/terobjs/cupboard"))
            return Pipe.Op.compose(Location.rot(new Coord3f(0, 1, 0), 4.712f), Location.scale(0.2f, 1, 0.62f), Location.xlate(new Coord3f(6,0,-8.8f)));
        else if ((resName.startsWith("gfx/terobjs/arch/palisade") || resName.startsWith("gfx/terobjs/arch/brickwall")) && !resName.contains("gate"))
            return Pipe.Op.compose(Location.scale(1, 1, OptWnd.palisadesAndBrickWallsScaleSlider.val/100f));
        else if (resName.equals("gfx/terobjs/stockpile-hide"))
            return Pipe.Op.compose(Location.scale(0.6f, 0.6f, 0.9f));
        else
            return null;
    } else
        return null;
    }
    
    @Override
    public Pipe.Op gobstate() {
	return op;
    }
}
