/*
 * HurricaneRAG — a tiny, dependency-free Retrieval-Augmented-Generation index
 * for the Hurricane client's AI knowledge base.
 *
 * It builds a local TF-IDF + cosine-similarity retrieval index over the Markdown
 * knowledge vault (ai-docs/), the agent instruction files, and (optionally) the
 * Java source headers, then answers natural-language queries by returning the most
 * relevant document chunks. No internet, no models, no external libraries: it runs
 * on the JDK that already builds the client (Java 11+ single-file source launch).
 *
 * USAGE (run from the repository root):
 *   java rag/HurricaneRAG.java index                 # build the index
 *   java rag/HurricaneRAG.java index --source        # also index Java source headers
 *   java rag/HurricaneRAG.java query "how do bots work?"
 *   java rag/HurricaneRAG.java query -k 8 "networking protocol message types"
 *   java rag/HurricaneRAG.java help
 *
 * The index is written to rag/rag-index.txt (UTF-8, line-based, Base64 text fields).
 *
 * This is "retrieval" RAG: it finds the right context for an LLM to read. The model
 * is lexical TF-IDF (great recall on a curated docs corpus, zero setup). For neural
 * embeddings, see rag/README.md (optional, requires Python + sentence-transformers).
 */
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.*;

public class HurricaneRAG {

    static final String INDEX_PATH = "rag/rag-index.txt";
    static final int MAX_CHUNK_STORE = 2200;   // cap stored chunk text length

    // Common English + a few code stopwords; keeps the index lean and relevant.
    static final Set<String> STOP = new HashSet<>(Arrays.asList(
        "a","an","the","and","or","but","if","then","else","for","while","do","of","to","in","on",
        "at","by","is","are","was","were","be","been","being","it","its","this","that","these","those",
        "as","with","from","into","via","per","so","not","no","yes","can","will","would","should","may",
        "you","your","i","we","they","he","she","them","his","her","our","us","my","me","which","who",
        "what","when","where","why","how","all","any","each","more","most","some","such","than","too",
        "very","just","also","about","up","out","over","under","again","once","here","there","get","got",
        "see","use","used","using","one","two","etc","e","g","ie","eg","vs","via"
    ));

    // ----------------------------- data model -----------------------------

    static final class Chunk {
        String path;        // source file (repo-relative)
        int startLine;      // 1-based
        int endLine;
        String heading;     // nearest heading / context
        String text;        // chunk body (capped)
        Map<String,Double> vec = new HashMap<>(); // L2-normalized tf-idf
    }

    // ----------------------------- entry point ----------------------------

    public static void main(String[] args) throws Exception {
        // Ensure UTF-8 output so doc punctuation (em-dashes, arrows, stars) renders correctly.
        try { System.setOut(new PrintStream(System.out, true, "UTF-8")); } catch (Exception ignored) {}
        if (args.length == 0) { help(); return; }
        switch (args[0]) {
            case "index": cmdIndex(Arrays.copyOfRange(args, 1, args.length)); break;
            case "query": cmdQuery(Arrays.copyOfRange(args, 1, args.length)); break;
            case "bundle": cmdBundle(Arrays.copyOfRange(args, 1, args.length)); break;
            case "help": case "-h": case "--help": help(); break;
            default:
                System.err.println("Unknown command: " + args[0]);
                help();
                System.exit(2);
        }
    }

    static void help() {
        System.out.println(String.join("\n",
            "HurricaneRAG — local TF-IDF retrieval over the Hurricane AI knowledge base.",
            "",
            "Run from the repository root:",
            "  java rag/HurricaneRAG.java index [--source] [--root <dir>]",
            "  java rag/HurricaneRAG.java query [-k N] [--root <dir>] \"your question\"",
            "  java rag/HurricaneRAG.java help",
            "",
            "Commands:",
            "  index            Build the retrieval index from ai-docs/ + agent files (+ README, docs).",
            "    --source       Also index Java source headers (class + public method signatures).",
            "  query \"...\"      Return the most relevant doc chunks for a question.",
            "    -k N           Number of results to return (default 6).",
            "  bundle           Concatenate the curated docs into llms-full.txt (for whole-context LLMs).",
            "",
            "Index file: " + INDEX_PATH
        ));
    }

