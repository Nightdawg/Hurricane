package haven;

import haven.render.*;
import haven.res.lib.tree.TreeScale;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class HidingBox extends SlottedNode implements Rendered {
    public boolean filled;
    private static final VertexArray.Layout LAYOUT = new VertexArray.Layout(new VertexArray.Layout.Input(Homo3D.vertex, new VectorFormat(3, NumberFormat.FLOAT32), 0, 0, 12));
	private Model model;
	private final Gob gob;
	private static final Map<String, Model> MODEL_CACHE = new HashMap<>();
	private static final float Z = 0.1f;
	public static final float WIDTH = 2f;

    // Hollow
	public static final Pipe.Op TOP = Pipe.Op.compose(Rendered.last, States.Depthtest.none, States.maskdepth);
	public static Pipe.Op SOLID_HOLLOW = Pipe.Op.compose(Pipe.Op.compose(new BaseColor(new Color(Integer.parseInt(OptWnd.hiddenObjectsColorSetting[0]), Integer.parseInt(OptWnd.hiddenObjectsColorSetting[1]), Integer.parseInt(OptWnd.hiddenObjectsColorSetting[2]), 170)), new States.LineWidth(WIDTH)), TOP);

    // Filled
    public static Pipe.Op SOLID_FILLED = Pipe.Op.compose(new BaseColor(OptWnd.hiddenObjectsColorOptionWidget.currentColor), new States.Facecull(States.Facecull.Mode.NONE), Rendered.last);

    private Pipe.Op state = null;

	private HidingBox(Gob gob, boolean filled) {
		model = getModel(gob, filled);
		this.gob = gob;
		updateState();
        this.filled = filled;
        this.state = filled ? SOLID_FILLED : SOLID_HOLLOW;
	}

	public static HidingBox forGob(Gob gob, boolean filled) {
		try {
			return new HidingBox(gob, filled);
		} catch (Loading ignored) { }
		return null;
	}

	@Override
	public void added(RenderTree.Slot slot) {
		super.added(slot);
		slot.ostate(state);
		updateState();
	}

	@Override
	public void draw(Pipe context, Render out) {
		if(model != null) {
			out.draw(context, model);
		}
	}

	public void updateState() {
		if (this.state != null)
			this.state = filled ? SOLID_FILLED : SOLID_HOLLOW;
		if(model != null && slots != null) {
			try {
				Model m = getModel(gob, filled);
				if(m != null && m != model) {
					model = m;
					slots.forEach(RenderTree.Slot::update);
				}
			}catch (Loading ignored) {}
			for (RenderTree.Slot slot : slots) {
				slot.ostate(state);
			}
		}
	}

	private static Model getModel(Gob gob, boolean filled) {
		Model model = null;
		Resource res = getResource(gob);
        String resName = res.name;
		TreeScale treeScale = null;
		float boxScale = 1.0f;
		boolean growingTreeOrBush = false;
		if ((resName.startsWith("gfx/terobjs/trees") && !resName.endsWith("log") && !resName.endsWith("oldtrunk")) || resName.startsWith("gfx/terobjs/bushes")) {
			treeScale = gob.getattr(TreeScale.class);
			if (treeScale != null) {
				if (treeScale.scale < 1.0f || treeScale.scale > 1.0f) { // ND: Don't care about the original scale, cause the collision always assumes it's a fully grown tree
					boxScale = 1.0f / treeScale.scale;
					growingTreeOrBush = true;
				}
			}
		}
		synchronized (MODEL_CACHE) {
			if (!growingTreeOrBush)
				model = MODEL_CACHE.get(resName + (filled ? "(filled)" : "(hollow)"));
			if(model == null) {
				List<List<Coord3f>> polygons = new LinkedList<>();
				final float boxScaleFinal = boxScale;
				Collection<Resource.Neg> negs = res.layers(Resource.Neg.class);
				if(negs != null) { // ND: This happens for stuff like stockpiles, so we manually draw them I guess
					for (Resource.Neg neg : negs) {
						List<Coord3f> box = new LinkedList<>();
						box.add(new Coord3f(neg.ac.x*boxScaleFinal, -neg.ac.y*boxScaleFinal, Z));
						box.add(new Coord3f(neg.bc.x*boxScaleFinal, -neg.ac.y*boxScaleFinal, Z));
						box.add(new Coord3f(neg.bc.x*boxScaleFinal, -neg.bc.y*boxScaleFinal, Z));
						box.add(new Coord3f(neg.ac.x*boxScaleFinal, -neg.bc.y*boxScaleFinal, Z));
						polygons.add(box);
					}
				}

				Collection<Resource.Obstacle> obstacles = res.layers(Resource.Obstacle.class);
				if(obstacles != null) {
					for (Resource.Obstacle obstacle : obstacles) {
						if("build".equals(obstacle.id)) {continue;}
						for (Coord2d[] polygon : obstacle.p) {
							polygons.add(Arrays.stream(polygon)
									.map(coord2d -> new Coord3f((float) coord2d.x*boxScaleFinal, (float) -coord2d.y*boxScaleFinal, Z))
									.collect(Collectors.toList()));
						}
					}
				}
				if(!polygons.isEmpty()) {
					List<Float> vertices = new LinkedList<>();

					for (List<Coord3f> polygon : polygons) {
						addLoopedVertices(vertices, polygon, filled);
					}

					float[] data = convert(vertices);
					VertexArray.Buffer vbo = new VertexArray.Buffer(data.length * 4, DataBuffer.Usage.STATIC, DataBuffer.Filler.of(data));
					VertexArray va = new VertexArray(LAYOUT, vbo);

					model = new Model((filled ? Model.Mode.TRIANGLE_FAN : Model.Mode.LINES), va, null);
					if (!growingTreeOrBush)
						MODEL_CACHE.put((resName + (filled ? "(filled)" : "(hollow)")), model);
				}
			}
		}
		return model;
	}

	private static float[] convert(List<Float> list) {
		float[] ret = new float[list.size()];
		int i = 0;
		for (Float value : list) {
			ret[i++] = value;
		}
		return ret;
	}

	private static void addLoopedVertices(List<Float> target, List<Coord3f> vertices, boolean filled) {
		int n = vertices.size();
        if (filled) {
            for (int i = 0; i < n; i++) {
				Coord3f a = vertices.get(i);
				Collections.addAll(target, a.x, a.y, a.z);
            }
        } else {
            for (int i = 0; i < n; i++) {
				Coord3f a = vertices.get(i);
				Coord3f b = vertices.get((i + 1) % n);
				Collections.addAll(target, a.x, a.y, a.z);
				Collections.addAll(target, b.x, b.y, b.z);
            }
        }
	}

	private static Resource getResource(Gob gob) {
		Resource res = gob.getres();
		if(res == null) {throw new Loading();}
		Collection<RenderLink.Res> links = res.layers(RenderLink.Res.class);
		for (RenderLink.Res link : links) {
			if(link.l instanceof RenderLink.MeshMat) {
				RenderLink.MeshMat mesh = (RenderLink.MeshMat) link.l;
				return mesh.mesh.get();
			}
		}
		return res;
	}
}
