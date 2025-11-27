/* Preprocessed source code */
/* $use: ui/tt/wpn/info */
import haven.*;
import haven.res.ui.tt.wpn.info.*;

/* >tt: Grievous */
@haven.FromResource(name = "ui/tt/wpn/grievous", version = 3)
public class Grievous extends WeaponInfo {
    public final double deg;

    public Grievous(Owner owner, double deg) {
	super(owner);
	this.deg = deg;
    }

    public static Grievous mkinfo(Owner owner, Object... args) {
	return(new Grievous(owner, ((Number)args[1]).doubleValue() * 0.01));
    }

    public String wpntips() {
	return(String.format("$col[227,140,64]{Grievous damage}: %.1f%%", deg * 100));
    }

    public int order() {return(80);}
}
