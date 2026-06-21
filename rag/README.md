# Hurricane RAG — local retrieval over the AI knowledge base

This folder contains a tiny **Retrieval-Augmented-Generation (RAG)** retrieval engine for the
Hurricane client's documentation. It lets an AI assistant (or you) ask a natural-language question
and get back the most relevant chunks of the [`ai-docs/`](../ai-docs/Home.md) knowledge vault and
agent files — the "retrieval" half of RAG, which is the part that actually makes an LLM's answers
grounded and accurate.

It is intentionally **zero-dependency**: it runs on the same JDK that builds the client
(Java 11+), needs **no internet, no models, and no libraries**. The retrieval model is classic
**TF-IDF + cosine similarity**, which gives excellent recall on a small, curated docs corpus.

## Quick start

Run everything from the **repository root**:

```bash
# 1) Build the index (over ai-docs/, AGENTS.md, CLAUDE.md, copilot-instructions, README, docs)
java rag/HurricaneRAG.java index

# 1b) Optionally also index Java source headers (class + public method signatures)
java rag/HurricaneRAG.java index --source

# 2) Ask questions
java rag/HurricaneRAG.java query "how do I add a new automation bot?"
java rag/HurricaneRAG.java query -k 8 "what are the network protocol message types?"
java rag/HurricaneRAG.java query "how does pathfinding avoid obstacles?"
java rag/HurricaneRAG.java query "thread safety rules for OCache"
```

Each result shows the **source file**, **line range**, the **nearest heading**, a **relevance
score** (cosine, 0–1), and a **snippet**. Open the cited file/lines to read the full context.

> No separate compile step is needed — `java SomeFile.java` uses Java's single-file source-code
> launcher. (You can still pre-compile with `javac rag/HurricaneRAG.java` if you prefer.)

## How it works

```
ai-docs/**/*.md + AGENTS.md + CLAUDE.md + copilot-instructions.md + README + docs
        │  (chunk by Markdown heading, with line ranges)
        ▼
  tokenize (lowercase, split camelCase/dotted identifiers, drop stopwords)
        │
        ▼
  TF-IDF weighting  →  L2-normalized sparse vectors  →  rag/rag-index.txt
        │
   query ─► same tokenizer ─► TF-IDF (stored IDF) ─► cosine similarity ─► top-K chunks
```

- **Chunking:** Markdown is split on headings (`#`..`######`); each chunk records its file, line
  span, and a `H1 :: section` heading for context.
- **Index format:** `rag/rag-index.txt` is a UTF-8, line-based file (`I` lines = IDF table,
  `C` lines = chunks with Base64 text fields and a sparse normalized vector). Human-inspectable.
- **Scoring:** both query and chunk vectors are L2-normalized, so cosine similarity is just a dot
  product. Unknown query terms are ignored.

## Files

| File | Purpose |
|---|---|
| `HurricaneRAG.java` | The retrieval tool (indexer + query + `bundle`). No dependencies. |
| `CodeMap.java` | Generates a structured map of all ~840 Java types. |
| `rag-index.txt` | Generated retrieval index (git-ignored; rebuild with `index`). |
| `code-map.jsonl` | Generated full code map (git-ignored; rebuild with `CodeMap`). |
| `ask.bat` / `ask.ps1` / `ask.sh` | Wrappers → `query`. |
| `index.bat` / `index.ps1` | Wrappers → `index`. |
| `codemap.bat` / `codemap.ps1` | Wrappers → `CodeMap`. |
| `.gitignore` | Ignores the generated index/code-map. |

## Companion generators

```bash
# Structured code map: rag/code-map.jsonl (full, per-type API) + ai-docs/reference/Class-Index.md
java rag/CodeMap.java

# Dependency graph + metrics: rag/import-graph.jsonl + ai-docs/reference/Code-Metrics.md
# (most-referenced/load-bearing classes, largest files, // ND: density, TODO counts)
java rag/DepGraph.java

# Whole knowledge base concatenated into one file (for models that want full context)
java rag/HurricaneRAG.java bundle      # writes ./llms-full.txt
```

Wrappers exist for all of the above: `rag/ask`, `rag/index`, `rag/codemap`, `rag/depgraph`
(`.bat` for Windows, `.ps1` for PowerShell, plus `rag/ask.sh`).

## Rebuilding

The index is **generated** and can go stale as docs change. Re-run `index` after editing
`ai-docs/` or the agent files. It's cheap (sub-second).

## Optional: neural embeddings (if you have Python)

The built-in engine is lexical (TF-IDF). If you want **semantic** embeddings and have Python
available, you can build an alternative index with `sentence-transformers`. This is **optional** and
not required — the Java tool above is the supported, always-works default.

```bash
# requires: pip install sentence-transformers numpy
python - <<'PY'
import glob, os, numpy as np
from sentence_transformers import SentenceTransformer
m = SentenceTransformer("all-MiniLM-L6-v2")
docs, meta = [], []
for f in glob.glob("ai-docs/**/*.md", recursive=True) + ["AGENTS.md","CLAUDE.md"]:
    t = open(f, encoding="utf-8").read()
    for i, chunk in enumerate(t.split("\n## ")):
        docs.append(chunk); meta.append((f, i))
emb = m.encode(docs, normalize_embeddings=True)
np.save("rag/emb.npy", emb)
import json; json.dump(meta, open("rag/emb-meta.json","w"))
print("Embedded", len(docs), "chunks")
PY
```

Querying that index is left as an exercise (encode the query with the same model, dot-product
against `emb.npy`, sort). For most agent workflows the Java TF-IDF tool is faster to set up and
entirely sufficient.

## Why this exists

LLM agents do better when handed the *right* context. Instead of pasting the whole repo, point your
agent at this tool: it asks a question, gets a handful of precise doc chunks (with file:line
citations), reads them, and acts. See [`../AGENTS.md`](../AGENTS.md) and
[`../ai-docs/Home.md`](../ai-docs/Home.md).
