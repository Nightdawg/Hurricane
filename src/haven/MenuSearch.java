package haven;

import java.util.*;
import java.awt.image.BufferedImage;
import haven.MenuGrid.Pagina;
import haven.UI.Grab;
import haven.MenuGrid.Interaction;
import haven.MenuGrid.PagButton;

public class MenuSearch extends Window {
    public final MenuGrid menu;
    public final Results rls;
    public final TextEntry sbox;
    private Pagina root;
    private List<Result> cur = Collections.emptyList();
    private List<Result> filtered = Collections.emptyList();
    private boolean recons = false;
	private Coord drag_start = null;
	private boolean drag_mode = false;
	private Grab grab = null;

    public class Result {
	public final PagButton btn;

	private Result(PagButton btn) {
	    this.btn = btn;
	}
    }

    private static final Text.Foundry elf = CharWnd.attrf;
    private static final int elh = elf.height() + UI.scale(2);
    public class Results extends SListBox<Result, Widget> {
	private Results(Coord sz) {
	    super(sz, elh);
	}

	protected List<Result> items() {return(filtered);}

	protected Widget makeitem(Result el, int idx, Coord sz) {
	    return(new ItemWidget<Result>(this, sz, el) {
		    {
			add(new IconText(sz) {
				protected BufferedImage img() {return(item.btn.img());}
				protected String text() {return(el.btn.name());}
				protected int margin() {return(0);}
				protected Text.Foundry foundry() {return(elf);}
			    }, Coord.z);
		    }

		    @Override public boolean mousedown(MouseDownEvent ev) {
				super.mousedown(ev);
				
				if(ev.b == 1){
					drag_start = ui.mc;
					drag_mode = false;
					grab = ui.grabmouse(this);
				}

				return(true);
		    }

			@Override public void mousemove(MouseMoveEvent ev) {
				if(!drag_mode && drag_start != null && drag_start.dist(ui.mc) > 40) {
					drag_mode = true;
				}
				super.mousemove(ev);
			}
	
			@Override public boolean mouseup(MouseUpEvent ev) {
				if((ev.b == 1) && (grab != null)) {
					if(drag_mode) {
						DropTarget.dropthing(ui.root, ui.mc, rls.sel.btn.pag);
					} else {
						menu.use(rls.sel.btn, new Interaction(), false);
					}
					
					drag_start = null;
					drag_mode = false;
					
					grab.remove();
					grab = null;
	
					setfocus(ui.gui.portrait); // ND: do this to defocus the search box after you select something. It's focusing on your portrait, which does nothing.
				}
				return super.mouseup(ev);
			}

		});
	}

		@Override
		public Object tooltip(Coord c, Widget prev) {
		try {
			int slot = slotat(c);
			final Result item = items().get(slot);
			if (item != null) {
				return new TexI(item.btn.rendertt(true));
			} else {
				return super.tooltip(c, prev);
			}
		} catch (Exception ignored){}
		return null;
		}
    }

    public MenuSearch(MenuGrid menu) {
	super(Coord.z, "Action search");
	this.menu = menu;
	rls = add(new Results(UI.scale(250, 500)), Coord.z);
	sbox = add(new TextEntry(UI.scale(250), "") {
		protected void changed() {
		    refilter();
		}

		public void activate(String text) {
		    if(rls.sel != null)
			menu.use(rls.sel.btn, new MenuGrid.Interaction(1, ui.modflags()), false);
		    if(!ui.modctrl)
			MenuSearch.this.wdgmsg("close");
		}
	    }, 0, rls.sz.y);
	pack();
	setroot(null);
    }

    private void refilter() {
	List<Result> found = Fuzzy.fuzzyFilterAndSort(sbox.text().toLowerCase(), this.cur);
	this.filtered = found;
	int idx = filtered.indexOf(rls.sel);
	if(idx < 0) {
	    if(filtered.size() > 0) {
		rls.change(filtered.get(0));
		rls.display(0);
	    }
	} else {
	    rls.display(idx);
	}
    }

    private void updlist() {
	recons = false;
	Pagina root = this.root;
	List<PagButton> found = new ArrayList<>();
	{
	    Collection<Pagina> leaves = new ArrayList<>();
	    synchronized(menu.paginae) {
		leaves.addAll(menu.paginae);
	    }
	    for(Pagina pag : leaves) {
		try {
		    if(root == null) {
			found.add(pag.button());
		    } else {
			for(Pagina parent = pag; parent != null; parent = parent.parent()) {
			    if(parent == root) {
				found.add(pag.button());
				break;
			    }
			}
		    }
		} catch(Loading l) {
		    recons = true;
		}
	    }
	}
	Collections.sort(found, Comparator.comparing(PagButton::name));
	Map<PagButton, Result> prev = new HashMap<>();
	for(Result pr : this.cur)
	    prev.put(pr.btn, pr);
	List<Result> results = new ArrayList<>();
	for(PagButton btn : found) {
	    Result pr = prev.get(btn);
	    if(pr != null)
		results.add(pr);
	    else
		results.add(new Result(btn));
	}
	this.cur = results;
	refilter();
    }

    public void setroot(Pagina nr) {
	root = nr;
	updlist();
	rls.sb.val = 0;
    }

    public void tick(double dt) {
//	if(menu.cur != root) // ND: commented these 2 lines so the search checks for EVERYTHING, not just the current sub-menu in the menu-grid
//	    setroot(menu.cur);
		if(recons)
			updlist();
		super.tick(dt);
    }

    public boolean keydown(KeyDownEvent ev) {
		if(ev.code == ev.awt.VK_DOWN) {
			int idx = filtered.indexOf(rls.sel);
			if((idx >= 0) && (idx < filtered.size() - 1)) {
				idx++;
				rls.change(filtered.get(idx));
				rls.display(idx);
			}
			return(true);
		} else if(ev.code == ev.awt.VK_UP) {
			int idx = filtered.indexOf(rls.sel);
			if(idx > 0) {
				idx--;
				rls.change(filtered.get(idx));
				rls.display(idx);
			}
			return(true);
		} else {
			return(super.keydown(ev));
		}
    }

	public void draw(GOut g) {
		super.draw(g);
		// Drawing the drag icon
		if(drag_mode && rls.sel != null) {
			GSprite ds = rls.sel.btn.spr();
			ui.drawafter(new UI.AfterDraw() {
				public void draw(GOut g) {
				ds.draw(g.reclip(ui.mc.sub(ds.sz().div(2)), ds.sz()));
				}
			});
		}
	}
}
