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
package com.github.tteofili.jtm.feed.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import com.github.tteofili.jtm.feed.BuildInfo;
import com.github.tteofili.jtm.feed.Feed;
import com.github.tteofili.jtm.feed.IssuesCollection;

public final class FeedUtils {

    /**
     * This class cannot be directly instantiated.
     */
    private FeedUtils() {
        // do nothing
    }

    public static Feed merge(Feed...feeds) {
        if (feeds == null || feeds.length == 0) {
            throw new IllegalArgumentException("Null or empty Feeds array can not be merged.");
        }
        return merge(Arrays.asList(feeds));
    }

    public static Feed merge(Collection<Feed> feeds) {
        if (feeds == null || feeds.isEmpty()) {
            throw new IllegalArgumentException("Null or empty Feeds collection can not be merged.");
        }

        Feed target = new Feed();
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

        feeds.forEach(source -> merge(source, target));

        return target;
    }

    public static void merge(Feed source, Feed target) {
        if (source == null || target == null) {
            throw new IllegalArgumentException("Both source and target Feed must be not null to be merged.");
        }
        target.getIssues().getIssues().addAll(source.getIssues().getIssues());
    }

}
