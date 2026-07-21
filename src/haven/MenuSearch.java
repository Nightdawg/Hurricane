/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import java.util.*;
import java.awt.image.BufferedImage;
import haven.MenuGrid.Pagina;
import haven.UI.Grab;
import haven.MenuGrid.Interaction;
import haven.MenuGrid.PagButton;

public abstract class MenuSearch extends Window {
    public final MenuGrid menu;
    public final Results rls;
    public final TextEntry sbox;
    protected List<Result> cur = Collections.emptyList();
    protected List<Result> filtered = Collections.emptyList();
    private boolean recons = true;
    private Coord drag_start = null;
    private boolean drag_mode = false;
    private Grab grab = null;

    public class Result {
	public final PagButton btn;

	protected Result(PagButton btn) {
	    this.btn = btn;
	}
    }

    public static final Text.Foundry elf = CharWnd.attrf;
    public static final int elh = elf.height() + UI.scale(2);
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

		    public boolean mousedown(MouseDownEvent ev) {
				super.mousedown(ev);

				if(ev.b == 1){
					drag_start = ui.mc;
					drag_mode = false;
					grab = ui.grabmouse(this);
				}

				return(true);
		    }

			public void mousemove(MouseMoveEvent ev) {
				if(!drag_mode && drag_start != null && drag_start.dist(ui.mc) > 40) {
					drag_mode = true;
				}
				super.mousemove(ev);
			}

			public boolean mouseup(MouseUpEvent ev) {
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

    public MenuSearch(String title, MenuGrid menu) {
	super(Coord.z, title);
	this.menu = menu;
	rls = add(new Results(UI.scale(250, 500)), Coord.z);
	sbox = add(new TextEntry(UI.scale(250), "") {
		protected void changed() {
		    refilter();
		}

		public void activate(String text) {
		    if(rls.sel != null)
			menu.use(rls.sel.btn, new MenuGrid.Interaction(1, ui.modflags()), false);
		    if(!ui.modctrl) {
			reqclose();
			settext("");
			refilter();
		    }
		}
	    }, 0, rls.sz.y);
	pack();
    }

    public MenuSearch(MenuGrid menu) {
	this("Action search", menu);
    }

    protected void refilter() {
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

    protected abstract boolean generate(List<PagButton> buf);

    protected void updlist() {
	recons = false;
	List<PagButton> buf = new ArrayList<>();
	if(generate(buf))
	    recons = true;
	Map<PagButton, Result> prev = new HashMap<>();
	for(Result pr : this.cur)
	    prev.put(pr.btn, pr);
	List<Result> results = new ArrayList<>();
	for(PagButton btn : buf) {
	    Result pr = prev.get(btn);
	    if(pr != null)
		results.add(pr);
	    else
		results.add(new Result(btn));
	}
	this.cur = results;
	refilter();
    }

    protected void recons() {
	recons = true;
    }

    public void tick(TickEvent ev) {
	if(ev.visible && recons) {
	    updlist();
	}
	super.tick(ev);
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

    public static class Main extends MenuSearch {
	private Pagina root;
	private int pagseq;

	public static final KeyBinding kb_itemcraft = KeyBinding.get("scm-itemcraft", KeyMatch.nil);
	public Main(MenuGrid menu) {
	    super(menu);
	    setroot(null);
	    add(new Button(sbox.sz.x, "Search by ingredient", false).action(() -> menu.wdgmsg("act", "itemcraft")),
		sbox.pos("bl").adds(0, 5)).setgkey(kb_itemcraft);
	    pagseq = menu.pagseq;
	    pack();
	}

	protected boolean generate(List<PagButton> buf) {
	    boolean recons = false;
	    Pagina root = this.root;
	    Collection<Pagina> leaves = new ArrayList<>();
	    synchronized(menu.paginae) {
		leaves.addAll(menu.paginae);
	    }
	    for(Pagina pag : leaves) {
		try {
		    if(root == null) {
			buf.add(pag.button());
		    } else {
			for(Pagina parent = pag; parent != null; parent = parent.parent()) {
			    if(parent == root) {
				buf.add(pag.button());
				break;
			    }
			}
		    }
		} catch(Loading l) {
		    recons = true;
		}
	    }
	    Collections.sort(buf, Comparator.comparing(PagButton::name));
	    return(recons);
	}

	public void setroot(Pagina nr) {
	    root = nr;
	    recons();
	    rls.sb.val = 0;
	}

	public void tick(TickEvent ev) {
	    if(ev.visible) {
		if(pagseq != menu.pagseq) {
		    recons();
		    pagseq = menu.pagseq;
		}
	    }
	    super.tick(ev);
	}
    }
}
