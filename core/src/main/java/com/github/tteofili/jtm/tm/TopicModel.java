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

import java.util.Collection;

import com.github.tteofili.jtm.feed.Identifiable;
import com.github.tteofili.jtm.feed.Issue;

/**
 * Topic modeling API for training and inference
 */
public interface TopicModel {

  /**
   * Fit the model with respect to the given issues
   * @param issues the Jira issues
   */
  void fit(Collection<Issue> issues);

  /**
   * Extract top {@code n} topics for the document having the given identifier
   * @param topN number of topics
   * @param documentId the document identifier
   * @return a collection of extracted topics
   */
  Collection<String> extractTopics(int topN, Identifiable documentId);

  /**
   * Extract top {@code n} topics for a given piece of text
   * @param topN number of topics
   * @param text the text to be analyzed
   * @return a collection of extracted topics
   */
  Collection<String> extractTopics(int topN, String text);
}
