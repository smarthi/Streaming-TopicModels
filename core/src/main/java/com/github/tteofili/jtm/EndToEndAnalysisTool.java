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
package com.github.tteofili.jtm;

import java.io.IOException;
import java.util.Collection;

import com.github.tteofili.jtm.aggregation.Topics;
import com.github.tteofili.jtm.feed.Feed;
import com.github.tteofili.jtm.feed.Issue;
import com.github.tteofili.jtm.tm.EmbeddingsTopicModel;
import com.github.tteofili.jtm.tm.TopicModel;
import org.apache.lucene.analysis.Analyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tool for analyzing Jira issues exported as XML by training and executing a topic model.
 *
 */
public class EndToEndAnalysisTool implements AnalysisTool {

  private static final Logger log = LoggerFactory.getLogger(EndToEndAnalysisTool.class);

  private final int epochs;

  private final int layerSize;

  private final int topN;

  private final boolean hierarchicalVectors;

  private final boolean includeComments;

  private final boolean generateClusters;

  private final String analyzerType;

  private final String vectorsOutputFile;

  public EndToEndAnalysisTool(int epochs,
                              int layerSize,
                              int topN,
                              boolean hierarchicalVectors,
                              boolean includeComments,
                              boolean generateClusters,
                              String analyzerType,
                              String vectorsOutputFile) {
    this.epochs = epochs;
    this.layerSize = layerSize;
    this.topN = topN;
    this.hierarchicalVectors = hierarchicalVectors;
    this.includeComments = includeComments;
    this.generateClusters = generateClusters;
    this.analyzerType = analyzerType;
    this.vectorsOutputFile = vectorsOutputFile;
  }

  @Override
  public Topics analyze(Feed feed) throws Exception {
    log.info("Analysing feed {} ({})",
             feed.getIssues().getTitle(),
             feed.getIssues().getBuildInfo());

    Collection<Issue> issues = feed.getIssues().getIssues();

    log.info("{} issues parsed", issues.size());

    Analyzer analyzer ;

    if ("opennlp".equalsIgnoreCase(analyzerType)) {
      analyzer = AnalysisUtils.openNLPAnalyzer();
    } else if ("simple".equalsIgnoreCase(analyzerType)) {
      analyzer = AnalysisUtils.simpleAnalyzer();
    } else if ("shingle-simple".equalsIgnoreCase(analyzerType)) {
      analyzer = AnalysisUtils.shingleSimpleAnalyzer();
    } else if ("shingle-opennlp".equalsIgnoreCase(analyzerType)) {
      analyzer = AnalysisUtils.shingleOpenNLPAnalyzer();
    } else {
      throw new IllegalArgumentException("undefined Analyzer of type '" + analyzerType + "'");
    }

    TopicModel topicModel = new EmbeddingsTopicModel(epochs, layerSize, hierarchicalVectors, includeComments, generateClusters, analyzer, vectorsOutputFile);
    topicModel.fit(issues);

    Topics topics = new Topics();

    feed.getIssues()
        .getIssues()
        .parallelStream()
        .forEach(issue -> {
            topicModel.extractTopics(topN, issue.getKey())
                      .forEach(topic -> {
                          log.debug("Topic '{}' extracted from issue '{}'", topic, issue.getTitle());
                          topics.add(topic, issue.getKey().getValue());
                      });
        });

    log.debug("Resulting topics : \n{}", topics.asSortedTopicCounts());
    return topics;
  }

  @Override
  public Topics analyze(String text) throws IOException {
    throw new UnsupportedOperationException();
  }

}
