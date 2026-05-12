package io.thingshub.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public record MultipartFile(String name, String originalFileName, String contentType, byte[] content) {

	public boolean isEmpty() {
		return (this.content == null || this.content.length == 0);
	}

	public long size() {
		return this.content != null ? this.content.length : 0;
	}

	public InputStream inputStream() throws IOException {
		return this.content != null ? new ByteArrayInputStream(this.content) : null;
	}

}