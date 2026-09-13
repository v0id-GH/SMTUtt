@echo off
title SMTU Schedule Backend Server
chcp 65001 > nul
echo ========================================================
echo   Starting SMTU Schedule FastAPI Server (SMTUtt)
echo ========================================================
echo.
cd /d "%~dp0backend"
python run.py
pause

