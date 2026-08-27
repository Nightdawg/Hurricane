/* Preprocessed source code */
package haven.res.lib.bollar;

import haven.*;
import haven.render.*;
import haven.render.sl.*;
import java.util.*;
import java.nio.*;
import static haven.render.sl.Type.*;
import static haven.render.sl.Cons.*;

@haven.FromResource(name = "lib/bollar", version = 4)
public class CamOffset extends State {
    public static final Attribute offset = new Attribute(Type.VEC3, "camoff");

    public static final ShaderMacro shader = prog -> {
	Homo3D.get(prog).eyev.mod(in -> add(in, vec4(offset.ref(), l(0))), 10);
    };
    public ShaderMacro shader() {return(shader);}

    public void apply(Pipe buf) {
	buf.put(RUtils.adhoc, this);
    }
}
