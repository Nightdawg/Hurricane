/*
 * CodeMap — generates a machine-readable map of the Hurricane Java source tree
 * to help AI assistants navigate ~840 classes without grepping the whole repo.
 *
 * It parses every src/**.java file (heuristically, no full Java parser) and emits:
 *   1) rag/code-map.jsonl              — one JSON object per type (full: methods, fields)
 *   2) ai-docs/reference/Class-Index.md — a compact, committed, human/AI-readable index
 *
 * Zero dependencies; runs via the JDK that builds the client (Java 11+ single-file launch):
 *   java rag/CodeMap.java                 # generate both outputs
 *   java rag/CodeMap.java --root <dir>    # use a different repo root (default ".")
 *
 * NOTE: parsing is regex/heuristic — great for navigation, not a compiler. Always
 * confirm exact signatures in the source file it points to.
 */
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.*;

public class CodeMap {

    static final String JSONL_OUT = "rag/code-map.jsonl";
    static final String MD_OUT = "ai-docs/reference/Class-Index.md";
    static final int MAX_METHODS = 80;

    static final class Type {
        String path, pkg, name, kind = "class", ext = "", fromResource = "";
        List<String> impl = new ArrayList<>();
        List<String> methods = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        int lines;
    }

    public static void main(String[] args) throws Exception {
        try { System.setOut(new PrintStream(System.out, true, "UTF-8")); } catch (Exception ignored) {}
        Path root = Paths.get(".");
        for (int i = 0; i < args.length; i++)
            if (args[i].equals("--root") && i + 1 < args.length) root = Paths.get(args[++i]);
        root = root.toRealPath();

        Path src = root.resolve("src");
        if (!Files.isDirectory(src)) { System.err.println("No src/ under " + root); System.exit(1); }

        List<Path> files = new ArrayList<>();
        Files.walkFileTree(src, new SimpleFileVisitor<Path>() {
            public FileVisitResult visitFile(Path f, BasicFileAttributes a) {
                if (f.toString().endsWith(".java")) files.add(f);
                return FileVisitResult.CONTINUE;
            }
        });
        files.sort(Comparator.comparing(Path::toString));

        List<Type> types = new ArrayList<>();
        for (Path f : files) {
            try { Type t = parse(root, f); if (t != null) types.add(t); }
            catch (Exception e) { System.err.println("skip " + f + ": " + e); }
        }

        writeJsonl(root.resolve(JSONL_OUT), types);
        writeMarkdown(root.resolve(MD_OUT), types);

        // stats
        Map<String,Integer> perPkg = new TreeMap<>();
        for (Type t : types) perPkg.merge(t.pkg, 1, Integer::sum);
        System.out.printf("Parsed %d types from %d files across %d packages.%n",
                types.size(), files.size(), perPkg.size());
        System.out.println("Wrote " + JSONL_OUT + " and " + MD_OUT);
    }

    static final Pattern PKG = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;");
    static final Pattern FROMRES = Pattern.compile("@FromResource\\s*\\(\\s*name\\s*=\\s*\"([^\"]+)\"");
    // primary type declaration (class/interface/enum/record)
    static final Pattern TYPE = Pattern.compile(
        "\\b(public|protected|private)?\\s*(?:static\\s+|final\\s+|abstract\\s+|sealed\\s+|non-sealed\\s+)*" +
        "\\b(class|interface|enum|record)\\s+([A-Za-z_]\\w*)");
    static final Pattern MEMBER = Pattern.compile(
        "^\\s*(public|protected)\\b[^=;{}]*");
    static final Set<String> CTRL = new HashSet<>(Arrays.asList(
        "if","for","while","switch","catch","return","else","do","synchronized","try","new"));

