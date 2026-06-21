/*
 * DepGraph — builds a lightweight dependency graph + metrics over the Hurricane Java
 * source tree to help AI assistants reason about coupling, load-bearing classes, and
 * where Hurricane's custom changes are concentrated.
 *
 * It emits:
 *   1) rag/import-graph.jsonl            — per type: the project types it references (fan-out)
 *   2) ai-docs/reference/Code-Metrics.md — committed report: biggest/most-referenced types,
 *                                          package sizes, // ND: density, TODO/FIXME totals
 *
 * Zero dependencies; run from the repo root with the JDK that builds the client (Java 11+):
 *   java rag/DepGraph.java
 *
 * Reference detection is heuristic: it counts a dependency when another project type's
 * simple name appears as a whole word in a file's (comment-stripped) code. Great for a
 * coupling overview; not a precise compiler-grade graph. Same-simple-name types across
 * packages are conflated (e.g. the several `Utils` classes).
 */
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.*;

public class DepGraph {

    static final String JSONL_OUT = "rag/import-graph.jsonl";
    static final String MD_OUT = "ai-docs/reference/Code-Metrics.md";

    static final class F {
        String name, pkg, path; int lines, nd, todo;
        Set<String> tokens = new HashSet<>();
        Set<String> impFqn = new HashSet<>();   // explicitly imported FQNs (+ static parents)
        Set<String> impWild = new HashSet<>();  // wildcard-imported packages
        List<String> deps = new ArrayList<>();
    }

    static final Pattern PKG = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;");
    static final Pattern IMPORT = Pattern.compile("^\\s*import\\s+(static\\s+)?([\\w.]+?)(\\.\\*)?\\s*;");
    static final Pattern TYPE = Pattern.compile(
        "\\b(?:public|protected|private)?\\s*(?:static\\s+|final\\s+|abstract\\s+|sealed\\s+|non-sealed\\s+)*" +
        "\\b(?:class|interface|enum|record)\\s+([A-Za-z_]\\w*)");
    static final Pattern IDENT = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    static final Pattern TODO = Pattern.compile("\\b(TODO|FIXME|XXX|HACK)\\b");

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

        List<F> all = new ArrayList<>();
        for (Path p : files) { try { all.add(parse(root, p)); } catch (Exception e) { System.err.println("skip " + p + ": " + e); } }

        // Project type registries for import-aware reference resolution.
        Set<String> names = new HashSet<>();             // all project simple names
        Set<String> fqns = new HashSet<>();              // pkg.Name
        Map<String,List<String>> pkgToNames = new HashMap<>();
        for (F f : all) {
            names.add(f.name);
            fqns.add(f.pkg + "." + f.name);
            pkgToNames.computeIfAbsent(f.pkg, k -> new ArrayList<>()).add(f.name);
        }

        // compute dependencies with import/package resolution, and fan-in
        Map<String,Integer> fanIn = new HashMap<>();
        for (F f : all) {
            // simple names this file can resolve to a PROJECT type without qualification
            Set<String> resolvable = new HashSet<>();
            resolvable.addAll(pkgToNames.getOrDefault(f.pkg, Collections.emptyList())); // same package
            for (String fqn : f.impFqn) {                 // explicit imports of project types
                if (fqns.contains(fqn)) resolvable.add(fqn.substring(fqn.lastIndexOf('.') + 1));
            }
            for (String wp : f.impWild)                   // wildcard imports of project packages
                resolvable.addAll(pkgToNames.getOrDefault(wp, Collections.emptyList()));

            TreeSet<String> deps = new TreeSet<>();
            for (String t : f.tokens)
                if (!t.equals(f.name) && names.contains(t) && resolvable.contains(t)) deps.add(t);
            f.deps.addAll(deps);
            for (String d : deps) fanIn.merge(d, 1, Integer::sum);
        }

        writeJsonl(root.resolve(JSONL_OUT), all);
        writeMetrics(root.resolve(MD_OUT), all, fanIn);

