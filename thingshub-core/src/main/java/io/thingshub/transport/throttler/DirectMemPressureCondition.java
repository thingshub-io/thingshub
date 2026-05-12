package io.thingshub.transport.throttler;

//import com.baidu.bifromq.baseenv.MemUsage;
//import com.baidu.bifromq.sysprops.props.IngressSlowDownDirectMemoryUsage;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DirectMemPressureCondition implements Condition {

	public static final DirectMemPressureCondition INSTANCE = new DirectMemPressureCondition();

//	private static final double MAX_DIRECT_MEMORY_USAGE = IngressSlowDownDirectMemoryUsage.INSTANCE.get();

	@Override
	public boolean meet() {
//		return MemUsage.local().nettyDirectMemoryUsage() > MAX_DIRECT_MEMORY_USAGE;
		return false;
	}
}