    static Type parse(Path root, Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        Type t = new Type();
        t.path = root.relativize(file).toString().replace('\\', '/');
        t.lines = lines.size();
        t.name = file.getFileName().toString().replace(".java", "");

        boolean inBlockComment = false;
        boolean primaryFound = false;
        int depthSeenForPrimary = -1;
        StringBuilder decl = null;       // accumulating a type declaration up to '{'

        for (String raw : lines) {
            String line = stripComments(raw, inBlockComment);
            inBlockComment = blockCommentStateAfter(raw, inBlockComment);

            Matcher pm = PKG.matcher(line);
            if (pm.find()) { t.pkg = pm.group(1); continue; }

            Matcher fr = FROMRES.matcher(line);
            if (fr.find()) t.fromResource = fr.group(1);

            // capture primary type declaration (first top-level type)
            if (!primaryFound) {
                if (decl == null) {
                    Matcher tm = TYPE.matcher(line);
                    if (tm.find()) {
                        t.kind = tm.group(2);
                        t.name = tm.group(3);
                        decl = new StringBuilder(line);
                    }
                } else {
                    decl.append(' ').append(line);
                }
                if (decl != null && line.contains("{")) {
                    parseDecl(decl.toString(), t);
                    primaryFound = true;
                    decl = null;
                }
                continue;
            }

            // collect public/protected members (methods & fields)
            Matcher mm = MEMBER.matcher(line);
            if (mm.find()) {
                String sig = line.trim().replaceAll("\\s+", " ");
                // strip trailing brace/body start
                int brace = sig.indexOf('{');
                if (brace >= 0) sig = sig.substring(0, brace).trim();
                sig = sig.replaceAll(";\\s*$", "").trim();
                if (sig.isEmpty()) continue;
                String firstWord = sig.replaceFirst("^(public|protected)\\s+", "").trim().split("[\\s(]")[0];
                if (CTRL.contains(firstWord)) continue;
                boolean isMethod = sig.contains("(") &&
                        (sig.indexOf('=') < 0 || sig.indexOf('(') < sig.indexOf('='));
                if (isMethod) {
                    if (t.methods.size() < MAX_METHODS) t.methods.add(sig);
                } else {
                    // a field — drop any initializer for a clean declarator
                    int eq = sig.indexOf('=');
                    if (eq >= 0) sig = sig.substring(0, eq).trim();
                    if (t.fields.size() < MAX_METHODS && !sig.isEmpty()
                            && !sig.matches(".*\\b(class|interface|enum|record)\\b.*"))
                        t.fields.add(sig);
                }
            }
        }
        if (t.pkg == null) t.pkg = "(default)";
        return t;
    }

    static void parseDecl(String decl, Type t) {
        int brace = decl.indexOf('{');
        if (brace >= 0) decl = decl.substring(0, brace);
        Matcher ext = Pattern.compile("\\bextends\\s+([\\w.<>,\\s\\[\\]]+?)(?=\\bimplements\\b|$)").matcher(decl);
        if (ext.find()) t.ext = ext.group(1).trim().replaceAll("\\s+", " ");
        Matcher imp = Pattern.compile("\\bimplements\\s+([\\w.<>,\\s\\[\\]]+)$").matcher(decl);
        if (imp.find()) {
            for (String s : imp.group(1).split(","))
                if (!s.trim().isEmpty()) t.impl.add(s.trim().replaceAll("\\s+", " "));
        }
    }

    // ---- crude comment handling (line-level) ----
    static String stripComments(String line, boolean inBlock) {
        if (inBlock) {
            int end = line.indexOf("*/");
            if (end < 0) return "";
            return stripComments(line.substring(end + 2), false);
        }
        int lc = line.indexOf("//");
        int bc = line.indexOf("/*");
        if (bc >= 0 && (lc < 0 || bc < lc)) {
            int end = line.indexOf("*/", bc + 2);
            if (end >= 0) return stripComments(line.substring(0, bc) + " " + line.substring(end + 2), false);
            return line.substring(0, bc);
        }
        if (lc >= 0) return line.substring(0, lc);
        return line;
    }
    static boolean blockCommentStateAfter(String line, boolean inBlock) {
        int i = 0;
        while (i < line.length()) {
            if (!inBlock) {
                int b = line.indexOf("/*", i);
                int l = line.indexOf("//", i);
                if (b < 0) break;
                if (l >= 0 && l < b) break; // rest is line comment
                int e = line.indexOf("*/", b + 2);
                if (e < 0) { inBlock = true; break; }
                i = e + 2;
            } else {
                int e = line.indexOf("*/", i);
                if (e < 0) break;
                inBlock = false; i = e + 2;
            }
        }
        return inBlock;
    }

