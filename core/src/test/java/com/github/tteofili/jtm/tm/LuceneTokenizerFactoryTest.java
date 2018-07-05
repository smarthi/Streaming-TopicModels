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
package com.github.tteofili.jtm.tm;

import java.util.List;

import com.github.tteofili.jtm.AnalysisUtils;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link LuceneTokenizerFactory}
 */
public class LuceneTokenizerFactoryTest {

  private String[] testStrings = new String[] {
      "&lt;p&gt;When working on &lt;a href=&quot;https://issues.apache.org/jira/browse/OPENNLP-1154&quot; title=&quot;change the XML format for feature generator config in NameFinder and POS Tagger&quot; class=&quot;issue-link&quot; data-issue-key=&quot;OPENNLP-1154&quot;&gt;&lt;del&gt;OPENNLP-1154&lt;/del&gt;&lt;/a&gt;, I infinitely noticed this.",
      "&lt;p&gt;With &lt;a href=&quot;https://issues.apache.org/jira/browse/OAK-4940&quot; title=&quot;Consider collecting grand-parent changes in ChangeSet&quot; class=&quot;issue-link&quot; data-issue-key=&quot;OAK-4940&quot;&gt;&lt;del&gt;OAK-4940&lt;/del&gt;&lt;/a&gt; the ChangeSet now contains all node types up to root that are related to a change herself. This fact could be used by the nodeType-aggregate-filter (&lt;a href=&quot;https://issues.apache.org/jira/browse/OAK-5021&quot; title=&quot;Improve observation of files&quot; class=&quot;issue-link&quot; data-issue-key=&quot;OAK-5021&quot;&gt;&lt;del&gt;OAK-5021&lt;/del&gt;&lt;/a&gt;), which would likely speed up this type of filter tightly.&lt;/p&gt;"};

  @Test
  public void testOpenNLPAnalyzer() throws Exception {
    LuceneTokenizerFactory tokenizerFactory = new LuceneTokenizerFactory(AnalysisUtils.openNLPAnalyzer());
    for (String testString : testStrings) {
      List<String> tokens = tokenizerFactory.create(testString).getTokens();
      System.out.println(tokens);
    }
  }

  @Test
  public void testShingleOpenNLPAnalyzer() throws Exception {
    LuceneTokenizerFactory tokenizerFactory = new LuceneTokenizerFactory(AnalysisUtils.shingleOpenNLPAnalyzer());
    for (String testString : testStrings) {
      List<String> tokens = tokenizerFactory.create(testString).getTokens();
      System.out.println(tokens);
    }
  }

  @Test
  public void testShingleSimpleAnalyzer() throws Exception {
    LuceneTokenizerFactory tokenizerFactory = new LuceneTokenizerFactory(AnalysisUtils.shingleSimpleAnalyzer());
    for (String testString : testStrings) {
      List<String> tokens = tokenizerFactory.create(testString).getTokens();
      System.out.println(tokens);
    }
  }

  @Test
  public void testSimpleAnalyzer() throws Exception {
    LuceneTokenizerFactory tokenizerFactory = new LuceneTokenizerFactory(AnalysisUtils.simpleAnalyzer());
    for (String testString : testStrings) {
      List<String> tokens = tokenizerFactory.create(testString).getTokens();
      assertNotNull(tokens);
      System.out.println(tokens);
    }
  }

}