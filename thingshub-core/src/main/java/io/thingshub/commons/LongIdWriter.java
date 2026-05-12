package io.thingshub.commons;

import java.lang.reflect.Type;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.writer.ObjectWriter;

public class LongIdWriter implements ObjectWriter<Long> {

	@Override
	public void write(JSONWriter jsonWriter, Object obj, Object fieldName, Type fieldType, long features) {
		if (obj == null) {
			jsonWriter.writeNull();
		} else {
			jsonWriter.writeString(obj.toString());
		}
	}

}