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
package com.github.tteofili.jtm.aggregation;

import java.io.Serializable;
import java.util.Collection;

public class Topics implements Serializable {

  private final TopicCounts topicCounts = new TopicCounts();

  public void add(String topic, String issueId) {
    topicCounts.add(topic, issueId);
  }

  public Collection<TopicCount> asSortedTopicCounts() {
    return topicCounts.asSortedTopics();
  }

  public boolean isEmpty() {
    return topicCounts.isEmpty();
  }

  @Override
  public String toString() {
    return "Topics{" +
        "topicCounts=" + topicCounts +
        '}';
  }

  public void addAll(Topics topics) {
    for (TopicCount tc : topics.asSortedTopicCounts()) {
      for (String i : tc.getIssues()) {
        this.topicCounts.add(tc.getTopic(), i);
      }
    }
  }
}
