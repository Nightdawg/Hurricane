#!/usr/bin/env bash
# Query the Hurricane AI knowledge base via the local RAG tool (Linux/macOS).
# Usage:  ./rag/ask.sh "how do I add a new bot?"
#         ./rag/ask.sh -k 8 "networking protocol message types"
cd "$(dirname "$0")/.." || exit 1
exec java rag/HurricaneRAG.java query "$@"
