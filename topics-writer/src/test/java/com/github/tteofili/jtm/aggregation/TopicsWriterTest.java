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

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link TopicsWriter}
 */
public class TopicsWriterTest {

  @Test
  public void testWriting() throws Exception {
    TopicsWriter topicsWriter = new TopicsWriter();
    Topics topics = new Topics();
    topics.add("foo", "oak-1000");
    topics.add("bar", "oak-2001");
    topics.add("foo", "oak-1001");
    topics.add("bar", "oak-2000");
    topics.add("xyz", "oak-8888");
    topics.add("bar", "oak-2002");
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    topicsWriter.write(topics, outputStream);
    outputStream.flush();
    String output = new String(outputStream.toByteArray(), Charset.defaultCharset());
    assertNotNull(output);
  }
}