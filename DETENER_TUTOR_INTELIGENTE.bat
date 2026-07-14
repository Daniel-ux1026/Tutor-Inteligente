@echo off
setlocal
title Tutor Inteligente - Detener
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0detener-proyecto.ps1"
echo.
echo El sistema fue detenido.
endlocal
