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
import haven.render.*;

public class StaticSprite extends Sprite {
    public final RenderTree.Node[] parts;
    
    public static final Factory fact = new Factory() {
	    public Sprite create(Owner owner, Resource res, Message sdt) {
		if((res.layer(FastMesh.MeshRes.class) != null) ||
		   (res.layer(RenderLink.Res.class) != null))
		    return(new StaticSprite(owner, res, sdt) {
			    public String toString() {
				return(String.format("#<static-sprite %s>", res.name));
			    }
			});
		return(null);
	    }
	};
    
    public StaticSprite(Owner owner, Resource res, RenderTree.Node[] parts) {
	super(owner, res);
	this.parts = parts;
    }

    public StaticSprite(Owner owner, Resource res, RenderTree.Node part) {
	this(owner, res, new RenderTree.Node[] {part});
    }
    
    public StaticSprite(Owner owner, Resource res, Message sdt) {
	super(owner, res);
	this.parts = lsparts(new RecOwner(), res, sdt);
    }
    
    public static RenderTree.Node[] lsparts(Owner owner, Resource res, Message sdt) {
	int fl = sdt.eom() ? 0xffff0000 : decnum(sdt);
	Collection<RenderTree.Node> rl = new LinkedList<>();
	for(FastMesh.MeshRes mr : res.layers(FastMesh.MeshRes.class)) {
	    if((mr.mat != null) && ((mr.id < 0) || (((1 << mr.id) & fl) != 0)))
		rl.add(mr.mat.get().apply(mr.m));
	}
	for(RenderLink.Res lr : res.layers(RenderLink.Res.class)) {
	    if((lr.id < 0) || (((1 << lr.id) & fl) != 0))
		rl.add(lr.l.make(owner));
	}
	if(res.layer(Audio.clip, "amb") != null)
	    rl.add(new ActAudio.Ambience(res));
	return(rl.toArray(new RenderTree.Node[0]));
    }

    public static RenderTree.Node[] lsparts(Resource res, Message sdt) {
	return(lsparts(null, res, sdt));
    }

    public void added(RenderTree.Slot slot) {
		if (!(owner instanceof ResDrawable)) {
			if (this.res.name.equals("gfx/terobjs/items/hats/magahat") || this.res.name.equals("gfx/terobjs/items/hats/darkmagahat")) {
				slot.cstate(Pipe.Op.compose(Location.rot(new Coord3f(0, 0, 1), 3.15f)));
			} else if (this.res.name.equals("gfx/terobjs/items/hats/caimancap")) {
				slot.cstate(Pipe.Op.compose(Location.rot(new Coord3f(0, 0, 1), 3.15f), Location.scale(1f, 1f, 1.05f), Location.rot(new Coord3f(0, 1, 0), 0.2f)));
			} else if (this.res.name.equals("gfx/terobjs/items/hats/tricorne")) { // ND: Jacob likes this hat
				slot.cstate(Pipe.Op.compose(Location.xlate(new Coord3f(0.2f, 0, 0)), Location.scale(1.17f, 1f, 1.2f)));
			}
		}
	for(RenderTree.Node p : parts)
	    slot.add(p);
    }
}
