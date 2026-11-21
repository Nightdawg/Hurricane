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
import haven.ffi.windows.Win32.*;
import haven.ffi.windows.COM.*;
import static haven.ffi.ABI.*;
import static java.lang.foreign.ValueLayout.ADDRESS;

public interface IMap<K, V> extends IInspectable {
    public static final WinRT.PIID<IMap> PIID = new WinRT.PIID<>(IMap.class, 0x3c2925fe_8519_45c1l, 0xaa79_197b6718c1c1l);
    static final COM com = COM.get();

    public static final Descriptor sig_Lookup = new Descriptor(6, FunctionDescriptor.of(com.HRESULT(), ADDRESS, ADDRESS, ADDRESS), ADDRESS);
    public V Lookup(K key);
    public static final Descriptor sig_get_Size = new Descriptor(7, FunctionDescriptor.of(com.HRESULT(), ADDRESS, ADDRESS), ValueLayout.JAVA_INT);
    public int get_Size();
    public static final Descriptor sig_HasKey = new Descriptor(8, FunctionDescriptor.of(com.HRESULT(), ADDRESS, ADDRESS, ADDRESS), C_BOOL);
    public boolean HasKey(K key);
}
