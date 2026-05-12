@echo off
setlocal enabledelayedexpansion

title Thingshub

:: Set BASE_DIR
set BASE_DIR=%~dp0
:: removed the last 5 chars(which means \bin\) to get the base DIR.
set BASE_DIR=%BASE_DIR:~0,-5%
set LOG_DIR=%BASE_DIR%\log

call "%~dp0pid.bat" PID thingshub.thingshub
if defined PID (
    echo Thingshub already started: %PID%
    exit /b 1
)

if exist "%BASE_DIR%\jre\bin\java.exe" (
    set JAVA_HOME=%BASE_DIR%\jre
	rem echo "Using bundled jre: !JAVA_HOME!"
)

if "" == "%JAVA_HOME%"  (
    echo Please set the JAVA_HOME variable in your environment or add your JRE to %BASE_DIR%\jre directory!
	exit /b 1
)

set JAVA_COMMAND="%JAVA_HOME%\bin\java"

rem check java version
for /f "usebackq tokens=*" %%a in (`"%JAVA_COMMAND%" -version 2^>^&1 `) do (
     set CHECK_JAVA_VERSION_OUTPUT=!CHECK_JAVA_VERSION_OUTPUT!%%a
     for /f "usebackq tokens=3 delims= " %%b in (`echo %%a ^|findstr /i version `) do (
        set JAVA_VERSION=%%b
            set JAVA_VERSION=!JAVA_VERSION:~1,-1!
            for /f "usebackq tokens=1 delims=." %%c in (`echo !JAVA_VERSION!`) do (
                set /a JAVA_MAJOR_VERSION=%%c
            )
     )
)

if "" == "%JAVA_MAJOR_VERSION%" (
    echo "Using %JAVA_COMMAND% check java version failed. %CHECK_JAVA_VERSION_OUTPUT% "
    exit /b 1
)
if %JAVA_MAJOR_VERSION% LSS 17 (
    echo "Too old java version %JAVA_MAJOR_VERSION%, at least Java 17 is required"
    exit /b 1
)

rem limit used memory 
set /a MEM_LIMIT = 4*1024*1024
call :total_memory_in_kb MEMORY %MEM_LIMIT%

rem Perf options
set THINGSHUB_JVM_OPTS=^
	-server ^
	-XX:MaxInlineLevel=15 ^
	-Djava.awt.headless=true

rem GC options
set THINGSHUB_JVM_OPTS=%THINGSHUB_JVM_OPTS% ^
	-XX:+UnlockExperimentalVMOptions ^
	-XX:+UnlockDiagnosticVMOptions ^
	-XX:+UseZGC ^
	-XX:ZAllocationSpikeTolerance=5 ^
	-XX:+HeapDumpOnOutOfMemoryError ^
	-XX:HeapDumpPath=%LOG_DIR% ^
	-Xlog:async ^
	-Xlog:gc:file=%LOG_DIR%\gc.log:time,tid,tags:filecount=5,filesize=50m
)

rem Heap options.
rem In general, heap memory is 70% of total memory, max direct memory is 20% of total memory
set MEMORY_FRACTION=70
set /a HEAP_MEMORY=!MEMORY!/100*!MEMORY_FRACTION!
set /a MIN_HEAP_MEMORY=!HEAP_MEMORY!/2

rem Calculate max direct memory based on total memory
rem Percentage of total memory to use for max direct memory
set MAX_DIRECT_MEMORY_FRACTION=20
set /a MAX_DIRECT_MEMORY=!MEMORY!/100*!MAX_DIRECT_MEMORY_FRACTION!

set META_SPACE_MEMORY=128m
set MAX_META_SPACE_MEMORY=500m
call :memory_in_mb XMS !MIN_HEAP_MEMORY!
call :memory_in_mb XMX !HEAP_MEMORY!
call :memory_in_mb MDMS !MAX_DIRECT_MEMORY!

set THINGSHUB_JVM_OPTS=%THINGSHUB_JVM_OPTS% ^
	-Xms!XMS!m ^
	-Xmx!XMX!m ^
	-XX:MetaspaceSize=!META_SPACE_MEMORY! ^
	-XX:MaxMetaspaceSize=!MAX_META_SPACE_MEMORY! ^
	-XX:MaxDirectMemorySize=!MDMS!m

