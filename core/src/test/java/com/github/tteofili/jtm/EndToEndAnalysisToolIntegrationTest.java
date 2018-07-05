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

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import com.github.tteofili.jtm.aggregation.Topics;
import com.github.tteofili.jtm.feed.Feed;
import com.github.tteofili.jtm.feed.jira.JiraFeedReader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Integration tests for {@link EndToEndAnalysisTool}
 */
@RunWith(Parameterized.class)
public class EndToEndAnalysisToolIntegrationTest {

  private final String resource;
  private final int epochs;
  private final int layerSize;
  private final int topN;
  private final String analyzerType;

  public EndToEndAnalysisToolIntegrationTest(String resource, int epochs, int layerSize, int topN, String analyzerType) {
    this.resource = resource;
    this.epochs = epochs;
    this.layerSize = layerSize;
    this.topN = topN;
    this.analyzerType = analyzerType;
  }

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    // resource, epochs, layerSize, topN, analyzerType
    return Arrays.asList(new Object[][] {
        {"/opennlp-issues.xml", 2, 60, 3, "opennlp"},
        {"/opennlp-issues.xml", 2, 60, 3, "simple"},
        {"/lucene-issues.xml", 2, 60, 3, "opennlp"},
        {"/lucene-issues.xml", 2, 60, 3, "simple"},
        {"/oak-issues.xml", 2, 60, 3, "opennlp"},
        {"/oak-issues.xml", 2, 60, 3, "simple"},
    });
  }

  @Test
  public void testExecution() throws Exception {
    EndToEndAnalysisTool endToEndAnalysisTool = new EndToEndAnalysisTool(epochs, layerSize, topN, false, true, false, analyzerType, null);
    InputStream inputStream = getClass().getResourceAsStream(resource);
    Feed feed = new JiraFeedReader().read(inputStream);
    Topics topics = endToEndAnalysisTool.analyze(feed);
    assertNotNull(topics);
    assertFalse(topics.isEmpty());
    inputStream.close();
  }
}