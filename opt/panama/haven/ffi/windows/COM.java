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
import java.lang.foreign.MemoryLayout.PathElement;
import static haven.ffi.ABI.*;
import static haven.ffi.FUtils.*;
import static haven.ffi.windows.Win32.*;
import static java.lang.foreign.ValueLayout.ADDRESS;

public abstract class COM {
    public static final int COINIT_APARTMENTTHREADED = 0x2;
    public static final int COINIT_MULTITHREADED     = 0x0;
    public static final int COINIT_DISABLE_OLE1DDE   = 0x4;
    public static final int COINIT_SPEED_OVER_MEMORY = 0x8;

    public static final int CLSCTX_INPROC_SERVER    = 0x1;
    public static final int CLSCTX_INPROC_HANDLER   = 0x2;
    public static final int CLSCTX_LOCAL_SERVER     = 0x4;
    public static final int CLSCTX_INPROC_SERVER16  = 0x8;
    public static final int CLSCTX_REMOTE_SERVER    = 0x10;
    public static final int CLSCTX_INPROC_HANDLER16 = 0x20;

    public static final int DVASPECT_CONTENT   = 1;
    public static final int DVASPECT_THUMBNAIL = 2;
    public static final int DVASPECT_ICON      = 4;
    public static final int DVASPECT_DOCPRINT  = 8;

    public static final int TYMED_NULL     =  0;
    public static final int TYMED_HGLOBAL  =  1;
    public static final int TYMED_FILE     =  2;
    public static final int TYMED_ISTREAM  =  4;
    public static final int TYMED_ISTORAGE =  8;
    public static final int TYMED_GDI      = 16;
    public static final int TYMED_MFPICT   = 32;
    public static final int TYMED_ENHMF    = 64;

    public static final int DATADIR_GET = 1;
    public static final int DATADIR_SET = 2;

    public abstract void CoUninitialize();
    public abstract void CoInitializeEx(int init);
    public abstract void OleUninitialize();
    public abstract void OleInitialize();
    public abstract <I extends IUnknown> I CoCreateInstance(CLSID clsid, IUnknown outer, int context, IID<I> iid);
    abstract MemorySegment CoTaskMemAlloc(int bytes);
    abstract void CoTaskMemFree(MemorySegment mem);
    public abstract void RegisterDragDrop(Handle hwnd, IDropTarget droptarget);
    public abstract void RevokeDragDrop(Handle hwnd);

    public static boolean SUCCEEDED(int hr) {return(hr >= 0);}

    public static abstract class FORMATETC extends StructInstance {
	protected FORMATETC(MemorySegment mem) {
	    super(mem);
	}

	public static FORMATETC fromcom(MemorySegment mem) {
	    return(COM.get().FORMATETC_fromcom(mem));
	}

	MemorySegment mem() {return(mem);}

	public abstract FORMATETC cfFormat(int value);
	public abstract int cfFormat();
	public abstract FORMATETC dwAspect(int value);
	public abstract int dwAspect();
	public abstract FORMATETC lindex(int value);
	public abstract int lindex();
	public abstract FORMATETC tymed(int value);
	public abstract int tymed();
    }
    public abstract MemoryLayout C_FORMATETC();
    public abstract FORMATETC FORMATETC();
    abstract FORMATETC FORMATETC_fromcom(MemorySegment mem);

    public static abstract class STGMEDIUM extends StructInstance {
	protected STGMEDIUM(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract STGMEDIUM tymed(int value);
	public abstract int tymed();
	public abstract STGMEDIUM handle(Handle value);
	public abstract Handle handle();
	abstract STGMEDIUM address(MemorySegment value);
	abstract MemorySegment address();

	public Object get() {
	    int t = tymed();
	    switch(t) {
	    case TYMED_HGLOBAL:
		return(new HGlobal(handle(), true));
	    case TYMED_FILE:
		return(address().getString(0, Win32.get().C_WCHARSET()));
	    default:
		throw(new ClassCastException("unexpected medium-type: " + t));
	    }
	}
    }
    public abstract MemoryLayout C_STGMEDIUM();
    public abstract STGMEDIUM STGMEDIUM();

    public static MemorySegment memguid(MemorySegment buf, UUID uuid) {
	buf.set(ValueLayout.JAVA_INT_UNALIGNED, 0, (int)(uuid.getMostSignificantBits() >>> 32));
	buf.set(ValueLayout.JAVA_SHORT_UNALIGNED, 4, (short)(uuid.getMostSignificantBits() >>> 16));
	buf.set(ValueLayout.JAVA_SHORT_UNALIGNED, 6, (short)(uuid.getMostSignificantBits() >>> 0));
	for(int i = 0; i < 8; i++)
	    buf.set(ValueLayout.JAVA_BYTE, 15 - i, (byte)(uuid.getLeastSignificantBits() >>> (i * 8)));
	return(buf);
    }

    public static MemorySegment memguid(UUID uuid) {
	return(memguid(Arena.ofAuto().allocate(16), uuid));
    }

    public static UUID guidmem(MemorySegment buf) {
	long msb = 0, lsb = 0;
	msb |= (buf.get(ValueLayout.JAVA_INT_UNALIGNED,   0) & 0xffffffffl) << 32;
	msb |= (buf.get(ValueLayout.JAVA_SHORT_UNALIGNED, 4) & 0x0000ffffl) << 16;
	msb |= (buf.get(ValueLayout.JAVA_SHORT_UNALIGNED, 6) & 0x0000ffffl) <<  0;
	for(int i = 0; i < 8; i++)
	    lsb |= (buf.get(ValueLayout.JAVA_BYTE, 15 - i) & 0xffl) << (i * 8);
	return(new UUID(msb, lsb));
    }

    public static class CLSID {
	public final UUID uuid;
	final MemorySegment mem;

	public CLSID(UUID uuid) {
	    this.uuid = uuid;
	    this.mem = memguid(uuid);
	}

	public CLSID(long msb, long lsb) {
	    this(new UUID(msb, lsb));
	}
    }

    public static class NoIIDException extends RuntimeException {
	public NoIIDException(String msg) {
	    super(msg);
	}
    }

    public static class IID<I extends IUnknown> {
	public final Class<I> iface;
	public final UUID uuid;
	final MemorySegment mem;

	public IID(Class<I> iface, UUID uuid) {
	    this.iface = iface;
	    this.uuid = uuid;
	    this.mem = memguid(uuid);
	}

	public IID(Class<I> iface, long msb, long lsb) {
	    this(iface, new UUID(msb, lsb));
	}

	public <I extends IUnknown> IID<I> cast(Class<I> cl) {
	    if(!cl.isAssignableFrom(iface))
		throw(new ClassCastException(iface + " -> " + cl));
	    @SuppressWarnings("unchecked") IID<I> checked = (IID<I>)this;
	    return(checked);
	}

	public static IID<? extends IUnknown> get(Type t) {
	    if(t instanceof Class) {
		Class<?> cl = (Class)t;
		try {
		    Field f = cl.getDeclaredField("IID");
		    IID<?> iid = (IID<?>)f.get(null);
		    if(iid.iface != cl)
			throw(new RuntimeException(cl.toString()));
		    return(iid);
		} catch(NoSuchFieldException e) {
		    throw(new NoIIDException("not a declared COM interface: " + cl));
		} catch(IllegalAccessException e) {
		    throw(new RuntimeException(e));
		}
	    } else if(t instanceof ParameterizedType) {
		ParameterizedType pt = (ParameterizedType)t;
		if((pt.getRawType() instanceof Class) && IUnknown.class.isAssignableFrom((Class<?>)pt.getRawType()))
		    return(WinRT.ipiid(((Class<?>)pt.getRawType()).asSubclass(IUnknown.class),
				       pt.getActualTypeArguments()));
	    }
	    throw(new NoIIDException(String.valueOf(t)));
	}

