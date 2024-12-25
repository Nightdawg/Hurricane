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

package haven;

import haven.automated.mapper.MappingClient;

import java.util.*;
import java.util.function.*;
import java.io.*;
import java.nio.file.*;
import java.net.URI;
import java.net.URLConnection;
import java.io.PrintStream;

public class Config {
    public static final Properties jarprops = getjarprops();
    public static final String confid = "Hurricane";
    public static final Variable<Boolean> par = Variable.def(() -> true);
    public final Properties localprops = getlocalprops();
	public static final String clientVersion = "v1.18";
	public static String githubLatestVersion = "Loading...";

    private static Config global = null;
    public static Config get() {
	if(global != null)
	    return(global);
	synchronized(Config.class) {
	    if(global == null)
		global = new Config();
	    return(global);
	}
    }

    private static Properties getjarprops() {
	Properties ret = new Properties();
	try(InputStream fp = Config.class.getResourceAsStream("boot-props")) {
	    if(fp != null)
		ret.load(fp);
	} catch(Exception exc) {
	    /* XXX? Catch all exceptions? It just seems dumb to
	     * potentially crash here for unforeseen reasons. */
	    new Warning(exc, "unexpected error occurred when loading local properties").issue();
	}
	return(ret);
    }

    private static Properties getlocalprops() {
	Properties ret = new Properties();
	try {
	    Path jar = Utils.srcpath(Config.class);
	    if(jar != null) {
		try(InputStream fp = Files.newInputStream(jar.resolveSibling("haven-config.properties"))) {
		    ret.load(fp);
		} catch(NoSuchFileException exc) {
		    /* That's quite alright. */
		}
	    }
	} catch(Exception exc) {
	    new Warning(exc, "unexpected error occurred when loading neighboring properties").issue();
	}
	return(ret);
    }

    public String getprop(String name, String def) {
	String ret;
	if((ret = jarprops.getProperty(name)) != null)
	    return(ret);
	if((ret = localprops.getProperty(name)) != null)
	    return(ret);
	return(Utils.getprop(name, def));
    }

    public static final Path parsepath(String p) {
	if((p == null) || p.equals(""))
	    return(null);
	return(Utils.path(p));
    }

    public static final URI parseuri(String url) {
	if((url == null) || url.equals(""))
	    return(null);
	return(Utils.uri(url));
    }

    public static void parsesvcaddr(String spec, Consumer<String> host, Consumer<Integer> port) {
	if((spec.length() > 0) && (spec.charAt(0) == '[')) {
	    int p = spec.indexOf(']');
	    if(p > 0) {
		String hspec = spec.substring(1, p);
		if(spec.length() == p + 1) {
		    host.accept(hspec);
		    return;
		} else if((spec.length() > p + 1) && (spec.charAt(p + 1) == ':')) {
		    host.accept(hspec);
		    port.accept(Integer.parseInt(spec.substring(p + 2)));
		    return;
		}
	    }
	}
	int p = spec.indexOf(':');
	if(p >= 0) {
	    host.accept(spec.substring(0, p));
	    port.accept(Integer.parseInt(spec.substring(p + 1)));
	    return;
	} else {
	    host.accept(spec);
	    return;
	}
    }

    public static class Variable<T> {
	public final Function<Config, T> init;
	private boolean inited = false;
	private T val;

	private Variable(Function<Config, T> init) {
	    this.init = init;
	}

	public T get() {
	    if(!inited) {
		synchronized(this) {
		    if(!inited) {
			val = init.apply(Config.get());
			inited = true;
		    }
		}
	    }
	    return(val);
	}

	public void set(T val) {
	    synchronized(this) {
		inited = true;
		this.val = val;
	    }
	}

	public static <V> Variable<V> def(Supplier<V> defval) {
	    return(new Variable<>(cfg -> defval.get()));
	}

	public static <V> Variable<V> prop(String name, Function<String, V> parse, Supplier<V> defval) {
	    return(new Variable<>(cfg -> {
			String pv = cfg.getprop(name, null);
			return((pv == null) ? defval.get() : parse.apply(pv));
	    }));
	}

	public static Variable<String> prop(String name, String defval) {
	    return(prop(name, Function.identity(), () -> defval));
	}
	public static Variable<Integer> propi(String name, int defval) {
	    return(prop(name, Integer::parseInt, () -> defval));
	}
	public static Variable<Boolean> propb(String name, boolean defval) {
	    return(prop(name, Utils::parsebool, () -> defval));
	}
	public static Variable<Double> propf(String name, Double defval) {
	    return(prop(name, Double::parseDouble, () -> defval));
	}
	public static Variable<byte[]> propb(String name, byte[] defval) {
	    return(prop(name, Utils::hex2byte, () -> defval));
	}
	public static Variable<URI> propu(String name, URI defval) {
	    return(prop(name, Config::parseuri, () -> defval));
	}
	public static Variable<URI> propu(String name, String defval) {
	    return(propu(name, parseuri(defval)));
	}
	public static Variable<Path> propp(String name, Path defval) {
	    return(prop(name, Config::parsepath, () -> defval));
	}
	public static Variable<Path> propp(String name, String defval) {
	    return(propp(name, parsepath(defval)));
	}
    }

    public static class Services {
	public static final Variable<URI> directory = Config.Variable.propu("haven.svcdir", "");
	public final URI rel;
	public final Properties props;

	public Services(URI rel, Properties props) {
	    this.rel = rel;
	    this.props = props;
	}

	private static Services fetch(URI uri) {
	    Properties props = new Properties();
	    if(uri != null) {
		Object[] data;
		try {
		    try(InputStream fp = Http.fetch(uri.toURL())) {
			data = new StreamMessage(fp).list();
		    }
		} catch(IOException exc) {
		    throw(new RuntimeException(exc));
		}
		for(Object d : data) {
		    Object[] p = (Object[])d;
		    props.put(p[0], p[1]);
		}
	    }
	    return(new Services(uri, props));
	}

	private static Services global = null;
	public static Services get() {
	    if(global != null)
		return(global);
	    synchronized(Services.class) {
		if(global == null)
		    global = fetch(directory.get());
		return(global);
	    }
	}

	public URI geturi(String name) {
	    String val = props.getProperty(name);
	    if(val == null)
		return(null);
	    return(rel.resolve(parseuri(val)));
	}

	public static Variable<URI> var(String name, String defval) {
	    URI def = parseuri(defval);
	    return new Variable<URI>(cfg -> {
		    String pv = cfg.getprop("haven." + name, null);
		    if(pv != null)
			return(parseuri(pv));
		    return(Services.get().geturi(name));
	    });
	}
    }

