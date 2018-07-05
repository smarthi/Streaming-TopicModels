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
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TopicCounts implements Serializable {

  private final Map<String, TopicCount> counts = new ConcurrentHashMap<>();

  private final AtomicInteger occurrencesCount = new AtomicInteger();

  public void add(Collection<String> topics) {
    for (String t : topics) {
      if (counts.containsKey(t)) {
        counts.get(t).increment();
      } else {
        counts.put(t, new TopicCount(t, 1));
      }
    }
  }

  public void add(String topic, String issueId) {
    counts.computeIfAbsent(topic, k -> new TopicCount(topic)).add(issueId);
  }

  Collection<TopicCount> asSortedTopics() {
    return new TreeSet<>(counts.values());
  }

  boolean isEmpty() {
    return counts.isEmpty();
  }

  @Override
  public String toString() {
    return "TopicCounts{" + asSortedTopics() + '}';
  }
}