	public String toString() {
	    return(String.format("#<com-iface %s for %s>", uuid, iface));
	}
    }

    MemorySegment getvtab(MemorySegment obj, int idx) {
	return(obj.reinterpret(ADDRESS.byteSize()).get(ADDRESS, 0)
	       .reinterpret(Long.MAX_VALUE).getAtIndex(ADDRESS, idx));
    }

    public static class Downcall {
	public final COM com;
	public final MethodHandle fun;

	public Downcall(COM com, MethodHandle fun) {
	    this.com = com;
	    this.fun = fun;
	}
    }

    public static class AdapterData {
	public final COM com;
	public final IID<?> iid;
	public final Descriptor desc;
	public final Method mth;

	public AdapterData(COM com, IID<?> iid, Descriptor desc, Method mth) {
	    this.com = com;
	    this.iid = iid;
	    this.desc = desc;
	    this.mth = mth;
	}
    }

    public static interface DowncallAdapter {
	public Object call(MemorySegment obj, Object[] args);

	public static interface Arg {
	    public Object convert(Arena st, Object val);
	}

	public static interface Constructor {
	    public DowncallAdapter make(AdapterData data);

	    public static final Constructor std = d -> {
		Method handler = smethod(d.mth.getDeclaringClass(), "downcall_" + d.mth.getName());
		if(handler != null)
		    return(new CustomDowncall(d, handler));
		else
		    return(d.com.new StandardDowncall(d.iid, d.desc, d.mth));
	    };
	}

	public static final DowncallAdapter nil = (self, args) -> {
	    throw(new RuntimeException("no downcall defined"));
	};
    }

    public static interface UpcallAdapter {
	public int call(Registry.LocalStub self, Object[] args);

	public static final UpcallAdapter nil = (self, args) -> {
	    throw(new RuntimeException("no upcall defined"));
	};

	public static interface Constructor {
	    public UpcallAdapter make(AdapterData data);

	    public static final Constructor std = d -> {
		return(d.com.new StandardUpcall(d.iid, d.desc, d.mth));
	    };
	}
    }

    public static class ValueConversion {
	public final MemoryLayout arg;
	public final Class<?> par;
	public final DowncallAdapter.Arg darg;
	public final Function<MemorySegment, Object> dret;
	public final Function<Object, Object> uarg;
	public final BiConsumer<Object, MemorySegment> uret;

	public ValueConversion(MemoryLayout arg, Class<?> par, DowncallAdapter.Arg darg, Function<MemorySegment, Object> dret, Function<Object, Object> uarg, BiConsumer<Object, MemorySegment> uret) {
	    this.arg = arg;
	    this.par = par;
	    this.darg = darg;
	    this.dret = dret;
	    this.uarg = uarg;
	    this.uret = uret;
	}
    }
    private static Map<Pair<MemoryLayout, Class<?>>, ValueConversion> valxfs = new HashMap<>();
    public static ValueConversion valxf(MemoryLayout arg, Class<?> par) {
	synchronized(valxfs) {
	    return(valxfs.get(new Pair<>(arg, par)));
	}
    }
    public static void valxf(ValueConversion xf) {
	synchronized(valxfs) {
	    valxfs.put(new Pair<>(xf.arg, xf.par), xf);
	}
    }

    private static Type actualize(Type type, IID<?> iid) {
	if(type instanceof Class)
	    return(type);
	if(type instanceof ParameterizedType) {
	    ParameterizedType pt = (ParameterizedType)type;
	    if(pt.getOwnerType() == null) {
		boolean ch = false;
		Type raw = pt.getRawType();
		Type araw = actualize(raw, iid);
		ch |= araw != raw;
		Type[] args = pt.getActualTypeArguments();
		Type[] aargs = new Type[args.length];
		for(int i = 0; i < args.length; i++)
		    ch |= (aargs[i] = actualize(args[i], iid)) != args[i];
		if(!ch)
		    return(type);
		return(new SimpleParameterizedType(null, araw, aargs));
	    }
	}
	if(type instanceof TypeVariable) {
	    TypeVariable<?> tv = (TypeVariable<?>)type;
	    if(tv.getGenericDeclaration() == iid.iface) {
		TypeVariable<?>[] vars = iid.iface.getTypeParameters();
		for(int i = 0; i < vars.length; i++) {
		    if(type.equals(vars[i]))
			return(((WinRT.IPIID)iid).ptypes[i]);
		}
		throw(new IllegalArgumentException(type + " not in type variables of " + iid.iface));
	    }
	}
	return(type);
    }

    public class StandardDowncall implements DowncallAdapter {
	private final int vindex;
	private final MethodHandle fun;
	private final Arg[] argxf;
	private final MemoryLayout outtype;
	private final Function<MemorySegment, Object> outxf;

