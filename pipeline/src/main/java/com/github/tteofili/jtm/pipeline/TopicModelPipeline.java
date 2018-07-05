/*
 * Copyright 2018 Tommaso Teofili and Simone Tripodi
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.github.tteofili.jtm.pipeline;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

import com.github.tteofili.jtm.EndToEndAnalysisTool;
import org.apache.commons.lang.StringUtils;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.RichAllWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.GlobalWindow;
import org.apache.flink.util.Collector;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.deeplearning4j.models.embeddings.learning.impl.elements.SkipGram;
import org.deeplearning4j.models.paragraphvectors.ParagraphVectors;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.documentiterator.LabelAwareIterator;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.nd4j.linalg.api.ndarray.INDArray;

import com.github.tteofili.jtm.AnalysisTool;
import com.github.tteofili.jtm.aggregation.Topics;
import com.github.tteofili.jtm.feed.Feed;
import com.github.tteofili.jtm.feed.Issue;
import com.google.common.base.Joiner;

import opennlp.tools.chunker.Chunker;
import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;

public class TopicModelPipeline implements AnalysisTool {

  private static final String TOKEN_TYPE = "opennlp.uima.Token";
  private static final String CHUNK_TYPE = "opennlp.uima.Chunk";
  private static final String POS_FEATURE_NAME = "pos";
  private static final String[] stopTags = new String[] {"CD", "VB", "RB", "JJ", "VBN", "VBG", ".", "JJS", "FW", "VBD"};

  @Override
  public Topics analyze(Feed feed) throws Exception {

    // opennlp models

    // TODO are they useful?
    // InputStream sentenceModelStream = EndToEndAnalysisTool.class.getResourceAsStream("/en-sent.bin");
    // SentenceModel sentdetectModel = new SentenceModel(sentenceModelStream);

    InputStream tokenizerModelStream = EndToEndAnalysisTool.class.getResourceAsStream("/en-token.bin");
    TokenizerModel tokenizerModel = new TokenizerModel(tokenizerModelStream);

    InputStream posStream = EndToEndAnalysisTool.class.getResourceAsStream("/en-pos-maxent.bin");
    POSModel posModel = new POSModel(posStream);

    InputStream chunkerStream = EndToEndAnalysisTool.class.getResourceAsStream("/en-chunker.bin");
    ChunkerModel chunkerModel = new ChunkerModel(chunkerStream);

    final StreamExecutionEnvironment env =
        StreamExecutionEnvironment.getExecutionEnvironment().setParallelism(1);

    env.getConfig().enableObjectReuse();
    env.setStreamTimeCharacteristic(TimeCharacteristic.IngestionTime);
    env.getConfig().registerTypeWithKryoSerializer(CASImpl.class, KryoCasSerializer.class);

    DataStreamSource<Issue> jiraIssueDataStreamSource = env.fromCollection(feed.getIssues().getIssues());

    TypeSystemDescription typeSystemDesc = UimaUtil.createTypeSystemDescription(
        TopicModelPipeline.class.getResourceAsStream("/TypeSystem.xml"));

    // create stream on CAS
    DataStream<CAS> casStream = jiraIssueDataStreamSource.map((MapFunction<Issue, CAS>) issue -> {
      CAS cas = UimaUtil.createEmptyCAS(typeSystemDesc);
      String documentText = Joiner.on(' ').join(issue.getLabels(),
                                                issue.getTitle(),
                                                issue.getDescription(),
                                                issue.getSummary());
      cas.setDocumentText(documentText);
      cas.setDocumentLanguage(issue.getKey().getValue());
//      cas.setSofaDataString(issue.getKey().getValue(), null);
      return cas;
    });

    Topics finalTopics = new Topics();

    RichAllWindowFunction<CAS, Embeddings, GlobalWindow> f = new RichAllWindowFunction<CAS, Embeddings, GlobalWindow>() {
      @Override
      public void apply(GlobalWindow window, Iterable<CAS> values, Collector<Embeddings> out) throws Exception {

        LabelAwareIterator iterator = new CASLabelAwareIterator(values, CHUNK_TYPE);

        int epochs = 1;
        int layerSize = 100;
        org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory tf = new DefaultTokenizerFactory();

        // TODO : fetch embeddings models from somewhere (e.g. file system)
        Word2Vec word2Vec = new Word2Vec.Builder()
            .iterate(iterator)
            .epochs(epochs)
            .elementsLearningAlgorithm(new SkipGram<>())
            .layerSize(layerSize)
            .useUnknown(true)
            .tokenizerFactory(tf)
            .build();
        word2Vec.fit();

        iterator.reset();

        ParagraphVectors paragraphVectors = new ParagraphVectors.Builder()
            .iterate(iterator)
            .epochs(epochs)
            .layerSize(layerSize)
            .tokenizerFactory(tf)
            .useUnknown(true)
            .useExistingWordVectors(word2Vec)
            .build();
        paragraphVectors.fit();

        Embeddings embeddings = new Embeddings(word2Vec, paragraphVectors, values);

        // TODO : store back updated embeddings somewhere (e.g. filesystem)

        out.collect(embeddings);
      }
    };

    casStream
        .map((MapFunction<CAS, CAS>) cas -> { // tokenize
          Type tokenType = cas.getTypeSystem().getType(TOKEN_TYPE);
          Tokenizer tokenizer = new TokenizerME(tokenizerModel);
          String text = cas.getDocumentText();
          Span[] spans = tokenizer.tokenizePos(text);
          for (Span s : spans) {
            AnnotationFS annotation = cas.createAnnotation(tokenType, s.getStart(), s.getEnd());
            cas.addFsToIndexes(annotation);
          }
          return cas;
        })
        .map((MapFunction<CAS, CAS>) cas -> { // pos tag
          Type tokenType = cas.getTypeSystem().getType(TOKEN_TYPE);
          POSTaggerME tagger = new POSTaggerME(posModel);
          FSIterator<AnnotationFS> iterator = cas.getAnnotationIndex(tokenType).iterator(false);
          while (iterator.hasNext()) {
            AnnotationFS next = iterator.next();
            next.setStringValue(tokenType.getFeatureByBaseName(POS_FEATURE_NAME), tagger.tag(new String[] {next.getCoveredText()})[0]);
          }
          return cas;
        })
        .map((MapFunction<CAS, CAS>) cas -> { // chunk
          Type tokenType = cas.getTypeSystem().getType(TOKEN_TYPE);
          Type chunkType = cas.getTypeSystem().getType(CHUNK_TYPE);
          AnnotationIndex<AnnotationFS> index = cas.getAnnotationIndex(tokenType);
          FSIterator<AnnotationFS> iterator = index.iterator(false);
          String[] tokens = new String[index.size()];
          String[] tags = new String[index.size()];
          int i = 0;
          while (iterator.hasNext()) {
            AnnotationFS next = iterator.next();
            Feature pos = tokenType.getFeatureByBaseName(POS_FEATURE_NAME);
            String stringValue = next.getStringValue(pos);
            String text = next.getCoveredText();
            tokens[i] = text;
            tags[i] = stringValue;
            i++;
          }
          Chunker chunker = new ChunkerME(chunkerModel);
          Span[] chunks = chunker.chunkAsSpans(tokens, tags);
          for (Span s : chunks) {
            AnnotationFS annotation = cas.createAnnotation(chunkType, s.getStart(), s.getEnd());
            cas.addFsToIndexes(annotation);
          }

          return cas;
        })
        .countWindowAll(100).apply(f)
        .map(new MapFunction<Embeddings, Topics>() {

          @Override
          public Topics map(Embeddings value) throws Exception {

            POSTaggerME tagger = new POSTaggerME(posModel);
            int topN = 3;
            ParagraphVectors paragraphVectors = value.paragraphVectors;
            Word2Vec word2vec = value.word2Vec;

            Topics topics = new Topics();
            for (CAS cas : value.values) {
              String key = cas.getDocumentLanguage();
              INDArray issueVector;
              try {
                issueVector = paragraphVectors.getLookupTable().vector(key);
              } catch (Throwable t) {
                issueVector = paragraphVectors.inferVector(cas.getDocumentText());
              }

              Collection<String> nearestWords = word2vec.wordsNearest(issueVector, topN);

              Collection<String> nearestLabelsWords = new LinkedList<>();
              for (String label : paragraphVectors.nearestLabels(issueVector, topN)) {
                INDArray nearestIssueVector = paragraphVectors.getLookupTable().vector(label);
                nearestLabelsWords.addAll(word2vec.wordsNearest(nearestIssueVector, topN));
              }

              nearestWords.addAll(nearestLabelsWords);

              INDArray wordVectorsMean = word2vec.getWordVectorsMean(nearestWords);
              Collection<String> extractedTopics = word2vec.wordsNearest(wordVectorsMean, topN);

              Collection<String> toRemove = new LinkedList<>();
              for (String t : extractedTopics) {
                String[] tags = tagger.tag(new String[] {t});
                String tag = tags[0];
                boolean stopTag = Arrays.binarySearch(stopTags, tag) >= 0;
                if (stopTag || StringUtils.isNumeric(t)) {
                  toRemove.add(t);
                }
              }
              extractedTopics.removeAll(toRemove);

              // TODO how to link issue to topics, here?
              for (String t : extractedTopics) {
                topics.add(t, key);
                finalTopics.add(t, key);
              }
            }
            return topics;
          }
        }).print();

    env.execute();
    return finalTopics;
  }

  @Override
  public Topics analyze(String text) throws IOException {
    throw new UnsupportedOperationException();
  }

  private static class Embeddings {
    private final Word2Vec word2Vec;
    private final ParagraphVectors paragraphVectors;
    private final Iterable<CAS> values;

    public Embeddings(Word2Vec word2Vec, ParagraphVectors paragraphVectors, Iterable<CAS> values) {
      this.word2Vec = word2Vec;
      this.paragraphVectors = paragraphVectors;
      this.values = values;
    }
  }

}
