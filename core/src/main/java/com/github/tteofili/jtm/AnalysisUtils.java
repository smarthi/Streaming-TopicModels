package com.github.tteofili.jtm;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.charfilter.HTMLStripCharFilterFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.TypeTokenFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.EnglishPossessiveFilterFactory;
import org.apache.lucene.analysis.opennlp.OpenNLPChunkerFilterFactory;
import org.apache.lucene.analysis.opennlp.OpenNLPPOSFilterFactory;
import org.apache.lucene.analysis.opennlp.OpenNLPTokenizerFactory;
import org.apache.lucene.analysis.pattern.PatternReplaceFilterFactory;
import org.apache.lucene.analysis.shingle.ShingleFilterFactory;
import org.apache.lucene.analysis.standard.ClassicTokenizerFactory;

/**
 *
 */
public class AnalysisUtils {

  public static Analyzer openNLPAnalyzer() throws Exception {
    String revisionsPattern = "r\\d+";
    String sentenceModel = "en-sent.bin";
    String tokenizerModel = "en-token.bin";
    String posModel = "en-pos-maxent.bin";
    String chunkerModel = "en-chunker.bin";
    return CustomAnalyzer.builder()
          .addCharFilter(HTMLStripCharFilterFactory.class)
          .withTokenizer(OpenNLPTokenizerFactory.class, OpenNLPTokenizerFactory.SENTENCE_MODEL,
              sentenceModel, OpenNLPTokenizerFactory.TOKENIZER_MODEL, tokenizerModel)
          .addTokenFilter(OpenNLPPOSFilterFactory.class, OpenNLPPOSFilterFactory.POS_TAGGER_MODEL, posModel)
          .addTokenFilter(OpenNLPChunkerFilterFactory.class, OpenNLPChunkerFilterFactory.CHUNKER_MODEL, chunkerModel)
          .addTokenFilter(TypeTokenFilterFactory.class, "types", "types.txt", "useWhitelist", "true")
        .addTokenFilter(LowerCaseFilterFactory.class)
        .addTokenFilter(PatternReplaceFilterFactory.class, "pattern", revisionsPattern,
            "replacement", "", "replace", "all")
        .build();

  }

  public static Analyzer shingleOpenNLPAnalyzer() throws Exception {
    String revisionsPattern = "r\\d+";
    String sentenceModel = "en-sent.bin";
    String tokenizerModel = "en-token.bin";
    String posModel = "en-pos-maxent.bin";
    String chunkerModel = "en-chunker.bin";
    return CustomAnalyzer.builder()
        .addCharFilter(HTMLStripCharFilterFactory.class)
        .withTokenizer(OpenNLPTokenizerFactory.class, OpenNLPTokenizerFactory.SENTENCE_MODEL,
            sentenceModel, OpenNLPTokenizerFactory.TOKENIZER_MODEL, tokenizerModel)
        .addTokenFilter(OpenNLPPOSFilterFactory.class, OpenNLPPOSFilterFactory.POS_TAGGER_MODEL, posModel)
        .addTokenFilter(OpenNLPChunkerFilterFactory.class, OpenNLPChunkerFilterFactory.CHUNKER_MODEL, chunkerModel)
        .addTokenFilter(TypeTokenFilterFactory.class, "types", "types.txt", "useWhitelist", "true")
        .addTokenFilter(LowerCaseFilterFactory.class)
        .addTokenFilter(PatternReplaceFilterFactory.class, "pattern", revisionsPattern,
            "replacement", "", "replace", "all")
        .addTokenFilter(ShingleFilterFactory.class, "minShingleSize", "2", "maxShingleSize",
            "3", "outputUnigrams", "true", "outputUnigramsIfNoShingles", "false", "tokenSeparator", " ")
        .build();

  }

  public static Analyzer simpleAnalyzer() throws Exception {
    String revisionsPattern = "r\\d+";
    return CustomAnalyzer.builder()
        .addCharFilter(HTMLStripCharFilterFactory.class)
        .withTokenizer(ClassicTokenizerFactory.class)
        .addTokenFilter(EnglishPossessiveFilterFactory.class)
        .addTokenFilter(LowerCaseFilterFactory.class)
        .addTokenFilter(PatternReplaceFilterFactory.class, "pattern", revisionsPattern, "replacement", "", "replace", "all")
        .build();
  }

  public static Analyzer shingleSimpleAnalyzer() throws Exception {
    String revisionsPattern = "r\\d+";
    return CustomAnalyzer.builder()
        .addCharFilter(HTMLStripCharFilterFactory.class)
        .withTokenizer(ClassicTokenizerFactory.class)
        .addTokenFilter(EnglishPossessiveFilterFactory.class)
        .addTokenFilter(LowerCaseFilterFactory.class)
        .addTokenFilter(PatternReplaceFilterFactory.class, "pattern", revisionsPattern, "replacement", "", "replace", "all")
        .addTokenFilter(ShingleFilterFactory.class, "minShingleSize", "2", "maxShingleSize",
            "3", "outputUnigrams", "true", "outputUnigramsIfNoShingles", "false", "tokenSeparator", " ")
        .build();
  }
}