	public StandardDowncall(IID<?> iid, Descriptor desc, Method mth) {
	    Win32 win = Win32.get();
	    this.vindex = desc.vindex;
	    this.outtype = desc.outtype;
	    Class<?>[] pars = mth.getParameterTypes();
	    Type[] gpars = mth.getGenericParameterTypes();
	    MemoryLayout[] args = desc.sig.argumentLayouts().toArray(new MemoryLayout[0]);
	    if(pars.length != args.length - ((outtype == null) ? 1 : 2))
		throw(new IllegalArgumentException(mth + ": " + Arrays.asList(pars) + " does not match " + Arrays.asList(args)));
	    argxf = new Arg[pars.length];
	    fun = ld.downcallHandle(desc.sig);
	    for(int i = 0; i < pars.length; i++) {
		Class<?> p = pars[i]; Type gp = gpars[i]; MemoryLayout a = args[i + 1];
		Arg xf;
		ValueConversion valxf = valxf(a, p);
		if((valxf != null) && (valxf.darg != null)) {
		    xf = valxf.darg;
		} else if(a == ValueLayout.JAVA_BYTE) {
		    if(p == Byte.TYPE) {
			xf = null;
		    } else if(p == Boolean.TYPE) {
			xf = (st, v) -> ((boolean)v) ? (byte)1 : (byte)0;
		    } else {
			throw(new IllegalArgumentException(p + " -> " + a));
		    }
		} else if(a == ValueLayout.JAVA_SHORT) {
		    if((p == Short.TYPE) || (p == Byte.TYPE)) {
			xf = null;
		    } else if(p == Boolean.TYPE) {
			xf = (st, v) -> ((boolean)v) ? (byte)1 : (byte)0;
		    } else {
			throw(new IllegalArgumentException(p + " -> " + a));
		    }
		} else if(a == ValueLayout.JAVA_INT) {
		    if((p == Integer.TYPE) || (p == Short.TYPE) || (p == Byte.TYPE)) {
			xf = null;
		    } else if(p == Boolean.TYPE) {
			xf = (st, v) -> ((boolean)v) ? (byte)1 : (byte)0;
		    } else {
			throw(new IllegalArgumentException(p + " -> " + a));
		    }
		} else if(a == ValueLayout.JAVA_LONG) {
		    if((p == Long.TYPE) || (p == Integer.TYPE) || (p == Short.TYPE) || (p == Byte.TYPE)) {
			xf = null;
		    } else if(p == Boolean.TYPE) {
			xf = (st, v) -> ((boolean)v) ? (byte)1 : (byte)0;
		    } else {
			throw(new IllegalArgumentException(p + " -> " + a));
		    }
		} else if(a == ADDRESS) {
		    if(p == MemorySegment.class) {
			xf = null;
		    } else if(p == Handle.class) {
			xf = (st, v) -> (v == null) ? MemorySegment.NULL : ((Handle)v).bits;
		    } else if(p == IID.class) {
			xf = (st, v) -> ((IID)v).mem;
		    } else if(p == CLSID.class) {
			xf = (st, v) -> ((CLSID)v).mem;
		    } else if(p == String.class) {
			xf = (st, v) -> (v == null) ? MemorySegment.NULL : st.allocateFrom((String)v, win.C_WCHARSET());
		    } else if(IUnknown.class.isAssignableFrom(p)) {
			IID<?> pariid = IID.get(actualize(gp, iid));
			xf = (st, v) -> reg.addrforobj2((IUnknown)v, pariid, false, st);
		    } else if(StructInstance.class.isAssignableFrom(p)) {
			xf = (st, v) -> StructInstance.extract((StructInstance)v);
		    } else {
			throw(new IllegalArgumentException(p + " -> " + a));
		    }
		} else {
		    throw(new IllegalArgumentException(p + " -> " + a));
		}
		argxf[i] = xf;
	    }
	    if(outtype == null) {
		outxf = null;
	    } else {
		Type grtype = actualize(mth.getGenericReturnType(), iid);
		Class<?> rtype;
		if(grtype instanceof Class) {
		    rtype = (Class<?>)grtype;
		} else if(grtype instanceof ParameterizedType) {
		    rtype = (Class<?>)((ParameterizedType)grtype).getRawType();
		} else {
		    throw(new IllegalArgumentException(String.valueOf(grtype)));
		}
		ValueConversion valxf = valxf(outtype, rtype);
		if((valxf != null) && (valxf.dret != null)) {
		    outxf = valxf.dret;
		} else if(rtype == Boolean.TYPE) {
		    if((outtype == ValueLayout.JAVA_INT) || (outtype == ValueLayout.JAVA_SHORT) || (outtype == ValueLayout.JAVA_BYTE)) {
			outxf = buf -> ((int)getint(buf, 0, outtype, true)) != 0;
		    } else {
			throw(new IllegalArgumentException(outtype + " -> " + rtype));
		    }
		} else if(rtype == Integer.TYPE) {
		    if((outtype == ValueLayout.JAVA_INT) || (outtype == ValueLayout.JAVA_SHORT) || (outtype == ValueLayout.JAVA_BYTE)) {
			outxf = buf -> (int)getint(buf, 0, outtype, true);
		    } else {
			throw(new IllegalArgumentException(outtype + " -> " + rtype));
		    }
		} else if(rtype == String.class) {
		    if(outtype == ADDRESS) {
			outxf = buf -> {
			    MemorySegment str = buf.get(ADDRESS, 0);
			    if(nullp(str))
				return(null);
			    String ret = str.reinterpret(Long.MAX_VALUE).getString(0, win.C_WCHARSET());
			    CoTaskMemFree(str);
			    return(ret);
			};
		    } else {
			throw(new IllegalArgumentException(outtype + " -> " + rtype));
		    }
		} else if(IUnknown.class.isAssignableFrom(rtype)) {
		    if(outtype == ADDRESS) {
			IID<?> outiid = IID.get(grtype);
			outxf = buf -> reg.objforaddr(buf.get(ADDRESS, 0), outiid, false);
		    } else {
			throw(new IllegalArgumentException(outtype + " -> " + rtype));
		    }
		} else {
		    throw(new IllegalArgumentException(outtype + " -> " + rtype));
		}
	    }
	}

	public Object call(MemorySegment obj, Object[] pars) {
	    if(nullp(obj))
		throw(new NullPointerException());
	    try(Arena st = Arena.ofConfined()) {
		Object[] args = new Object[pars.length + ((outtype == null) ? 2 : 3)];
		args[0] = getvtab(obj, vindex);
		args[1] = obj;
		for(int i = 0; i < pars.length; i++)
		    args[i + 2] = (argxf[i] == null) ? pars[i] : argxf[i].convert(st, pars[i]);
		MemorySegment outbuf = null;
		if(outtype != null) {
		    outbuf = st.allocate(outtype);
		    args[pars.length + 2] = outbuf;
		}
		int rv;
		try {
		    rv = (int)fun.invokeWithArguments(args);
		} catch(Throwable t) {
		    throw(new InvocationException(t));
		}
		if(!SUCCEEDED(rv))
		    throw(new HResultError(rv));
		if(outtype == null)
		    return(rv);
		return(outxf.apply(outbuf));
	    }
	}
    }

    public static class CustomDowncall implements DowncallAdapter {
	private final Downcall info;
	private final Method handler;

	public CustomDowncall(AdapterData d, Method handler) {
	    this.info = new Downcall(d.com, ld.downcallHandle(d.desc.sig));
	    this.handler = handler;
	}

	public Object call(MemorySegment obj, Object[] pars) {
	    Object[] args = new Object[pars.length + 2];
	    args[0] = info;
	    args[1] = obj;
	    System.arraycopy(pars, 0, args, 2, pars.length);
	    return(Utils.invoke(handler, null, args));
	}
    }

    public static class COMReturn extends HResultError {
	public COMReturn(int hresult) {
	    super(hresult);
	}
    }

    public class StandardUpcall implements UpcallAdapter {
	private final Function<Object, Object>[] argxf;
	private final BiConsumer<Object, MemorySegment> outxf;
	private final Method method;
	private final MemoryLayout outtype;
	private final boolean resret;