    private static void usage(PrintStream out) {
	out.println("usage: haven.jar [OPTIONS] [SERVER[:PORT]]");
	out.println("Options include:");
	out.println("  -h                 Display this help");
	out.println("  -d                 Display debug text");
	out.println("  -P                 Enable profiling");
	out.println("  -G                 Enable GPU profiling");
	out.println("  -f                 Fullscreen mode");
	out.println("  -U URL             Use specified external resource URL");
	out.println("  -r DIR             Use specified resource directory (or HAVEN_RESDIR)");
	out.println("  -A AUTHSERV[:PORT] Use specified authentication server");
	out.println("  -u USER            Authenticate as USER (together with -C)");
	out.println("  -C HEXCOOKIE       Authenticate with specified hex-encoded cookie");
	out.println("  -p PREFSPEC        Use alternate preference prefix");
    }

    public static void cmdline(String[] args) {
	PosixArgs opt = PosixArgs.getopt(args, "hdPGfU:r:A:u:C:p:");
	if(opt == null) {
	    usage(System.err);
	    System.exit(1);
	}
	for(char c : opt.parsed()) {
	    switch(c) {
	    case 'h':
		usage(System.out);
		System.exit(0);
		break;
	    case 'd':
		UIPanel.dbtext.set(true);
		break;
	    case 'P':
		UIPanel.profile.set(true);
		break;
	    case 'G':
		UIPanel.profilegpu.set(true);
		break;
	    case 'f':
		MainFrame.initfullscreen.set(true);
		break;
	    case 'r':
		Resource.resdir.set(Utils.path(opt.arg));
		break;
	    case 'A':
		parsesvcaddr(opt.arg, Bootstrap.authserv::set, Bootstrap.authport::set);
		break;
	    case 'U':
		try {
		    Resource.resurl.set(Utils.uri(opt.arg));
		} catch(IllegalArgumentException e) {
		    System.err.println(e);
		    System.exit(1);
		}
		break;
	    case 'u':
		Bootstrap.authuser.set(opt.arg);
		break;
	    case 'C':
		Bootstrap.authck.set(Utils.hex2byte(opt.arg));
		break;
	    case 'p':
		Utils.prefspec.set(opt.arg);
		break;
	    }
	}
	if(opt.rest.length > 0)
	    parsesvcaddr(opt.rest[0], Bootstrap.defserv::set, Bootstrap.mainport::set);
	if(opt.rest.length > 1)
	    Bootstrap.servargs.set(Utils.splice(opt.rest, 1));
    }

    static {
	Console.setscmd("par", new Console.Command() {
		public void run(Console cons, String[] args) {
		    par.set(Utils.parsebool(args[1]));
		}
	    });
    }

	public static final LinkedHashMap<String, String> properKeyNames = new LinkedHashMap<String, String>(){{
		put("None", " ");
		put("Back Quote", "`");
		put("Equals", "=");
		put("Minus", "-");
		put("Open Bracket", "[");
		put("Close Bracket", "]");
		put("Back Slash", "\\");
		put("Semicolon", ";");
		put("Quote", "'");
		put("Period", ".");
		put("Comma", ",");
		put("Slash", "/");
		put("Up", "↑");
		put("Down", "↓");
		put("Left", "←");
		put("Right", "→");
	}};

	public static final HashMap<String, String> mandatoryAlwaysEnabledMapIcons = new LinkedHashMap<String, String>(){{
		// ND: Map Icons that should ALWAYS be enabled. Players should be forced to see these at all times.
		// The key is the icon name, the value is the error message sent when they try to disable the icon.
		put("Player", "You should ALWAYS see players on the map. I don't care what you have to say.");
		put("Cave Passage", "Let's keep Caves visible, yeah?");
		put("Swirling Vortex", "Vortexes can be dangerous. You don't want to miss them, right?");
		put("Boost Speed", "You need to see Speed Boosts at all times. Keep them enabled.");
		put("Burrow", "Burrows can hide things, like someone trying to ambush you. Keep them enabled.");
	}};

	public static final List<String> statsAndAttributesOrder = new ArrayList<String>(){{
		// ND: I ordered them backwards, in case there's some stupid stat I overlooked, like "Swimming".
		// When they're ordered like this, the overlooked stat should show up last, rather than first.
		add("Swimming");
		add("Lore");
		add("Survival");
		add("Farming");
		add("Cooking");
		add("Carpentry");
		add("Masonry");
		add("Smithing");
		add("Sewing");
		add("Stealth");
		add("Exploration");
		add("Marksmanship");
		add("Melee Combat");
		add("Unarmed Combat");
		add("Psyche");
		add("Will");
		add("Dexterity");
		add("Charisma");
		add("Perception");
		add("Constitution");
		add("Intelligence");
		add("Agility");
		add("Strength");
	}};

	public static final String[] critterResPaths = {
			"gfx/kritter/bayshrimp/bayshrimp",
			"gfx/kritter/bogturtle/bogturtle",
			"gfx/kritter/brimstonebutterfly/brimstonebutterfly",
			"gfx/kritter/cavecentipede/cavecentipede",
			"gfx/kritter/cavemoth/cavemoth",
			"gfx/kritter/chicken/chick",
			"gfx/kritter/chicken/chicken", // ND: This seems to be the model for wild chickens, both hens and roosters.
			"gfx/kritter/chicken/hen", // ND: This might be pointless?
			"gfx/kritter/chicken/rooster", // ND: This might be pointless?
			"gfx/kritter/crab/crab",
			"gfx/kritter/dragonfly/dragonfly",
			"gfx/kritter/earthworm/earthworm",
			"gfx/kritter/firefly/firefly",
			"gfx/kritter/forestlizard/forestlizard",
			"gfx/kritter/forestsnail/forestsnail",
			"gfx/kritter/frog/frog",
			"gfx/kritter/grasshopper/grasshopper",
			"gfx/kritter/hedgehog/hedgehog",
			"gfx/kritter/irrbloss/irrbloss",
			"gfx/kritter/jellyfish/jellyfish",
			"gfx/kritter/ladybug/ladybug",
			"gfx/kritter/lobster/lobster",
			"gfx/kritter/magpie/magpie",
			"gfx/kritter/mallard/mallard", // ND: I haven't checked yet, but I assume it could be the same case as with the chickens
			"gfx/kritter/mallard/mallard-f", // ND: This might be pointless?
			"gfx/kritter/mallard/mallard-m", // ND: This might be pointless?
			"gfx/kritter/mole/mole",
			"gfx/kritter/monarchbutterfly/monarchbutterfly",
			"gfx/kritter/moonmoth/moonmoth",
			"gfx/kritter/opiumdragon/opiumdragon",
			"gfx/kritter/ptarmigan/ptarmigan",
			"gfx/kritter/quail/quail",
			"gfx/kritter/rat/rat",
			"gfx/kritter/rockdove/rockdove",
			"gfx/kritter/sandflea/sandflea",
			"gfx/kritter/seagull/seagull",
			"gfx/kritter/silkmoth/silkmoth",
			"gfx/kritter/springbumblebee/springbumblebee",
			"gfx/kritter/squirrel/squirrel",
			"gfx/kritter/stagbeetle/stagbeetle",
			"gfx/kritter/stalagoomba/stalagoomba",
			"gfx/kritter/tick/tick",
			"gfx/kritter/toad/toad",
			"gfx/kritter/waterstrider/waterstrider",
			"gfx/kritter/woodgrouse/woodgrouse-f", // ND: Only female can be chased, males will fight you
			"gfx/kritter/woodworm/woodworm",
			"gfx/kritter/whirlingsnowflake/whirlingsnowflake",
			"gfx/kritter/bullfinch/bullfinch",

			"gfx/terobjs/items/grub", // ND: lmao
			"gfx/terobjs/items/hoppedcow",
			"gfx/terobjs/items/mandrakespirited",
			"gfx/terobjs/items/itsybitsyspider",
	};

