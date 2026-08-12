
package haven.sprites;

import haven.*;
import haven.render.*;
import haven.render.sl.*;
import haven.sprites.sky.*;

import java.awt.*;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import static haven.render.sl.Cons.*;

public class SkyBoxSprite extends Sprite {

	/* Loaded on first use, not during class initialisation. A missing or
	 * corrupt galaxy.res used to fail this whole class with
	 * ExceptionInInitializerError, taking the procedural sky -- which
	 * needs no texture at all -- down with it. */
	private static TextureCube.SamplerCube galaxy = null;
	private static boolean galaxyfailed = false;

	/* Blocking: RUtils.CubeFill.mktex does a Loading.waitfor
	 * (RUtils.java:201). Call this from the UI thread only -- never from
	 * a Uniform value lambda, which runs on the render thread every
	 * frame and would both stall it and contend on this monitor.
	 *
	 * Its one caller, SkyboxShader.current(), runs from Sprite.added,
	 * which the render tree invokes with tree.lock() held. So the first
	 * galaxy-mode load still blocks the tree once. That is no worse than
	 * the class-initialiser load it replaces and it happens at most once
	 * per session; fixing it properly means an async load with a
	 * procedural sky shown meanwhile, which is not worth the machinery
	 * here. */
	public static synchronized TextureCube.SamplerCube galaxy() {
		if(galaxyfailed)
			return(null);
		if(galaxy == null) {
			try {
				galaxy = new TextureCube.SamplerCube(new RUtils.CubeFill(() -> Resource.local().loadwait("customclient/skybox/galaxy").layer(Resource.imgc).img).mktex());
				/* ND: The wrapmode fixes the texture edges being off
				 * by 1 pixel. Keep it. */
				galaxy.wrapmode(Texture.Wrapping.CLAMP);
			} catch(RuntimeException e) {
				galaxyfailed = true;
				return(null);
			}
		}
		return(galaxy);
	}
	static final Pipe.Op smat;
	VertexBuf.VertexData posa;
	VertexBuf vbuf;
	Model smod;

	public SkyBoxSprite(final Owner owner, final Resource resource) {
		super(owner, resource);
		init();
	}


	private void init() {
		int max = 6;
		/* Contains the terrain (825 units along an axis) with room to
		 * spare. Do not retune: the cube is player-centred, so it must
		 * exceed FreeCam's 3000-unit zoom-out (MapView.java:304) to keep
		 * the camera inside it, while FollowCam's 2000 far plane
		 * (MapView.java:218) wants it under ~1150. Nothing satisfies
		 * both -- fixing that means anchoring the cube to the camera
		 * instead of the player. */
		float size = 2500.0f;

		FloatBuffer wfbuf = Utils.wfbuf(max * 3 * 2);
		FloatBuffer wfbuf2 = Utils.wfbuf(max * 3 * 2);

		wfbuf.put(0, -size).put(1, -size).put(2, size);
		wfbuf2.put(0, 0).put(1, 0).put(2, 1);

		wfbuf.put(3, size).put(4, -size).put(5, size);
		wfbuf2.put(3, 0).put(4, 0).put(5, 1);

		wfbuf.put(6, size).put(7, size).put(8, size);
		wfbuf2.put(6, 0).put(7, 0).put(8, 1);

		wfbuf.put(9, -size).put(10, size).put(11, size);
		wfbuf2.put(9, 0).put(10, 0).put(11, 1);

		// Bottom face vertices (z = -size):
		wfbuf.put(12, -size).put(13, -size).put(14, -size);
		wfbuf2.put(12, 0).put(13, 0).put(14, -1);

		wfbuf.put(15, size).put(16, -size).put(17, -size);
		wfbuf2.put(15, 0).put(16, 0).put(17, -1);

		wfbuf.put(18, size).put(19, size).put(20, -size);
		wfbuf2.put(18, 0).put(19, 0).put(20, -1);

		wfbuf.put(21, -size).put(22, size).put(23, -size);
		wfbuf2.put(21, 0).put(22, 0).put(23, -1);

//		for (int i = 8; i < 12; i++) {
//			int off = i * 3;
//			wfbuf.put(off, -size).put(off + 1, -size).put(off + 2, size);
//			wfbuf2.put(off, 0).put(off + 1, 0).put(off + 2, 1);
//		}

		this.posa = new VertexBuf.VertexData(wfbuf);
		this.vbuf = new VertexBuf(this.posa, new VertexBuf.NormalData(wfbuf2));

		this.smod = new Model(Model.Mode.TRIANGLES, this.vbuf.data(),
				new Model.Indices(max * 6, NumberFormat.UINT16, DataBuffer.Usage.STATIC, this::sidx));
	}