	@SuppressWarnings("unchecked")
	public StandardUpcall(IID<?> iid, Descriptor desc, Method mth) {
	    Win32 win = Win32.get();
	    this.method = mth;
	    outtype = desc.outtype;
	    Class<?>[] pars = mth.getParameterTypes();
	    Type[] gpars = mth.getGenericParameterTypes();
	    MemoryLayout[] args = desc.sig.argumentLayouts().toArray(new MemoryLayout[0]);
	    argxf = new Function[pars.length];
	    if(pars.length != args.length - ((outtype == null) ? 1 : 2))
		throw(new IllegalArgumentException(mth + ": " + Arrays.asList(pars) + " does not match " + Arrays.asList(args)));
	    for(int i = 0; i < pars.length; i++) {
		Class<?> p = pars[i]; Type gp = gpars[i]; MemoryLayout a = args[i + 1];
		Function<Object, Object> xf;
		ValueConversion valxf = valxf(a, p);
		if((valxf != null) && (valxf.uarg != null)) {
		    xf = valxf.uarg;
		} else if((a == ValueLayout.JAVA_BYTE) || (a == ValueLayout.JAVA_SHORT) ||
		   (a == ValueLayout.JAVA_INT)  || (a == ValueLayout.JAVA_LONG)) {
		    if(p == Byte.TYPE)
			xf = v -> ((Number)v).byteValue();
		    else if(p == Short.TYPE)
			xf = v -> ((Number)v).shortValue();
		    else if(p == Integer.TYPE)
			xf = v -> ((Number)v).intValue();
		    else if(p == Long.TYPE)
			xf = v -> ((Number)v).longValue();
		    else
			throw(new IllegalArgumentException(a + " -> " + p));
		} else if(a == ADDRESS) {
		    if(p == MemorySegment.class) {
			xf = null;
		    } else if(p == String.class) {
			xf = v -> nullp((MemorySegment)v) ? null : ((MemorySegment)v).reinterpret(Long.MAX_VALUE).getString(0, win.C_WCHARSET());
		    } else if(p == Handle.class) {
			xf = v -> Handle.of((MemorySegment)v);
		    } else if(IUnknown.class.isAssignableFrom(p)) {
			IID<?> pariid = IID.get(actualize(gp, iid));
			xf = v -> reg.objforaddr((MemorySegment)v, pariid, true);
		    } else if(StructInstance.class.isAssignableFrom(p)) {
			try {
			    Method cons = p.getDeclaredMethod("fromcom", MemorySegment.class);
			    if(!Modifier.isStatic(cons.getModifiers()))
				throw(new IllegalArgumentException("not statuc " + cons));
			    xf = v -> p.cast(Utils.invoke(cons, null, v));
			} catch(NoSuchMethodException e) {
			    throw(new IllegalArgumentException("cannot construct " + p));
			}
		    } else {
			throw(new IllegalArgumentException(a + " -> " + p));
		    }
		} else {
		    throw(new IllegalArgumentException(a + " -> " + p));
		}
		argxf[i] = xf;
	    }
	    Class<?> rtype = mth.getReturnType();
	    if(outtype == null) {
		if(rtype == Integer.TYPE)
		    resret = true;
		else if(rtype == Void.TYPE)
		    resret = false;
		else
		    throw(new IllegalArgumentException(rtype + " -> void"));
		outxf = null;
	    } else {
		resret = false;
		ValueConversion valxf = valxf(outtype, rtype);
		if((valxf != null) && (valxf.uret != null)) {
		    outxf = valxf.uret;
		} else if(outtype == ValueLayout.JAVA_BYTE) {
		    outxf = (v, buf) -> buf.set(ValueLayout.JAVA_BYTE, 0, ((Number)v).byteValue());
		} else if(outtype == ValueLayout.JAVA_SHORT) {
		    outxf = (v, buf) -> buf.set(ValueLayout.JAVA_SHORT, 0, ((Number)v).shortValue());
		} else if(outtype == ValueLayout.JAVA_INT) {
		    outxf = (v, buf) -> buf.set(ValueLayout.JAVA_INT, 0, ((Number)v).intValue());
		} else if(outtype == ValueLayout.JAVA_LONG) {
		    outxf = (v, buf) -> buf.set(ValueLayout.JAVA_LONG, 0, ((Number)v).longValue());
		} else if(outtype == ADDRESS) {
		    if(rtype == String.class) {
			outxf = (v, buf) -> {
			    if(v == null) {
				buf.set(ADDRESS, 0, MemorySegment.NULL);
				return;
			    }
			    try(Arena st = Arena.ofConfined()) {
				MemorySegment enc = st.allocateFrom((String)v, win.C_WCHARSET());
				MemorySegment ret = CoTaskMemAlloc((int)enc.byteSize());
				MemorySegment.copy(enc, 0, ret, 0, enc.byteSize());
				buf.set(ADDRESS, 0, ret);
			    }
			};
		    } else if(IUnknown.class.isAssignableFrom(rtype)) {
			IID<?> outiid = IID.get(actualize(mth.getGenericReturnType(), iid));
			outxf = (v, buf) -> buf.set(ADDRESS, 0, reg.addrforobj2((IUnknown)v, outiid, true, null));
		    } else {
			throw(new IllegalArgumentException(rtype + " -> "  + outtype));
		    }
		} else {
		    throw(new IllegalArgumentException(rtype + " -> "  + outtype));
		}
	    }
	}

	public int call(Registry.LocalStub stub, Object[] args) {
	    try {
		Object[] pars = new Object[argxf.length];
		for(int i = 0; i < pars.length; i++)
		    pars[i] = (argxf[i] == null) ? args[i] : (argxf[i].apply(args[i]));
		Object rv = Utils.invoke(method, stub.obj, pars);
		if(outtype != null)
		    outxf.accept(rv, ((MemorySegment)args[args.length - 1]).reinterpret(outtype.byteSize()));
		if(resret)
		    return(((Number)rv).intValue());
		return(0);
	    } catch(COMReturn e) {
		return(e.hresult);
	    }
	}
    }

    public static class Descriptor {
	public final int vindex;
	public final FunctionDescriptor sig;
	public final MemoryLayout outtype;
	DowncallAdapter.Constructor mda = DowncallAdapter.Constructor.std;
	UpcallAdapter.Constructor mua = UpcallAdapter.Constructor.std;

	public Descriptor(int vindex, FunctionDescriptor sig, MemoryLayout outtype) {
	    this.vindex = vindex;
	    this.sig = sig;
	    this.outtype = outtype;
	}

	public Descriptor(int vindex, FunctionDescriptor sig) {
	    this(vindex, sig, null);
	}

	public Descriptor downcall(DowncallAdapter.Constructor mda) {
	    this.mda = mda; return(this);
	}
	public Descriptor downcall(DowncallAdapter da) {
	    return(downcall(data -> da));
	}
	public Descriptor upcall(UpcallAdapter.Constructor mua) {
	    this.mua = mua; return(this);
	}
	public Descriptor upcall(UpcallAdapter ua) {
	    return(upcall(data -> ua));
	}

	private static final Map<Method, Descriptor> cache = new HashMap<>();
	public static Descriptor get(Method method) {
	    synchronized(cache) {
		Descriptor ret = cache.get(method);
		if(ret != null)
		    return(ret);
		try {
		    Field f = method.getDeclaringClass().getDeclaredField("sig_" + method.getName());
		    if(!Modifier.isStatic(f.getModifiers()))
			return(null);
		    ret = (Descriptor)f.get(null);
		    cache.put(method, ret);
		    return(ret);
		} catch(ReflectiveOperationException e) {
		    return(null);
		}
	    }
	}
    }

