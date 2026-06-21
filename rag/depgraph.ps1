#!/usr/bin/env pwsh
# Regenerate the dependency graph + metrics (rag/import-graph.jsonl + ai-docs/reference/Code-Metrics.md).
$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try { java rag/DepGraph.java @args } finally { Pop-Location }
