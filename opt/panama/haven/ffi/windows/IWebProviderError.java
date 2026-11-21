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

import java.lang.invoke.*;
import java.lang.foreign.*;
import haven.ffi.windows.Win32.*;
import haven.ffi.windows.COM.*;
import haven.ffi.windows.WinRT.*;
import static haven.ffi.ABI.*;
import static java.lang.foreign.ValueLayout.ADDRESS;

public interface IWebProviderError extends IInspectable {
    public static final IID<IWebProviderError> IID = new IID<>(IWebProviderError.class, 0xdb191bb1_50c5_4809l, 0x8dca_09c99410245cl);
    public static final String RTNAME = "Windows.Security.Authentication.Web.Core.WebProviderResult";
    static final COM com = COM.get();
    static final WinRT ro = WinRT.get();

    public static final Descriptor sig_get_ErrorCode = new Descriptor(6, FunctionDescriptor.of(com.HRESULT(), ADDRESS, ADDRESS), ValueLayout.JAVA_INT);
    public int get_ErrorCode();
    public static final Descriptor sig_get_ErrorMessage = new Descriptor(7, FunctionDescriptor.of(com.HRESULT(), ADDRESS, ADDRESS), ro.HSTRING());
    public WinRT.HString get_ErrorMessage();
    public static final Descriptor sig_get_Properties = new Descriptor(8, FunctionDescriptor.of(com.HRESULT(), ADDRESS, ADDRESS), ADDRESS);
    public IMap<HString, HString> get_Properties();
}
