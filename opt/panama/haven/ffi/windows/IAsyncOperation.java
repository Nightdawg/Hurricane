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
import static java.lang.foreign.ValueLayout.ADDRESS;

public interface IAsyncOperation<R> extends IInspectable {
    public static final int Started   = 0;
    public static final int Completed = 1;
    public static final int Canceled  = 2;
    public static final int Error     = 3;
    public static final WinRT.PIID<IAsyncOperation> PIID = new WinRT.PIID<>(IAsyncOperation.class, 0x9fc2b0bb_e446_44e2l, 0xaa61_9cab8f636af2l);
    static final COM com = COM.get();

    public static final Descriptor sig_put_Completed = new Descriptor(6, FunctionDescriptor.of(com.HRESULT(), ADDRESS, ADDRESS));
    public void put_Completed(IAsyncOperationCompletedHandler<R> handler);
    public static final Descriptor sig_get_Completed = new Descriptor(7, FunctionDescriptor.of(com.HRESULT(), ADDRESS, ADDRESS), ADDRESS);
    public IAsyncOperationCompletedHandler<R> get_Completed();
    public static final Descriptor sig_GetResults = new Descriptor(8, FunctionDescriptor.of(com.HRESULT(), ADDRESS, ADDRESS), ADDRESS);
    public R GetResults();
}
