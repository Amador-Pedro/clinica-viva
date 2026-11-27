@echo off
cd /d "%~dp0"
if exist "%~dp0runtime\bin\javaw.exe" (
    start "" "%~dp0runtime\bin\javaw.exe" -jar "%~dp0build\clinica-viva.jar"
    exit /b
)
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0build.ps1" -Executar
if errorlevel 1 pause