rem Extra options.

rem Uncomment if you get StackOverflowError.
rem set THINGSHUB_JVM_OPTS=%THINGSHUB_JVM_OPTS% -Xss16m

rem Uncomment to set preference to IPv4 stack.
rem set THINGSHUB_JVM_OPTS=%THINGSHUB_JVM_OPTS% -Djava.net.preferIPv4Stack=true

rem Uncomment to enable reverse DNS lookup.
rem set THINGSHUB_JVM_OPTS=%THINGSHUB_JVM_OPTS% -Dsun.net.spi.nameservice.provider.1=default -Dsun.net.spi.nameservice.provider.2=dns,sun

rem Remote debugging (JPDA). Uncomment and change if remote debugging is required.
rem set THINGSHUB_JVM_OPTS=%THINGSHUB_JVM_OPTS% -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8787 

set THINGSHUB_LIBS=%BASE_DIR%\lib\*

for /F %%F in ('dir /A:D /b "%THINGSHUB_LIBS%"') do (
	if not "%%F" == "optional" (
		set THINGSHUB_LIBS=%THINGSHUB_LIBS%;%BASE_DIR%\lib\%%F\*
	)
)

rem JAVA [17,)
if %JAVA_MAJOR_VERSION% GEQ 17 (
    set THINGSHUB_JVM_OPTS= %THINGSHUB_JVM_OPTS% ^
    --add-opens=java.base/jdk.internal.access=ALL-UNNAMED ^
    --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED ^
    --add-opens=java.base/sun.nio.ch=ALL-UNNAMED ^
    --add-opens=java.base/sun.util.calendar=ALL-UNNAMED ^
    --add-opens=java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED ^
    --add-opens=jdk.internal.jvmstat/sun.jvmstat.monitor=ALL-UNNAMED ^
    --add-opens=java.base/sun.reflect.generics.reflectiveObjects=ALL-UNNAMED ^
    --add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED ^
    --add-opens=java.base/java.io=ALL-UNNAMED ^
    --add-opens=java.base/java.nio=ALL-UNNAMED ^
    --add-opens=java.base/java.net=ALL-UNNAMED ^
    --add-opens=java.base/java.util=ALL-UNNAMED ^
    --add-opens=java.base/java.util.concurrent=ALL-UNNAMED ^
    --add-opens=java.base/java.util.concurrent.locks=ALL-UNNAMED ^
    --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED ^
    --add-opens=java.base/java.lang=ALL-UNNAMED ^
    --add-opens=java.base/java.lang.invoke=ALL-UNNAMED ^
    --add-opens=java.base/java.math=ALL-UNNAMED ^
    --add-opens=java.sql/java.sql=ALL-UNNAMED ^
    --add-opens=java.base/java.lang.reflect=ALL-UNNAMED ^
    --add-opens=java.base/java.time=ALL-UNNAMED ^
    --add-opens=java.base/java.text=ALL-UNNAMED ^
    --add-opens=java.management/sun.management=ALL-UNNAMED ^
    --add-opens java.desktop/java.awt.font=ALL-UNNAMED
)

%JAVA_COMMAND% %THINGSHUB_JVM_OPTS% -cp "%BASE_DIR%\etc\;%THINGSHUB_LIBS%" -Dfile.encoding=UTF-8 -Dthingshub.home="%BASE_DIR%" io.thingshub.Starter thingshub.thingshub %*

goto :eof

:total_memory_in_kb
   if [%2] EQU [] (
      for /f "skip=1" %%i in ('wmic os get TotalVisibleMemorySize') do (
        if %%i geq 0 (
           set /a %1=%%i
        )
      )
   ) else (
      set %1=%2
   )
   goto :eof
:memory_in_mb
    set /a %1=%2/1024
    goto :eof
:memory_in_gb
    set /a %1=%2/1024/1024
    goto :eof
:pid
    wmic process where "commandline like '%%%2%%'" get processid | find /v /i "processid" 2^>^&1
    goto :eof

