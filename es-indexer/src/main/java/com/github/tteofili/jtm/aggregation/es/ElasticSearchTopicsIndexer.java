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
package com.github.tteofili.jtm.aggregation.es;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeSet;

import com.github.tteofili.jtm.aggregation.TopicCount;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.xpack.client.PreBuiltXPackTransportClient;

import com.github.tteofili.jtm.aggregation.Topics;
import com.github.tteofili.jtm.aggregation.TopicsIndexer;
import com.github.tteofili.jtm.feed.Feed;

public class ElasticSearchTopicsIndexer implements TopicsIndexer {

    private TransportClient client;

    @Override
    public String getIndexerName() {
        return "es";
    }

    @Override
    public void setUp() throws Exception {
        Properties properties = new Properties();
        InputStream stream = getClass().getResourceAsStream("/es.properties");

        try {
            properties.load(stream);
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                // do nothing
            }
        }

        Settings.Builder settings = Settings.builder();
        set("cluster.name", properties, settings);
        set("xpack.security.user", properties, settings);
        client = new PreBuiltXPackTransportClient(settings.build());

        String address = properties.getProperty("address");
        int port = Integer.parseInt(properties.getProperty("port"));

        client.addTransportAddresses(new TransportAddress(new InetSocketAddress(address, port)));
    }

    private static void set(String property, Properties sources, Settings.Builder target) {
        target.put(property, sources.getProperty(property));
    }

    @Override
    public void index(Feed feed, Topics extractedTopics) throws Exception {
        String indexName = feed.getIssues().getTitle().toLowerCase();

        String type = "doc";

        for (TopicCount entry : extractedTopics.asSortedTopicCounts()) {
            XContentBuilder jsonBuilder = XContentFactory.jsonBuilder()
                            .startObject()
                            .field("topic", entry.getTopic())
                            .array("issues", entry.getIssues())
                            .endObject();

            IndexResponse response = client.prepareIndex(indexName, type, entry.getTopic())
              .setSource(jsonBuilder)
              .get();
            // TODO handle the result
        }
    }

    @Override
    public void tearDown() throws Exception {
        client.close();
    }

}
