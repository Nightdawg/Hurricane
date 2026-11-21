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

package haven.ffi.windows;

import java.util.*;
import java.lang.invoke.*;
import java.lang.foreign.*;
import haven.ffi.*;
import haven.ffi.windows.Win32.*;
import haven.ffi.windows.COM.*;
import haven.ffi.windows.WinRT.*;
import static haven.ffi.FUtils.*;
import static java.lang.foreign.ValueLayout.ADDRESS;

public interface IInspectable extends IUnknown {
    static final COM com = COM.get();
    static final WinRT ro = WinRT.get();
    public static final IID<IInspectable> IID = new IID<>(IInspectable.class, 0xaf86e2e0_b12d_4c6al, 0x9c5a_d7aa65101e90l);

    public static final Descriptor sig_GetIids = new Descriptor(3, FunctionDescriptor.of(com.HRESULT(), ADDRESS, ADDRESS, ADDRESS)).upcall(UpcallAdapter.nil);
    public UUID[] GetIids();
    public static UUID[] downcall_GetIids(COM.Downcall inf, MemorySegment self) {
	try(Arena st = Arena.ofConfined()) {
	    MemorySegment nbuf = st.allocate(com.ULONG()), lbuf = st.allocate(ADDRESS);
	    int rv;
	    try {
		rv = (int)inf.fun.invoke(inf.com.getvtab(self, sig_GetIids.vindex), self, nbuf, lbuf);
	    } catch(Throwable t) {
		throw(new InvocationException(t));
	    }
	    if(!inf.com.SUCCEEDED(rv))
		throw(new HResultError(rv));
	    UUID[] ret = new UUID[(int)getint(nbuf, 0, com.ULONG(), false)];
	    MemorySegment list = lbuf.get(ADDRESS, 0).reinterpret(ADDRESS.byteSize() * 16);
	    for(int i = 0; i < ret.length; i++)
		ret[i] = COM.guidmem(list.asSlice(i * 16));
	    inf.com.CoTaskMemFree(list);
	    return(ret);
	}
    }

    public static final Descriptor sig_GetRuntimeClassName = new Descriptor(4, FunctionDescriptor.of(com.HRESULT(), ADDRESS, ADDRESS), ro.HSTRING());
    public HString GetRuntimeClassName();
}
