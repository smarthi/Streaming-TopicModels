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
package com.github.tteofili.jtm.cli;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import com.github.tteofili.jtm.AnalysisTool;
import com.github.tteofili.jtm.aggregation.Topics;
import com.github.tteofili.jtm.feed.BuildInfo;
import com.github.tteofili.jtm.feed.Feed;
import com.github.tteofili.jtm.feed.IssuesCollection;
import com.github.tteofili.jtm.feed.io.stax.JiraFeedStaxWriter;
import com.github.tteofili.jtm.feed.utils.FeedUtils;

import picocli.CommandLine.Command;

/**
 * Command use to merge multiple feeds into one.
 * This command accepts export files (e.g. Jira XML export). In order to use a directory containing multiple exports use something like 'bin/jtm merge -r jira `ls -d /path/to/issues/*.* `'
 */
@Command(name = "merge", description = "Merges all the input feeds in a single one")
public class MergeCommand extends AbstractCommand implements AnalysisTool {

    private Feed target;

    @Override
    protected AnalysisTool getAnalisysTool() {
        return this;
    }

    @Override
    protected void setUp() throws Exception {
        log.info("Initializing target feed...");

        target = new Feed();
        target.setVersion("1.0");

        IssuesCollection issuesCollection = new IssuesCollection();
        BuildInfo buildInfo = new BuildInfo();
        buildInfo.setBuildDate(new Date());
        buildInfo.setBuildNumber(System.currentTimeMillis());
        buildInfo.setVersion(System.getProperty("project.version"));
        issuesCollection.setBuildInfo(buildInfo);
        //issuesCollection.setLanguage("en");
        issuesCollection.setDescription("Auto-generated feed by JTM");
        target.setIssues(issuesCollection);
    }

    @Override
    public Topics analyze(Feed feed) throws Exception {
        log.info("Merging {} to the target feed...", feed.getIssues().getTitle());
        FeedUtils.merge(feed, target);
        log.info("{} issues successfully merged to the target feed", feed.getIssues().getIssues().size());
        return null;
    }

    @Override
    public Topics analyze(String text) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void tearDown() throws Exception {
        File outputFile = new File(outputDir, "aggregated-issues" + System.currentTimeMillis() + ".xml");

        log.info("Writing merge result to '{}' output file...", outputFile);

        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(outputFile);
            new JiraFeedStaxWriter().write(stream, target);
        } finally {
            log.info("Merge written to '{}'", outputFile);

            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // swallow it
                }
            }
        }
    }

}
