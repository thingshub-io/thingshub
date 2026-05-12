@echo off
setlocal enabledelayedexpansion

set PID=
call "%~dp0pid.bat" PID thingshub.thingshub
if "%PID%" == "" (
    echo No thingshub process found
    exit /b 1
)
echo Found thingshub process(%PID%) and stopping it
wmic process where "processid=%PID%" delete
echo Thingshub process(%PID%) was stopped
endlocal