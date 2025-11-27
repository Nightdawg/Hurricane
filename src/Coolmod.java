/* Preprocessed source code */
/* $use: ui/tt/wpn/info */
import haven.*;
import haven.res.ui.tt.wpn.info.*;

/* >tt: Coolmod */
@haven.FromResource(name = "ui/tt/wpn/coolmod", version = 3)
public class Coolmod extends WeaponInfo {
    public final double mod;

    public Coolmod(Owner owner, double mod) {
	super(owner);
	this.mod = mod;
    }

    public static Coolmod mkinfo(Owner owner, Object... args) {
	return(new Coolmod(owner, ((Number)args[1]).doubleValue() * 0.01));
    }

    public String wpntips() {
	return(String.format("$col[85,164,237]{Attack cooldown}: %d%%", Math.round(mod * 100)));
    }

    public int order() {return(125);}
}
