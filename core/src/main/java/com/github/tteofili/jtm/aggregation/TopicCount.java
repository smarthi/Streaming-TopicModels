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
import java.util.concurrent.atomic.AtomicInteger;

import org.threadly.concurrent.collections.ConcurrentArrayList;

public class TopicCount implements Comparable<TopicCount>, Serializable {

  public String getTopic() {
    return topic;
  }

  AtomicInteger getCount() {
    return count;
  }

  private final String topic;
    private AtomicInteger count;
    private final Collection<String> issues = new ConcurrentArrayList<>();


    TopicCount(String topic, int initialValue) {
      this.topic = topic;
      this.count = new AtomicInteger(initialValue);
    }

    TopicCount(String topic) {
      this(topic, 0);
    }

    void increment() {
      this.count.incrementAndGet();
    }

    @Override
    public int compareTo(TopicCount o) {
      return o.count.intValue() - this.count.intValue();
    }

    @Override
    public String toString() {
      return topic + " : " + count + issues;
    }

    public void add(String issueId) {
      this.issues.add(issueId);
      this.increment();
    }

    public Collection<String> getIssues() {
      return issues;
    }
  }