        int totalLines = all.stream().mapToInt(x -> x.lines).sum();
        int totalNd = all.stream().mapToInt(x -> x.nd).sum();
        int totalTodo = all.stream().mapToInt(x -> x.todo).sum();
        System.out.printf("Analyzed %d types, %,d lines, %d // ND: markers, %d TODO/FIXME.%n",
            all.size(), totalLines, totalNd, totalTodo);
        System.out.println("Wrote " + JSONL_OUT + " and " + MD_OUT);
    }

    static F parse(Path root, Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        F f = new F();
        f.path = root.relativize(file).toString().replace('\\', '/');
        f.lines = lines.size();
        f.name = file.getFileName().toString().replace(".java", "");
        boolean inBlock = false;
        StringBuilder code = new StringBuilder();
        boolean primaryFound = false;
        for (String raw : lines) {
            if (raw.contains("// ND:")) f.nd++;
            if (TODO.matcher(raw).find()) f.todo++;
            String line = strip(raw, inBlock);
            inBlock = blockAfter(raw, inBlock);
            Matcher pm = PKG.matcher(line);
            if (pm.find()) { f.pkg = pm.group(1); }
            Matcher imp = IMPORT.matcher(line);
            if (imp.find()) {
                String fqn = imp.group(2);
                boolean wild = imp.group(3) != null;
                boolean isStatic = imp.group(1) != null;
                if (wild) {
                    f.impWild.add(fqn);
                } else if (isStatic) {
                    int d = fqn.lastIndexOf('.');           // static import: type = parent of member
                    if (d > 0) f.impFqn.add(fqn.substring(0, d));
                } else {
                    f.impFqn.add(fqn);
                }
                continue;                                    // don't count import line as code references
            }
            if (!primaryFound) { Matcher tm = TYPE.matcher(line); if (tm.find()) { f.name = tm.group(1); primaryFound = true; } }
            code.append(line).append('\n');
        }
        if (f.pkg == null) f.pkg = "(default)";
        Matcher im = IDENT.matcher(code);
        while (im.find()) f.tokens.add(im.group());
        return f;
    }

    static void writeJsonl(Path out, List<F> all) throws IOException {
        Files.createDirectories(out.getParent());
        try (BufferedWriter w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            for (F f : all) {
                StringBuilder sb = new StringBuilder("{");
                sb.append("\"name\":").append(js(f.name));
                sb.append(",\"package\":").append(js(f.pkg));
                sb.append(",\"path\":").append(js(f.path));
                sb.append(",\"lines\":").append(f.lines);
                sb.append(",\"nd\":").append(f.nd);
                sb.append(",\"todo\":").append(f.todo);
                sb.append(",\"fanOut\":").append(f.deps.size());
                sb.append(",\"dependsOn\":[");
                for (int i = 0; i < f.deps.size(); i++) { if (i > 0) sb.append(','); sb.append(js(f.deps.get(i))); }
                sb.append("]}");
                w.write(sb.toString()); w.newLine();
            }
        }
    }

    static void writeMetrics(Path out, List<F> all, Map<String,Integer> fanIn) throws IOException {
        Files.createDirectories(out.getParent());
        int totalLines = all.stream().mapToInt(x -> x.lines).sum();
        int totalNd = all.stream().mapToInt(x -> x.nd).sum();
        int totalTodo = all.stream().mapToInt(x -> x.todo).sum();

        List<F> byLines = new ArrayList<>(all); byLines.sort((a, b) -> b.lines - a.lines);
        List<F> byNd = new ArrayList<>(all); byNd.sort((a, b) -> b.nd - a.nd);
        List<Map.Entry<String,Integer>> byFanIn = new ArrayList<>(fanIn.entrySet());
        byFanIn.sort((a, b) -> b.getValue() - a.getValue());

        Map<String,int[]> pkg = new TreeMap<>(); // [types, lines]
        for (F f : all) { int[] v = pkg.computeIfAbsent(f.pkg, k -> new int[2]); v[0]++; v[1] += f.lines; }
        List<Map.Entry<String,int[]>> byPkg = new ArrayList<>(pkg.entrySet());
        byPkg.sort((a, b) -> b.getValue()[1] - a.getValue()[1]);

        try (BufferedWriter w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            w.write("---\ntitle: Code Metrics (generated)\naliases: [Code Metrics, Hotspots, Complexity]\ntags: [reference, generated]\n---\n\n");
            w.write("# Code Metrics (generated)\n\n");
            w.write("> [!warning] Generated file — do not edit by hand.\n> Regenerate with `java rag/DepGraph.java`. Reference detection is heuristic (whole-word\n> simple-name matching); same-name types across packages are conflated. Full per-type fan-out is in\n> `rag/import-graph.jsonl`.\n\n");
            w.write(String.format("**Totals:** %d types · %,d source lines · %d `// ND:` Hurricane markers · %d TODO/FIXME/XXX/HACK.%n%n", all.size(), totalLines, totalNd, totalTodo));
            w.write("See [[Package-Map]], [[Key-Classes]], [[Class-Index]].\n\n");

            w.write("## Most-referenced types (fan-in) — the load-bearing classes\n\n");
            w.write("How many files reference each type. High fan-in = change with extra care.\n\n");
            w.write("| Type | Referenced by (files) |\n|---|---|\n");
            for (int i = 0; i < Math.min(35, byFanIn.size()); i++)
                w.write(String.format("| `%s` | %d |%n", byFanIn.get(i).getKey(), byFanIn.get(i).getValue()));
            w.write("\n");

            w.write("## Largest types by line count\n\n");
            w.write("| Type | Package | Lines |\n|---|---|---|\n");
            for (int i = 0; i < Math.min(35, byLines.size()); i++) {
                F f = byLines.get(i);
                w.write(String.format("| `%s` | `%s` | %d |%n", f.name, f.pkg, f.lines));
            }
            w.write("\n");

            w.write("## Hurricane change density — files with the most `// ND:` markers\n\n");
            w.write("`// ND:` comments mark intentional Hurricane (Nightdawg) behavior. High counts = heavily customized.\n\n");
            w.write("| File | `// ND:` markers | Lines |\n|---|---|---|\n");
            int shown = 0;
            for (F f : byNd) { if (f.nd == 0) break; w.write(String.format("| `%s` | %d | %d |%n", f.path, f.nd, f.lines)); if (++shown >= 25) break; }
            w.write("\n");

            w.write("## Packages by total source lines\n\n");
            w.write("| Package | Types | Lines |\n|---|---|---|\n");
            for (int i = 0; i < Math.min(30, byPkg.size()); i++) {
                Map.Entry<String,int[]> e = byPkg.get(i);
                w.write(String.format("| `%s` | %d | %,d |%n", e.getKey(), e.getValue()[0], e.getValue()[1]));
            }
            w.write("\n#reference #generated\n");
        }
    }

    // crude comment handling (shared style with CodeMap)
    static String strip(String line, boolean inBlock) {
        if (inBlock) { int e = line.indexOf("*/"); if (e < 0) return ""; return strip(line.substring(e + 2), false); }
        int lc = line.indexOf("//"), bc = line.indexOf("/*");
        if (bc >= 0 && (lc < 0 || bc < lc)) { int e = line.indexOf("*/", bc + 2); if (e >= 0) return strip(line.substring(0, bc) + " " + line.substring(e + 2), false); return line.substring(0, bc); }
        if (lc >= 0) return line.substring(0, lc);
        return line;
    }
    static boolean blockAfter(String line, boolean inBlock) {
        int i = 0;
        while (i < line.length()) {
            if (!inBlock) { int b = line.indexOf("/*", i), l = line.indexOf("//", i); if (b < 0) break; if (l >= 0 && l < b) break; int e = line.indexOf("*/", b + 2); if (e < 0) { inBlock = true; break; } i = e + 2; }
            else { int e = line.indexOf("*/", i); if (e < 0) break; inBlock = false; i = e + 2; }
        }
        return inBlock;
    }
    static String js(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) { char c = s.charAt(i);
            switch (c) { case '"': b.append("\\\""); break; case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n"); break; case '\r': b.append("\\r"); break; case '\t': b.append("\\t"); break;
                default: if (c < 0x20) b.append(String.format("\\u%04x", (int)c)); else b.append(c); } }
        return b.append("\"").toString();
    }
}
