@echo off
REM (Re)build the Hurricane RAG index.
REM Usage:  rag\index.bat            (docs only)
REM         rag\index.bat --source   (also index Java source headers)
setlocal
pushd "%~dp0.."
java rag/HurricaneRAG.java index %*
popd
endlocal
