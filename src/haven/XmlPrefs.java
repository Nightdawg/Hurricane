package haven;

import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import java.util.prefs.*;
import java.util.concurrent.atomic.*;

/* File-backed Preferences implementation that persists to an XML file under
 * the local data directory (see Utils.prefs()). Writes are buffered in memory
 * and flushed to disk asynchronously by a single background thread, so that
 * settings changes never block the calling (typically UI/render) thread on
 * disk I/O or on the inter-process file lock. Rapid changes are coalesced into
 * a single write, and concurrent clients are kept consistent by reloading the
 * on-disk file and merging pending changes on top before each write. */
public class XmlPrefs extends AbstractPreferences {
    /* Identity sentinel marking a pending removal; compared with ==. */
    private static final String REMOVED = new String("\u0000removed\u0000");
    private static final long DEBOUNCE_MS = 200;
    private static final long MAX_DELAY_MS = 1000;
    private static final int LOCK_TRIES = 50;
    private static final long LOCK_WAIT_MS = 20;

    private final Path path;
    private final Path lockpath;
    private final Properties props;
    private final Map<String, String> pending;
    private final String prefix;
    private final Object lock;
    private final Object iolock;
    private final AtomicLong seq;
    private volatile boolean shutdown = false;

    private XmlPrefs(Path path, Properties props) {
	super(null, "");
	this.path = path;
	this.lockpath = path.resolveSibling(path.getFileName().toString() + ".lock");
	this.props = props;
	this.pending = new LinkedHashMap<>();
	this.prefix = "";
	this.lock = new Object();
	this.iolock = new Object();
	this.seq = new AtomicLong();
    }

    private XmlPrefs(XmlPrefs parent, String name) {
	super(parent, name);
	this.path = parent.path;
	this.lockpath = parent.lockpath;
	this.props = parent.props;
	this.pending = parent.pending;
	this.prefix = parent.prefix + name + "/";
	this.lock = parent.lock;
	this.iolock = parent.iolock;
	this.seq = parent.seq;
    }

    public static XmlPrefs create(Path path, Preferences migrate) {
	Properties props = new Properties();
	XmlPrefs ret = new XmlPrefs(path, props);
	ret.init(migrate);
	ret.startWriter();
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

    private void startWriter() {
	Thread writer = new Thread(this::writerloop, "Preferences writer");
	writer.setDaemon(true);
	writer.start();
	try {
	    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
		    shutdown = true;
		    synchronized(lock) {lock.notifyAll();}
		    /* Let the writer finish any in-flight write (it holds iolock
		     * during disk I/O) before we drain whatever remains, so a
		     * change made right before quitting isn't lost. */
		    try {writer.join(2000);} catch(InterruptedException e) {}
		    flushPending();
		}, "Preferences flush"));
	} catch(IllegalStateException e) {
	    /* JVM already shutting down; nothing to register. */
	}
    }

    /* Background loop: wait for pending changes, debounce/coalesce them, then
     * flush once. Runs only on the root node. */
    private void writerloop() {
	while(true) {
	    synchronized(lock) {
		while(pending.isEmpty() && !shutdown) {
		    try {lock.wait();} catch(InterruptedException e) {}
		}
		if(pending.isEmpty() && shutdown)
		    return;
		long start = System.currentTimeMillis();
		long observed = seq.get();
		while(!shutdown) {
		    long remain = MAX_DELAY_MS - (System.currentTimeMillis() - start);
		    if(remain <= 0)
			break;
		    try {lock.wait(Math.min(DEBOUNCE_MS, remain));} catch(InterruptedException e) {}
		    long now = seq.get();
		    if(now == observed)
			break;
		    observed = now;
		}
	    }
	    flushOnce();
	}
    }

    /* Snapshot the pending changes, merge them onto the current on-disk state,
     * and write the result. File I/O happens under iolock (not under lock), so
     * the UI thread can keep reading/writing in-memory prefs meanwhile. */
    private void flushOnce() {
	Map<String, String> batch;
	synchronized(lock) {
	    if(pending.isEmpty())
		return;
	    batch = new LinkedHashMap<>(pending);
	    pending.clear();
	}
	boolean ok = withFileLock(() -> {
		Properties disk = new Properties();
		load(disk);
		for(Map.Entry<String, String> ent : batch.entrySet()) {
		    if(ent.getValue() == REMOVED)
			disk.remove(ent.getKey());
		    else
			disk.put(ent.getKey(), ent.getValue());
		}
		write(disk);
		synchronized(lock) {
		    props.clear();
		    props.putAll(disk);
		    for(Map.Entry<String, String> ent : pending.entrySet()) {
			if(ent.getValue() == REMOVED)
			    props.remove(ent.getKey());
			else
			    props.put(ent.getKey(), ent.getValue());
		    }
		}
	    });
	if(!ok) {
	    synchronized(lock) {
		for(Map.Entry<String, String> ent : batch.entrySet())
		    pending.putIfAbsent(ent.getKey(), ent.getValue());
		seq.incrementAndGet();
	    }
	    try {Thread.sleep(LOCK_WAIT_MS * 5);} catch(InterruptedException e) {}
	}
    }

    /* Synchronously drain all pending changes to disk (used by flush() and on
     * JVM shutdown). */
    private void flushPending() {
	for(int i = 0; i < 8; i++) {
	    synchronized(lock) {
		if(pending.isEmpty())
		    return;
	    }
	    flushOnce();
	}
    }

    private boolean withFileLock(Runnable task) {
	synchronized(iolock) {
	    Path dir = path.getParent();
	    try {
		if(dir != null)
		    Files.createDirectories(dir);
		try(FileChannel ch = FileChannel.open(lockpath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
		    FileLock fl = null;
		    try {
			for(int i = 0; (i < LOCK_TRIES) && (fl == null); i++) {
			    try {
				fl = ch.tryLock();
			    } catch(OverlappingFileLockException e) {
				/* Should not happen since iolock serializes us
				 * within this JVM, but guard regardless. */
			    }
			    if(fl == null) {
				try {
				    Thread.sleep(LOCK_WAIT_MS);
				} catch(InterruptedException ie) {
				    Thread.currentThread().interrupt();
				    return(false);
				}
			    }
			}
			if(fl == null)
			    return(false);
			task.run();
			return(true);
		    } finally {
			if(fl != null)
			    fl.release();
		    }
		}
	    } catch(IOException e) {
		new Warning(e, String.format("could not lock preferences file: %s", path)).level(Warning.ERROR).issue();
		return(false);
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

    private void signal(String key, String val) {
	synchronized(lock) {
	    if(val == REMOVED)
		props.remove(key);
	    else
		props.setProperty(key, val);
	    pending.put(key, val);
	    seq.incrementAndGet();
	    lock.notifyAll();
	}
    }

    protected String getSpi(String key) {
	synchronized(lock) {
	    return(props.getProperty(prefix + key));
	}
    }

    protected void putSpi(String key, String val) {
	signal(prefix + key, val);
    }

    protected void removeSpi(String key) {
	signal(prefix + key, REMOVED);
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
	flushPending();
    }

    protected void syncSpi() {
	withFileLock(() -> {
	    Properties disk = new Properties();
	    if(load(disk)) {
		synchronized(lock) {
		    props.clear();
		    props.putAll(disk);
		    for(Map.Entry<String, String> ent : pending.entrySet()) {
			if(ent.getValue() == REMOVED)
			    props.remove(ent.getKey());
			else
			    props.put(ent.getKey(), ent.getValue());
		    }
		}
	    }
	});
    }
}
