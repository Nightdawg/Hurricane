#!/usr/bin/env pwsh
# (Re)build the Hurricane RAG index.
# Usage:  ./rag/index.ps1           # docs only
#         ./rag/index.ps1 --source  # also index Java source headers
$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try { java rag/HurricaneRAG.java index @args } finally { Pop-Location }
