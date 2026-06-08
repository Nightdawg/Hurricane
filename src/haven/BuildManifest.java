package haven;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BuildManifest {
    private static final class Entry {
        final String path;
        final long size;
        final String sha256;

        Entry(String path, long size, String sha256) {
            this.path = path;
            this.size = size;
            this.sha256 = sha256;
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2)
            throw new IllegalArgumentException("Usage: BuildManifest <root-dir> <output-file>");

        Path root = Paths.get(args[0]).toAbsolutePath().normalize();
        Path output = Paths.get(args[1]).toAbsolutePath().normalize();
        Path outputRel = root.relativize(output).normalize();

        List<Entry> entries = new ArrayList<>();
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path rel = root.relativize(file).normalize();
                if (rel.equals(outputRel))
                    return FileVisitResult.CONTINUE;
                if (!attrs.isRegularFile())
                    return FileVisitResult.CONTINUE;
                entries.add(new Entry(toManifestPath(rel), attrs.size(), sha256(file)));
                return FileVisitResult.CONTINUE;
            }
        });
        entries.sort(Comparator.comparing(entry -> entry.path));

        Path parent = output.getParent();
        if (parent != null)
            Files.createDirectories(parent);

        try (BufferedWriter out = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            out.write("{\n");
            out.write("  \"version\": 1,\n");
            out.write("  \"generatedAt\": \"" + jsonEscape(Instant.now().toString()) + "\",\n");
            out.write("  \"files\": [\n");
            for (int i = 0; i < entries.size(); i++) {
                Entry entry = entries.get(i);
                out.write("    {\"path\":\"" + jsonEscape(entry.path) + "\",\"size\":" + entry.size + ",\"sha256\":\"" + entry.sha256 + "\"}");
                if (i + 1 < entries.size())
                    out.write(",");
                out.write("\n");
            }
            out.write("  ]\n");
            out.write("}\n");
        }
    }

    private static String toManifestPath(Path rel) {
        return rel.toString().replace('\\', '/');
    }

    private static String sha256(Path file) throws IOException {
        MessageDigest digest = newDigest();
        byte[] buffer = new byte[8192];
        try (java.io.InputStream in = Files.newInputStream(file)) {
            for (;;) {
                int read = in.read(buffer);
                if (read < 0)
                    break;
                digest.update(buffer, 0, read);
            }
        }
        return hex(digest.digest());
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch(NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String hex(byte[] data) {
        char[] out = new char[data.length * 2];
        final char[] digits = "0123456789abcdef".toCharArray();
        for (int i = 0, j = 0; i < data.length; i++) {
            int b = data[i] & 0xff;
            out[j++] = digits[b >>> 4];
            out[j++] = digits[b & 0x0f];
        }
        return new String(out);
    }

    private static String jsonEscape(String s) {
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
            case '\\':
                out.append("\\\\");
                break;
            case '"':
                out.append("\\\"");
                break;
            case '\b':
                out.append("\\b");
                break;
            case '\f':
                out.append("\\f");
                break;
            case '\n':
                out.append("\\n");
                break;
            case '\r':
                out.append("\\r");
                break;
            case '\t':
                out.append("\\t");
                break;
            default:
                if (c < 0x20)
                    out.append(String.format("\\u%04x", (int)c));
                else
                    out.append(c);
            }
        }
        return out.toString();
    }
}