    private static IID<?>[] getiids(Class<?> fcl, Collection<WinRT.IPIID<?>> hints) {
	class Buffer {
	    final Map<Class<?>, WinRT.IPIID> ihints = new HashMap<>();
	    final Collection<IID<?>> iids = new ArrayList<>();

	    Buffer() {
		for(WinRT.IPIID<?> hint : hints)
		    ihints.put(hint.iface, hint);
		addclass(fcl, new Type[fcl.getTypeParameters().length]);
	    }

	    void addclass(Class<?> cl, Type[] tpars) {
		TypeVariable<?>[] vars = cl.getTypeParameters();
		WinRT.IPIID hint = ihints.get(cl);
		if(hint != null) {
		    for(int i = 0; i < vars.length; i++) {
			if(tpars[i] == null)
			    tpars[i] = hint.ptypes[i];
			else if(!tpars[i].equals(hint.ptypes[i]))
			    throw(new IllegalArgumentException(cl + ": " + tpars[i] + " != " + hint.ptypes[i]));
		    }
		}

		addiid(cl, tpars);

		addsuper(cl, cl.getGenericSuperclass(), vars, tpars);
		for(Type iface : cl.getGenericInterfaces())
		    addsuper(cl, iface, vars, tpars);
	    }

	    void addiid(Class<?> cl, Type[] tpars) {
		if(IUnknown.class.isAssignableFrom(cl)) {
		    try {
			if(tpars.length == 0) {
			    iids.add(IID.get(cl));
			} else {
			    for(int i = 0; i < tpars.length; i++) {
				if(tpars[i] == null)
				    return;
			    }
			    iids.add(IID.get(new SimpleParameterizedType(null, cl, tpars)));
			}
		    } catch(NoIIDException e) {
		    }
		}
	    }

	    Type actualize(Type type, Class<?> vcl, TypeVariable<?>[] vars, Type[] tpars) {
		if(type instanceof Class)
		    return(type);
		if(type instanceof ParameterizedType) {
		    ParameterizedType pt = (ParameterizedType)type;
		    if(pt.getOwnerType() == null) {
			boolean ch = false;
			Type raw = pt.getRawType();
			Type araw = actualize(raw, vcl, vars, tpars);
			ch |= araw != raw;
			Type[] args = pt.getActualTypeArguments();
			Type[] aargs = new Type[args.length];
			for(int i = 0; i < args.length; i++)
			    ch |= (aargs[i] = actualize(args[i], vcl, vars, tpars)) != args[i];
			if(!ch)
			    return(type);
			return(new SimpleParameterizedType(null, araw, aargs));
		    }
		}
		if(type instanceof TypeVariable) {
		    TypeVariable<?> tv = (TypeVariable<?>)type;
		    if(tv.getGenericDeclaration() == vcl) {
			for(int i = 0; i < vars.length; i++) {
			    if(type.equals(vars[i]))
				return((tpars[i] != null) ? tpars[i] : type);
			}
			throw(new IllegalArgumentException(type + " not in type variables of " + vcl));
		    }
		}
		return(type);
	    }

	    void addsuper(Class<?> ccl, Type st, TypeVariable<?>[] vars, Type[] tpars) {
		if(st instanceof Class) {
		    Class<?> scl = (Class<?>)st;
		    /* Apparently, lambda objects of parameterized classes don't declare
		     * themselves as parameterized types. That's what the IID hint is
		     * basically for. */
		    addclass(scl, new Type[scl.getTypeParameters().length]);
		} else if(st instanceof ParameterizedType) {
		    ParameterizedType pt = (ParameterizedType)st;
		    Class<?> scl = (Class<?>)pt.getRawType();
		    Type[] apars = pt.getActualTypeArguments();
		    Type[] stpars = new Type[apars.length];
		    for(int i = 0; i < stpars.length; i++)
			stpars[i] = actualize(apars[i], ccl, vars, tpars);
		}
	    }
	}
	return(new Buffer().iids.toArray(new IID<?>[0]));
    }

    public class Registry {
	private final Map<IUnknown, LocalStub> stubsbyobj = new HashMap<>();
	private final Map<Long, LocalStub.Iface> stubsbyaddr = new HashMap<>();
	private final Map<Long, Reference<Foreign>> foreign = new HashMap<>();

	public class Callback {
	    public final MemorySegment stub;
	    public final Method method;
	    public final UpcallAdapter ua;

	    private static Class<?> maptype(MemoryLayout type) {
		if(type == ValueLayout.JAVA_BYTE)
		    return(Byte.TYPE);
		else if(type == ValueLayout.JAVA_SHORT)
		    return(Short.TYPE);
		else if(type == ValueLayout.JAVA_INT)
		    return(Integer.TYPE);
		else if(type == ValueLayout.JAVA_LONG)
		    return(Long.TYPE);
		else if(type == ADDRESS)
		    return(MemorySegment.class);
		else if(type instanceof StructLayout)
		    return(MemorySegment.class);
		throw(new IllegalArgumentException(String.valueOf(type)));
	    }

	    Callback(Arena alloc, IID<?> iid, Method mth, Descriptor desc) {
		MemoryLayout[] args = desc.sig.argumentLayouts().toArray(new MemoryLayout[0]);
		Class<?>[] pars = new Class<?>[args.length];
		for(int i = 0; i < args.length; i++)
		    pars[i] = maptype(args[i]);
		Optional<MemoryLayout> rtype = desc.sig.returnLayout();
		MethodType type = MethodType.methodType(rtype.map(Callback::maptype).orElse(Void.TYPE), pars);
		MethodHandle handler = callback.bindTo(this);
		handler = handler.asCollector(Object[].class, args.length - 1);
		handler = handler.asType(type);
		this.stub = ld.upcallStub(handler, desc.sig, alloc);
		this.method = mth;
		this.ua = desc.mua.make(new AdapterData(COM.this, iid, desc, mth));
	    }

	    private static final MethodHandle callback = vlookup(MethodHandles.lookup(), Callback.class, "callback", Integer.TYPE,
								 MemorySegment.class, Object[].class);
	    private int callback(MemorySegment self, Object[] args) {
		return(upcallwrap(() -> {
		    LocalStub.Iface ip;
		    synchronized(Registry.this) {
			ip = stubsbyaddr.get(self.address());
		    }
		    if(ip == null) {
			Warning.warn("call to " + method + " on unregistered stub $" + Long.toUnsignedString(self.address(), 16));
			return(HResultError.E_FAIL);
		    }
		    LocalStub stub = ip.stub();
		    if((method != null) && !method.getDeclaringClass().isInstance(stub.obj)) {
			Warning.warn("call to " + method + " on non-matching " + stub.obj);
			return(HResultError.E_FAIL);
		    }
		    return(ua.call(stub, args));
		}, HResultError.E_FAIL));
	    }
	}

	public class VTable<I extends IUnknown> {
	    public final MemorySegment vtbl;
	    private final Callback[] retain;

	    VTable(IID<I> iid) {
		Class<I> iface = iid.iface;
		Arena alloc = Arena.ofAuto();
		Callback[] table = new Callback[3];
		table[0] = new Callback(alloc, iid, null, desc_QueryInterface);
		table[1] = new Callback(alloc, iid, null, desc_AddRef);
		table[2] = new Callback(alloc, iid, null, desc_Release);
		for(Method mth : iface.getMethods()) {
		    if(!IUnknown.class.isAssignableFrom(mth.getDeclaringClass()) || (mth.getDeclaringClass() == IUnknown.class))
			continue;
		    Descriptor desc = Descriptor.get(mth);
		    if(desc == null)
			continue;
		    if(desc.vindex >= table.length)
			table = Arrays.copyOf(table, desc.vindex + 1);
		    table[desc.vindex] = new Callback(alloc, iid, mth, desc);
		}
		vtbl = alloc.allocate(ADDRESS, table.length);
		for(int i = 0; i < table.length; i++) {
		    if(table[i] != null)
			vtbl.setAtIndex(ADDRESS, i, table[i].stub);
		}
		retain = table;
	    }
	}
	private final Map<IID<? extends IUnknown>, VTable<? extends IUnknown>> vtables = new WeakHashMap<>();
	public <I extends IUnknown> VTable<I> vtable(IID<I> iid) {
	    synchronized(vtables) {
		@SuppressWarnings("unchecked") VTable<I> ret = (VTable<I>)vtables.computeIfAbsent(iid, k -> new VTable<>(k));
		return(ret);
	    }
	}

