@echo off
setlocal
title Tutor Inteligente - Inicio completo
cd /d "%~dp0"
echo Iniciando Tutor Inteligente con SQL Server local...
echo.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0iniciar-proyecto.ps1" -SinDocker -SinEntrenarIA -AbrirNavegador
if errorlevel 1 (
  echo.
  echo No se pudo iniciar el proyecto. Revisa el mensaje anterior.
  pause
)
endlocal
