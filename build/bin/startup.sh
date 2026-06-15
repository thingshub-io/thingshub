#!/bin/bash

error_exit ()
{
    echo "ERROR: $1 !!"
    exit 1
}

pid() {
  echo "$(ps -ef | grep thingshub.thingshub | grep java | grep -v grep | awk '{print $2}')"
}

total_memory() {
  if [ -n "$MEM_LIMIT" ]; then
    echo $MEM_LIMIT
  elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    local total_kb=$(cat /proc/meminfo | grep 'MemTotal' | awk -F : '{print $2}' | awk '{print $1}' | sed 's/^[ \t]*//g')
    echo "$((total_kb * 1024))"
  elif [[ "$OSTYPE" == "darwin"* ]]; then
    echo "$(sysctl hw.memsize | awk -F : '{print $2}' | xargs)"
  else
    echo -1
  fi
}

memory_in_mb() {
  echo $(($1 / 1024 / 1024))
}

if [ -n "$(pid)" ]; then
  echo "Thinshub already started: $(pid)"
  exit 1
fi

BASE_DIR=`cd $(dirname $0)/..; pwd`
LOG_DIR="$BASE_DIR/log"

# Check JRE
# -e 是否存在该目录或者文件
if [ -e "$BASE_DIR/jre/bin/java" ]; then
	JAVA_HOME=$BASE_DIR/jre
	JAVA_CMD="$JAVA_HOME/bin/java"
	JAVA_VERSION=$(echo $("$JAVA_CMD" -version 2>&1) | awk -F\" '/version/{print $2}')
	JAVA_MAJOR_VERSION=$(echo $JAVA_VERSION | awk -F "." '{print $1}')
else
	if [ -z "$JAVA_HOME" ]; then
		echo "Please set the JAVA_HOME variable in your environment or add your JRE to $BASE_DIR/jre directory!"
		exit 1
	else
		JAVA_CMD="$JAVA_HOME/bin/java"
		JAVA_VERSION=$(echo $("$JAVA_CMD" -version 2>&1) | awk -F\" '/version/{print $2}')
		JAVA_MAJOR_VERSION=$(echo $JAVA_VERSION | awk -F "." '{print $1}')
	fi
fi

if [[ $(expr $JAVA_MAJOR_VERSION) -lt 17 ]]; then
  echo "Too old java version $JAVA_VERSION, at least Java 17 is required"
  exit 1
fi

echo "Using java version $JAVA_VERSION locating at '$JAVA_HOME'"

# Perf options
THINGSHUB_JVM_OPTS="-server \
	-XX:MaxInlineLevel=15 \
	-Djava.awt.headless=true \
	"

# Heap options
# In general, heap memory is 70% of total memory, max direct memory is 20% of total memory
MEM_LIMIT=8*1024*1024*1024
MEMORY=$(total_memory)

MEMORY_FRACTION=70 # Percentage of total memory to use
HEAP_MEMORY=$(($MEMORY /100 * $MEMORY_FRACTION))
MIN_HEAP_MEMORY=$(($HEAP_MEMORY / 2))

# Calculate max direct memory based on total memory
MAX_DIRECT_MEMORY_FRACTION=20 # Percentage of total memory to use for max direct memory
MAX_DIRECT_MEMORY=$(($MEMORY /100 * $MAX_DIRECT_MEMORY_FRACTION))

META_SPACE_MEMORY=128m
MAX_META_SPACE_MEMORY=500m

THINGSHUB_JVM_OPTS="$THINGSHUB_JVM_OPTS \
	-Xms$(memory_in_mb $MIN_HEAP_MEMORY)m \
	-Xmx$(memory_in_mb $HEAP_MEMORY)m \
	-XX:MetaspaceSize=$META_SPACE_MEMORY \
	-XX:MaxMetaspaceSize=$MAX_META_SPACE_MEMORY \
	-XX:MaxDirectMemorySize=$(memory_in_mb ${MAX_DIRECT_MEMORY})m
	"
	
echo "Heap Memory: $(memory_in_mb $HEAP_MEMORY)m, Max Metaspace: $MAX_META_SPACE_MEMORY, Max Direct Memory: $(memory_in_mb ${MAX_DIRECT_MEMORY})m"
	
# GC options
THINGSHUB_JVM_OPTS="$THINGSHUB_JVM_OPTS -XX:+UnlockExperimentalVMOptions \
  -XX:+UnlockDiagnosticVMOptions \
  -XX:+UseZGC \
  -XX:ZAllocationSpikeTolerance=5 \
  -Xlog:async \
  -Xlog:gc:file=$LOG_DIR/gc-%t.log:time,tid,tags:filecount=5,filesize=50m \
  -XX:+CrashOnOutOfMemoryError \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=$LOG_DIR \
  "

# Extra options

# Uncomment if you get StackOverflowError. On 64 bit systems this value can be larger, e.g. -Xss16m
# THINGSHUB_JVM_OPTS="${THINGSHUB_JVM_OPTS} -Xss4m"

#
# Uncomment to set preference for IPv4 stack.
# THINGSHUB_JVM_OPTS="${THINGSHUB_JVM_OPTS} -Djava.net.preferIPv4Stack=true"

#
# Remote debugging (JPDA). Uncomment and change if remote debugging is required.
# THINGSHUB_JVM_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8787 ${JVM_OPTS}"



if [ "$JAVA_MAJOR_VERSION" -ge 17 ] ; then
    THINGSHUB_JVM_OPTS="$THINGSHUB_JVM_OPTS \
        --add-opens=java.base/jdk.internal.access=ALL-UNNAMED \
        --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED \
        --add-opens=java.base/sun.nio.ch=ALL-UNNAMED \
        --add-opens=java.base/sun.util.calendar=ALL-UNNAMED \
        --add-opens=java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED \
        --add-opens=jdk.internal.jvmstat/sun.jvmstat.monitor=ALL-UNNAMED \
        --add-opens=java.base/sun.reflect.generics.reflectiveObjects=ALL-UNNAMED \
        --add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED \
        --add-opens=java.base/java.io=ALL-UNNAMED \
        --add-opens=java.base/java.nio=ALL-UNNAMED \
        --add-opens=java.base/java.net=ALL-UNNAMED \
        --add-opens=java.base/java.util=ALL-UNNAMED \
        --add-opens=java.base/java.util.concurrent=ALL-UNNAMED \
        --add-opens=java.base/java.util.concurrent.locks=ALL-UNNAMED \
        --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED \
        --add-opens=java.base/java.lang=ALL-UNNAMED \
        --add-opens=java.base/java.lang.invoke=ALL-UNNAMED \
        --add-opens=java.base/java.math=ALL-UNNAMED \
        --add-opens=java.sql/java.sql=ALL-UNNAMED \
        --add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
        --add-opens=java.base/java.time=ALL-UNNAMED \
        --add-opens=java.base/java.text=ALL-UNNAMED \
        --add-opens=java.management/sun.management=ALL-UNNAMED \
        --add-opens java.desktop/java.awt.font=ALL-UNNAMED \
        "
fi

SEP=":";

THINGSHUB_LIBS="$BASE_DIR/lib/*"

SAVEIFS=$IFS
IFS=$(echo -en "\n\b")

for file in $BASE_DIR/lib/*
do
    if [ -d $file ] ; then
        THINGSHUB_LIBS=${THINGSHUB_LIBS:-}$SEP$file/*
    fi
done

IFS=$SAVEIFS


# Enable core dump generation
ulimit -c unlimited

nohup $JAVA_CMD $THINGSHUB_JVM_OPTS -cp "$BASE_DIR/etc/;$THINGSHUB_LIBS" -Dfile.encoding=UTF-8 -Dthingshub.home="$BASE_DIR" io.thingshub.Starter thingshub.thingshub >/dev/null 2>&1 &

PIDS=$(pid)
echo "Thingshub($PIDS) started，you can check the state from ${BASE_DIR}/log/thingshub.log"
