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

import java.io.File;
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
 * Tool for analyzing Jira issues exported as XML by using a pretrained topic model.
 *
 */
public class StaticAnalysisTool implements AnalysisTool {

  private static final Logger log = LoggerFactory.getLogger(StaticAnalysisTool.class);

  private final File paragraphVectorsModelFile;
  private final int topN;

  public StaticAnalysisTool(File paragraphVectorsModelFile, int topN) {
    this.paragraphVectorsModelFile = paragraphVectorsModelFile;
    this.topN = topN;
  }

  @Override
  public Topics analyze(Feed feed) throws Exception {
    log.info("Analysing feed {} ({})",
             feed.getIssues().getTitle(),
             feed.getIssues().getBuildInfo());

    TopicModel topicModel = new EmbeddingsTopicModel(paragraphVectorsModelFile);

    Topics topics = new Topics();

    feed.getIssues()
        .getIssues()
        .parallelStream()
        .forEach(issue -> {
            topicModel.extractTopics(topN, issue.getKey())
                      .parallelStream()
                      .forEach(topic -> {
                          log.info("Topic '{}' extracted from issue '{}'", topic, issue.getTitle());
                          topics.add(topic, issue.getKey().getValue());
                      });
        });

    return topics;
  }

  @Override
  public Topics analyze(String text) throws IOException {
    TopicModel topicModel = new EmbeddingsTopicModel(paragraphVectorsModelFile);

    Topics topics = new Topics();

    topicModel.extractTopics(topN, text)
        .parallelStream()
        .forEach(topic -> {
          log.info("Topic '{}' extracted from text '{}'", topic, text);
          topics.add(topic, text);
        });
    return topics;
  }

}
