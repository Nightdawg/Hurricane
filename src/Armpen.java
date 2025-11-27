/* Preprocessed source code */
/* $use: ui/tt/wpn/info */
import haven.*;
import haven.res.ui.tt.wpn.info.*;

/* >tt: Armpen */
@haven.FromResource(name = "ui/tt/wpn/armpen", version = 8)
public class Armpen extends WeaponInfo {
    public final double deg;

    public Armpen(Owner owner, double deg) {
	super(owner);
	this.deg = deg;
    }

    public static Armpen mkinfo(Owner owner, Object... args) {
	return(new Armpen(owner, ((Number)args[1]).doubleValue() * 0.01));
    }

    public String wpntips() {
	return(String.format("$col[227,200,64]{Armor penetration}: %.1f%%", deg * 100));
    }

    public int order() {return(100);}
}
