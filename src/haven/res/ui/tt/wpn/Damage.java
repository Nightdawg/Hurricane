package haven.res.ui.tt.wpn;
/* Preprocessed source code */
/* $use: ui/tt/wpn/info */
//import haven.*;
import haven.res.ui.tt.wpn.info.*;

/* >tt: Damage */
@haven.FromResource(name = "ui/tt/wpn/dmg", version = 4)
public class Damage extends WeaponInfo {
    public final int dmg;

    public Damage(Owner owner, int dmg) {
	super(owner);
	this.dmg = dmg;
    }

    public static Damage mkinfo(Owner owner, Object... args) {
	return(new Damage(owner, ((Number)args[1]).intValue()));
    }

    public String wpntips() {
	return("$col[227,64,64]{Damage}: " + dmg);
    }

    public int order() {return(50);}
}
