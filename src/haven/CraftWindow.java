package haven;

import java.util.HashMap;
import java.util.Map;

public class CraftWindow extends Window {
	private static final IBox frame = new IBox.Scaled("customclient/tab", "tl", "tr", "bl", "br", "extvl", "extvr", "extht", "exthb");
	private final TabStrip<MenuGrid.Pagina> tabStrip;
	private final Map<MenuGrid.Pagina, TabStrip.Button<MenuGrid.Pagina>> tabs = new HashMap<MenuGrid.Pagina, TabStrip.Button<MenuGrid.Pagina>>();
	public Makewindow makeWidget;
	private MenuGrid.Pagina lastAction;

	public CraftWindow() {
		super(Coord.z, "Crafting");
		tabStrip = add(new TabStrip<MenuGrid.Pagina>() {
			protected void selected(Button<MenuGrid.Pagina> button) {
				for (Map.Entry<MenuGrid.Pagina, Button<MenuGrid.Pagina>> entry : tabs.entrySet()) {
					MenuGrid.Pagina pagina = entry.getKey();
					if (entry.getValue().equals(button) && pagina != lastAction) {
						ui.gui.wdgmsg("act", (Object[])pagina.button().act().ad);
						lastAction = null;
						break;
					}
				}
			}
		});
		setfocusctl(true);
	}

	public void setLastAction(MenuGrid.Pagina value) {
		lastAction = value;
	}

	@Override
	public void wdgmsg(Widget sender, String msg, Object... args) {
		if((sender == this) && (msg.equals("close"))) {
			hide();
		} else {
			super.wdgmsg(sender, msg, args);
		}
	}

	@Override
	public <T extends Widget> T add(T child) {
		child = super.add(child);
		if (child instanceof Makewindow) {
			if (lastAction != null) {
				addTab(lastAction);
			}
			makeWidget = (Makewindow) child;
			makeWidget.c = new Coord(5, tabStrip.sz.y + 5);
			makeWidget.resize(Math.max(makeWidget.sz.x, tabStrip.sz.x), makeWidget.sz.y);
		}
		return child;
	}

	@Override
	public void cdestroy(Widget w) {
		if (makeWidget == w) {
			makeWidget = null;
			if (visible)
				hide();
		}
	}

	@Override
	public void cdraw(GOut g) {
		super.cdraw(g);
		frame.draw(g, new Coord(0, Math.max(0, tabStrip.sz.y - 1)), csz().sub(0, tabStrip.sz.y));
	}

	@Override
	public void resize(Coord sz) {
		super.resize(sz.add(5, 5));
	}


	@Override
	public void hide() {
		super.hide();
		if (makeWidget != null)
			makeWidget.wdgmsg("close");
	}


	private void addTab(MenuGrid.Pagina pagina) {
		if (tabs.containsKey(pagina)) {
			TabStrip.Button<MenuGrid.Pagina> old = tabs.get(pagina);
			tabStrip.remove(old);
		}
		Tex icon = new TexI(PUtils.convolvedown(lastAction.res.get().layer(Resource.imgc).img, UI.scale(new Coord(26, 26)), CharWnd.iconfilter));
		String text = "";
		TabStrip.Button<MenuGrid.Pagina> added = tabStrip.insert(0, icon, text, pagina);
		added.tag = pagina;
		tabStrip.select(added);
		if (tabStrip.getButtonCount() > 12) {
			removeTab(tabStrip.getButtonCount() - 1);
		}
		tabs.put(lastAction, added);
        this.getparent(GameUI.class).setfocus(this);
	}

	private void removeTab(int index) {
		TabStrip.Button<MenuGrid.Pagina> removed = tabStrip.remove(index);
		tabs.values().remove(removed);
	}
}
