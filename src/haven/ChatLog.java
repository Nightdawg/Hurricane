package haven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Supplier;

final class ChatLog {
    private static final DateTimeFormatter timestamp = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss XXX", Locale.ROOT);
    private final String account, character;
    private Path directory;
    private double retryAfter;

    ChatLog(String account, String character) {
	this.account = account;
	this.character = character;
    }

    static String accountKey(UI ui) {
	if((ui == null) || (ui.sess == null) || (ui.sess.user.name == null) || ui.sess.user.name.isEmpty())
	    return(null);
	return(hash(ui.sess.user.name));
    }

    private static String hash(String value) {
	return(Utils.hex.enc(Digest.hash(Digest.SHA256, value.getBytes(Utils.utf8))));
    }

    static boolean enabled() {
	CheckBox checkbox = OptWnd.logChatChannelsCheckBox;
	return((checkbox != null) && checkbox.state());
    }

    private static String singleLine(String text) {
	return(text.replace("\\", "\\\\").replace("\r", "\\r").replace("\n", "\\n")
	       .replace("\u0085", "\\u0085").replace("\u2028", "\\u2028").replace("\u2029", "\\u2029"));
    }

    void append(Supplier<String> channel, Supplier<String> sender, String message) {
	if(Utils.rtime() < retryAfter)
	    return;
	OffsetDateTime now = OffsetDateTime.now();
	String line = "[" + timestamp.format(now) + "] [" + singleLine(channel.get()) + "] " +
	    ((sender == null) ? "" : singleLine(sender.get()) + ": ") + singleLine(message) + "\n";
	try {
	    if(directory == null) {
		Path base = Path.of(Client.gameDir);
		String slug = character.replaceAll("[^a-zA-Z0-9_-]", "_");
		if(slug.length() > 32)
		    slug = slug.substring(0, 32);
		directory = base.resolve("chat-logs").resolve(account).resolve("character-" + slug + "-" + hash(character));
	    }
	    Files.createDirectories(directory);
	    Files.writeString(directory.resolve(now.toLocalDate() + ".log"), line, Utils.utf8,
			      StandardOpenOption.CREATE, StandardOpenOption.APPEND);
	} catch(IOException | SecurityException e) {
	    new Warning(e, "Could not write chat log; retrying in one minute").issue();
	    retryAfter = Utils.rtime() + 60;
	}
    }
}
