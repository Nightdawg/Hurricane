@echo off
REM Query the Hurricane AI knowledge base via the local RAG tool.
REM Usage:  rag\ask.bat "how do I add a new bot?"
REM         rag\ask.bat -k 8 "networking protocol message types"
setlocal
pushd "%~dp0.."
java rag/HurricaneRAG.java query %*
popd
endlocal
