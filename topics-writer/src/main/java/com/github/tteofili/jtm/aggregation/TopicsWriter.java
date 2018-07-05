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

import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;

import com.github.tteofili.jtm.aggregation.Topics;

public class TopicsWriter {

    private final JsonGeneratorFactory factory;

    public TopicsWriter() {
        Map<String, Object> properties = new HashMap<>(1);
        properties.put(JsonGenerator.PRETTY_PRINTING, true);
        factory = Json.createGeneratorFactory(properties);
    }

    public void write(Topics topics, OutputStream stream) {
        if (topics == null) {
            throw new IllegalArgumentException("Topics can not be null");
        }
        if (stream == null) {
            throw new IllegalArgumentException("Target stream cannot be null");
        }

        JsonGenerator generator = factory.createGenerator(stream);

        generator.writeStartArray();

        Collection<TopicCount> topicCounts = topics.asSortedTopicCounts();
        topicCounts.forEach(entry -> {
            generator.writeStartObject()
                     .write("topic", entry.getTopic())
                     .write("occurrences", entry.getCount().intValue())
                     .writeStartArray("issues");

            entry.getIssues().forEach(generator::write);

            generator.writeEnd().writeEnd();
        });

        generator.writeEnd().close();
    }

}