	public static final String[] beastResPaths = {
			"gfx/kritter/bear/bear",
			"gfx/kritter/lynx/lynx",
			"gfx/kritter/walrus/walrus",
			"gfx/kritter/mammoth/mammoth",
			"gfx/kritter/troll/troll",
			"gfx/kritter/spermwhale/spermwhale",
			"gfx/kritter/orca/orca",
			"gfx/kritter/moose/moose",
			"gfx/kritter/wolf/wolf",
			"gfx/kritter/bat/bat",
			"gfx/kritter/goldeneagle/goldeneagle",
			"gfx/kritter/eagleowl/eagleowl",
			"gfx/kritter/caveangler/caveangler",
			"gfx/kritter/boar/boar",
			"gfx/kritter/badger/badger",
			"gfx/kritter/wolverine/wolverine",
			"gfx/kritter/boreworm/boreworm",
			"gfx/kritter/ooze/greenooze",
			"gfx/kritter/adder/adder",
			"gfx/kritter/rat/caverat",
			"gfx/kritter/goat/wildgoat",
			"gfx/kritter/cavelouse/cavelouse"
	};

	public static final String[] housesResPaths = {
			"gfx/terobjs/arch/logcabin",
			"gfx/terobjs/arch/timberhouse",
			"gfx/terobjs/arch/stonestead",
			"gfx/terobjs/arch/stonemansion",
			"gfx/terobjs/arch/greathall",
			"gfx/terobjs/arch/stonetower",
			"gfx/terobjs/arch/windmill",
			"gfx/terobjs/arch/primitivetent",
			"gfx/terobjs/arch/greenhouse",
	};

	public static final String[] containersResPaths = {
			// ND: Each container might have different peekrbufs for each state. This needs to be checked for each new container, in each state (Empty & Closed || Empty & Open, Full & Closed || Full & Open).
			"gfx/terobjs/cupboard",
			"gfx/terobjs/chest",
			"gfx/terobjs/crate",
			"gfx/terobjs/largechest",
			"gfx/terobjs/coffer",
			"gfx/terobjs/exquisitechest",
			//"gfx/terobjs/wbasket", // ND: This one only has open/closed peekrbufs, no other fullness indicators lmao
			"gfx/terobjs/birchbasket",
			"gfx/terobjs/metalcabinet",
			"gfx/terobjs/stonecasket",
			"gfx/terobjs/bonechest",
			"gfx/terobjs/leatherbasket",
			"gfx/terobjs/woodbox",
			"gfx/terobjs/linencrate",
			"gfx/terobjs/map/stonekist",
			"gfx/terobjs/map/jotunclam",
			"gfx/terobjs/thatchbasket",
			"gfx/terobjs/barrel",
	};

	public static final String[] workstationsResPaths = {
			"gfx/terobjs/ttub",
			"gfx/terobjs/dframe",
			"gfx/terobjs/cheeserack",
			"gfx/terobjs/gardenpot",
	};


	public final static Set<String> stoneItemBaseNames = new HashSet<String>(Arrays.asList(
			"gneiss",
			"basalt",
			"cinnabar",
			"sunstone",
			"dolomite",
			"feldspar",
			"flint",
			"granite",
			"hornblende",
			"limestone",
			"marble",
			"porphyry",
			"quartz",
			"sandstone",
			"schist",
			"zincspar",
			"apatite",
			"sodalite",
			"fluorospar",
			"soapstone",
			"olivine",
			"gabbro",
			"alabaster",
			"microlite",
			"mica",
			"kyanite",
			"corund",
			"orthoclase",
			"breccia",
			"diabase",
			"arkose",
			"diorite",
			"slate",
			"jasper",
			"rhyolite",
			"pegmatite",
			"greenschist",
			"eclogite",
			"pumice",
			"serpentine",
			"chert",
			"graywacke",
			"halite" // rock salt
	));

	public final static Set<String> coalItemBaseNames = new HashSet<String>(Arrays.asList(
			"blackcoal",
			"coal"
	));

	public final static Set<String> oreItemBaseNames = new HashSet<String>(Arrays.asList(
			"cassiterite",
			"chalcopyrite",
			"malachite",
			"ilmenite",
			"limonite",
			"hematite",
			"magnetite",
			"peacockore",
			"leadglance",
			"cuprite"
	));

	public final static Set<String> preciousOreItemBaseNames = new HashSet<String>(Arrays.asList(
			"galena",
			"argentite",
			"hornsilver",
			"petzite",
			"sylvanite",
			"nagyagite"
	));

	public final static Set<String> minedCuriosItemBaseNames = new HashSet<String>(Arrays.asList(
			"catgold",
			"petrifiedshell",
			"strangecrystal"
	));

