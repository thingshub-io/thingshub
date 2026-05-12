@echo off
setlocal enabledelayedexpansion
if "%2" == "" (
    echo "Usage: %0 ReturnVar ProcessName"
    exit /b 1
)
set PID_FILE=pid
wmic process where "commandline like '%%%2%%' and not name='wmic.exe'" get processId 2>^&1 | find /v /i "processId" >%PID_FILE%
set PID=
for /f "tokens=*" %%a in (!PID_FILE!) do (
    if [!PID!] EQU [] (
        set PID=%%a
    )
)
del %PID_FILE%
for /f "delims=0123456789 " %%a in ("%PID%") do (
    endlocal & set %1=
    exit /b 1
)

endlocal & set %1=%PID%
