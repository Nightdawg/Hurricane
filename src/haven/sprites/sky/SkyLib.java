package haven.sprites.sky;

import haven.render.sl.*;
import static haven.render.sl.Function.PDir.*;
import static haven.render.sl.Type.*;

/* Shared GLSL for the procedural sky.
 *
 * Every function here is pure maths over its parameters -- no uniforms are
 * referenced from inside a body, so nothing here needs to know about game
 * state. SkyPalette passes the state in at the call site.
 *
 * Handedness: the four public entry points (sky_colA, sky_colB, sky_horA,
 * sky_horB) take world-space Z-up directions and convert to Y-up exactly
 * once, at their first line. Every helper below them is Y-up already and
 * must not convert again.
 *
 * Local variables declared inside raw bodies are prefixed sk_ so they cannot
 * collide with the symbols the SL compiler generates. */
public abstract class SkyLib {
    /* A statement written as literal GLSL. "$0", "$1", ... are replaced by
     * the given expressions, which emit their own generated symbol names --
     * so parameter and uniform names are never hardcoded. */
    static Statement raw(String glsl, Expression... args) {
	return(new Statement() {
		public void walk(Walker w) {
		    for(Expression a : args)
			w.el(a);
		}

		public void output(Output out) {
		    int p = 0;
		    while(true) {
			int i = glsl.indexOf('$', p);
			if(i < 0) {
			    out.write(glsl.substring(p));
			    break;
			}
			out.write(glsl.substring(p, i));
			int j = i + 1;
			while((j < glsl.length()) && Character.isDigit(glsl.charAt(j)))
			    j++;
			args[Integer.parseInt(glsl.substring(i + 1, j))].output(out);
			p = j;
		    }
		}
	    });
    }

    /* A bare GLSL identifier, for referring to a local declared inside a raw
     * body. Only valid inside the raw block that declares that name -- there
     * is no Cons factory that can name a raw local, which is why this
     * exists. */
    static Expression id(String name) {
	return(new Expression() {
		public void walk(Walker w) {}
		public void output(Output out) {out.write(name);}
	    });
    }

    /* How far the fog colour is pulled toward its own luminance. 0 = the raw
     * sky colour, 1 = grey. Tuned in Task 12; the prototype settled near
     * 0.35 for Mode B. */
    public static final double DESAT = 0.35;

    /* Z-up world direction -> Y-up sky-maths direction. */
    public static final Function yup = new Function.Def(VEC3, "sky_yup") {{
	Expression v = param(IN, VEC3).ref();
	code.add(raw("return normalize(vec3($0.x, $0.z, $0.y));\n", v));
    }};
}