	public static final HashSet<String> maneuvers =  new HashSet<>(Arrays.asList(
			"paginae/atk/toarms", "paginae/atk/shield", "paginae/atk/parry",
			"paginae/atk/oakstance", "paginae/atk/dorg", "paginae/atk/chinup",
			"paginae/atk/bloodlust", "paginae/atk/combmed"));
	public static final HashMap<String, Long> nonAttackDefences = new HashMap<String, Long>()
	{{
		put("paginae/atk/regain", 2100L);
		put("paginae/atk/zigzag", 3000L);
		put("paginae/atk/yieldground", 1800L);
		put("paginae/atk/sidestep", 1500L);
		put("paginae/atk/qdodge", 1500L);
		put("paginae/atk/jump", 1500L);
		put("paginae/atk/artevade", 2400L);
		put("paginae/atk/dash", 3200L); // ND: Assuming 5/5 cards are used, this is the minimum cooldown. Nobody uses dash, but just to be safe, I added it here.

		// ND: Every other "defense" move is actually an attack, which is affected by agility, so we don't add them here.
	}};

	// TODO: ND: I need to remake these so they're not hardcoded. Matias gave me some hint on how to do this
	// ND: All of these stupid values need to be hardcoded this way. There are ranges between the cooldown is set.
	// The server doesn't send you a decimal cooldown, and each move and melee attack seems to have absolutely no pattern for those ranges.
	// What, you're using a b12? Well 125% attack speed means there are more cooldown ranges too, cause fuck you lmao. Same for cutblade.
	// I'm not bothering with the pickaxe. What moron would bring a pickaxe to combat anyway?
	public static final HashMap<String, Double> meleeAttackMoves = new HashMap<String, Double>()
	{{
		put("paginae/atk/cleave", 80D);
		put("paginae/atk/chop", 40D);
//		put("paginae/atk/fullcircle", 40D); // ND: Full Circle attacks multiple targets.
		put("paginae/atk/barrage", 20D);
		put("paginae/atk/ravenbite", 40D);
		put("paginae/atk/sideswipe", 25D);
		put("paginae/atk/sting", 50D);
//		put("paginae/atk/sos", 50D); // ND: Storm of Swords attacks multiple targets.
	}};

