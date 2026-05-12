package io.thingshub.logging;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.WildcardQuery;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;

public class LogQueryParser {

	public static Query parse(String queryStr, int minTermFreq, int minDocFreq, IndexReader indexReader, Analyzer analyzer) throws IOException {
		if (StrUtil.isBlank(queryStr) || queryStr.equals(LogConstants.SYMBOY_ANY)) {
			return getMatchAllQuery();
		}

		if (!queryStr.matches("([\\s\\S]*(AND|OR|and|or)[\\s\\S]*)|([\\s\\S]*:[\\s\\S]*)")) {
			return getMoreLikeThisQuery(queryStr, minTermFreq, minDocFreq, indexReader, analyzer);
		}

		String[] queryStrs = queryStr.split("(\\s+AND\\s+|\\s+OR\\s+|\\s+and\\s+|\\s+or\\s+)");
		return getBooleanQuery(queryStr, queryStrs, minTermFreq, minDocFreq, indexReader, analyzer);
	}

	private static Query getMatchAllQuery() {
		MatchAllDocsQuery query = new MatchAllDocsQuery();
		return query;
	}

	private static Query getMoreLikeThisQuery(String queryStr, int minTermFreq, int minDocFreq, IndexReader indexReader, Analyzer analyzer) throws IOException {
		MoreLikeThis mlt = new MoreLikeThis(indexReader);
		mlt.setMinTermFreq(minTermFreq);
		mlt.setMinDocFreq(minDocFreq);
		mlt.setAnalyzer(analyzer);
		Reader reader = new StringReader(queryStr);
		String fieldName = LogConstants.FIELD_MSG;
		mlt.setFieldNames(new String[] { fieldName });
		Query mltQuery = null;
		try {
			mltQuery = mlt.like(fieldName, reader);
		} catch (IOException e) {
			throw new LogException("create morelikethis query failed: ", e);
		}
		return mltQuery;
	}

	private static Query getRangeQuery(String fieldName, long start, long end, final boolean minInclusive, final boolean maxInclusive) {
		return LongPoint.newRangeQuery(fieldName, start, end);
	}

	private static Query getWildcardQuery(String fieldName, String queryStr) {
		Term wildTerm = new Term(fieldName, queryStr);
		return new WildcardQuery(wildTerm);
	}

