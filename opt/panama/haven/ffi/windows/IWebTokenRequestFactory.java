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
import static haven.ffi.ABI.*;
import static java.lang.foreign.ValueLayout.ADDRESS;

public interface IWebTokenRequestFactory extends IInspectable {
    static final COM com = COM.get();
    static final WinRT ro = WinRT.get();
    public static final WinRT.HString ACTNAME = ro.HString("Windows.Security.Authentication.Web.Core.WebTokenRequest");
    public static final IID<IWebTokenRequestFactory> IID = new IID<>(IWebTokenRequestFactory.class, 0x6cf2141c_0ff0_4c67l, 0xb84f_99ddbe4a72c9l);

    public static final Descriptor sig_Create = new Descriptor(6, FunctionDescriptor.of(com.HRESULT(), ADDRESS, ADDRESS, ro.HSTRING(), ro.HSTRING(), ADDRESS), ADDRESS);
    public IWebTokenRequest Create(IWebAccountProvider provider, WinRT.HString scope, WinRT.HString clientid);
    public static final Descriptor sig_CreateWithPrompType = new Descriptor(7, FunctionDescriptor.of(com.HRESULT(),ADDRESS,  ADDRESS, ro.HSTRING(), ro.HSTRING(), C_ENUM, ADDRESS), ADDRESS);
    public IWebTokenRequest CreateWithPromptType(IWebAccountProvider provider, WinRT.HString scope, WinRT.HString clientid, int prompttype);
    public static final Descriptor sig_CreateWithProvider = new Descriptor(8, FunctionDescriptor.of(com.HRESULT(), ADDRESS, ADDRESS, ADDRESS), ADDRESS);
    public IWebTokenRequest CreateWithProvider(IWebAccountProvider provider);
    public static final Descriptor sig_CreateWithScope = new Descriptor(9, FunctionDescriptor.of(com.HRESULT(), ADDRESS, ADDRESS, ro.HSTRING(), ADDRESS), ADDRESS);
    public IWebTokenRequest CreateWithScope(IWebAccountProvider provider, WinRT.HString scope);
}
