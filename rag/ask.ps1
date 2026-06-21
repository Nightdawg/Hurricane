#!/usr/bin/env pwsh
# Query the Hurricane AI knowledge base via the local RAG tool.
# Usage:  ./rag/ask.ps1 "how do I add a new bot?"
#         ./rag/ask.ps1 -k 8 "networking protocol message types"
$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try { java rag/HurricaneRAG.java query @args } finally { Pop-Location }
