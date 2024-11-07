package haven;

import haven.render.*;
import haven.res.lib.tree.TreeScale;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class CollisionBox extends SlottedNode implements Rendered {
    private static final VertexArray.Layout LAYOUT = new VertexArray.Layout(new VertexArray.Layout.Input(Homo3D.vertex, new VectorFormat(3, NumberFormat.FLOAT32), 0, 0, 12));
	private Model model;
	private final Gob gob;
	public static final Map<String, Model> MODEL_CACHE = new HashMap<>();
	private static final float Z = 0.2f;
	public static final float WIDTH = 2f;

	public static final Pipe.Op TOP = Pipe.Op.compose(Rendered.last, States.Depthtest.none, States.maskdepth);
	public static Pipe.Op SOLID_HOLLOW = Pipe.Op.compose(new ColorMask(OptWnd.collisionBoxColorOptionWidget.currentColor), new States.LineWidth(WIDTH), TOP);

    private Pipe.Op state = null;

	private CollisionBox(Gob gob) {
		model = getModel(gob);
		this.gob = gob;
		updateState();
        this.state = SOLID_HOLLOW;
	}

	public static CollisionBox forGob(Gob gob) {
		try {
			return new CollisionBox(gob);
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
		this.state = SOLID_HOLLOW;
		if (gob.getres() != null && gob.getres().name.equals("gfx/terobjs/cupboard")) {
			model = getModel(gob);
			if (OptWnd.flatCupboardsCheckBox.a)
				this.state = Pipe.Op.compose(SOLID_HOLLOW, Location.rot(new Coord3f(0, 1, 0), 4.712f), Location.scale(1.615f, 1, 1), Location.xlate(new Coord3f(5.45f, 0, 4.7f)));
			else
				this.state = SOLID_HOLLOW;
		}
		if(model != null && slots != null) {
			try {
				Model m = getModel(gob);
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

	private static Model getModel(Gob gob) {
		Model model = null;
		Coord bboxa = new Coord(0,0);
		Coord bboxb = new Coord(0,0);
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
		boolean aurochsSpecialCase = false;
		if(res.name.endsWith("/cattle")){
			for (GAttrib g : gob.attr.values()) {
				if (g instanceof Drawable) {
					if (g instanceof Composite) {
						Composite c = (Composite) g;
						if (c.comp.cmod.size() > 0) {
							for (Composited.MD item : c.comp.cmod) {
								if (item.mod.get().basename().equals("aurochs")){
									growingTreeOrBush = true;
									aurochsSpecialCase = true;
								}
							}
						}
					}
				}
			}
		}
		synchronized (MODEL_CACHE) {
			if (!growingTreeOrBush)
				model = MODEL_CACHE.get(resName);
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
						bboxa.x = neg.ac.x;
						bboxa.y = neg.ac.y;
						bboxb.x = neg.bc.x;
						bboxb.y = neg.bc.y;
						if (!aurochsSpecialCase) {
							polygons.add(box);
						}
					}
				}

				Collection<Resource.Obstacle> obstacles = res.layers(Resource.Obstacle.class);
				if(obstacles != null) {
					Optional<Coord2d> minX;
					Optional<Coord2d> minY;
					Optional<Coord2d> maxX;
					Optional<Coord2d> maxY;
					for (Resource.Obstacle obstacle : obstacles) {
						if("build".equals(obstacle.id)) {continue;}
						for (Coord2d[] polygon : obstacle.p) {
							minX = Arrays.stream(polygon)
									.min(Comparator.comparingDouble(Coord2d::getX));
							minY = Arrays.stream(polygon)
									.min(Comparator.comparingDouble(Coord2d::getY));

							maxX = Arrays.stream(polygon)
									.max(Comparator.comparingDouble(Coord2d::getX));
							maxY = Arrays.stream(polygon)
									.max(Comparator.comparingDouble(Coord2d::getY));
							bboxa.x = (int) minX.get().getX();
							bboxa.y = (int) minY.get().getX();
							bboxb.x = (int) maxX.get().getX();
							bboxb.y = (int) maxY.get().getY();
							if (!aurochsSpecialCase) {
								polygons.add(Arrays.stream(polygon)
										.map(coord2d -> new Coord3f((float) coord2d.x * boxScaleFinal, (float) -coord2d.y * boxScaleFinal, Z))
										.collect(Collectors.toList()));
							}
						}
					}
				}
				if (polygons.isEmpty()){
					List<List<Coord3f>> polygons2 = new LinkedList<>();
					List<Coord3f> box = new LinkedList<>();
					float ax = 0, bx = 0, ay = 0, by = 0;
					if (res.name.startsWith("gfx/kritter/cattle/calf")) {
						ax = -9F; bx = 9F; ay = -3F; by = 3F;
					} else if (res.name.startsWith("gfx/kritter/sheep/lamb")) {
						ax = -4F; bx = 5F; ay = -2F; by = 2F;
					} else if (res.name.startsWith("gfx/kritter/goat/")) {
						ax = -3F; bx = 4F; ay = -2F; by = 2F;
					} else if (res.name.startsWith("gfx/kritter/pig/")) {
						ax = -6F; bx = 6F; ay = -3F; by = 3F;
					} else if (res.name.startsWith("gfx/kritter/horse/")) {
						ax = -8F; bx = 8F; ay = -4F; by = 4F;
					} else if (res.name.startsWith("gfx/kritter/cattle/cattle")) {
						if (aurochsSpecialCase){
							ax = -11.8F; bx = 11.8F; ay = -3.8F; by = 3.8F;
						}
					}
					if (ax != 0 && bx != 0 && ay != 0 && by != 0) {
						box.add(new Coord3f(ax, -ay, Z));
						box.add(new Coord3f(bx, -ay, Z));
						box.add(new Coord3f(bx, -by, Z));
						box.add(new Coord3f(ax, -by, Z));
						bboxa.x = (int)ax;
						bboxa.y = (int)ay;
						bboxb.x = (int)bx;
						bboxb.y = (int)by;
						polygons2.add(box);
						List<Float> vertices = new LinkedList<>();
						for (List<Coord3f> polygon : polygons2) {
							addLoopedVertices(vertices, polygon);
						}
						float[] data = convert(vertices);
						VertexArray.Buffer vbo = new VertexArray.Buffer(data.length * 4, DataBuffer.Usage.STATIC, DataBuffer.Filler.of(data));
						VertexArray va = new VertexArray(LAYOUT, vbo);
						model = new Model(Model.Mode.LINES, va, null);
						model.bbox = new Model.BoundingBox(bboxa, bboxb);
						if (!growingTreeOrBush)
							MODEL_CACHE.put(resName, model);
					}
				}
				if(!polygons.isEmpty()) {
					List<Float> vertices = new LinkedList<>();

					for (List<Coord3f> polygon : polygons) {
						addLoopedVertices(vertices, polygon);
					}

					float[] data = convert(vertices);
					VertexArray.Buffer vbo = new VertexArray.Buffer(data.length * 4, DataBuffer.Usage.STATIC, DataBuffer.Filler.of(data));
					VertexArray va = new VertexArray(LAYOUT, vbo);

					model = new Model((Model.Mode.LINES), va, null);
					model.bbox = new Model.BoundingBox(bboxa, bboxb);
					if (!growingTreeOrBush)
						MODEL_CACHE.put(resName, model);
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

	private static void addLoopedVertices(List<Float> target, List<Coord3f> vertices) {
		int n = vertices.size();
		for (int i = 0; i < n; i++) {
			Coord3f a = vertices.get(i);
			Coord3f b = vertices.get((i + 1) % n);
			Collections.addAll(target, a.x, a.y, a.z);
			Collections.addAll(target, b.x, b.y, b.z);
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
