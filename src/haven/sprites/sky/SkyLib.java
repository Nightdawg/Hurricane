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

    /* --- where the sky sits on the screen ---------------------------- */

    /* This game's camera never shows the horizon. FreeCam defaults to 45
     * degrees above the player (MapView.java:287) with a 15-degree vertical
     * half-field (MapView.java:124-128), so every ray on screen points 30 to
     * 60 degrees BELOW horizontal. Feeding those directions to sky maths
     * gives one flat colour, which is what the first revisions rendered.
     *
     * So the sky's elevation is driven by the ray's angle above the camera
     * axis instead of by its world elevation, stretched by GAIN and dropped
     * by TILT. Azimuth still comes from the world, so the sun stays where the
     * shadows put it and the sky turns with the camera.
     *
     * GAIN stretches the 30-degree field into roughly 90 degrees of sky, so
     * the gradient reads instead of being a slice. TILT then puts the sky's
     * own horizon just under where the loaded terrain ends -- around 30% down
     * the screen at the default zoom -- so the band the player actually sees
     * is horizon haze at the bottom and open sky above it.
     *
     * Both fade out as the camera levels off (see sky_elev): at zero pitch
     * the real horizon IS on screen, and the ray's own elevation is then the
     * right answer with no stretching at all. */
    public static final double GAIN = 3.0;
    public static final double TILT = -0.227;   /* radians, about -13 degrees */

    /* Sky elevation, in radians, for one fragment.
     *
     * ey, ez are the y and z of its eye-space position: atan(ey, -ez) is the
     * angle above the camera axis, and its iso-lines are exactly horizontal
     * screen rows. (Taking asin of the normalised direction instead bows them
     * into arcs, which renders as a curved seam across the sky.)
     *
     * pitch is how far the camera looks down, in radians. */
    public static final Function elev = new Function.Def(FLOAT, "sky_elev") {{
	Expression ey = param(IN, FLOAT).ref();
	Expression ez = param(IN, FLOAT).ref();
	Expression pitch = param(IN, FLOAT).ref();
	code.add(raw("float sk_a = atan($0, -$1);\n" +
		     "float sk_k = clamp($2 / 0.7853982, 0.0, 1.0);\n" +
		     "return (sk_a * mix(1.0, " + GAIN + ", sk_k)) + (" + TILT + " * sk_k);\n",
		     ey, ez, pitch));
    }};

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
	/* The day zenith is deeper, and the exponent higher, than the
	 * prototype's. The prototype was judged on a full sky dome; here only
	 * the 0-to-30-degree band above the terrain is ever on screen, and at
	 * 0.42 that band was almost entirely the pale horizon end of the
	 * gradient -- measured 12 points of separation across the whole
	 * visible sky. 0.75 moves the pale part down into the strip the fog
	 * covers and leaves open blue above it. */
	code.add(raw("float sk_day = clamp($1.y * 2.5 + 0.25, 0.0, 1.0);\n" +
		     "vec3 sk_zen = mix(vec3(0.015, 0.020, 0.055), vec3(0.085, 0.27, 0.72), sk_day);\n" +
		     "vec3 sk_hor = mix(vec3(0.045, 0.050, 0.090), vec3(0.70, 0.82, 0.95), sk_day);\n" +
		     "float sk_t = pow(clamp($0.y, 0.0, 1.0), 0.75);\n" +
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
	/* The 0.45 is the cloud deck's height over its own distance: raising
	 * it from 0.12 stops the pattern piling up at the horizon and spreads
	 * it across the band that is actually on screen.
	 *
	 * The 4.5 is frequency. 1.6 put well under one noise cell on screen, so
	 * the clouds resolved to a single flat wash, and 2.0 was barely better:
	 * the visible band spans 2.6 by 1.6 cells there, so one cloud is a blur
	 * covering half the screen with no shape to read. 4.5 gives several
	 * masses with sky between them. (6.0 goes too far the other way, into
	 * roughly twenty features across the window, which reads as grain.) */
	/* The wind is added OUTSIDE the frequency multiply, and that placement
	 * is load-bearing. It used to be inside, which tied drift speed to
	 * cloud size: raising the frequency from 2.0 to 4.5 to give the clouds
	 * shape silently made them drift 2.25 times faster as well. Keep them
	 * separate so either can be tuned without touching the other.
	 *
	 * The constants are in noise cells per GAME second, and game time runs
	 * fast: measured 3.56 game-seconds per real second off the in-game
	 * clock (15:12:10 to 15:12:38 across a 7.87 s capture), which matches
	 * Glob.itimefac = 3.0 plus the server's own rate.
	 *
	 * The old 0.010 therefore worked out at 0.045 * 3.56 = 0.160 cells per
	 * REAL second. The visible band is about 4.3 cells wide, so a cloud
	 * crossed the screen in 27 seconds -- measured independently by block
	 * correlation on a capture at 65 to 90 px/s. Real clouds take minutes:
	 * a deck at 2 km under a 10 m/s wind sweeps this 53-degree field in
	 * roughly 380 s. 0.0050 gives 243 s, close to that and still plainly
	 * moving -- 7.8 px/s, so a cloud shifts its own width in about a
	 * minute. */
	code.add(raw("if($0.y <= 0.005) return $3;\n" +
		     "vec2 sk_uv = $0.xz / ($0.y + 0.45) * (0.55 * 4.5) + vec2($2 * 0.0050, $2 * 0.0020);\n" +
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
		     /* Thresholds measured over 300k samples of this noise
		      * (four octaves: mean 0.469, standard deviation 0.123).
		      *
		      * 0.48 to 0.86 was the prototype's: the upper bound sat
		      * above the noise's own maximum, so coverage came out at
		      * 5.8% and the sky was in practice cloudless. 0.38 to 0.58
		      * fixed the coverage but the ramp was 1.6 standard
		      * deviations wide, so 30% of the sky landed at a partial
		      * value -- neither cloud nor clear, which renders as a
		      * diffuse veil rather than as clouds. This window is half
		      * a standard deviation: 30% coverage, 8% partial. */
		     "float sk_c = smoothstep(0.51, 0.57, sk_p);\n" +
		     "float sk_fade = smoothstep(0.0, 0.10, $0.y);\n" +
		     /* The lit colour has to be well over 1, and this is why:
		      * tone() is Reinhard with a white point of 0.72, so a cloud
		      * at 1.0 linear lands within a couple of levels of the sky
		      * it sits on. Measured on the day sky at the elevations the
		      * camera actually shows -- sky (151, 169, 191), cloud
		      * (177, 177, 179) facing away from the sun -- the whole
		      * difference was 10 levels of luminance, and mostly a loss
		      * of saturation rather than a gain of brightness. Clouds
		      * were being drawn over about 40% of the sky and read as
		      * flat grey. At 2.2 the same comparison gives 29 levels
		      * away from the sun and 53 toward it.
		      *
		      * The bias drops 0.62 to 0.55 so the lit term spans more of
		      * its range instead of saturating; that is what puts shape
		      * inside a cloud rather than one even tone. */
		     "float sk_lit = clamp(dot(normalize(vec3($0.x, 0.35, $0.z)), $1) * 0.5 + 0.55, 0.0, 1.0);\n" +
		     "float sk_day = clamp($1.y * 3.0 + 0.35, 0.05, 1.0);\n" +
		     "vec3 sk_cc = mix(vec3(0.30, 0.32, 0.40), vec3(2.20, 2.13, 2.02), sk_lit) * sk_day;\n" +
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
	Expression e = param(IN, FLOAT).ref();
	Expression d = id("sk_d"), s = id("sk_s"), col = id("sk_col");
	code.add(raw("vec3 sk_d = $0;\n" +
		     "vec3 sk_s = $1;\n" +
		     /* Rebuild the ray: elevation from sky_elev, azimuth kept
		      * from the world. An earlier revision used the elevation's
		      * SINE here rather than its angle, which put the top of
		      * the screen near the zenith and squeezed the whole
		      * azimuth circle -- clouds and stars then covered a
		      * fraction of one noise cell and rendered as flat wash. */
		     "float sk_e = clamp($8, -1.5533, 1.5533);\n" +
		     "vec2 sk_hz = vec2(sk_d.x, sk_d.z);\n" +
		     "float sk_hl = length(sk_hz);\n" +
		     "sk_hz = (sk_hl < 1.0e-5) ? vec2(1.0, 0.0) : (sk_hz / sk_hl);\n" +
		     "sk_hz *= cos(sk_e);\n" +
		     "sk_d = vec3(sk_hz.x, sin(sk_e), sk_hz.y);\n" +
		     "vec3 sk_col = $2 + $3 + $4;\n" +
		     "sk_col = $5;\n" +
		     "return mix($6, vec3(1.0), $7);\n",
		     yup.call(wd), yup.call(ws),
		     baseA.call(d, s), disc.call(d, s), stars.call(d, s, t),
		     clouds.call(d, s, t, col, Cons.l(4)),
		     tone.call(col), night, e));
    }};

    public static final Function colB = new Function.Def(VEC3, "sky_colB") {{
	Expression wd = param(IN, VEC3).ref();
	Expression ws = param(IN, VEC3).ref();
	Expression night = param(IN, FLOAT).ref();
	Expression t = param(IN, FLOAT).ref();
	Expression e = param(IN, FLOAT).ref();
	Expression d = id("sk_d"), s = id("sk_s"), col = id("sk_col");
	code.add(raw("vec3 sk_d = $0;\n" +
		     "vec3 sk_s = $1;\n" +
		     /* Same rebuild as colA -- see the note there. */
		     "float sk_e = clamp($8, -1.5533, 1.5533);\n" +
		     "vec2 sk_hz = vec2(sk_d.x, sk_d.z);\n" +
		     "float sk_hl = length(sk_hz);\n" +
		     "sk_hz = (sk_hl < 1.0e-5) ? vec2(1.0, 0.0) : (sk_hz / sk_hl);\n" +
		     "sk_hz *= cos(sk_e);\n" +
		     "sk_d = vec3(sk_hz.x, sin(sk_e), sk_hz.y);\n" +
		     "vec3 sk_col = $2 + $3 + $4;\n" +
		     "sk_col = $5;\n" +
		     "return mix($6, vec3(1.0), $7);\n",
		     yup.call(wd), yup.call(ws),
		     baseB.call(d, s), disc.call(d, s), stars.call(d, s, t),
		     clouds.call(d, s, t, col, Cons.l(5)),
		     tone.call(col), night, e));
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
		     /* Low, because the fog has to meet the drawn sky where the
		      * terrain stops -- a few degrees over the horizon, not the
		      * 20 the old 0.26 worked out to. Too high and the fog
		      * lands visibly bluer than the sky it is supposed to
		      * dissolve into. */
		     "vec3 sk_h = normalize(vec3(sk_w.x, 0.12, sk_w.z));\n" +
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
