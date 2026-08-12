package haven;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* Text handling for the server's Prospecting window, kept out of the widget so
 * it can be checked without loading any UI class - Window's static initialisers
 * fetch textures, which no headless test can do. */
public class ProspectingText {
    /* The only prospecting result whose position we can trust: the deposit is
     * under the player's feet. The directional variants say nothing about where
     * the player stands, so they deliberately do not match. */
    private static final Pattern DETECT =
	Pattern.compile("There appears to be (.*) directly below\\.");

    /* Returns the substance named in a prospecting label, or null if the label
     * is not a direct find. */
    public static String detect(String text) {
	if(text == null)
	    return(null);
	Matcher m = DETECT.matcher(text.trim());
	if(!m.matches())
	    return(null);
	String substance = m.group(1).trim();
	return(substance.isEmpty() ? null : substance);
    }

    /* "black coal" -> "Black Coal (below)" */
    public static String markerName(String substance) {
	StringBuilder buf = new StringBuilder();
	for(String word : substance.split(" ")) {
	    if(word.isEmpty())
		continue;
	    if(buf.length() > 0)
		buf.append(' ');
	    buf.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
	}
	return(buf.toString() + " (below)");
    }
}
