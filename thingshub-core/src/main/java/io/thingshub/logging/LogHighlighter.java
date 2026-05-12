package io.thingshub.logging;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.Scorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;

public class LogHighlighter {

	private Highlighter highlighter;

	private LogHighlighter(String preTag, String postTag, Query query, int fragmentSize) {
//		Assert.isNull(preTag);
//		Assert.isNull(postTag);
//		Assert.isNull(query);

		Formatter formatter = new SimpleHTMLFormatter(preTag, postTag);
		Scorer scorer = new QueryScorer(query);
		highlighter = new Highlighter(formatter, scorer);
		Fragmenter fragmenter = new SimpleFragmenter(fragmentSize);
		highlighter.setTextFragmenter(fragmenter);
	}

	public static LogHighlighter highlight(String preTag, String postTag, Query query, int fragmentSize) {
		return new LogHighlighter(preTag, postTag, query, fragmentSize);
	}

	public String getBestFragment(Analyzer analyzer, Document doc, String fieldName) {
		try {
			return highlighter.getBestFragment(analyzer, fieldName, doc.get(fieldName));
		} catch (Exception e) {
			throw new LogException(e.getMessage(), e);
		}
	}

	public String getBestFragment(Analyzer analyzer, String fieldName, String text) {
		try {
			return highlighter.getBestFragment(analyzer, fieldName, text);
		} catch (Exception e) {
			throw new LogException(e.getMessage(), e);
		}
	}
}