    // ---- output ----
    static void writeJsonl(Path out, List<Type> types) throws IOException {
        Files.createDirectories(out.getParent());
        try (BufferedWriter w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            for (Type t : types) {
                StringBuilder sb = new StringBuilder("{");
                sb.append("\"name\":").append(js(t.name));
                sb.append(",\"kind\":").append(js(t.kind));
                sb.append(",\"package\":").append(js(t.pkg));
                sb.append(",\"path\":").append(js(t.path));
                sb.append(",\"lines\":").append(t.lines);
                if (!t.ext.isEmpty()) sb.append(",\"extends\":").append(js(t.ext));
                if (!t.impl.isEmpty()) sb.append(",\"implements\":").append(jsArr(t.impl));
                if (!t.fromResource.isEmpty()) sb.append(",\"fromResource\":").append(js(t.fromResource));
                if (!t.methods.isEmpty()) sb.append(",\"methods\":").append(jsArr(t.methods));
                if (!t.fields.isEmpty()) sb.append(",\"fields\":").append(jsArr(t.fields));
                sb.append("}");
                w.write(sb.toString()); w.newLine();
            }
        }
    }

    static void writeMarkdown(Path out, List<Type> types) throws IOException {
        Files.createDirectories(out.getParent());
        Map<String,List<Type>> byPkg = new TreeMap<>();
        for (Type t : types) byPkg.computeIfAbsent(t.pkg, k -> new ArrayList<>()).add(t);
        try (BufferedWriter w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            w.write("---\ntitle: Class Index (generated)\naliases: [Class Index, Symbol Index, Code Map]\ntags: [reference, generated]\n---\n\n");
            w.write("# Class Index (generated)\n\n");
            w.write("> [!warning] Generated file — do not edit by hand.\n");
            w.write("> Regenerate with `java rag/CodeMap.java` (or `rag/codemap.bat`). Heuristic parse; the\n");
            w.write("> source file is always the source of truth. Full machine-readable data (incl. methods)\n");
            w.write("> is in `rag/code-map.jsonl`.\n\n");
            w.write(String.format("Total: **%d types** across **%d packages**. See [[Package-Map]] and [[Key-Classes]].%n%n", types.size(), byPkg.size()));
            w.write("Each row: type, kind, supertypes, line count. File path = `src/<package-as-path>/<Name>.java`.\n\n");
            for (Map.Entry<String,List<Type>> e : byPkg.entrySet()) {
                e.getValue().sort(Comparator.comparing(x -> x.name.toLowerCase()));
                w.write("## `" + e.getKey() + "`  (" + e.getValue().size() + ")\n\n");
                w.write("| Type | Kind | Extends / Implements | Lines |\n|---|---|---|---|\n");
                for (Type t : e.getValue()) {
                    String sup = t.ext.isEmpty() ? "" : ("→ " + t.ext);
                    if (!t.impl.isEmpty()) sup += (sup.isEmpty() ? "" : " ") + ": " + String.join(", ", t.impl);
                    if (!t.fromResource.isEmpty()) sup = "@FromResource(" + t.fromResource + ") " + sup;
                    sup = sup.replace("|", "\\|").trim();
                    w.write(String.format("| `%s` | %s | %s | %d |%n", t.name, t.kind, sup, t.lines));
                }
                w.write("\n");
            }
            w.write("#reference #generated\n");
        }
    }

    static String js(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '\t': b.append("\\t"); break;
                default: if (c < 0x20) b.append(String.format("\\u%04x", (int)c)); else b.append(c);
            }
        }
        return b.append("\"").toString();
    }
    static String jsArr(List<String> xs) {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < xs.size(); i++) { if (i > 0) b.append(","); b.append(js(xs.get(i))); }
        return b.append("]").toString();
    }
}
