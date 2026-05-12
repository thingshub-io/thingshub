package io.thingshub.logging;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public final class LogWriter {

	private IndexWriter indexWriter;

	private Directory directory;

	private LogWriter(Analyzer analyzer, String dir) {
		assert analyzer != null;
		assert dir != null;

		IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
		try {
			directory = FSDirectory.open(Paths.get(dir));
			indexWriter = new IndexWriter(directory, indexWriterConfig);
		} catch (IOException e) {
			throw new LogException("初始化IndexWriter失败", e);
		}
	}

	public static LogWriter getInstance(Analyzer analyzer, String dir) {
		return new LogWriter(analyzer, dir);
	}

	@Deprecated
	public void addField(Document doc, String name, String value, FieldType fieldType) {
		Field field = new Field(name, value, fieldType);
		doc.add(field);
	}

	public void addDocument(Document doc) {
		try {
			indexWriter.addDocument(doc);
		} catch (IOException e) {
			throw new LogException("添加Document失败", e);
		}
	}

	public int numRamDocs() {
		return indexWriter.numRamDocs();
	}

	public void commit() {
		try {
			indexWriter.commit();
		} catch (IOException e) {
			throw new LogException("IndexWriter commit失败", e);
		}
	}

	public void flush() {
		try {
			indexWriter.flush();
		} catch (IOException e) {
			throw new LogException("IndexWriter flush失败", e);
		}
	}

	public void deleteDocuments(Query query) {
		try {
			indexWriter.deleteDocuments(query);
		} catch (IOException e) {
			throw new LogException("删除Documents失败", e);
		}
	}

	public void close() {
		try {
			indexWriter.close();
			directory.close();
		} catch (IOException e) {
			throw new LogException("关闭IndexWriter失败", e);
		}
	}
}
