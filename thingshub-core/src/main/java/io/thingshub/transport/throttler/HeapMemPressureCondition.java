package io.thingshub.transport.throttler;

//import com.baidu.bifromq.baseenv.MemUsage;
//import com.baidu.bifromq.sysprops.props.IngressSlowDownHeapMemoryUsage;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HeapMemPressureCondition implements Condition {

	public static final HeapMemPressureCondition INSTANCE = new HeapMemPressureCondition();

//    private static final double MAX_HEAP_MEMORY_USAGE = IngressSlowDownHeapMemoryUsage.INSTANCE.get();

	public boolean meet() {
//        return MemUsage.local().heapMemoryUsage() > MAX_HEAP_MEMORY_USAGE;

		return false;
	}

	@Override
	public String toString() {
		return "HighHeapMemoryUsage";
	}
}
