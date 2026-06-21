@echo off
REM Regenerate the structured code map (rag/code-map.jsonl + ai-docs/reference/Class-Index.md).
setlocal
pushd "%~dp0.."
java rag/CodeMap.java %*
popd
endlocal
