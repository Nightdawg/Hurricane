package haven;

import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import java.util.prefs.*;

public class XmlPrefs extends AbstractPreferences {
    private final Path path;
    private final Path lockpath;
    private final Properties props;
    private final String prefix;
    private final Object lock;

    private XmlPrefs(Path path, Properties props) {
	super(null, "");
	this.path = path;
	this.lockpath = path.resolveSibling(path.getFileName().toString() + ".lock");
	this.props = props;
	this.prefix = "";
	this.lock = new Object();
    }

    private XmlPrefs(XmlPrefs parent, String name) {
	super(parent, name);
	this.path = parent.path;
	this.lockpath = parent.lockpath;
	this.props = parent.props;
	this.prefix = parent.prefix + name + "/";
	this.lock = parent.lock;
    }

    public static XmlPrefs create(Path path, Preferences migrate) {
	Properties props = new Properties();
	XmlPrefs ret = new XmlPrefs(path, props);
	ret.init(migrate);
	return(ret);
    }

    private void init(Preferences migrate) {
	withFileLock(() -> {
	    boolean loaded = load(props);
	    if(!loaded && migrate != null) {
		try {
		    for(String key : migrate.keys()) {
			String val = migrate.get(key, null);
			if(val != null)
			    props.setProperty(key, val);
		    }
		    write(props);
		} catch(BackingStoreException | SecurityException e) {
		    new Warning(e, "could not migrate old preferences").level(Warning.ERROR).issue();
		}
	    }
	});
    }

    private void withFileLock(Runnable task) {
	synchronized(lock) {
	    Path dir = path.getParent();
	    try {
		if(dir != null)
		    Files.createDirectories(dir);
		try(FileChannel ch = FileChannel.open(lockpath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
		    FileLock ignored = ch.lock()) {
		    task.run();
		}
	    } catch(IOException e) {
		new Warning(e, String.format("could not lock preferences file: %s", path)).level(Warning.ERROR).issue();
	    }
	}
    }

    private boolean load(Properties dst) {
	if(!Files.exists(path))
	    return(false);
	Properties loaded = new Properties();
	try(InputStream fp = Files.newInputStream(path)) {
	    loaded.loadFromXML(fp);
	    dst.clear();
	    dst.putAll(loaded);
	    return(true);
	} catch(IOException e) {
	    new Warning(e, String.format("could not read preferences file: %s", path)).level(Warning.ERROR).issue();
	    return(false);
	}
    }

    private void write(Properties src) {
	Path tmp = null;
	try {
	    Path dir = path.getParent();
	    if(dir != null)
		Files.createDirectories(dir);
	    tmp = Files.createTempFile((dir == null) ? Paths.get(".") : dir, path.getFileName().toString(), ".tmp");
	    try(OutputStream fp = Files.newOutputStream(tmp, StandardOpenOption.TRUNCATE_EXISTING)) {
		src.storeToXML(fp, "Hurricane preferences", "UTF-8");
	    }
	    try {
		Files.move(tmp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
	    } catch(IOException e) {
		/* ATOMIC_MOVE can fail on Windows (e.g. AccessDenied) or on
		 * filesystems that don't support it; fall back to a non-atomic
		 * replace, which is still safe while holding the file lock. */
		Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
	    }
	    tmp = null;
	} catch(IOException e) {
	    new Warning(e, String.format("could not write preferences file: %s", path)).level(Warning.ERROR).issue();
	} finally {
	    if(tmp != null) {
		try {
		    Files.deleteIfExists(tmp);
		} catch(IOException e) {
		    new Warning(e, String.format("could not remove temporary preferences file: %s", tmp)).issue();
		}
	    }
	}
    }

    private void update(String key, String val) {
	withFileLock(() -> {
	    load(props);
	    if(val == null)
		props.remove(key);
	    else
		props.setProperty(key, val);
	    write(props);
	});
    }

    private void reload() {
	withFileLock(() -> load(props));
    }

    protected String getSpi(String key) {
	synchronized(lock) {
	    return(props.getProperty(prefix + key));
	}
    }

    protected void putSpi(String key, String val) {
	update(prefix + key, val);
    }

    protected void removeSpi(String key) {
	update(prefix + key, null);
    }

    protected String[] keysSpi() {
	synchronized(lock) {
	    List<String> keys = new ArrayList<>();
	    for(Object key : props.keySet()) {
		if(key instanceof String) {
		    String k = (String)key;
		    if(k.startsWith(prefix)) {
			String sub = k.substring(prefix.length());
			if(sub.indexOf('/') < 0)
			    keys.add(sub);
		    }
		}
	    }
	    return(keys.toArray(new String[0]));
	}
    }

    protected String[] childrenNamesSpi() {
	synchronized(lock) {
	    Set<String> children = new TreeSet<>();
	    for(Object key : props.keySet()) {
		if(key instanceof String) {
		    String k = (String)key;
		    if(k.startsWith(prefix)) {
			int p = k.indexOf('/', prefix.length());
			if(p >= 0)
			    children.add(k.substring(prefix.length(), p));
		    }
		}
	    }
	    return(children.toArray(new String[0]));
	}
    }

    protected AbstractPreferences childSpi(String name) {
	return(new XmlPrefs(this, name));
    }

    protected void removeNodeSpi() {}
    protected void flushSpi() {
	withFileLock(() -> write(props));
    }
    protected void syncSpi() {reload();}
}
