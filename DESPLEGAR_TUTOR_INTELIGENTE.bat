@echo off
setlocal
chcp 65001 >nul
title Tutor Inteligente - Despliegue automatico seguro
cd /d "%~dp0"

where powershell.exe >nul 2>&1
if errorlevel 1 (
  echo [ERROR] No se encontro Windows PowerShell.
  pause
  exit /b 1
)

powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0desplegar-proyecto.ps1" %*
set "RESULTADO=%ERRORLEVEL%"

echo.
if "%RESULTADO%"=="0" (
  echo Proceso finalizado correctamente.
) else (
  echo El proceso termino con errores. Revisa el mensaje anterior.
)
echo.
pause
exit /b %RESULTADO%
