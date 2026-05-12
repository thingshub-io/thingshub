package io.thingshub.commons;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.writer.ObjectWriter;

public class LongIdListWriter implements ObjectWriter<List<Long>> {

	@SuppressWarnings("unchecked")
	@Override
	public void write(JSONWriter jsonWriter, Object obj, Object fieldName, Type fieldType, long features) {
		if (obj == null) {
			jsonWriter.writeNull();
		} else {
			List<Long> l = (List<Long>) obj;
			List<String> s = l.stream().map(Objects::toString).toList();
			jsonWriter.writeString(s);
		}
	}

}