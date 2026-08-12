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

    /* --- Mode A: analytic gradient (Y-up, no sun disc) ---------------- */

    public static final Function baseA = new Function.Def(VEC3, "sky_baseA") {{
	Expression d = param(IN, VEC3).ref();
	Expression s = param(IN, VEC3).ref();
	code.add(raw("float sk_day = clamp($1.y * 2.5 + 0.25, 0.0, 1.0);\n" +
		     "vec3 sk_zen = mix(vec3(0.015, 0.020, 0.055), vec3(0.16, 0.38, 0.78), sk_day);\n" +
		     "vec3 sk_hor = mix(vec3(0.045, 0.050, 0.090), vec3(0.72, 0.83, 0.95), sk_day);\n" +
		     "float sk_t = pow(clamp($0.y, 0.0, 1.0), 0.42);\n" +
		     "vec3 sk_col = mix(sk_hor, sk_zen, sk_t);\n" +
		     "float sk_sd = max(dot($0, $1), 0.0);\n" +
		     "float sk_dusk = exp(-abs($1.y) * 7.0);\n" +
		     "sk_col += vec3(1.0, 0.42, 0.13) * pow(sk_sd, 5.0) * (1.0 - clamp($0.y, 0.0, 1.0)) * sk_dusk * 1.1;\n" +
		     "sk_col += vec3(1.0, 0.72, 0.35) * pow(sk_sd, 40.0) * clamp($1.y + 0.15, 0.0, 1.0) * 0.8;\n" +
		     /* Below the horizon the gradient term above is clamped
		      * flat, and that is the only region this game's camera
		      * ever shows: FreeCam sits at 45 degrees with a 30-degree
		      * vertical field, so the screen spans 30 to 60 degrees
		      * BELOW horizontal and the horizon is never in frame.
		      * Continue into a ground haze so the visible band has
		      * depth instead of being one flat colour. */
		     "sk_col = mix(sk_col, sk_hor * vec3(0.55, 0.54, 0.52),\n" +
		     "             pow(clamp(-$0.y, 0.0, 1.0), 0.7));\n" +
		     "return sk_col;\n", d, s));
    }};

    /* --- Mode B: Rayleigh + Mie (Y-up, no sun disc) ------------------- */

    public static final Function baseB = new Function.Def(VEC3, "sky_baseB") {{
	Expression d = param(IN, VEC3).ref();
	Expression s = param(IN, VEC3).ref();
	code.add(raw("const float sk_Br = 0.0025, sk_Bm = 0.0003, sk_g = 0.98;\n" +
		     "vec3 sk_nitro = vec3(0.650, 0.570, 0.475);\n" +
		     "vec3 sk_Kr = sk_Br / pow(sk_nitro, vec3(4.0));\n" +
		     "vec3 sk_Km = sk_Bm / pow(sk_nitro, vec3(0.84));\n" +
		     "vec3 sk_pos = $0;\n" +
		     /* Clamping y to 0 turns a straight-down direction into
		      * vec3(0), and normalize(vec3(0)) is NaN -- which then
		      * poisons the whole returned colour. Reachable from colB
		      * through the cube's bottom face at maximum camera
		      * elevation (MapView.java:337 clamps telev to pi/2). */
		     "sk_pos.y = max(sk_pos.y, 1.0e-4);\n" +
		     "float sk_mu = dot(normalize(sk_pos), $1);\n" +
		     "float sk_ray = 3.0 / (8.0 * 3.14159) * (1.0 + sk_mu * sk_mu);\n" +
		     "vec3 sk_mie = (sk_Kr + sk_Km * (1.0 - sk_g * sk_g) / (2.0 + sk_g * sk_g)\n" +
		     "               / pow(1.0 + sk_g * sk_g - 2.0 * sk_g * sk_mu, 1.5)) / (sk_Br + sk_Bm);\n" +
		     "vec3 sk_day = exp(-exp(-((sk_pos.y + $1.y * 4.0) * (exp(-sk_pos.y * 16.0) + 0.1) / 80.0) / sk_Br)\n" +
		     "              * (exp(-sk_pos.y * 16.0) + 0.1) * sk_Kr / sk_Br)\n" +
		     "              * exp(-sk_pos.y * exp(-sk_pos.y * 8.0) * 4.0) * exp(-sk_pos.y * 2.0) * 4.0;\n" +
		     "vec3 sk_nit = vec3(1.0 - exp($1.y)) * 0.2;\n" +
		     "vec3 sk_out = sk_ray * sk_mie * mix(sk_day, sk_nit, -$1.y * 0.2 + 0.5);\n" +
		     /* Same below-horizon continuation as baseA -- see the
		      * note there. sk_pos.y was already clamped positive, so
		      * sk_out holds the horizon value for downward rays. */
		     "return mix(sk_out, sk_out * vec3(0.55, 0.54, 0.52),\n" +
		     "           pow(clamp(-$0.y, 0.0, 1.0), 0.7));\n", d, s));
    }};

    /* --- clouds (Y-up) ----------------------------------------------- */

    public static final Function clouds = new Function.Def(VEC3, "sky_clouds") {{
	Expression d = param(IN, VEC3).ref();
	Expression s = param(IN, VEC3).ref();
	Expression t = param(IN, FLOAT).ref();
	Expression sky = param(IN, VEC3).ref();
	Expression oct = param(IN, INT).ref();
	code.add(raw("if($0.y <= 0.005) return $3;\n" +
		     "vec2 sk_uv = ($0.xz / ($0.y + 0.12) * 0.55 + vec2($2 * 0.010, $2 * 0.004)) * 1.6;\n" +
		     "float sk_p = 0.0;\n" +
		     "{\n" +
		     "    vec2 sk_q = sk_uv;\n" +
		     "    float sk_amp = 0.5;\n" +
		     "    for(int sk_o = 0; sk_o < 8; sk_o++) {\n" +
		     "        if(sk_o >= $4) break;\n" +
		     "        vec2 sk_i = floor(sk_q), sk_f = fract(sk_q);\n" +
		     "        vec2 sk_u = sk_f * sk_f * (3.0 - 2.0 * sk_f);\n" +
		     "        float sk_h0 = fract(sin(dot(sk_i + vec2(0.0, 0.0), vec2(127.1, 311.7))) * 43758.5453123);\n" +
		     "        float sk_h1 = fract(sin(dot(sk_i + vec2(1.0, 0.0), vec2(127.1, 311.7))) * 43758.5453123);\n" +
		     "        float sk_h2 = fract(sin(dot(sk_i + vec2(0.0, 1.0), vec2(127.1, 311.7))) * 43758.5453123);\n" +
		     "        float sk_h3 = fract(sin(dot(sk_i + vec2(1.0, 1.0), vec2(127.1, 311.7))) * 43758.5453123);\n" +
		     "        sk_p += sk_amp * mix(mix(sk_h0, sk_h1, sk_u.x), mix(sk_h2, sk_h3, sk_u.x), sk_u.y);\n" +
		     "        sk_q *= 2.03;\n" +
		     "        sk_amp *= 0.5;\n" +
		     "    }\n" +
		     "}\n" +
		     "float sk_c = smoothstep(0.48, 0.86, sk_p);\n" +
		     "float sk_fade = smoothstep(0.0, 0.16, $0.y);\n" +
		     "float sk_lit = clamp(dot(normalize(vec3($0.x, 0.35, $0.z)), $1) * 0.5 + 0.62, 0.0, 1.0);\n" +
		     "float sk_day = clamp($1.y * 3.0 + 0.35, 0.05, 1.0);\n" +
		     "vec3 sk_cc = mix(vec3(0.30, 0.32, 0.40), vec3(1.0, 0.97, 0.93), sk_lit) * sk_day;\n" +
		     "sk_cc = mix(sk_cc, sk_cc * vec3(1.25, 0.85, 0.62), exp(-abs($1.y) * 6.0) * 0.85);\n" +
		     "return mix($3, sk_cc, sk_c * sk_fade * 0.88);\n",
		     d, s, t, sky, oct));
    }};

    /* --- public entry points (Z-up in, tonemapped out) --------------- */

    public static final Function colA = new Function.Def(VEC3, "sky_colA") {{
	Expression wd = param(IN, VEC3).ref();
	Expression ws = param(IN, VEC3).ref();
	Expression night = param(IN, FLOAT).ref();
	Expression t = param(IN, FLOAT).ref();
	Expression d = id("sk_d"), s = id("sk_s"), col = id("sk_col");
	code.add(raw("vec3 sk_d = $0;\n" +
		     "vec3 sk_s = $1;\n" +
		     "vec3 sk_col = $2 + $3 + $4;\n" +
		     "sk_col = $5;\n" +
		     "return mix($6, vec3(1.0), $7);\n",
		     yup.call(wd), yup.call(ws),
		     baseA.call(d, s), disc.call(d, s), stars.call(d, s, t),
		     clouds.call(d, s, t, col, Cons.l(3)),
		     tone.call(col), night));
    }};

    public static final Function colB = new Function.Def(VEC3, "sky_colB") {{
	Expression wd = param(IN, VEC3).ref();
	Expression ws = param(IN, VEC3).ref();
	Expression night = param(IN, FLOAT).ref();
	Expression t = param(IN, FLOAT).ref();
	Expression d = id("sk_d"), s = id("sk_s"), col = id("sk_col");
	code.add(raw("vec3 sk_d = $0;\n" +
		     "vec3 sk_s = $1;\n" +
		     "vec3 sk_col = $2 + $3 + $4;\n" +
		     "sk_col = $5;\n" +
		     "return mix($6, vec3(1.0), $7);\n",
		     yup.call(wd), yup.call(ws),
		     baseB.call(d, s), disc.call(d, s), stars.call(d, s, t),
		     clouds.call(d, s, t, col, Cons.l(5)),
		     tone.call(col), night));
    }};

    /* Fog colour. Deliberately calls base* (no sun disc) so a 6x overbright
     * disc can never be averaged into the haze, and deliberately shares
     * tone() with col* so fog and sky stay in one colour space. */
    public static final Function horA = new Function.Def(VEC3, "sky_horA") {{
	Expression wd = param(IN, VEC3).ref();
	Expression ws = param(IN, VEC3).ref();
	Expression night = param(IN, FLOAT).ref();
	Expression h = id("sk_h"), s = id("sk_s"), acc = id("sk_acc");
	code.add(raw("vec3 sk_w = $0;\n" +
		     "vec3 sk_s = $1;\n" +
		     "vec3 sk_h = normalize(vec3(sk_w.x, 0.26, sk_w.z));\n" +
		     "vec3 sk_acc = $2;\n" +
		     "return mix($3, vec3(1.0), $4);\n",
		     yup.call(wd), yup.call(ws),
		     baseA.call(h, s),
		     tone.call(desat.call(acc, Cons.l(DESAT))),
		     night));
    }};

    public static final Function horB = new Function.Def(VEC3, "sky_horB") {{
	Expression wd = param(IN, VEC3).ref();
	Expression ws = param(IN, VEC3).ref();
	Expression night = param(IN, FLOAT).ref();
	Expression s = id("sk_s"), acc = id("sk_acc");
	Expression t0 = id("sk_t0"), t1 = id("sk_t1"), t2 = id("sk_t2"), t3 = id("sk_t3"), t4 = id("sk_t4");
	code.add(raw("vec3 sk_w = $0;\n" +
		     "vec3 sk_s = $1;\n" +
		     /* Guard: looking straight down makes sk_w.xz zero, and
		      * normalize(vec3(0)) is NaN -- which then poisons the
		      * mix() in SkyFog even at a fog factor of 0. Reachable
		      * on the free camera at steep elevation. */
		     "vec2 sk_hz = sk_w.xz;\n" +
		     "if(dot(sk_hz, sk_hz) < 1.0e-8) sk_hz = vec2(1.0, 0.0);\n" +
		     "vec3 sk_f = normalize(vec3(sk_hz.x, 0.0, sk_hz.y));\n" +
		     "vec3 sk_t0 = normalize(sk_f + vec3(0.0, 0.02, 0.0));\n" +
		     "vec3 sk_t1 = normalize(sk_f + vec3(0.0, 0.09, 0.0));\n" +
		     "vec3 sk_t2 = normalize(sk_f + vec3(0.0, 0.20, 0.0));\n" +
		     "vec3 sk_t3 = normalize(sk_f + vec3(0.0, 0.36, 0.0));\n" +
		     "vec3 sk_t4 = normalize(sk_f + vec3(0.0, 0.58, 0.0));\n" +
		     "vec3 sk_acc = $2 * 0.16 + $3 * 0.22 + $4 * 0.26 + $5 * 0.22 + $6 * 0.14;\n" +
		     "return mix($7, vec3(1.0), $8);\n",
		     yup.call(wd), yup.call(ws),
		     baseB.call(t0, s), baseB.call(t1, s), baseB.call(t2, s),
		     baseB.call(t3, s), baseB.call(t4, s),
		     tone.call(desat.call(acc, Cons.l(DESAT))),
		     night));
    }};
}
