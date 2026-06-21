#!/usr/bin/env pwsh
# Regenerate the structured code map (rag/code-map.jsonl + ai-docs/reference/Class-Index.md).
$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try { java rag/CodeMap.java @args } finally { Pop-Location }