    // ------------------------------- indexing -----------------------------

    static void cmdIndex(String[] args) throws Exception {
        boolean withSource = false;
        Path root = Paths.get(".");
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--source")) withSource = true;
            else if (args[i].equals("--root") && i + 1 < args.length) root = Paths.get(args[++i]);
        }
        root = root.toRealPath();

        List<Chunk> chunks = new ArrayList<>();

        // 1) Markdown knowledge vault + agent files + selected docs.
        List<Path> mdFiles = collectDocFiles(root);

        for (Path p : mdFiles) chunkMarkdown(root, p, chunks);

        // 2) Optional: Java source headers (signatures only — keeps the index small).
        if (withSource) {
            List<Path> javaFiles = new ArrayList<>();
            collectByExt(javaFiles, root.resolve("src"), ".java");
            for (Path p : javaFiles) chunkJavaHeader(root, p, chunks);
        }

        if (chunks.isEmpty()) {
            System.err.println("No documents found to index. Are you in the repo root?");
            System.exit(1);
        }

        // 3) Compute document frequencies + IDF.
        Map<String,Integer> df = new HashMap<>();
        List<Map<String,Integer>> tfs = new ArrayList<>(chunks.size());
        for (Chunk c : chunks) {
            Map<String,Integer> tf = termFreq(c.heading + " \n " + c.text);
            tfs.add(tf);
            for (String t : tf.keySet()) df.merge(t, 1, Integer::sum);
        }
        int N = chunks.size();
        Map<String,Double> idf = new HashMap<>();
        for (Map.Entry<String,Integer> e : df.entrySet())
            idf.put(e.getKey(), Math.log((N + 1.0) / (e.getValue() + 1.0)) + 1.0);

        // 4) Build L2-normalized TF-IDF vectors.
        for (int i = 0; i < chunks.size(); i++) {
            Map<String,Integer> tf = tfs.get(i);
            Map<String,Double> vec = chunks.get(i).vec;
            double norm = 0;
            for (Map.Entry<String,Integer> e : tf.entrySet()) {
                double w = (1.0 + Math.log(e.getValue())) * idf.getOrDefault(e.getKey(), 0.0);
                if (w != 0) { vec.put(e.getKey(), w); norm += w * w; }
            }
            norm = Math.sqrt(norm);
            if (norm > 0) for (Map.Entry<String,Double> e : vec.entrySet()) e.setValue(e.getValue() / norm);
        }