	public static final HashMap<String, Double> unarmedAttackMoves = new HashMap<String, Double>()
	{{
		put("paginae/atk/flex", 30D);
		put("paginae/atk/gojug", 40D); // ND: Jugular tooltip says cooldown is 45, but it's actually 40. SMH.
		put("paginae/atk/haymaker", 50D);
		put("paginae/atk/kick", 45D);
		put("paginae/atk/knockteeth", 35D);
		put("paginae/atk/lefthook", 40D);
		put("paginae/atk/lowblow", 50D);
		put("paginae/atk/oppknock", 45D);
		put("paginae/atk/pow", 30D);
//		put("paginae/atk/punchboth", 40D); // ND: Punch 'em Both attacks 2 targets.
		put("paginae/atk/ripapart", 60D);
		put("paginae/atk/stealthunder", 40D);
		put("paginae/atk/takedown", 50D);
		put("paginae/atk/uppercut", 30D);
	}};
	public static final HashMap<Double, HashMap<Double, ArrayList<Double>>> attackCooldownNumbers = new HashMap<Double, HashMap<Double, ArrayList<Double>>>(){{
		put(20D, new HashMap<Double, ArrayList<Double>>(){{
			put (18D, new ArrayList<Double>(){{add(0D);add(0.579D);}});
			put (19D, new ArrayList<Double>(){{add(0.580D);add(0.837D);}});
			put (20D, new ArrayList<Double>(){{add(0.838D);add(1.188D);}});
			put (21D, new ArrayList<Double>(){{add(1.189D);add(1.659D);}});
			put (22D, new ArrayList<Double>(){{add(1.660D);add(2D);}});
		}});
		put(25D, new HashMap<Double, ArrayList<Double>>(){{
			put (23D, new ArrayList<Double>(){{add(0D);add(0.648D);}});
			put (24D, new ArrayList<Double>(){{add(0.649D);add(0.868D);}});
			put (25D, new ArrayList<Double>(){{add(0.869D);add(1.148D);}});
			put (26D, new ArrayList<Double>(){{add(1.149D);add(1.503D);}});
			put (27D, new ArrayList<Double>(){{add(1.504D);add(1.948D);}});
			put (28D, new ArrayList<Double>(){{add(1.949D);add(2D);}});
		}});
		put(30D, new HashMap<Double, ArrayList<Double>>(){{
			put (27D, new ArrayList<Double>(){{add(0D);add(0.543D);}});
			put (28D, new ArrayList<Double>(){{add(0.544D);add(0.698D);}});
			put (29D, new ArrayList<Double>(){{add(0.699D);add(0.889D);}});
			put (30D, new ArrayList<Double>(){{add(0.890D);add(1.122D);}});
			put (31D, new ArrayList<Double>(){{add(1.123D);add(1.407D);}});
			put (32D, new ArrayList<Double>(){{add(1.408D);add(1.751D);}});
			put (33D, new ArrayList<Double>(){{add(1.752D);add(2D);}});
		}});
		put(35D, new HashMap<Double, ArrayList<Double>>(){{
			put (32D, new ArrayList<Double>(){{add(0D);add(0.595D);}});
			put (33D, new ArrayList<Double>(){{add(0.596D);add(0.735D);}});
			put (34D, new ArrayList<Double>(){{add(0.736D);add(0.904D);}});
			put (35D, new ArrayList<Double>(){{add(0.905D);add(1.104D);}});
			put (36D, new ArrayList<Double>(){{add(1.105D);add(1.341D);}});
			put (37D, new ArrayList<Double>(){{add(1.342D);add(1.620D);}});
			put (38D, new ArrayList<Double>(){{add(1.621D);add(1.948D);}});
			put (39D, new ArrayList<Double>(){{add(1.949D);add(2D);}});
		}});
		put(40D, new HashMap<Double, ArrayList<Double>>(){{
			put (36D, new ArrayList<Double>(){{add(0D);add(0.526D);}});
			put (37D, new ArrayList<Double>(){{add(0.527D);add(0.636D);}});
			put (38D, new ArrayList<Double>(){{add(0.637D);add(0.765D);}});
			put (39D, new ArrayList<Double>(){{add(0.766D);add(0.915D);}});
			put (40D, new ArrayList<Double>(){{add(0.916D);add(1.090D);}});
			put (41D, new ArrayList<Double>(){{add(1.091D);add(1.293D);}});
			put (42D, new ArrayList<Double>(){{add(1.294D);add(1.528D);}});
			put (43D, new ArrayList<Double>(){{add(1.529D);add(1.798D);}});
			put (44D, new ArrayList<Double>(){{add(1.799D);add(2D);}});
		}});
		put(45D, new HashMap<Double, ArrayList<Double>>(){{
			put (41D, new ArrayList<Double>(){{add(0D);add(0.567D);}});
			put (42D, new ArrayList<Double>(){{add(0.568D);add(0.670D);}});
			put (43D, new ArrayList<Double>(){{add(0.671D);add(0.788D);}});
			put (44D, new ArrayList<Double>(){{add(0.789D);add(0.924D);}});
			put (45D, new ArrayList<Double>(){{add(0.925D);add(1.080D);}});
			put (46D, new ArrayList<Double>(){{add(1.081D);add(1.258D);}});
			put (47D, new ArrayList<Double>(){{add(1.259D);add(1.460D);}});
			put (48D, new ArrayList<Double>(){{add(1.461D);add(1.689D);}});
			put (49D, new ArrayList<Double>(){{add(1.690D);add(1.948D);}});
			put (50D, new ArrayList<Double>(){{add(1.949D);add(2D);}});

		}});
		put(50D, new HashMap<Double, ArrayList<Double>>(){{
			put (45D, new ArrayList<Double>(){{add(0D);add(0.516D);}});
			put (46D, new ArrayList<Double>(){{add(0.517D);add(0.601D);}});
			put (47D, new ArrayList<Double>(){{add(0.602D);add(0.698D);}});
			put (48D, new ArrayList<Double>(){{add(0.699D);add(0.807D);}});
			put (49D, new ArrayList<Double>(){{add(0.808D);add(0.932D);}});
			put (50D, new ArrayList<Double>(){{add(0.933D);add(1.072D);}});
			put (51D, new ArrayList<Double>(){{add(1.073D);add(1.229D);}});
			put (52D, new ArrayList<Double>(){{add(1.230D);add(1.407D);}});
			put (53D, new ArrayList<Double>(){{add(1.408D);add(1.605D);}});
			put (54D, new ArrayList<Double>(){{add(1.606D);add(1.828D);}});
			put (55D, new ArrayList<Double>(){{add(1.829D);add(2D);}});
		}});
		put(60D, new HashMap<Double, ArrayList<Double>>(){{
			put (54D, new ArrayList<Double>(){{add(0D);add(0.510D);}});
			put (55D, new ArrayList<Double>(){{add(0.511D);add(0.579D);}});
			put (56D, new ArrayList<Double>(){{add(0.580D);add(0.656D);}});
			put (57D, new ArrayList<Double>(){{add(0.657D);add(0.742D);}});
			put (58D, new ArrayList<Double>(){{add(0.743D);add(0.837D);}});
			put (59D, new ArrayList<Double>(){{add(0.838D);add(0.943D);}});
			put (60D, new ArrayList<Double>(){{add(0.944D);add(1.059D);}});
			put (61D, new ArrayList<Double>(){{add(1.060D);add(1.188D);}});
			put (62D, new ArrayList<Double>(){{add(1.189D);add(1.330D);}});
			put (63D, new ArrayList<Double>(){{add(1.331D);add(1.487D);}});
			put (64D, new ArrayList<Double>(){{add(1.488D);add(1.659D);}});
			put (65D, new ArrayList<Double>(){{add(1.660D);add(1.847D);}});
			put (66D, new ArrayList<Double>(){{add(1.848D);add(2D);}});
		}});
		put(80D, new HashMap<Double, ArrayList<Double>>(){{
			put (72D, new ArrayList<Double>(){{add(0D);add(0.502D);}});
			put (73D, new ArrayList<Double>(){{add(0.503D);add(0.552D);}});
			put (74D, new ArrayList<Double>(){{add(0.553D);add(0.607D);}});
			put (75D, new ArrayList<Double>(){{add(0.608D);add(0.666D);}});
			put (76D, new ArrayList<Double>(){{add(0.667D);add(0.731D);}});
			put (77D, new ArrayList<Double>(){{add(0.732D);add(0.800D);}});
			put (78D, new ArrayList<Double>(){{add(0.801D);add(0.875D);}});
			put (79D, new ArrayList<Double>(){{add(0.876D);add(0.957D);}});
			put (80D, new ArrayList<Double>(){{add(0.958D);add(1.044D);}});
			put (81D, new ArrayList<Double>(){{add(1.045D);add(1.138D);}});
			put (82D, new ArrayList<Double>(){{add(1.139D);add(1.240D);}});
			put (83D, new ArrayList<Double>(){{add(1.241D);add(1.349D);}});
			put (84D, new ArrayList<Double>(){{add(1.350D);add(1.466D);}});
			put (85D, new ArrayList<Double>(){{add(1.467D);add(1.592D);}});
			put (86D, new ArrayList<Double>(){{add(1.593D);add(1.727D);}});
			put (87D, new ArrayList<Double>(){{add(1.728D);add(1.872D);}});
			put (88D, new ArrayList<Double>(){{add(1.873D);add(2D);}});
		}});
	}};