	private final Descriptor desc_QueryInterface =
	    new Descriptor(0, FunctionDescriptor.of(HRESULT(), ADDRESS, REFIID(), LPVOID()), ADDRESS)
	    .upcall(LocalStub::QueryInterface);
	private final Descriptor desc_AddRef =
	    new Descriptor(0, FunctionDescriptor.of(UINT(), ADDRESS))
	    .upcall(LocalStub::AddRef);
	private final Descriptor desc_Release =
	    new Descriptor(0, FunctionDescriptor.of(UINT(), ADDRESS))
	    .upcall(LocalStub::Release);
	public class LocalStub {
	    final Arena alloc = Arena.ofAuto();;
	    final AtomicInteger rc = new AtomicInteger(1);
	    final IUnknown obj;
	    final Map<IID<?>, Iface> ifaces = new HashMap<>();

	    LocalStub(IUnknown obj) {
		this.obj = obj;
	    }

	    public class Iface<I extends IUnknown> {
		final IID<I> iid;
		final MemorySegment mem;

		Iface(IID<I> iid) {
		    this.iid = iid;
		    mem = alloc.allocate(ADDRESS);
		    mem.set(ADDRESS, 0, vtable(iid).vtbl);
		}

		LocalStub stub() {return(LocalStub.this);}

		public String toString() {
		    return(String.format("#<com-iface @ %x for %s, %s>", mem.address(), iid, obj));
		}
	    }

	    <I extends IUnknown> Iface<I> iface(IID<I> iid) {
		synchronized(ifaces) {
		    @SuppressWarnings("unchecked") Iface<I> ip = (Iface<I>)ifaces.get(iid);
		    if(ip == null) {
			synchronized(Registry.this) {
			    ifaces.put(iid, ip = new Iface<>(iid));
			    stubsbyaddr.put(ip.mem.address(), ip);
			}
		    }
		    return(ip);
		}
	    }

	    Registry reg() {return(Registry.this);}

	    private IID<?>[] iids = null;
	    static int QueryInterface(LocalStub stub, Object[] args) {
		if(stub.iids == null) {
		    Collection<WinRT.IPIID<?>> hints = new ArrayList<>();
		    for(IID<?> iid : stub.ifaces.keySet()) {
			if(iid instanceof WinRT.IPIID)
			    hints.add((WinRT.IPIID<?>)iid);;
		    }
		    stub.iids = getiids(stub.obj.getClass(), hints);
		}
		UUID tgtid = guidmem(((MemorySegment)args[0]).reinterpret(16));
		for(IID<?> iid : stub.iids) {
		    if(iid.uuid.equals(tgtid)) {
			((MemorySegment)args[1]).reinterpret(ADDRESS.byteSize()).set(ADDRESS, 0, stub.reg().addrforobj2(stub.obj, iid, true, null));
			return(0);
		    }
		}
		return(HResultError.E_NOINTERFACE);
	    }

	    static int AddRef(LocalStub stub, Object[] args) {
		return(stub.rc.incrementAndGet());
	    }

	    static int Release(LocalStub stub, Object[] args) {
		int rc = stub.rc.decrementAndGet();
		if(rc == 0) {
		    Registry reg = stub.reg();
		    synchronized(reg) {
			for(LocalStub.Iface iface : stub.ifaces.values()) {
			    if(reg.stubsbyaddr.remove(iface.mem.address()) != iface)
				throw(new RuntimeException("interface-pointer mismatch for " + iface + ", " + stub.obj + " when released"));
			}
			if(reg.stubsbyobj.remove(stub.obj) != stub)
			    throw(new RuntimeException("stub mismatch for " + stub.obj + " when released"));
		    }
		}
		return(rc);
	    }

	    public String toString() {
		return(String.format("#<com-stub %s>", obj));
	    }
	}

	private final Map<Pair<IID<?>, Method>, DowncallAdapter> dcache = new ConcurrentHashMap<>();
	public class Foreign implements InvocationHandler {
	    private static final Method mth_toString = vmethod(Object.class, "toString");
	    public final IID<?> iid;
	    final MemorySegment mem;
	    Object proxy = null;

	    Foreign(MemorySegment mem, IID<?> iid) {
		this.iid = iid;
		this.mem = mem;
	    }

	    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if(method.equals(mth_toString))
		    return("#<com-proxy " + iid.iface + " @ " + Long.toUnsignedString(mem.address(), 16) + ">");
		if(args == null)
		    args = new Object[0];
		DowncallAdapter da = dcache.get(new Pair<>(iid, method));
		if(da == null) {
		    Descriptor desc = Descriptor.get(method);
		    if(desc != null)
			da = desc.mda.make(new AdapterData(COM.this, iid, desc, method));
		    else
			da = DowncallAdapter.nil;
		    dcache.put(new Pair<>(iid, method), da);
		}
		if(da != DowncallAdapter.nil)
		    return(da.call(mem, args));
		if(method.isDefault())
		    return(InvocationHandler.invokeDefault(proxy, method, args));
		throw(new RuntimeException("no downcall implementation for " + method));
	    }
	}

	final int VINDEX_ADDREF = 1;
	final MethodHandle native_AddRef = ld.downcallHandle(FunctionDescriptor.of(ULONG(), ADDRESS));
	void addref(MemorySegment obj) {
	    MemorySegment rfun = getvtab(obj, VINDEX_ADDREF);
	    try {
		native_AddRef.invoke(rfun, obj);
	    } catch(Throwable t) {
		throw(new InvocationException(t));
	    }
	}

	final int VINDEX_RELEASE = 2;
	final MethodHandle native_Release = ld.downcallHandle(FunctionDescriptor.of(ULONG(), ADDRESS));
	int release(MemorySegment obj) {
	    MemorySegment rfun = getvtab(obj, VINDEX_RELEASE);
	    int rv;
	    try {
		rv = (int)native_Release.invoke(rfun, obj);
	    } catch(Throwable t) {
		throw(new InvocationException(t));
	    }
	    return(rv);
	}

	public <I extends IUnknown> I objforaddr(MemorySegment mem, IID<I> iid, boolean borrowed) {
	    if(nullp(mem))
		return(null);
	    synchronized(this) {
		{
		    LocalStub.Iface<?> ip = stubsbyaddr.get(mem.address());
		    if(ip != null) {
			LocalStub stub = ip.stub();
			if(!borrowed)
			    stub.rc.decrementAndGet();
			return(iid.iface.cast(stub.obj));
		    }
		}
		{
		    Reference<Foreign> ref = foreign.get(mem.address());
		    Foreign obj = (ref == null) ? null : ref.get();
		    if(obj == null) {
			if(borrowed)
			    addref(mem);
			obj = new Foreign(mem, iid);
			obj.proxy = Proxy.newProxyInstance(iid.iface.getClassLoader(), new Class<?>[] {iid.iface}, obj);
			Reference<Foreign> nref = new WeakReference<>(obj);
			Finalizer.finalize(obj, () -> {
			    if(foreign.remove(mem.address()) != nref)
				throw(new AssertionError());
			    release(mem);
			});
			foreign.put(mem.address(), nref);
		    } else {
			if(!borrowed)
			    release(mem);
		    }
		    return(iid.iface.cast(obj.proxy));
		}
	    }
	}