	private static Query getBooleanQuery(String queryExpression, String[] queryStrs, int minTermFreq, int minDocFreq, IndexReader indexReader,
			Analyzer analyzer) throws IOException {
		Builder booleanQueryBuilder = new BooleanQuery.Builder();
		String queryStr;
		List<String> multiValue = new ArrayList<>();
		List<String> multiField = new ArrayList<>();
		List<BooleanClause.Occur> mutilsOccur = new ArrayList<>();
		for (int i = 0; i < queryStrs.length; i++) {
			queryStr = queryStrs[i];
			if (!queryStr.contains(LogConstants.SYMBOY_COLON)) {
				continue;
			}
			int symboyColonIndx = queryStr.indexOf(LogConstants.SYMBOY_COLON);
			String fieldName = queryStr.substring(0, symboyColonIndx).trim();
			String value = queryStr.substring(symboyColonIndx + 1).trim();
			boolean isBracketRange = value.startsWith(LogConstants.SYMBOY_START_BRACKET) && value.endsWith(LogConstants.SYMBOY_END_BRACKET);
			boolean isBraceRange = value.startsWith(LogConstants.SYMBOY_START_BRACE) && value.endsWith(LogConstants.SYMBOY_END_BRACE);
			if (isBracketRange || isBraceRange) {
				value = isBracketRange ? value.replace(LogConstants.SYMBOY_START_BRACKET, "").replace(LogConstants.SYMBOY_END_BRACKET, "").toUpperCase()
						: value.replace(LogConstants.SYMBOY_START_BRACE, "").replace(LogConstants.SYMBOY_END_BRACE, "").toUpperCase();
				String[] rangeValueArray = value.split(LogConstants.CHARACTER_TO);
				String startRangeValue = rangeValueArray[0].trim();
				String endRangeValue = rangeValueArray[1].trim();
				long start = DateUtil.parse(startRangeValue).getTime();
				long end = DateUtil.parse(endRangeValue).getTime();
				Query numericRangeQuery = getRangeQuery(fieldName, start, end, isBracketRange ? true : false, isBracketRange ? true : false);
				BooleanClause booleanClause = isOrBoolean(i, queryExpression, queryStrs) ? new BooleanClause(numericRangeQuery, Occur.SHOULD)
						: new BooleanClause(numericRangeQuery, Occur.MUST);
				booleanQueryBuilder.add(booleanClause);
			} else if (value.contains(LogConstants.SYMBOY_ANY) || value.contains("?")) {
				Query wildcardQuery = getWildcardQuery(fieldName, value);
				BooleanClause booleanClause = isOrBoolean(i, queryExpression, queryStrs) ? new BooleanClause(wildcardQuery, Occur.SHOULD)
						: new BooleanClause(wildcardQuery, Occur.MUST);
				booleanQueryBuilder.add(booleanClause);
			} else if (fieldName.equals(LogConstants.FIELD_MSG)) {
				Query moreLikeThisQuery = getMoreLikeThisQuery(queryExpression, minTermFreq, minDocFreq, indexReader, analyzer);
				BooleanClause booleanClause = isOrBoolean(i, queryStr, queryStrs) ? new BooleanClause(moreLikeThisQuery, Occur.SHOULD)
						: new BooleanClause(moreLikeThisQuery, Occur.MUST);
				booleanQueryBuilder.add(booleanClause);
			} else {
				multiValue.add(value);
				multiField.add(fieldName);
				Occur occur = isOrBoolean(i, queryExpression, queryStrs) ? Occur.SHOULD : Occur.MUST;
				mutilsOccur.add(occur);
			}
		}
		if (!multiField.isEmpty()) {
			Query mutiFieldQuery = null;
			try {
				mutiFieldQuery = MultiFieldQueryParser.parse(multiValue.stream().toArray(String[]::new), multiField.stream().toArray(String[]::new),
						mutilsOccur.stream().toArray(BooleanClause.Occur[]::new), analyzer);
			} catch (ParseException e) {
				throw new LogException("MutiFieldQuery parse exception!", e);
			}
			booleanQueryBuilder.add(new BooleanClause(mutiFieldQuery, Occur.MUST));
		}
		return booleanQueryBuilder.build();
	}

	private static boolean isOrBoolean(int conditionIndex, String queryExpression, String[] queryStrs) {
		boolean isOrBoolean = true;
		if (conditionIndex + 1 >= queryStrs.length) {
			return false;
		}
		StringBuilder regexBuilder = new StringBuilder();
		String condition;
		String temp;
		String replaceStr;
		for (int i = 0; i <= conditionIndex; i++) {
			condition = queryStrs[i];
			temp = condition + "(\\s+AND[\\s\\S]*|\\s+and[\\s\\S]*)";
			regexBuilder.append(temp);
			int start = 0;
			int end = temp.length();
			int tempLength = temp.length();
			if (queryExpression.matches(regexBuilder.toString())) {
				replaceStr = condition + "(\\s+AND\\s+|\\s+and\\s+)";
				if (i == 0) {
					regexBuilder.replace(start, end, replaceStr);
				} else {
					end = regexBuilder.length();
					start = end - tempLength;
					regexBuilder.replace(start, end, replaceStr);
				}
				isOrBoolean = false;
			} else {
				replaceStr = condition + "(\\s+OR\\s+|\\s+or\\s+)";
				if (i == 0) {
					regexBuilder.replace(start, end, replaceStr);
				} else {
					end = regexBuilder.length();
					start = end - tempLength;
					regexBuilder.replace(start, end, replaceStr);
				}
			}
		}
		return isOrBoolean;
	}
}