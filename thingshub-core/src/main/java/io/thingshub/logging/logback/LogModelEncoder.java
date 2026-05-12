package io.thingshub.logging.logback;

import java.util.Map;

import com.alibaba.fastjson2.JSON;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import io.thingshub.logging.LogModel;

public class LogModelEncoder extends PatternLayoutEncoder {

	@Override
	public byte[] encode(ILoggingEvent event) {
		Map<String, String> mdcPropertyMap = event.getMDCPropertyMap();

		IThrowableProxy err = event.getThrowableProxy();

		// 错误信息格式化
		StringBuilder errMsg = new StringBuilder();
		if (err != null) {
			errMsg.append("\n");
			errMsg.append(err.getClassName());
			errMsg.append(" : ");
			errMsg.append(err.getMessage());
			errMsg.append(" \n ");
			StackTraceElementProxy[] errTrack = err.getStackTraceElementProxyArray();
			for (StackTraceElementProxy stackTraceElementProxy : errTrack) {
				errMsg.append(stackTraceElementProxy.getStackTraceElement());
				errMsg.append(" \n ");
			}
			errMsg.delete(errMsg.length() - 3, errMsg.length());
		}

		StringBuilder msg = new StringBuilder();
		msg.append(event.getFormattedMessage()).append(errMsg);

		LogModel logModel = LogModel.builder() //
				.timestamp(event.getTimeStamp()) //
				.thread(event.getThreadName()) //
				.level(event.getLevel().levelStr) //
				.logger(event.getLoggerName()) //
				.line(event.getCallerData().length == 0 ? 0 : event.getCallerData()[0].getLineNumber()) //
				.msg(msg.toString()).build();

		return JSON.toJSONString(logModel).getBytes();
	}
}
