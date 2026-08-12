package haven;

/**
 * Self-checking tests for ProspectingText. No JUnit: lib/ has no test jar and
 * build.xml has no test target.
 *
 * Run: make check
 */
public class ProspectingTextTest {
    private static int total = 0;
    private static int failures = 0;

    private static void check(String name, Object expected, Object actual) {
	total++;
	boolean ok = (expected == null) ? (actual == null) : expected.equals(actual);
	if (!ok) failures++;
	System.out.printf("%-58s %s%n", name,
			  ok ? "PASS" : "FAIL (expected " + expected + ", got " + actual + ")");
    }

    private static void detectTests() {
	check("detect: single-word substance",
	      "microlite",
	      ProspectingText.detect("There appears to be microlite directly below."));
	check("detect: multi-word substance",
	      "black coal",
	      ProspectingText.detect("There appears to be black coal directly below."));
	check("detect: surrounding whitespace is ignored",
	      "cassiterite",
	      ProspectingText.detect("  There appears to be cassiterite directly below.  "));
	check("detect: directional variant is not a find",
	      null,
	      ProspectingText.detect("There appears to be cassiterite somewhere to the north."));
	check("detect: unrelated label",
	      null,
	      ProspectingText.detect("Nothing of interest here."));
	check("detect: blank substance",
	      null,
	      ProspectingText.detect("There appears to be  directly below."));
	check("detect: null label",
	      null,
	      ProspectingText.detect(null));
    }

    private static void markerNameTests() {
	check("markerName: capitalises a single word",
	      "Microlite (below)",
	      ProspectingText.markerName("microlite"));
	check("markerName: capitalises every word",
	      "Black Coal (below)",
	      ProspectingText.markerName("black coal"));
	check("markerName: leaves an already-capitalised word alone",
	      "Cassiterite (below)",
	      ProspectingText.markerName("Cassiterite"));
    }

    public static void main(String[] args) {
	detectTests();
	System.out.println();
	markerNameTests();
	System.out.println();
	System.out.println(failures == 0
			   ? (total + " of " + total + " passed")
			   : (failures + " of " + total + " failed"));
	System.exit(failures == 0 ? 0 : 1);
    }
}