	public static final HashMap<String, HashMap<Double, ArrayList<Double>>> b12AttackCooldownNumbers = new HashMap<String, HashMap<Double, ArrayList<Double>>>(){{
		put("paginae/atk/barrage", new HashMap<Double, ArrayList<Double>>(){{
			put (23D, new ArrayList<Double>(){{add(0D);add(0.648D);}});
			put (24D, new ArrayList<Double>(){{add(0.649D);add(0.868D);}});
			put (25D, new ArrayList<Double>(){{add(0.869D);add(1.148D);}});
			put (26D, new ArrayList<Double>(){{add(1.149D);add(1.503D);}});
			put (27D, new ArrayList<Double>(){{add(1.504D);add(1.948D);}});
			put (28D, new ArrayList<Double>(){{add(1.949D);add(2D);}});
		}});
		put("paginae/atk/sideswipe", new HashMap<Double, ArrayList<Double>>(){{
			put (28D, new ArrayList<Double>(){{add(0D);add(0.524D);}});
			put (29D, new ArrayList<Double>(){{add(0.525D);add(0.668D);}});
			put (30D, new ArrayList<Double>(){{add(0.669D);add(0.843D);}});
			put (31D, new ArrayList<Double>(){{add(0.844D);add(1.057D);}});
			put (32D, new ArrayList<Double>(){{add(1.058D);add(1.315D);}});
			put (33D, new ArrayList<Double>(){{add(1.316D);add(1.626D);}});
			put (34D, new ArrayList<Double>(){{add(1.627D);add(1.998D);}});
			put (35D, new ArrayList<Double>(){{add(1.999D);add(2D);}});
		}});
		put("paginae/atk/chop", new HashMap<Double, ArrayList<Double>>(){{
			put (45D, new ArrayList<Double>(){{add(0D);add(0.516D);}});
			put (46D, new ArrayList<Double>(){{add(0.517D);add(0.601D);}});
			put (47D, new ArrayList<Double>(){{add(0.602D);add(0.698D);}});
			put (48D, new ArrayList<Double>(){{add(0.699D);add(0.807D);}});
			put (49D, new ArrayList<Double>(){{add(0.808D);add(0.932D);}});
			put (50D, new ArrayList<Double>(){{add(0.933D);add(1.072D);}});
			put (51D, new ArrayList<Double>(){{add(1.073D);add(1.229D);}});
			put (52D, new ArrayList<Double>(){{add(1.230D);add(1.407D);}});
			put (53D, new ArrayList<Double>(){{add(1.408D);add(1.605D);}});
			put (54D, new ArrayList<Double>(){{add(1.606D);add(1.828D);}});
			put (55D, new ArrayList<Double>(){{add(1.829D);add(2D);}});
		}});
		put("paginae/atk/ravenbite", new HashMap<Double, ArrayList<Double>>(){{
			put (45D, new ArrayList<Double>(){{add(0D);add(0.516D);}});
			put (46D, new ArrayList<Double>(){{add(0.517D);add(0.601D);}});
			put (47D, new ArrayList<Double>(){{add(0.602D);add(0.698D);}});
			put (48D, new ArrayList<Double>(){{add(0.699D);add(0.807D);}});
			put (49D, new ArrayList<Double>(){{add(0.808D);add(0.932D);}});
			put (50D, new ArrayList<Double>(){{add(0.933D);add(1.072D);}});
			put (51D, new ArrayList<Double>(){{add(1.073D);add(1.229D);}});
			put (52D, new ArrayList<Double>(){{add(1.230D);add(1.407D);}});
			put (53D, new ArrayList<Double>(){{add(1.408D);add(1.605D);}});
			put (54D, new ArrayList<Double>(){{add(1.606D);add(1.828D);}});
			put (55D, new ArrayList<Double>(){{add(1.829D);add(2D);}});
		}});
//		put("paginae/atk/sting", new HashMap<Double, ArrayList<Double>>(){{}}); // B12 can't sting
		put("paginae/atk/cleave", new HashMap<Double, ArrayList<Double>>(){{
			put (91D, new ArrayList<Double>(){{add(0D);add(0.536D);}});
			put (92D, new ArrayList<Double>(){{add(0.537D);add(0.579D);}});
			put (93D, new ArrayList<Double>(){{add(0.580D);add(0.624D);}});
			put (94D, new ArrayList<Double>(){{add(0.625D);add(0.673D);}});
			put (95D, new ArrayList<Double>(){{add(0.674D);add(0.724D);}});
			put (96D, new ArrayList<Double>(){{add(0.725D);add(0.779D);}});
			put (97D, new ArrayList<Double>(){{add(0.780D);add(0.837D);}});
			put (98D, new ArrayList<Double>(){{add(0.838D);add(0.899D);}});
			put (99D, new ArrayList<Double>(){{add(0.900D);add(0.965D);}});
			put (100D, new ArrayList<Double>(){{add(0.966D);add(1.035D);}});
			put (101D, new ArrayList<Double>(){{add(1.036D);add(1.109D);}});
			put (102D, new ArrayList<Double>(){{add(1.110D);add(1.188D);}});
			put (103D, new ArrayList<Double>(){{add(1.189D);add(1.272D);}});
			put (104D, new ArrayList<Double>(){{add(1.273D);add(1.360D);}});
			put (105D, new ArrayList<Double>(){{add(1.361D);add(1.454D);}});
			put (106D, new ArrayList<Double>(){{add(1.455D);add(1.553D);}});
			put (107D, new ArrayList<Double>(){{add(1.554D);add(1.659D);}});
			put (108D, new ArrayList<Double>(){{add(1.660D);add(1.770D);}});
			put (109D, new ArrayList<Double>(){{add(1.771D);add(1.887D);}});
			put (110D, new ArrayList<Double>(){{add(1.888D);add(2D);}});
		}});
	}};