	private FillBuffer sidx(final Model.Indices indices, final Environment environment) {
		final FillBuffer fillbuf = environment.fillbuf(indices);
		final ShortBuffer sb = fillbuf.push().asShortBuffer();

		for (int i = 0; i < 6; i++) {
			int base = i * 6;
			switch (i) {
				case 0: // top
					sb.put(base, (short) 0).put(base + 1, (short) 1).put(base + 2, (short) 2);
					sb.put(base + 3, (short) 0).put(base + 4, (short) 2).put(base + 5, (short) 3);
					break;
				case 1: // bottom (flipped)
					sb.put(base, (short) 4).put(base + 1, (short) 6).put(base + 2, (short) 5);
					sb.put(base + 3, (short) 4).put(base + 4, (short) 7).put(base + 5, (short) 6);
					break;
				case 2: // front (flipped)
					sb.put(base, (short) 0).put(base + 1, (short) 5).put(base + 2, (short) 1);
					sb.put(base + 3, (short) 0).put(base + 4, (short) 4).put(base + 5, (short) 5);
					break;
				case 3: // back
					sb.put(base, (short) 3).put(base + 1, (short) 2).put(base + 2, (short) 6);
					sb.put(base + 3, (short) 3).put(base + 4, (short) 6).put(base + 5, (short) 7);
					break;
				case 4: // left
					sb.put(base, (short) 0).put(base + 1, (short) 3).put(base + 2, (short) 7);
					sb.put(base + 3, (short) 0).put(base + 4, (short) 7).put(base + 5, (short) 4);
					break;
				case 5: // right (flipped)
					sb.put(base, (short) 1).put(base + 1, (short) 6).put(base + 2, (short) 2);
					sb.put(base + 3, (short) 1).put(base + 4, (short) 5).put(base + 5, (short) 6);
					break;
			}
		}

		return fillbuf;
	}

	public void added(final RenderTree.Slot slot) {
		slot.ostate(Pipe.Op.compose(new States.Facecull(States.Facecull.Mode.FRONT), Location.goback("gobx"),
					    SkyboxShader.current(), Clickable.notClickable));
		slot.add(this.smod, smat);
	}

	static {
		smat = new BaseColor(new Color(255, 255, 255, 255));
	}

	/* Retyped from Slot<State> to Slot<SkyboxShader> so the sampler
	 * uniform can read the resolved cubemap straight off the state. */
	private static final State.Slot<SkyboxShader> surfslot = new State.Slot<>(State.Slot.Type.DRAW, SkyboxShader.class);

	public static class SkyboxShader extends State {
		/* One instance per variant, held statically: the shader program
		 * cache is keyed on ShaderMacro identity, so a fresh macro per
		 * sprite would compile a fresh program per sprite. */
		public static final SkyboxShader sky_a = new SkyboxShader(Mode.SKY_A, null);
		public static final SkyboxShader sky_b = new SkyboxShader(Mode.SKY_B, null);

		public enum Mode {SKY_A, SKY_B, GALAXY}

		/* Picks a variant from the cached preferences. Galaxy degrades to
		 * the procedural sky when its resource is unavailable.
		 *
		 * Called from Sprite.added, i.e. the UI thread -- which is where
		 * the blocking galaxy() load belongs. The resolved sampler is
		 * stored on the returned state, so the uniform never has to load
		 * anything and never sees null. */
		public static SkyboxShader current() {
			if(SkyPalette.style == 1) {
				TextureCube.SamplerCube tex = galaxy();
				if(tex != null)
					return(new SkyboxShader(Mode.GALAXY, tex));
			}
			return(SkyPalette.hq ? sky_b : sky_a);
		}

		public final Mode mode;
		public final TextureCube.SamplerCube tex;
		private final ShaderMacro shader;

		private SkyboxShader(Mode mode, TextureCube.SamplerCube tex) {
			this.mode = mode;
			this.tex = tex;
			this.shader = (mode == Mode.GALAXY) ? galaxymacro : skymacro(mode);
		}

		/* Reads the sampler off the state in the pipe -- no loading, no
		 * lock, no null. */
		private static final Uniform ssky = new Uniform(Type.SAMPLERCUBE, p -> p.get(surfslot).tex, surfslot);

		/* One shared macro instance: shader programs are cached on macro
		 * identity, so a per-state macro would compile a fresh program
		 * every time the galaxy state is rebuilt.
		 *
		 * The bare Homo3D.fragedir(prog.fctx) call is NOT redundant --
		 * see the note below the code. */
		private static final ShaderMacro galaxymacro = prog -> {
			Homo3D.fragedir(prog.fctx);
			FragColor.fragcol(prog.fctx).mod(
				in -> mul(in, textureCube(ssky.ref(), SkyPalette.viewdir(prog.fctx))), 0);
		};

		private static ShaderMacro skymacro(Mode mode) {
			return(prog -> {
				Homo3D.fragedir(prog.fctx);
				Function f = (mode == Mode.SKY_B) ? SkyLib.colB : SkyLib.colA;
				FragColor.fragcol(prog.fctx).mod(
					in -> vec4(f.call(SkyPalette.viewdir(prog.fctx),
							  SkyPalette.u_sundir.ref(),
							  SkyPalette.u_night.ref(),
							  Glob.FrameInfo.globtime()),
						   pick(in, "a")), 0);
			});
		}

		public ShaderMacro shader() {return(shader);}

		public void apply(Pipe buf) {
			buf.put(surfslot, this);
		}
	}

}
