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
package com.github.tteofili.jtm.feed.jira.io.stax;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.github.tteofili.jtm.feed.Feed;
import com.github.tteofili.jtm.feed.IssuesCollection;
import com.github.tteofili.jtm.feed.Range;
import com.github.tteofili.jtm.feed.io.stax.JiraFeedStaxReader;

@RunWith(Parameterized.class)
public class JiraFeedStaxReaderTest{

    private final String resource;

    public JiraFeedStaxReaderTest(String resource) {
        this.resource = resource;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
      return Arrays.asList(new Object[][] {
          {"lucene-issues.xml"},
          {"oak-issues.xml"},
          {"opennlp-issues.xml"},
      });
    }

    @Test
    public void testParse() throws Exception {
        InputStream input = getClass().getResourceAsStream(resource);
        Feed feed = new JiraFeedStaxReader().read(input, false);
        input.close();

        IssuesCollection issuesCollection = feed.getIssues();
        Range range = issuesCollection.getRange();

        assertEquals(range.getEnd() - range.getStart(), issuesCollection.getIssues().size());
    }
}