	public static final HashMap<String, HashMap<Double, ArrayList<Double>>> cutbladeAttackCooldownNumbers = new HashMap<String, HashMap<Double, ArrayList<Double>>>(){{
		put("paginae/atk/barrage", new HashMap<Double, ArrayList<Double>>(){{
			put (22D, new ArrayList<Double>(){{add(0D);add(0.636D);}});
			put (23D, new ArrayList<Double>(){{add(0.637D);add(0.862D);}});
			put (24D, new ArrayList<Double>(){{add(0.863D);add(1.155D);}});
			put (25D, new ArrayList<Double>(){{add(1.156D);add(1.528D);}});
			put (26D, new ArrayList<Double>(){{add(1.529D);add(2D);}});

		}}); // barrage
		put("paginae/atk/sideswipe", new HashMap<Double, ArrayList<Double>>(){{
			put (27D, new ArrayList<Double>(){{add(0D);add(0.543D);}});
			put (28D, new ArrayList<Double>(){{add(0.544D);add(0.698D);}});
			put (29D, new ArrayList<Double>(){{add(0.699D);add(0.889D);}});
			put (30D, new ArrayList<Double>(){{add(0.890D);add(1.122D);}});
			put (31D, new ArrayList<Double>(){{add(1.123D);add(1.407D);}});
			put (32D, new ArrayList<Double>(){{add(1.408D);add(1.751D);}});
			put (33D, new ArrayList<Double>(){{add(1.752D);add(2D);}});
		}});
		put("paginae/atk/chop", new HashMap<Double, ArrayList<Double>>(){{
			put (43D, new ArrayList<Double>(){{add(0D);add(0.502D);}});
			put (44D, new ArrayList<Double>(){{add(0.503D);add(0.588D);}});
			put (45D, new ArrayList<Double>(){{add(0.589D);add(0.687D);}});
			put (46D, new ArrayList<Double>(){{add(0.688D);add(0.800D);}});
			put (47D, new ArrayList<Double>(){{add(0.801D);add(0.929D);}});
			put (48D, new ArrayList<Double>(){{add(0.930D);add(1.075D);}});
			put (49D, new ArrayList<Double>(){{add(1.076D);add(1.240D);}});
			put (50D, new ArrayList<Double>(){{add(1.241D);add(1.426D);}});
			put (51D, new ArrayList<Double>(){{add(1.427D);add(1.636D);}});
			put (52D, new ArrayList<Double>(){{add(1.637D);add(1.872D);}});
			put (53D, new ArrayList<Double>(){{add(1.873D);add(2D);}});
		}});
//		put("paginae/atk/ravenbite", new HashMap<Double, ArrayList<Double>>(){{}}); // Cutblade can't ravenbite
		put("paginae/atk/sting", new HashMap<Double, ArrayList<Double>>(){{
			put (54D, new ArrayList<Double>(){{add(0D);add(0.510D);}});
			put (55D, new ArrayList<Double>(){{add(0.510D);add(0.579D);}});
			put (56D, new ArrayList<Double>(){{add(0.580D);add(0.656D);}});
			put (57D, new ArrayList<Double>(){{add(0.657D);add(0.742D);}});
			put (58D, new ArrayList<Double>(){{add(0.743D);add(0.837D);}});
			put (59D, new ArrayList<Double>(){{add(0.838D);add(0.943D);}});
			put (60D, new ArrayList<Double>(){{add(0.944D);add(1.059D);}});
			put (61D, new ArrayList<Double>(){{add(1.060D);add(1.188D);}});
			put (62D, new ArrayList<Double>(){{add(1.189D);add(1.330D);}});
			put (63D, new ArrayList<Double>(){{add(1.331D);add(1.487D);}});
			put (64D, new ArrayList<Double>(){{add(1.488D);add(1.659D);}});
			put (65D, new ArrayList<Double>(){{add(1.660D);add(1.847D);}});
			put (66D, new ArrayList<Double>(){{add(1.848D);add(2D);}});
		}});
		put("paginae/atk/cleave", new HashMap<Double, ArrayList<Double>>(){{
			put (87D, new ArrayList<Double>(){{add(0D);add(0.522D);}});
			put (88D, new ArrayList<Double>(){{add(0.523D);add(0.565D);}});
			put (89D, new ArrayList<Double>(){{add(0.566D);add(0.612D);}});
			put (90D, new ArrayList<Double>(){{add(0.613D);add(0.661D);}});
			put (91D, new ArrayList<Double>(){{add(0.662D);add(0.714D);}});
			put (92D, new ArrayList<Double>(){{add(0.715D);add(0.771D);}});
			put (93D, new ArrayList<Double>(){{add(0.772D);add(0.831D);}});
			put (94D, new ArrayList<Double>(){{add(0.832D);add(0.895D);}});
			put (95D, new ArrayList<Double>(){{add(0.896D);add(0.964D);}});
			put (96D, new ArrayList<Double>(){{add(0.965D);add(1.037D);}});
			put (97D, new ArrayList<Double>(){{add(1.038D);add(1.114D);}});
			put (98D, new ArrayList<Double>(){{add(1.115D);add(1.197D);}});
			put (99D, new ArrayList<Double>(){{add(1.198D);add(1.284D);}});
			put (100D, new ArrayList<Double>(){{add(1.285D);add(1.378D);}});
			put (101D, new ArrayList<Double>(){{add(1.379D);add(1.476D);}});
			put (102D, new ArrayList<Double>(){{add(1.477D);add(1.581D);}});
			put (103D, new ArrayList<Double>(){{add(1.582D);add(1.693D);}});
			put (104D, new ArrayList<Double>(){{add(1.694D);add(1.810D);}});
			put (105D, new ArrayList<Double>(){{add(1.811D);add(1.935D);}});
			put (106D, new ArrayList<Double>(){{add(1.936D);add(2D);}});
		}});
	}};

	public static final Map<String, String[]> cures = new HashMap<>();
	static {
		cures.put("paginae/wound/addervenom", new String[]{
				"gfx/invobjs/jar-snakejuice"
		});
		cures.put("paginae/wound/antburn", new String[]{
				"gfx/invobjs/herbs/yarrow"
		});
		cures.put("paginae/wound/beesting", new String[]{
				"gfx/invobjs/antpaste",
				"gfx/invobjs/graygrease",
				"gfx/invobjs/kelpcream"
		});
		cures.put("paginae/wound/blackeye", new String[]{
				"gfx/invobjs/hartshornsalve",
				"gfx/invobjs/honeybroadaid",
				"gfx/invobjs/toadbutter"
		});
		cures.put("paginae/wound/bladekiss", new String[]{
				"gfx/invobjs/gauze",
				"gfx/invobjs/toadbutter"
		});
		cures.put("paginae/wound/blunttrauma", new String[]{
				"gfx/invobjs/camomilecompress",
				"gfx/invobjs/gauze",
				"gfx/invobjs/hartshornsalve",
				"gfx/invobjs/leech",
				"gfx/invobjs/opium",
				"gfx/invobjs/toadbutter",
				"gfx/invobjs/jar-willowweep"
		});
		cures.put("paginae/wound/bruise", new String[]{
				"gfx/invobjs/leech",
				"gfx/invobjs/jar-willowweep"
		});
		cures.put("paginae/wound/coalcough", new String[]{
				"gfx/invobjs/opium"
		});
		cures.put("paginae/wound/concussion", new String[]{
				"gfx/invobjs/coldcompress",
				"gfx/invobjs/opium",
				"gfx/invobjs/jar-willowweep"
		});
		cures.put("paginae/wound/crabcaressed", new String[]{
				"gfx/invobjs/antpaste"
		});
		cures.put("paginae/wound/cruelincision", new String[]{
				"gfx/invobjs/gauze",
				"gfx/invobjs/rootfill",
				"gfx/invobjs/stitchpatch"
		});
		cures.put("paginae/wound/deepcut", new String[]{
				"gfx/invobjs/coldcut",
				"gfx/invobjs/gauze",
				"gfx/invobjs/herbs/waybroad",
				"gfx/invobjs/honeybroadaid",
				"gfx/invobjs/rootfill",
				"gfx/invobjs/stingingpoultice"
		});
		cures.put("paginae/wound/fellslash", new String[]{
				"gfx/invobjs/gauze",
		});
		cures.put("paginae/wound/infectedsore", new String[]{
				"gfx/invobjs/antpaste",
				"gfx/invobjs/camomilecompress",
				"gfx/invobjs/opium",
				"gfx/invobjs/soapbar"
		});
		cures.put("paginae/wound/jellysting", new String[]{
				"gfx/invobjs/graygrease"
		});
		cures.put("paginae/wound/leechburns", new String[]{
				"gfx/invobjs/toadbutter"
		});
		cures.put("paginae/wound/midgebite", new String[]{
				"gfx/invobjs/herbs/yarrow"
		});
		cures.put("paginae/wound/nastylaceration", new String[]{
				"gfx/invobjs/stitchpatch",
				"gfx/invobjs/toadbutter"
		});
		cures.put("paginae/wound/nicksnknacks", new String[]{
				"gfx/invobjs/herbs/yarrow",
				"gfx/invobjs/honeybroadaid"
		});
		cures.put("paginae/wound/punchsore", new String[]{
				"gfx/invobjs/mudointment",
				"gfx/invobjs/opium",
				"gfx/invobjs/jar-willowweep"
		});
		cures.put("paginae/wound/sandfleabites", new String[]{
				"gfx/invobjs/graygrease",
				"gfx/invobjs/herbs/yarrow"
		});
		cures.put("paginae/wound/scrapesncuts", new String[]{
				"gfx/invobjs/herbs/yarrow",
				"gfx/invobjs/honeybroadaid",
				"gfx/invobjs/mudointment"
		});
		cures.put("paginae/wound/sealfinger", new String[]{
				"gfx/invobjs/antpaste",
				"gfx/invobjs/hartshornsalve",
				"gfx/invobjs/kelpcream"
		});
		cures.put("paginae/wound/severemauling", new String[]{
				"gfx/invobjs/hartshornsalve",
				"gfx/invobjs/opium"
		});
		cures.put("paginae/wound/somethingbroken", new String[]{
				"gfx/invobjs/splint"
		});
		cures.put("paginae/wound/swampfever", new String[]{
				"gfx/invobjs/jar-snakejuice"
		});
		cures.put("paginae/wound/swollenbump", new String[]{
				"gfx/invobjs/coldcompress",
				"gfx/invobjs/coldcut",
				"gfx/invobjs/leech",
				"gfx/invobjs/stingingpoultice"
		});
		cures.put("paginae/wound/unfaced", new String[]{
				"gfx/invobjs/kelpcream",
				"gfx/invobjs/leech",
				"gfx/invobjs/mudointment",
				"gfx/invobjs/toadbutter"
		});
		cures.put("paginae/wound/wretchedgore", new String[]{
				"gfx/invobjs/stitchpatch"
		});
		cures.put("paginae/wound/soresnout", new String[]{
				"gfx/invobjs/antpaste",
				"gfx/invobjs/mudointment",
				"gfx/invobjs/toadbutter",
				"gfx/invobjs/graygrease"
		});
		cures.put("paginae/wound/deepworm", new String[]{
				"gfx/invobjs/jar-tansyextract"
		});
	}
	enum Color{
		GREEN(0),
		YELLOW(1),
		RED(2),
		BLUE(3);

