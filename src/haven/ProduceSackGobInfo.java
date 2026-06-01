package haven;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ProduceSackGobInfo extends GobInfo {
	private static final Map<String, Tex> contentTexCache = new HashMap<>();

    protected ProduceSackGobInfo(Gob owner) {
	super(owner);
	center = new Pair<>(0.5, 1.0);
    }
    
    @Override
    protected boolean enabled() {
		return OptWnd.showProduceSackTextCheckBox.a;
    }

    @Override
    protected Tex render() {
	if(gob == null || gob.getres() == null) {return null;}
	up(6);
	return content();
    }

    private Tex content() {
	String resName = gob.getres().name;
	if(resName == null) {return null;}
	Optional<String> contents = Optional.empty();
	
	if(resName.startsWith("gfx/terobjs/producesack")) {
		Message sdt = gob.sdtm();
		if(!sdt.eom()) {
			int resid = sdt.uint16();
			if((resid & 0x8000) != 0) {
				resid &= ~0x8000;
			}

			Session session = gob.context(Session.class);
			Indir<Resource> cres = session.getres2(resid);
			if(cres != null) {
				try {
					contents = Optional.of(cres.get().basename());
				} catch (Exception ignored){}
			}
		}
	}
	
	if(contents.isPresent()) {
	    String text = contents.get();
		if (!text.isEmpty()) {
			text = text.substring(0, 1).toUpperCase() + text.substring(1);
		}
		text = removePrefix(text);
		if (!contentTexCache.containsKey(text)) {
			Tex contentTex = new TexI(ItemInfo.catimgsh(3, 0, null, PUtils.strokeImg(Text.std.renderstroked(text, Color.white, Color.black).img)));
			contentTexCache.put(text, contentTex);
			return contentTex;
		} else {
			return contentTexCache.get(text);
		}
	}
	return null;
    }

    @Override
    public String toString() {
	Resource res = gob.getres();
	return String.format("GobInfo<%s>", res != null ? res.name : "<loading>");
    }


	public static String removePrefix(String input) {
		if (input != null) {
			// Convert the input string to lowercase
			input = input.toLowerCase();

			// Define a list of prefixes to check
			String[] prefixes = {"wurst-", "wblock-", "board-", "seed-"};

			// Iterate over the prefixes and remove any that match the start of the string
			for (String prefix : prefixes) {
				if (input.startsWith(prefix)) {
					input = input.substring(prefix.length());
					break;  // Once a prefix is removed, no need to check further
				}
			}

			// Capitalize the first letter of the remaining string
			if (input.length() > 0) {
				input = input.substring(0, 1).toUpperCase() + input.substring(1);
			}
		}

		// Return the modified string, or the original if no modifications were made
		return input;
	}



}