	public <I extends IUnknown> MemorySegment addrforobj(I obj, IID<I> iid, boolean get, Object retainer) {
	    if(obj == null)
		return(MemorySegment.NULL);
	    InvocationHandler h = null;
	    try {
		h = Proxy.getInvocationHandler(obj);
	    } catch(IllegalArgumentException e) {
	    }
	    if(h instanceof Foreign) {
		Foreign f = (Foreign)h;
		if(!iid.iface.isInstance(obj))
		    throw(new ClassCastException("foreign instance is " + f.iid.iface + ", not " + iid.iface));
		if(get)
		    addref(f.mem);
		return(f.mem);
	    }
	    if(obj instanceof IUnknown) {
		synchronized(this) {
		    LocalStub stub = stubsbyobj.get(obj);
		    if(stub == null) {
			if(!get && (retainer == null))
			    throw(new RuntimeException("cannot create new borrowed stubs without a retainer"));
			LocalStub nstub = new LocalStub(obj);
			stubsbyobj.put(obj, nstub);
			if(retainer != null)
			    Finalizer.finalize(retainer, () -> LocalStub.Release(nstub, new Object[0]));
			stub = nstub;
		    } else {
			if(get)
			    stub.rc.incrementAndGet();
		    }
		    LocalStub.Iface<I> ip = stub.iface(iid);
		    return(ip.mem);
		}
	    }
	    throw(new IllegalArgumentException(String.valueOf(obj)));
	}

	public <I extends IUnknown> MemorySegment addrforobj2(IUnknown obj, IID<I> iface, boolean get, Object retainer) {
	    return(addrforobj(iface.iface.cast(obj), iface, get, retainer));
	}
    }
    final Registry reg = new Registry();

    public abstract MemoryLayout DWORD();
    public abstract MemoryLayout UINT();
    public abstract MemoryLayout ULONG();
    public abstract MemoryLayout HRESULT();
    public abstract MemoryLayout REFIID();
    public abstract MemoryLayout LPVOID();
    public abstract MemoryLayout HWND();

    static class Win64Unicode extends COM {
	private static final MemoryLayout HRESULT    = Win32.Win64Unicode.HRESULT;
	private static final MemoryLayout DWORD      = Win32.Win64Unicode.DWORD;
	private static final MemoryLayout WORD       = Win32.Win64Unicode.WORD;
	private static final MemoryLayout LPVOID     = Win32.Win64Unicode.LPVOID;
	private static final MemoryLayout UINT       = Win32.Win64Unicode.UINT;
	private static final MemoryLayout LONG       = Win32.Win64Unicode.LONG;
	private static final MemoryLayout ULONG      = Win32.Win64Unicode.ULONG;
	private static final MemoryLayout CLIPFORMAT = Win32.Win64Unicode.CLIPFORMAT;
	private static final MemoryLayout HANDLE     = Win32.Win64Unicode.HANDLE;
	private static final MemoryLayout HBITMAP    = Win32.Win64Unicode.HBITMAP;
	private static final MemoryLayout HGLOBAL    = Win32.Win64Unicode.HGLOBAL;
	private static final MemoryLayout HWND       = Win32.Win64Unicode.HWND;
	static final MemoryLayout LPOLESTR = ADDRESS;
	static final MemoryLayout REFCLSID = ADDRESS;
	static final MemoryLayout REFIID   = ADDRESS;
	private final SymbolLookup ole32 = loadlib("OLE32.DLL", Arena.global());

	public MemoryLayout DWORD()   {return(DWORD);}
	public MemoryLayout UINT()    {return(UINT);}
	public MemoryLayout ULONG()   {return(ULONG);}
	public MemoryLayout HRESULT() {return(HRESULT);}
	public MemoryLayout REFIID()  {return(REFIID);}
	public MemoryLayout LPVOID()  {return(LPVOID);}
	public MemoryLayout HWND()    {return(HWND);}

