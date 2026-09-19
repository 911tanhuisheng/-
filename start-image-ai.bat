@echo off
setlocal
title Mayue OJ Image AI Launcher
cd /d "%~dp0"

echo ======================================================
echo   Mayue OJ - Image AI Installer and Launcher
echo   First run installs ComfyUI, CUDA PyTorch and models.
echo ======================================================
echo.

powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -Command "$p = Join-Path (Get-Location) 'setup-and-start-image-ai.ps1'; $s = [IO.File]::ReadAllText($p, [Text.Encoding]::UTF8); Invoke-Expression $s"
set "EXIT_CODE=%ERRORLEVEL%"

echo.
if "%EXIT_CODE%"=="0" goto success
goto failure

:success
echo.
echo ======================================================
echo [SUCCESS] Image AI services are running.
echo YOLO/OCR health: http://127.0.0.1:8090/health
echo ComfyUI page   : http://127.0.0.1:8188
echo ======================================================
echo Opening ComfyUI in your browser...
start "" "http://127.0.0.1:8188"
echo.
echo This window can be closed after you confirm the status above.
pause
exit /b 0

:failure
echo [FAILED] Installation or startup failed.
echo Log: %~dp0.runtime-logs\image-ai-launch.log
echo.
pause
exit /b %EXIT_CODE%
