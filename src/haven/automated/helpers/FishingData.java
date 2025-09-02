package haven.automated.helpers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class FishingData {
    public static final Set<String> fishingHooks = new HashSet<>(Arrays.asList(
            "Bone Hook", "Chitin Hook", "Metal Hook", "Gold Hook"
    ));

    public static final Set<String> baits = new HashSet<>(Arrays.asList(
            "Woodworm", "Entrails", "Earthworm"
    ));

    public static final Set<String> fishes = new HashSet<>(Arrays.asList(
            "Abyss", "Asp", "Bass", "Bream", "Brill", "Burbot", "Carp", "Catfish",
            "Cave Angler", "Cave Sculpin", "Cavelacanth", "Chub", "Cod", "Eel",
            "Grayling", "Haddock", "Herring", "Ide", "Lavaret", "Mackerel", "Mullet",
            "Pale Ghostfish", "Perch", "Pike", "Plaice", "Pomfret", "Roach", "Rose Fish",
            "Ruffe", "Saithe", "Salmon", "Silver Bream", "Smelt", "Sturgeon", "Tench",
            "Trout", "Whiting", "Zander", "Zope"
    ));
}
