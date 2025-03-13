package haven;

import java.util.prefs.*;
import org.json.JSONArray;
import java.util.*;
import java.nio.*;

public class RegistryStore {
    public static final Config.Variable<String> prefspec = Config.Variable.prop("haven.prefspec", "hafen-Hurricane");
    public static Preferences prefs() {
        return Utils.prefs();
    }

    public static String getpref(String prefname, String def) {
	try {
	    return(prefs().get(prefname, def));
	} catch(SecurityException e) {
	    return(def);
	}
    }

    public static void setpref(String prefname, String val) {
	try {
	    if(val == null)
		prefs().remove(prefname);
	    else
		prefs().put(prefname, val);
	} catch(SecurityException e) {
	}
    }

	static String[] getprefsa(String prefname, String[] def) { // ND: Get prefs array
		try {
			String jsonstr = getpref(prefname, null);
			if (jsonstr == null)
				return def;
			JSONArray ja = new JSONArray(jsonstr);
			String[] ra = new String[ja.length()];
			for (int i = 0; i < ja.length(); i++)
				ra[i] = ja.getString(i);
			return ra;
		} catch (SecurityException e) {
			return def;
		} catch (Exception ex) {
			ex.printStackTrace();
			return def;
		}
	}

	static void setprefsa(String prefname, String[] val) { // ND: Set prefs array
		try {
			String jsonarr = "";
			for (String s : val)
				jsonarr += "\"" + s + "\",";
			if (jsonarr.length() > 0)
				jsonarr = jsonarr.substring(0, jsonarr.length() - 1);
			setpref(prefname, "[" + jsonarr + "]");
		} catch (SecurityException e) {
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

    public static int getprefi(String prefname, int def) {
	try {
	    return(prefs().getInt(prefname, def));
	} catch(SecurityException e) {
	    return(def);
	}
    }

    public static void setprefi(String prefname, int val) {
	try {
	    prefs().putInt(prefname, val);
	} catch(SecurityException e) {
	}
    }

    public static double getprefd(String prefname, double def) {
	try {
	    return(prefs().getDouble(prefname, def));
	} catch(SecurityException e) {
	    return(def);
	}
    }

    public static void setprefd(String prefname, double val) {
	try {
	    prefs().putDouble(prefname, val);
	} catch(SecurityException e) {
	}
    }

    public static boolean getprefb(String prefname, boolean def) {
	try {
	    return(prefs().getBoolean(prefname, def));
	} catch(SecurityException e) {
	    return(def);
	}
    }

    public static void setprefb(String prefname, boolean val) {
	try {
	    prefs().putBoolean(prefname, val);
	} catch(SecurityException e) {
	}
    }

    public static Coord getprefc(String prefname, Coord def) {
	try {
	    String val = prefs().get(prefname, null);
	    if(val == null)
		return(def);
	    int x = val.indexOf('x');
	    if(x < 0)
		return(def);
	    return(new Coord(Integer.parseInt(val.substring(0, x)), Integer.parseInt(val.substring(x + 1))));
	} catch(SecurityException e) {
	    return(def);
	}
    }

    public static void setprefc(String prefname, Coord val) {
	try {
	    String enc = (val == null) ? "" : val.x + "x" + val.y;
	    prefs().put(prefname, enc);
	} catch(SecurityException e) {
	}
    }

    public static byte[] getprefb(String prefname, byte[] def) {
	try {
	    return(prefs().getByteArray(prefname, def));
	} catch(SecurityException e) {
	    return(def);
	}
    }

    public static void setprefb(String prefname, byte[] val) {
	try {
	    prefs().putByteArray(prefname, val);
	} catch(SecurityException e) {
	}
    }

    public static List<String> getprefsl(String prefname, String[] def) {
	byte[] enc = getprefb(prefname, null);
	if(enc == null)
	    return((def == null) ? null : Arrays.asList(def));
	ByteBuffer buf = ByteBuffer.wrap(enc);
	ArrayList<String> ret = new ArrayList<>();
	for(int i = 0, s = 0; i < buf.capacity(); i++) {
	    if(buf.get(i) == 0) {
		buf.position(s).limit(i);
		CharBuffer dec = Utils.utf8.decode(buf);
		ret.add(dec.toString());
		s = i + 1;
		buf.limit(buf.capacity());
	    }
	}
	ret.trimToSize();
	return(ret);
    }

    public static void setprefsl(String prefname, Iterable<? extends CharSequence> val) {
	ByteBuffer buf = ByteBuffer.allocate(1024);
	for(CharSequence str : val) {
	    ByteBuffer enc = Utils.utf8.encode(CharBuffer.wrap(str));
	    buf = Utils.growbuf(buf, enc.remaining() + 1);
	    buf.put(enc);
	    buf.put((byte)0);
	}
	buf.flip();
	byte[] enc = new byte[buf.remaining()];
	buf.get(enc);
	setprefb(prefname, enc);
    }

    public static String getprop(String propname, String def) {
	try {
	    String ret;
	    if((ret = System.getProperty(propname)) != null)
		return(ret);
	    return(def);
	} catch(SecurityException e) {
	    return(def);
	}
    }
}
