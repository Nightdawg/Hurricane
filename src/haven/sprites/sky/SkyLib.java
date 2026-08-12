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

    /* --- shared output transform ------------------------------------- */

    /* Reinhard + gamma. Sky colour and fog colour MUST both pass through
     * this and only this, or they diverge and the horizon seam returns. */
    public static final Function tone = new Function.Def(VEC3, "sky_tone") {{
	Expression c = param(IN, VEC3).ref();
	code.add(raw("vec3 sk_c = $0 / ($0 + vec3(0.72));\n" +
		     "return pow(sk_c, vec3(1.0 / 2.2));\n", c));
    }};

    /* Pull a colour toward its own luminance. This is what stops the
     * saturated sunrise band from making the sky/ground seam obvious. */
    public static final Function desat = new Function.Def(VEC3, "sky_desat") {{
	Expression c = param(IN, VEC3).ref();
	Expression a = param(IN, FLOAT).ref();
	code.add(raw("return mix($0, vec3(dot($0, vec3(0.2126, 0.7152, 0.0722))), $1);\n", c, a));
    }};

    /* --- shared sky features (Y-up) ---------------------------------- */

    public static final Function disc = new Function.Def(VEC3, "sky_disc") {{
	Expression d = param(IN, VEC3).ref();
	Expression s = param(IN, VEC3).ref();
	code.add(raw("return vec3(1.0, 0.96, 0.86) * pow(max(dot($0, $1), 0.0), 3000.0) * 6.0;\n", d, s));
    }};

    public static final Function stars = new Function.Def(VEC3, "sky_stars") {{
	Expression d = param(IN, VEC3).ref();
	Expression s = param(IN, VEC3).ref();
	Expression t = param(IN, FLOAT).ref();
	code.add(raw("float sk_night = clamp(-$1.y * 3.0, 0.0, 1.0);\n" +
		     "if(sk_night <= 0.001) return vec3(0.0);\n" +
		     "vec2 sk_uv = floor($0.xz / (abs($0.y) + 0.25) * 240.0);\n" +
		     "float sk_n = fract(sin(dot(sk_uv, vec2(127.1, 311.7))) * 43758.5453123);\n" +
		     "float sk_st = smoothstep(0.9965, 1.0, sk_n) * (0.6 + 0.4 * sin($2 * 2.0 + sk_n * 90.0));\n" +
		     "return vec3(sk_st) * sk_night * clamp($0.y * 3.0, 0.0, 1.0);\n", d, s, t));
    }};
}
