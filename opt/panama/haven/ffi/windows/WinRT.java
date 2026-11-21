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

import haven.*;
import haven.ffi.*;
import java.util.*;
import java.util.function.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.nio.*;
import java.lang.invoke.*;
import java.lang.foreign.*;
import java.lang.ref.*;
import java.lang.reflect.*;
import java.lang.annotation.*;
import java.nio.charset.Charset;
import java.lang.foreign.MemoryLayout.PathElement;
import static haven.ffi.ABI.*;
import static haven.ffi.FUtils.*;
import static haven.ffi.windows.Win32.*;
import static haven.ffi.windows.COM.*;
import static java.lang.foreign.ValueLayout.ADDRESS;

public abstract class WinRT {
    public static final UUID TYPE_NS = new UUID(0x11f47ad5_7b73_42c0l, 0xabae_878b1e16adeel);
    public static final int RO_INIT_SINGLETHREADED = 0;
    public static final int RO_INIT_MULTITHREADED  = 1;

    public static interface HString {
    }
    public abstract HString HString(String str);
    public abstract MemoryLayout HSTRING();

    public abstract void RoUninitialize();
    public abstract void RoInitialize(int init);
    public abstract <I extends IUnknown> I RoGetActivationFactory(WinRT.HString id, IID<I> iid);

    public <I extends IUnknown> I RoGetActivationFactory(Class<I> cl) {
	IID<I> iid = IID.get(cl).cast(cl);
	HString actnm;
	try {
	    Field nmf = cl.getDeclaredField("ACTNAME");
	    actnm = (HString)nmf.get(null);
	} catch(NoSuchFieldException e) {
	    throw(new ClassCastException("not a declared WinRT class: " + cl));
	} catch(IllegalAccessException e) {
	    throw(new RuntimeException(e));
	}
	return(RoGetActivationFactory(actnm, iid));
    }

    /* XXX: This is quite heuristic. I don't really know formally when
     * to use the "runtime class type" vs the raw interface type. */
    private static String ckrttype(Class<?> base, UUID defiface) {
	String rtname = null;
	try {
	    Field f = base.getDeclaredField("RTNAME");
	    rtname = (String)f.get(null);
	} catch(NoSuchFieldException e) {
	} catch(IllegalAccessException e) {
	    throw(new RuntimeException(e));
	}
	if(rtname == null)
	    return("{" + defiface + "}");
	else
	    return("rc(" + rtname + ";{" + defiface + "})");
    }

    private static String typename(Type type) {
	if(type instanceof Class) {
	    Class<?> cl = (Class)type;
	    if(cl == HString.class)
		return("string");
	    if(IUnknown.class.isAssignableFrom(cl))
		return(ckrttype(cl, COM.IID.get(cl).uuid));
	} else if(type instanceof ParameterizedType) {
	    ParameterizedType pt = (ParameterizedType)type;
	    if((pt.getRawType() instanceof Class) && IUnknown.class.isAssignableFrom((Class)pt.getRawType())) {
		PIID<?> piid = PIID.get(((Class<?>)pt.getRawType()).asSubclass(IUnknown.class));
		Type[] ptypes = pt.getActualTypeArguments();
		String[] tnames = new String[ptypes.length + 1];
		tnames[0] = "{" + piid.uuid + "}";
		for(int i = 0; i < ptypes.length; i++)
		    tnames[i + 1] = typename(ptypes[i]);
		return("pinterface(" + String.join(";", tnames) + ")");
	    }
	}
	throw(new IllegalArgumentException(String.valueOf(type)));
    }

    public static class PIID<I extends IUnknown> {
	public final Class<I> main;
	public final UUID uuid;

	public PIID(Class<I> main, UUID uuid) {
	    this.main = main;
	    this.uuid = uuid;
	}

	public PIID(Class<I> main, long msb, long lsb) {
	    this(main, new UUID(msb, lsb));
	}

	public static <I extends IUnknown> PIID<I> get(Class<I> cl) {
	    try {
		Field f = cl.getDeclaredField("PIID");
		PIID<?> piid = (PIID<?>)f.get(null);
		if(piid.main != cl)
		    throw(new RuntimeException(cl.toString()));
		@SuppressWarnings("unchecked") PIID<I> checked = (PIID<I>)piid;
		return(checked);
	    } catch(NoSuchFieldException e) {
		throw(new ClassCastException("not a declared WinRT interface: " + cl));
	    } catch(IllegalAccessException e) {
		throw(new RuntimeException(e));
	    }
	}
    }

    public static class IPIID<I extends IUnknown> extends IID<I> {
	public Type[] ptypes;

	public IPIID(Class<I> iface, UUID uuid, Type[] ptypes) {
	    super(iface, uuid);
	    this.ptypes = ptypes;
	}

	public String toString() {
	    return(String.format("#<com-iface %s for %s>", uuid, new SimpleParameterizedType(null, iface, ptypes)));
	}
    }

    private static final Map<Object, IPIID<?>> ipiids = new HashMap<>();
    static <I extends IUnknown> IID<I> ipiid(Class<I> maintype, Type... ptypes) {
	PIID<I> piid = PIID.get(maintype);
	Object key = Arrays.asList(piid, Arrays.asList(ptypes));
	synchronized(ipiids) {
	    @SuppressWarnings("unchecked") IPIID<I> ret = (IPIID<I>)ipiids.get(key);
	    if(ret == null) {
		String sig = typename(new SimpleParameterizedType(null, maintype, ptypes));
		UUID uuid = Utils.uuid5(TYPE_NS, sig);
		ipiids.put(key, ret = new IPIID<>(maintype, uuid, ptypes));
	    }
	    return(ret);
	}
    }

