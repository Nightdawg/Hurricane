/* Preprocessed source code */
/* $use: ui/tt/wpn/info */
import haven.*;
import haven.res.ui.tt.wpn.info.*;

/* >tt: Range */
@haven.FromResource(name = "ui/tt/wpn/range", version = 2)
public class Range extends WeaponInfo {
    public final double mod;

    public Range(Owner owner, double mod) {
	super(owner);
	this.mod = mod;
    }

    public static Range mkinfo(Owner owner, Object... args) {
	return(new Range(owner, ((Number)args[1]).doubleValue() * 0.01));
    }

    public String wpntips() {
	return(String.format("$col[70,194,80]{Range}: %d%%", Math.round(mod * 100)));
    }

    public int order() {return(110);}
}