		private int order;

		private Color(int order) {
			this.order = order;
		}
		public int getOrder() {
			return order;
		}
	}
	public static class AttackInfo{
		private boolean isMC;
		private Color[] colors;
		private int damage;
		private double dmgMul;

		public AttackInfo(Color[] colors, int damage)	// UA attacks dont have dmg multiplier, but have fixed dmg.
		{
			this.colors = colors;
			this.damage = damage;
			this.isMC = false;
		}
		public AttackInfo(Color[] colors, double dmgMul) // Vice-versa for MC attacks. (careful to use int and double values :) )
		{
			this.colors = colors;
			this.dmgMul = dmgMul;
			this.isMC = true;
		}
		public int getDmg()
		{
			return this.damage;
		}
		public double getDmgMul()
		{
			return this.dmgMul;
		}
		public Color[] getColors()
		{
			return this.colors;
		}
		public boolean isMC()
		{
			return this.isMC;
		}
	}
	public static final Map<String,AttackInfo> MapAttInfo = new HashMap<>();
	static{
		MapAttInfo.put("fullcircle", new AttackInfo(new Color[]{Color.RED, Color.YELLOW},1.0d));
		MapAttInfo.put("knockteeth", new AttackInfo(new Color[]{Color.RED},30));
		MapAttInfo.put("cleave", new AttackInfo(new Color[]{Color.RED, Color.BLUE},1.5d));
		MapAttInfo.put("gojug", new AttackInfo(new Color[]{Color.RED,Color.GREEN},40));
		MapAttInfo.put("chop", new AttackInfo(new Color[]{Color.GREEN},1.0d));
		MapAttInfo.put("haymaker", new AttackInfo(new Color[]{Color.YELLOW},20));
		MapAttInfo.put("kick", new AttackInfo(new Color[]{Color.YELLOW},25));
		MapAttInfo.put("lefthook", new AttackInfo(new Color[]{Color.BLUE},15));
		MapAttInfo.put("lowblow", new AttackInfo(new Color[]{Color.BLUE},20));
		MapAttInfo.put("pow", new AttackInfo(new Color[]{Color.GREEN},10));
		MapAttInfo.put("punchboth", new AttackInfo(new Color[]{Color.GREEN,Color.YELLOW},10));
		MapAttInfo.put("barrage", new AttackInfo(new Color[]{Color.RED},0.25d));
		MapAttInfo.put("ravenbite", new AttackInfo(new Color[]{Color.GREEN,Color.YELLOW},1.1d));
		MapAttInfo.put("lefthook", new AttackInfo(new Color[]{Color.BLUE},15));
		MapAttInfo.put("ripapart", new AttackInfo(new Color[]{Color.GREEN,Color.YELLOW,Color.RED,Color.BLUE},50));
		MapAttInfo.put("sideswipe", new AttackInfo(new Color[]{Color.YELLOW},0.75d));
		MapAttInfo.put("sting", new AttackInfo(new Color[]{Color.GREEN,Color.BLUE},1.25d));
		MapAttInfo.put("sos", new AttackInfo(new Color[]{Color.YELLOW,Color.BLUE},1.0d));
		MapAttInfo.put("takedown", new AttackInfo(new Color[]{Color.YELLOW,Color.RED},40));
		MapAttInfo.put("uppercut", new AttackInfo(new Color[]{Color.GREEN,Color.BLUE},30));
	}

	private static String playername;

	public static void setPlayerName(String playername) {
		Config.playername = playername;
	}

	public static void initAutomapper(UI ui) {
		if (MappingClient.initialized()) {
			MappingClient.destroy();
		}
		MappingClient.init(ui.sess.glob);
		MappingClient automapper = MappingClient.getInstance();
		if (automapper != null)
			automapper.SetPlayerName(OptWnd.liveLocationNameTextEntry.buf.line() + " (" + playername + ")");
	}

}
