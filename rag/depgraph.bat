@echo off
REM Regenerate the dependency graph + metrics (rag/import-graph.jsonl + ai-docs/reference/Code-Metrics.md).
setlocal
pushd "%~dp0.."
java rag/DepGraph.java %*
popd
endlocal