        writeIndex(root.resolve(INDEX_PATH), idf, chunks);
        System.out.printf("Indexed %d chunks from %d markdown file(s)%s -> %s%n",
            chunks.size(), mdFiles.size(), withSource ? " (+ Java source headers)" : "", INDEX_PATH);
        System.out.printf("Vocabulary: %d terms.%n", idf.size());
    }

    static void addIfExists(List<Path> list, Path p) { if (Files.isRegularFile(p)) list.add(p); }

    /** The curated set of documents that make up the knowledge base (order matters for bundling). */
    static List<Path> collectDocFiles(Path root) throws IOException {
        List<Path> files = new ArrayList<>();
        addIfExists(files, root.resolve("AGENTS.md"));
        addIfExists(files, root.resolve("CLAUDE.md"));
        addIfExists(files, root.resolve(".github/copilot-instructions.md"));
        addIfExists(files, root.resolve("README.md"));
        // ai-docs vault, sorted for stable order (Home first if present)
        List<Path> vault = new ArrayList<>();
        collectByExt(vault, root.resolve("ai-docs"), ".md");
        vault.sort(Comparator.comparing((Path p) -> !p.getFileName().toString().equals("Home.md"))
                .thenComparing(p -> p.toString().toLowerCase()));
        files.addAll(vault);
        // Plain-text design docs that are valuable context.
        addIfExists(files, root.resolve("nd-notes.txt"));
        addIfExists(files, root.resolve("doc/resource-code"));
        return files;
    }

    /** Concatenate the curated docs into a single llms-full.txt for whole-context LLMs. */
    static void cmdBundle(String[] args) throws Exception {
        Path root = Paths.get(".");
        for (int i = 0; i < args.length; i++)
            if (args[i].equals("--root") && i + 1 < args.length) root = Paths.get(args[++i]);
        root = root.toRealPath();
        List<Path> files = collectDocFiles(root);
        Path out = root.resolve("llms-full.txt");
        long bytes = 0;
        try (BufferedWriter w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            w.write("# Hurricane Client — Full AI Knowledge Base (generated bundle)\n");
            w.write("# Concatenation of AGENTS.md + ai-docs/ + design docs. Regenerate: java rag/HurricaneRAG.java bundle\n");
            w.write("# Source of truth is the repository; if this conflicts with code, the code wins.\n\n");
            for (Path p : files) {
                String rel = root.relativize(p).toString().replace('\\', '/');
                String body = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
                w.write("\n\n");
                w.write("================================================================================\n");
                w.write("FILE: " + rel + "\n");
                w.write("================================================================================\n\n");
                w.write(body);
                bytes += body.length();
            }
        }
        System.out.printf("Bundled %d documents into llms-full.txt (%,d chars).%n", files.size(), bytes);
    }

    static void collectByExt(List<Path> out, Path dir, String ext) throws IOException {
        if (!Files.isDirectory(dir)) return;
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override public FileVisitResult visitFile(Path f, BasicFileAttributes a) {
                if (f.toString().toLowerCase().endsWith(ext)) out.add(f);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /** Split a markdown file into heading-delimited chunks (with line ranges). */
    static void chunkMarkdown(Path root, Path file, List<Chunk> out) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        String rel = root.relativize(file).toString().replace('\\', '/');
        String h1 = rel; // fall back to path until we see an H1
        Pattern head = Pattern.compile("^(#{1,6})\\s+(.*)$");
        StringBuilder body = new StringBuilder();
        String curHeading = rel;
        int chunkStart = 1;
        boolean inFrontMatter = false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            // skip YAML front matter delimiters but keep contents searchable
            if (i == 0 && line.trim().equals("---")) { inFrontMatter = true; continue; }
            if (inFrontMatter && line.trim().equals("---")) { inFrontMatter = false; continue; }
            Matcher m = head.matcher(line);
            if (m.matches()) {
                // flush previous chunk
                flush(out, rel, chunkStart, i, curHeading, body);
                body.setLength(0);
                String htext = m.group(2).trim();
                if (m.group(1).length() == 1) h1 = htext;
                curHeading = (h1.equals(htext) ? htext : (h1 + " :: " + htext));
                chunkStart = i + 1;
                body.append(htext).append('\n');
            } else {
                body.append(line).append('\n');
            }
        }
        flush(out, rel, chunkStart, lines.size(), curHeading, body);
    }

    static void flush(List<Chunk> out, String rel, int start, int end, String heading, StringBuilder body) {
        String text = body.toString().trim();
        if (text.isEmpty()) return;
        // ignore trivially short chunks (e.g. a lone heading)
        if (text.replaceAll("\\s+", " ").length() < 12) return;
        Chunk c = new Chunk();
        c.path = rel; c.startLine = start; c.endLine = end; c.heading = heading;
        c.text = text.length() > MAX_CHUNK_STORE ? text.substring(0, MAX_CHUNK_STORE) : text;
        out.add(c);
    }

    /** Index a Java file as a single chunk: package, class decl, public signatures, leading comment. */
    static void chunkJavaHeader(Path root, Path file, List<Chunk> out) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        String rel = root.relativize(file).toString().replace('\\', '/');
        StringBuilder sb = new StringBuilder();
        String cls = file.getFileName().toString().replace(".java", "");
        Pattern sig = Pattern.compile(
            "^\\s*(package\\s+[\\w.]+|(public|protected)\\s+.*\\b(class|interface|enum|record)\\b.*|" +
            "(public|protected)\\s+[\\w<>\\[\\],?\\s.]+\\s+\\w+\\s*\\(.*)");
        int kept = 0;
        for (String line : lines) {
            if (sig.matcher(line).find()) {
                sb.append(line.trim()).append('\n');
                if (++kept > 120) break;
            }
        }
        if (sb.length() == 0) return;
        Chunk c = new Chunk();
        c.path = rel; c.startLine = 1; c.endLine = lines.size();
        c.heading = "Java :: " + cls;
        String text = "Java source " + rel + "\n" + sb;
        c.text = text.length() > MAX_CHUNK_STORE ? text.substring(0, MAX_CHUNK_STORE) : text;
        out.add(c);
    }

    // ------------------------------- querying -----------------------------

    static void cmdQuery(String[] args) throws Exception {
        int k = 6;
        Path root = Paths.get(".");
        StringBuilder q = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-k") && i + 1 < args.length) k = Integer.parseInt(args[++i]);
            else if (args[i].equals("--root") && i + 1 < args.length) root = Paths.get(args[++i]);
            else q.append(args[i]).append(' ');
        }
        root = root.toRealPath();
        String query = q.toString().trim();
        if (query.isEmpty()) { System.err.println("Empty query. Try: query \"how do bots work?\""); System.exit(2); }

        Path idxPath = root.resolve(INDEX_PATH);
        if (!Files.isRegularFile(idxPath)) {
            System.err.println("No index found at " + INDEX_PATH + ". Run: java rag/HurricaneRAG.java index");
            System.exit(1);
        }

        Map<String,Double> idf = new HashMap<>();
        List<Chunk> chunks = readIndex(idxPath, idf);

        // Build the query vector (tf-idf with stored idf), L2-normalized.
        Map<String,Integer> qtf = termFreq(query);
        Map<String,Double> qvec = new HashMap<>();
        double qnorm = 0;
        for (Map.Entry<String,Integer> e : qtf.entrySet()) {
            Double termIdf = idf.get(e.getKey());
            if (termIdf == null) continue;            // unknown term -> ignore
            double w = (1.0 + Math.log(e.getValue())) * termIdf;
            qvec.put(e.getKey(), w);
            qnorm += w * w;
        }
        qnorm = Math.sqrt(qnorm);
        if (qnorm == 0) {
            System.out.println("No indexed terms matched your query. Try different keywords.");
            return;
        }
        for (Map.Entry<String,Double> e : qvec.entrySet()) e.setValue(e.getValue() / qnorm);

        // Score by cosine similarity (both sides normalized -> dot product).
        ArrayList<double[]> scored = new ArrayList<>(); // [index, score]
        for (int i = 0; i < chunks.size(); i++) {
            double dot = 0;
            Map<String,Double> v = chunks.get(i).vec;
            // iterate the smaller map
            Map<String,Double> small = qvec.size() < v.size() ? qvec : v;
            Map<String,Double> big   = small == qvec ? v : qvec;
            for (Map.Entry<String,Double> e : small.entrySet()) {
                Double o = big.get(e.getKey());
                if (o != null) dot += e.getValue() * o;
            }
            if (dot > 0) scored.add(new double[]{i, dot});
        }
        scored.sort((a, b) -> Double.compare(b[1], a[1]));

        System.out.println("Query: " + query);
        System.out.println("Top " + Math.min(k, scored.size()) + " results:\n");
        if (scored.isEmpty()) { System.out.println("(no matches)"); return; }
        for (int r = 0; r < Math.min(k, scored.size()); r++) {
            Chunk c = chunks.get((int) scored.get(r)[0]);
            double score = scored.get(r)[1];
            System.out.printf("%d. [%.3f] %s  (lines %d-%d)%n", r + 1, score, c.path, c.startLine, c.endLine);
            System.out.println("   # " + c.heading);
            System.out.println(snippet(c.text, 360));
            System.out.println();
        }
    }

    static String snippet(String text, int max) {
        String t = text.replaceAll("\\s+", " ").trim();
        if (t.length() > max) t = t.substring(0, max) + " …";
        // wrap-ish for terminal readability
        StringBuilder sb = new StringBuilder();
        int col = 0;
        for (String w : t.split(" ")) {
            if (col == 0) sb.append("   ");
            if (col + w.length() > 96) { sb.append("\n   "); col = 0; }
            sb.append(w).append(' '); col += w.length() + 1;
        }
        return sb.toString();
    }

    // ----------------------------- tokenization ---------------------------

    static Map<String,Integer> termFreq(String text) {
        Map<String,Integer> tf = new HashMap<>();
        for (String tok : tokenize(text)) tf.merge(tok, 1, Integer::sum);
        return tf;
    }

    /** Lowercase tokens; also split camelCase / dotted identifiers into subtokens. */
    static List<String> tokenize(String text) {
        List<String> out = new ArrayList<>();
        Matcher m = Pattern.compile("[A-Za-z0-9_]+").matcher(text);
        while (m.find()) {
            String raw = m.group();
            // whole token (lowercased)
            addTok(out, raw.toLowerCase());
            // split camelCase: fooBarBaz -> foo bar baz ; HTTPServer -> http server
            for (String part : raw.split("(?<=[a-z0-9])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])|_"))
                if (!part.isEmpty()) addTok(out, part.toLowerCase());
        }
        return out;
    }

    static void addTok(List<String> out, String t) {
        if (t.length() < 2) return;
        if (STOP.contains(t)) return;
        out.add(t);
    }

    // ------------------------------ index io ------------------------------

    static void writeIndex(Path path, Map<String,Double> idf, List<Chunk> chunks) throws IOException {
        Files.createDirectories(path.getParent());
        try (BufferedWriter w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            w.write("#HURRICANE-RAG-INDEX v1"); w.newLine();
            w.write("#IDF " + idf.size()); w.newLine();
            for (Map.Entry<String,Double> e : idf.entrySet()) {
                w.write("I\t" + e.getKey() + "\t" + e.getValue()); w.newLine();
            }
            w.write("#CHUNKS " + chunks.size()); w.newLine();
            for (Chunk c : chunks) {
                StringBuilder vec = new StringBuilder();
                boolean first = true;
                for (Map.Entry<String,Double> e : c.vec.entrySet()) {
                    if (!first) vec.append(',');
                    vec.append(e.getKey()).append(':').append(e.getValue());
                    first = false;
                }
                w.write("C\t" + b64(c.path) + "\t" + c.startLine + "\t" + c.endLine + "\t"
                        + b64(c.heading) + "\t" + b64(c.text) + "\t" + vec);
                w.newLine();
            }
        }
    }

    static List<Chunk> readIndex(Path path, Map<String,Double> idf) throws IOException {
        List<Chunk> chunks = new ArrayList<>();
        try (BufferedReader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') continue;
                if (line.charAt(0) == 'I') {
                    String[] f = line.split("\t", 3);
                    idf.put(f[1], Double.parseDouble(f[2]));
                } else if (line.charAt(0) == 'C') {
                    String[] f = line.split("\t", 7);
                    Chunk c = new Chunk();
                    c.path = unb64(f[1]);
                    c.startLine = Integer.parseInt(f[2]);
                    c.endLine = Integer.parseInt(f[3]);
                    c.heading = unb64(f[4]);
                    c.text = unb64(f[5]);
                    if (f.length > 6 && !f[6].isEmpty()) {
                        for (String pair : f[6].split(",")) {
                            int idx = pair.lastIndexOf(':');
                            c.vec.put(pair.substring(0, idx), Double.parseDouble(pair.substring(idx + 1)));
                        }
                    }
                    chunks.add(c);
                }
            }
        }
        return chunks;
    }

    static String b64(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }
    static String unb64(String s) {
        return new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8);
    }
}