	private final MethodHandle CoUninitialize = ld.downcallHandle(ole32.find("CoUninitialize").get(), FunctionDescriptor.ofVoid());
	public void CoUninitialize() {
	    try {
		CoUninitialize.invoke();
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle CoInitializeEx = ld.downcallHandle(ole32.find("CoInitializeEx").get(), FunctionDescriptor.of(HRESULT, LPVOID, DWORD));
	public void CoInitializeEx(int init) {
	    int rv;
	    try {
		rv = (int)CoInitializeEx.invoke(MemorySegment.NULL, init);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(!SUCCEEDED(rv))
		throw(new HResultError(rv));
	}

	private final MethodHandle OleUninitialize = ld.downcallHandle(ole32.find("OleUninitialize").get(), FunctionDescriptor.ofVoid());
	public void OleUninitialize() {
	    try {
		OleUninitialize.invoke();
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle OleInitialize = ld.downcallHandle(ole32.find("OleInitialize").get(), FunctionDescriptor.of(HRESULT, LPVOID));
	public void OleInitialize() {
	    int rv;
	    try {
		rv = (int)OleInitialize.invoke(MemorySegment.NULL);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(!SUCCEEDED(rv))
		throw(new HResultError(rv));
	}

	private final MethodHandle CoCreateInstance = ld.downcallHandle(ole32.find("CoCreateInstance").get(), FunctionDescriptor.of(HRESULT, REFCLSID, ADDRESS, DWORD, REFIID, LPVOID));
	public <I extends IUnknown> I CoCreateInstance(CLSID clsid, IUnknown outer, int context, IID<I> iid) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment buf = st.allocate(LPVOID);
		MemorySegment outerp = (outer == null) ? MemorySegment.NULL : reg.addrforobj(outer, IUnknown.IID, false, st);
		int rv;
		try {
		    rv = (int)CoCreateInstance.invoke(clsid.mem, outerp, context, iid.mem, buf);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(!SUCCEEDED(rv))
		    throw(new HResultError(rv));
		return(reg.objforaddr(buf.get(ADDRESS, 0), iid, false));
	    }
	}

	private final MethodHandle CoTaskMemAlloc = ld.downcallHandle(ole32.find("CoTaskMemAlloc").get(), FunctionDescriptor.of(LPVOID, SIZE_T));
	MemorySegment CoTaskMemAlloc(int bytes) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)CoTaskMemAlloc.invoke(bytes);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    return(nullp(rv) ? null : rv);
	}

	private final MethodHandle CoTaskMemFree = ld.downcallHandle(ole32.find("CoTaskMemFree").get(), FunctionDescriptor.ofVoid(LPVOID));
	void CoTaskMemFree(MemorySegment mem) {
	    try {
		CoTaskMemFree.invoke(mem);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle RegisterDragDrop = ld.downcallHandle(ole32.find("RegisterDragDrop").get(), FunctionDescriptor.of(HRESULT, HWND, ADDRESS));
	public void RegisterDragDrop(Handle hwnd, IDropTarget droptarget) {
	    MemorySegment dt = reg.addrforobj(droptarget, IDropTarget.IID, true, null);
	    int rv;
	    try {
		rv = (int)RegisterDragDrop.invoke(hwnd.bits, dt);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    reg.release(dt);
	    if(!SUCCEEDED(rv))
		throw(new HResultError(rv));
	}

	private final MethodHandle RevokeDragDrop = ld.downcallHandle(ole32.find("RevokeDragDrop").get(), FunctionDescriptor.of(HRESULT, HWND));
	public void RevokeDragDrop(Handle hwnd) {
	    int rv;
	    try {
		rv = (int)RevokeDragDrop.invoke(hwnd.bits);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(!SUCCEEDED(rv))
		throw(new HResultError(rv));
	}

	static final StructLayout _FORMATETC = struct(new MemoryLayout[] {
		CLIPFORMAT.withName("cfFormat"),
		ADDRESS.withName("ptd"),
		DWORD.withName("dwAspect"),
		LONG.withName("lindex"),
		DWORD.withName("tymed"),
	    });
	public static class FORMATETC extends COM.FORMATETC {
	    private FORMATETC(MemorySegment mem) {
		super(mem);
	    }
	    private FORMATETC(Arena alloc) {
		this(alloc.allocate(_FORMATETC.byteSize()));
	    }
	    private FORMATETC() {
		this(Arena.ofAuto());
	    }

	    protected StructLayout $layout() {return(_FORMATETC);}

	    private static final VarHandle cfFormat = _FORMATETC.varHandle(PathElement.groupElement("cfFormat"));
	    public FORMATETC cfFormat(int value) {cfFormat.set(mem, 0, (short)value); return(this);}
	    public int cfFormat() {return((int)cfFormat.get(mem, 0));}
	    private static final VarHandle dwAspect = _FORMATETC.varHandle(PathElement.groupElement("dwAspect"));
	    public FORMATETC dwAspect(int value) {dwAspect.set(mem, 0, value); return(this);}
	    public int dwAspect() {return((int)dwAspect.get(mem, 0));}
	    private static final VarHandle lindex = _FORMATETC.varHandle(PathElement.groupElement("lindex"));
	    public FORMATETC lindex(int value) {lindex.set(mem, 0, value); return(this);}
	    public int lindex() {return((int)lindex.get(mem, 0));}
	    private static final VarHandle tymed = _FORMATETC.varHandle(PathElement.groupElement("tymed"));
	    public FORMATETC tymed(int value) {tymed.set(mem, 0, value); return(this);}
	    public int tymed() {return((int)tymed.get(mem, 0));}
	}
	public MemoryLayout C_FORMATETC() {return(_FORMATETC);}
	public FORMATETC FORMATETC() {return(new FORMATETC());}
	FORMATETC FORMATETC_fromcom(MemorySegment mem) {return(new FORMATETC(mem.reinterpret(_FORMATETC.byteSize())));}

	static final StructLayout _STGMEDIUM = struct(new MemoryLayout[] {
		DWORD.withName("tymed"),
		MemoryLayout.unionLayout(new MemoryLayout[] {
		    HANDLE.withName("handle"),
		    ADDRESS.withName("address"),
		}).withName("data"),
		ADDRESS.withName("pUnkForRelease"),
	    });
	public static class STGMEDIUM extends COM.STGMEDIUM {
	    private STGMEDIUM(MemorySegment mem) {
		super(mem);
	    }
	    private STGMEDIUM(Arena alloc) {
		this(alloc.allocate(_STGMEDIUM.byteSize()));
	    }
	    private STGMEDIUM() {
		this(Arena.ofAuto());
	    }

	    protected StructLayout $layout() {return(_STGMEDIUM);}

	    private static final VarHandle tymed = _STGMEDIUM.varHandle(PathElement.groupElement("tymed"));
	    public STGMEDIUM tymed(int value) {tymed.set(mem, 0, value); return(this);}
	    public int tymed() {return((int)tymed.get(mem, 0));}
	    private static final VarHandle handle = _STGMEDIUM.varHandle(PathElement.groupElement("data"), PathElement.groupElement("handle"));
	    public STGMEDIUM handle(Handle value) {handle.set(mem, 0, (value == null) ? MemorySegment.NULL : value.bits); return(this);}
	    public Handle handle() {return(Handle.of((MemorySegment)handle.get(mem, 0)));}
	    private static final VarHandle address = _STGMEDIUM.varHandle(PathElement.groupElement("data"), PathElement.groupElement("address"));
	    STGMEDIUM address(MemorySegment value) {address.set(mem, 0, (value == null) ? MemorySegment.NULL : value); return(this);}
	    MemorySegment address() {return((MemorySegment)handle.get(mem, 0));}
	}
	public MemoryLayout C_STGMEDIUM() {return(_STGMEDIUM);}
	public STGMEDIUM STGMEDIUM() {return(new STGMEDIUM());}

	private final MethodHandle ReleaseStgMedium = ld.downcallHandle(ole32.find("ReleaseStgMedium").get(), FunctionDescriptor.ofVoid(ADDRESS));
	void ReleaseStgMedium(MemorySegment sm) {
	    try {
		RevokeDragDrop.invoke(sm);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	{
	    Win32 win = Win32.get();
	    valxf(new ValueConversion(Win32.Win64Unicode._POINT, Coord.class,
				      (st, v) -> win.POINT(st).c((Coord)v),
				      null,
				      v -> win.POINT((MemorySegment)v).c(),
				      null));
	    valxf(new ValueConversion(_STGMEDIUM, COM.STGMEDIUM.class,
				      null,
				      buf -> {
					  STGMEDIUM ret = STGMEDIUM();
					  MemorySegment mem = ret.mem();
					  MemorySegment.copy(buf, 0, mem, 0, ret.$layout().byteSize());
					  Finalizer.finalize(ret, () -> ReleaseStgMedium(mem));
					  return(ret);
				      },
				      null,
				      (v, buf) -> {
					  MemorySegment.copy(((STGMEDIUM)v).mem(), 0, buf, 0, _STGMEDIUM.byteSize());
				      }));
	    valxf(new ValueConversion(ADDRESS, IID.class,
				      (st, v) -> ((IID)v).mem,
				      null, null, null));
	}
    }

    private static COM instance = null;
    public static COM get() {
	if(instance == null) {
	    synchronized(COM.class) {
		if(instance == null)
		    instance = tryload("COM", Win64Unicode::new);
	    }
	}
	return(instance);
    }

    public static void main(String[] args) throws Exception {
	COM com = COM.get();
	com.CoInitializeEx(COINIT_APARTMENTTHREADED | COINIT_DISABLE_OLE1DDE);
	IFileDialog fd = com.CoCreateInstance(IFileDialog.CLSID_FileOpenDialog, null, CLSCTX_INPROC_SERVER, IFileDialog.IID);
	fd.SetFileTypes(new String[][] {{"All files", "*.*"}});
	int cookie = fd.Advise(new IFileDialogEvents() {
	    public void OnFolderChanging(IFileDialog fd, IShellItem folder) {
		Debug.dump(fd, folder.GetDisplayName(IShellItem.SIGDN_FILESYSPATH));
	    }
	});
	fd.Show(null);
	IShellItem ret = fd.GetResult();
	Debug.dump(ret.GetDisplayName(IShellItem.SIGDN_NORMALDISPLAY));
	Debug.dump(ret.GetDisplayName(IShellItem.SIGDN_PARENTRELATIVEPARSING));
	Debug.dump(ret.GetDisplayName(IShellItem.SIGDN_DESKTOPABSOLUTEPARSING));
	Debug.dump(ret.GetDisplayName(IShellItem.SIGDN_PARENTRELATIVEEDITING));
	Debug.dump(ret.GetDisplayName(IShellItem.SIGDN_DESKTOPABSOLUTEEDITING));
	Debug.dump(ret.GetDisplayName(IShellItem.SIGDN_FILESYSPATH));
	Debug.dump(ret.GetDisplayName(IShellItem.SIGDN_URL));
	Debug.dump(ret.GetDisplayName(IShellItem.SIGDN_PARENTRELATIVEFORADDRESSBAR));
	Debug.dump(ret.GetDisplayName(IShellItem.SIGDN_PARENTRELATIVE));
	Debug.dump(ret.GetDisplayName(IShellItem.SIGDN_PARENTRELATIVEFORUI));
	fd = null;
	for(int i = 0; i < 100; i++) {
	    System.gc();
	    Thread.sleep(100);
	}
    }
}
