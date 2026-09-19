@echo off
setlocal
title Mayue OJ Hard Hat Training Test
cd /d "%~dp0"

echo ======================================================
echo   Hard Hat YOLO Training Smoke Test
echo   2 percent data, 1 epoch, RTX GPU
echo ======================================================
echo.
echo Please close ComfyUI and vision-ai before this test.
echo.

"%~dp0vision-ai\.venv\Scripts\python.exe" "%~dp0vision-ai\smoke_test_hard_hat.py"
set "EXIT_CODE=%ERRORLEVEL%"
echo.
if "%EXIT_CODE%"=="0" (
  echo [SUCCESS] CUDA, dataset and YOLO training are working.
  echo Result: vision-ai\runs\hard-hat-smoke-test\weights\best.pt
) else (
  echo [FAILED] Read the error above. Restart Windows if CUDA reports an unknown error.
)
echo.
pause
exit /b %EXIT_CODE%
