package io.thingshub.logging;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public final class LogSearcher {

	public LogHighlighter tlogHighlighter;

	public Analyzer analyzer;

	private IndexSearcher indexSearcher;

	private Directory directory;

	private DirectoryReader directoryReader;

	private LogSearcher(String dir, Analyzer analyzer) {
		assert dir != null;

		try {
			this.analyzer = analyzer;

			directory = FSDirectory.open(Paths.get(dir));
			this.directoryReader = DirectoryReader.open(directory);
			indexSearcher = new IndexSearcher(directoryReader);
		} catch (IOException e) {
			throw new LogException(e.getMessage(), e);
		}
	}

	public static LogSearcher getInstance(String dir, Analyzer analyzer) {
		return new LogSearcher(dir, analyzer);
	}

	public ScoreDoc[] search(String keyword, int minTermFreq, int minDocFreq, String preTag, String postTag, int fragmentSize) throws IOException {
		Query query = LogQueryParser.parse(keyword, minTermFreq, minDocFreq, this.directoryReader, this.analyzer);
		SortField sortField = new SortField(LogConstants.FIELD_SORT_ID, SortField.Type.LONG, true);
		Sort sort = new Sort(sortField);
		ScoreDoc[] scoreDocs = null;
		try {
			scoreDocs = indexSearcher.search(query, fragmentSize, sort).scoreDocs;
		} catch (IOException e) {
			throw new LogException(e.getMessage(), e);
		}
		return scoreDocs;
	}

	public ScoreDoc[] fuzzySearch(String fieldName, String text, String preTag, String postTag, int fragmentSize) {
		Term term = new Term(fieldName, text);
		Query query = new FuzzyQuery(term);
		tlogHighlighter = LogHighlighter.highlight(preTag, postTag, query, fragmentSize);
		ScoreDoc[] scoreDocs;
		try {
			scoreDocs = indexSearcher.search(query, fragmentSize).scoreDocs;
		} catch (IOException e) {
			throw new LogException(e.getMessage(), e);
		}
		return scoreDocs;
	}

	public ScoreDoc[] phraseSearch(String fieldName, String text, String preTag, String postTag, int fragmentSize) {
		Term term = new Term(fieldName, text);
		PhraseQuery.Builder builder = new PhraseQuery.Builder();
		builder.add(term);
		PhraseQuery query = builder.build();
		tlogHighlighter = LogHighlighter.highlight(preTag, postTag, query, fragmentSize);
		ScoreDoc[] scoreDocs;
		try {
			SortField sortField = new SortField(LogConstants.FIELD_SORT_ID, SortField.Type.LONG, true);
			Sort sort = new Sort(sortField);
			scoreDocs = indexSearcher.search(query, Integer.MAX_VALUE, sort).scoreDocs;
		} catch (IOException e) {
			throw new LogException(e.getMessage(), e);
		}
		return scoreDocs;
	}

	public ScoreDoc[] matchAllDocsSearch() {
		MatchAllDocsQuery query = new MatchAllDocsQuery();
		ScoreDoc[] scoreDocs;
		try {
			SortField sortField = new SortField(LogConstants.FIELD_SORT_ID, SortField.Type.LONG, true);
			Sort sort = new Sort(sortField);
			scoreDocs = indexSearcher.search(query, Integer.MAX_VALUE, sort).scoreDocs;
		} catch (IOException e) {
			throw new LogException(e.getMessage(), e);
		}
		return scoreDocs;
	}

	public ScoreDoc[] multiFieldSearch(String[] queryStrs, String[] fields, Occur[] occurs, String preTag, String postTag, int fragmentSize) {
		ScoreDoc[] scoreDocs;
		try {
			Query query = MultiFieldQueryParser.parse(queryStrs, fields, occurs, analyzer);
			tlogHighlighter = LogHighlighter.highlight(preTag, postTag, query, fragmentSize);
			scoreDocs = indexSearcher.search(query, Integer.MAX_VALUE).scoreDocs;
		} catch (Exception e) {
			throw new LogException(e.getMessage(), e);
		}
		return scoreDocs;
	}

	public Document hitDocument(ScoreDoc scoreDoc) {
		try {
			return indexSearcher.doc(scoreDoc.doc);
		} catch (IOException e) {
			throw new LogException(e.getMessage(), e);
		}
	}
}
