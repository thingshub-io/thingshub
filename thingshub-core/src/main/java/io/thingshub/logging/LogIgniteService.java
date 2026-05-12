package io.thingshub.logging;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.ignite.services.Service;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;

import io.thingshub.logging.logback.LuceneAppender;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogIgniteService implements LogService, Service {

	private static final long serialVersionUID = -1058646077226493195L;

	@Override
	public List<LogModel> search(LogSearchParams searchParams) {
		final String dir = LuceneAppender.dir();
		if (dir == null || dir.isBlank()) {
			return null;
		}

		if (!new File(dir).exists()) {
			return null;
		}

		log.debug(">>> Search docs with keyword '{}' on this node from directory '{}'", searchParams.getKeyword(), dir);

		// TODO 优化IndexSearcher的实例化
		CJKAnalyzer analyzer = new CJKAnalyzer();
		LogSearcher sysLogSearcher = LogSearcher.getInstance(dir, analyzer);
		ScoreDoc[] scoreDocs;
		try {
			scoreDocs = sysLogSearcher.search(searchParams.getKeyword(), 1, 1, LogConstants.HIGHLIGHT_PRE_TAG, LogConstants.HIGHLIGHT_POST_TAG,
					searchParams.getSize());
			return buildBizLogs(searchParams.getKeyword(), scoreDocs, sysLogSearcher, analyzer);
		} catch (IOException e) {
			return null;
		}
	}

	private List<LogModel> buildBizLogs(String keyword, ScoreDoc[] scoreDocs, LogSearcher searcher, Analyzer analyzer) {
		if (scoreDocs == null || scoreDocs.length == 0) {
			return null;
		}

		List<LogModel> bizLogs = new ArrayList<>();
		for (final ScoreDoc scoreDoc : scoreDocs) {
			Document theDoc = searcher.hitDocument(scoreDoc);
			String msg = theDoc.get(LogConstants.FIELD_MSG);
			if (!keyword.isBlank() && (!keyword.matches("([\\s\\S]*(AND|OR|and|or)[\\s\\S]*)|([\\s\\S]*:[\\s\\S]*)") || !keyword.matches("[\\s\\S]*:[\\s\\S]*"))
					&& !LogConstants.SYMBOY_ANY.equals(keyword)) {
				msg = fillPreAndPostTagOnTargetString(LogConstants.HIGHLIGHT_PRE_TAG, LogConstants.HIGHLIGHT_POST_TAG, keyword, msg);
			}
			String level = theDoc.get(LogConstants.FIELD_LEVEL);
			long time = (theDoc.getField(LogConstants.FIELD_TIMESTAMP)) == null ? 0 : (Long) theDoc.getField(LogConstants.FIELD_TIMESTAMP).numericValue();
			String host = theDoc.get(LogConstants.FIELD_HOST);

			bizLogs.add(LogModel.builder().host(host).timestamp(time).level(level).msg(msg).score(scoreDoc.score).build());
		}

		return bizLogs;
	}

	private String fillPreAndPostTagOnTargetString(String preTag, String postTag, String target, String source) {
		StringBuilder sb = new StringBuilder();
		int preTagIndex;
		int postTagIndex;
		while (source.length() > 0) {
			preTagIndex = source.indexOf(target);
			if (preTagIndex == -1) {
				break;
			}

			postTagIndex = target.length() + preTagIndex;
			sb.append(source.substring(0, preTagIndex)).append(preTag).append(source.substring(preTagIndex, postTagIndex)).append(postTag);
			source = source.substring(postTagIndex);
		}

		if (source.length() > 0) {
			sb.append(source);
		}

		return sb.toString();
	}

}