    static class Win64Unicode extends WinRT {
	private static final MemoryLayout HRESULT    = Win32.Win64Unicode.HRESULT;
	private static final MemoryLayout DWORD      = Win32.Win64Unicode.DWORD;
	private static final MemoryLayout UINT32     = Win32.Win64Unicode.UINT32;
	private static final MemoryLayout HANDLE     = Win32.Win64Unicode.HANDLE;
	private static final MemoryLayout REFIID     = COM.Win64Unicode.REFIID;
	private static final MemoryLayout HSTRING = HANDLE;
	private static final Charset C_WCHARSET = Win32.Win64Unicode.C_WCHARSET;
	private final SymbolLookup combase = loadlib("COMBASE.DLL", Arena.global());
	private final COM com = COM.get();

	public MemoryLayout HSTRING() {return(HSTRING);}

	private final MethodHandle WindowsGetStringRawBuffer = ld.downcallHandle(combase.find("WindowsGetStringRawBuffer").get(), FunctionDescriptor.of(ADDRESS, HSTRING, ADDRESS));
	String WindowsGetStringRawBuffer(Handle string) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment lbuf = st.allocate(UINT32);
		MemorySegment rv;
		try {
		    rv = (MemorySegment)WindowsGetStringRawBuffer.invoke(string.bits, lbuf);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		int len = (int)getint(lbuf, 0, UINT32, false);
		char[] copy = new char[len];
		MemorySegment.copy(rv.reinterpret(len * 2), 0, MemorySegment.ofArray(copy), 0, len * 2);
		return(new String(copy));
	    }
	}

	private final MethodHandle WindowsDeleteString = ld.downcallHandle(combase.find("WindowsDeleteString").get(), FunctionDescriptor.of(HRESULT, HSTRING));
	void WindowsDeleteString(Handle string) {
	    int rv;
	    try {
		rv = (int)WindowsDeleteString.invoke(string.bits);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(!SUCCEEDED(rv))
		throw(new HResultError(rv));
	}

	private final MethodHandle WindowsCreateString = ld.downcallHandle(combase.find("WindowsCreateString").get(), FunctionDescriptor.of(HRESULT, ADDRESS, UINT32, ADDRESS));
	Handle WindowsCreateString(String str) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment buf = st.allocate(ADDRESS);
		int rv;
		try {
		    rv = (int)WindowsCreateString.invoke(st.allocateFrom(str, C_WCHARSET), str.length(), buf);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(!SUCCEEDED(rv))
		    throw(new HResultError(rv));
		return(Handle.of(buf.get(ADDRESS, 0)));
	    }
	}

	private final MethodHandle WindowsDuplicateString = ld.downcallHandle(combase.find("WindowsDuplicateString").get(), FunctionDescriptor.of(HRESULT, ADDRESS, ADDRESS));
	Handle WindowsDuplicateString(Handle str) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment buf = st.allocate(ADDRESS);
		int rv;
		try {
		    rv = (int)WindowsDuplicateString.invoke(str.bits, buf);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(!SUCCEEDED(rv))
		    throw(new HResultError(rv));
		return(Handle.of(buf.get(ADDRESS, 0)));
	    }
	}

	public class HString implements WinRT.HString {
	    public final Handle bk;

	    public HString(Handle bk) {
		this.bk = bk;
		Win64Unicode lib = Win64Unicode.this;
		Finalizer.finalize(this, () -> lib.WindowsDeleteString(bk));
	    }

	    public String toString() {
		return(WindowsGetStringRawBuffer(bk));
	    }
	}
	public HString HString(String str) {
	    return(new HString(WindowsCreateString(str)));
	}

	private final MethodHandle RoUninitialize = ld.downcallHandle(combase.find("RoUninitialize").get(), FunctionDescriptor.ofVoid());
	public void RoUninitialize() {
	    try {
		RoUninitialize.invoke();
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle RoInitialize = ld.downcallHandle(combase.find("RoInitialize").get(), FunctionDescriptor.of(HRESULT, DWORD));
	public void RoInitialize(int init) {
	    int rv;
	    try {
		rv = (int)RoInitialize.invoke(init);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(!SUCCEEDED(rv))
		throw(new HResultError(rv));
	}

	private final MethodHandle RoGetActivationFactory = ld.downcallHandle(combase.find("RoGetActivationFactory").get(), FunctionDescriptor.of(HRESULT, HSTRING, REFIID, ADDRESS));
	public <I extends IUnknown> I RoGetActivationFactory(WinRT.HString id, IID<I> iid) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment buf = st.allocate(ADDRESS);
		int rv;
		try {
		    rv = (int)RoGetActivationFactory.invoke(((HString)id).bk.bits, iid.mem, buf);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(!SUCCEEDED(rv))
		    throw(new HResultError(rv));
		return(com.reg.objforaddr(buf.get(ADDRESS, 0), iid, false));
	    }
	}

	{
	    valxf(new ValueConversion(ADDRESS, WinRT.HString.class,
				      (st, v) -> ((HString)v).bk.bits,
				      buf -> new HString(Handle.of(buf.get(ADDRESS, 0))),
				      v -> new HString(WindowsDuplicateString(Handle.of((MemorySegment)v))),
				      (v, buf) -> buf.set(ADDRESS, 0, WindowsDuplicateString(((HString)v).bk).bits)));
	}
    }

    private static WinRT instance = null;
    public static WinRT get() {
	if(instance == null) {
	    synchronized(WinRT.class) {
		if(instance == null)
		    instance = tryload("WinRT", Win64Unicode::new);
	    }
	}
	return(instance);
    }
}
