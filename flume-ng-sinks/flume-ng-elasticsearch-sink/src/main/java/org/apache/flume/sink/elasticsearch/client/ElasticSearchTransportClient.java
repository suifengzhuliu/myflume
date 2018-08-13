/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.flume.sink.elasticsearch.client;

import static org.apache.flume.sink.elasticsearch.ElasticSearchSinkConstants.DEFAULT_PORT;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.sink.elasticsearch.ElasticSearchEventSerializer;
import org.apache.flume.sink.elasticsearch.ElasticSearchIndexRequestBuilderFactory;
import org.apache.flume.sink.elasticsearch.IndexNameBuilder;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

public class ElasticSearchTransportClient implements ElasticSearchClient {
	public static final Logger logger = LoggerFactory.getLogger(ElasticSearchTransportClient.class);
	private TransportClient client;
	private TransportAddress[] serverAddresses;
	private ElasticSearchEventSerializer serializer;
	private BulkRequestBuilder bulkRequestBuilder;
	private ElasticSearchIndexRequestBuilderFactory indexRequestBuilderFactory;

	@VisibleForTesting
	void setBulkRequestBuilder(BulkRequestBuilder bulkRequestBuilder) {
		this.bulkRequestBuilder = bulkRequestBuilder;
	}

	public ElasticSearchTransportClient(String[] hostNames, String clusterName,
			ElasticSearchEventSerializer serializer) {
		configureHostnames(hostNames);
		this.serializer = serializer;
		openClient(clusterName);
	}

	public ElasticSearchTransportClient(String[] hostNames, String clusterName,
			ElasticSearchIndexRequestBuilderFactory indexBuilder) {
	    configureHostnames(hostNames);
	    this.indexRequestBuilderFactory = indexBuilder;
	    openClient(clusterName);
	}

	private void openClient(String clusterName) {

		Settings settings = Settings.builder().put("cluster.name", clusterName).build();
		TransportClient transportClient = new PreBuiltTransportClient(settings);

		for (TransportAddress host : serverAddresses) {
			transportClient.addTransportAddress(host);
		}

		if (client != null) {
			client.close();
		}
		client = transportClient;
	}

	private void configureHostnames(String[] hostNames) {
		logger.warn(Arrays.toString(hostNames));
		serverAddresses = new TransportAddress[hostNames.length];
		for (int i = 0; i < hostNames.length; i++) {
			String[] hostPort = hostNames[i].trim().split(":");
			String host = hostPort[0].trim();
			int port = hostPort.length == 2 ? Integer.parseInt(hostPort[1].trim()) : DEFAULT_PORT;
			try {
				serverAddresses[i] = new TransportAddress(InetAddress.getByName(host), port);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void configure(Context context) {
		// TODO Auto-generated method stub

	}

	@Override
	public void close() {
		if (client != null) {
			client.close();
		}
		client = null;

	}

	@Override
	public void addEvent(Event event, IndexNameBuilder indexNameBuilder, String indexType, long ttlMs)
			throws Exception {
		if (bulkRequestBuilder == null) {
			bulkRequestBuilder = client.prepareBulk();
		}
		IndexRequestBuilder indexRequestBuilder = null;

		// String body = new String(event.getBody());
		// JSONObject json = JSONObject.parseObject(body);
		// XContentBuilder builder = jsonBuilder().startObject();
		// Set<String> keySet = json.keySet();
		// for (String key : keySet) {
		// builder.field(key, json.getString(key));
		// }
		// builder.endObject();

		// XContentBuilder build = serializer.getContentBuilder(event);

		String indexName = indexNameBuilder.getIndexName(event);
		// logger.info("the indexname is {},the indextype is {}", indexName,
		// indexType);

		if (indexRequestBuilderFactory == null) {
			indexRequestBuilder = client.prepareIndex(indexNameBuilder.getIndexName(event), indexType)
					.setSource(serializer.getContentBuilder(event));
//			indexRequestBuilder = client.prepareIndex(indexName, indexType).setSource(event.getBody(), XContentType.JSON);
			
		} else {
			indexRequestBuilder = indexRequestBuilderFactory.createIndexRequest(client,
					indexNameBuilder.getIndexPrefix(event), indexType, event);
		}

		indexRequestBuilder = client.prepareIndex(indexName, indexType).setSource(event.getBody(), XContentType.JSON);
		bulkRequestBuilder.add(indexRequestBuilder);
	}

	@Override
	public void execute() throws Exception {
		try {
			BulkResponse bulkResponse = bulkRequestBuilder.execute().actionGet();
			if (bulkResponse.hasFailures()) {
				throw new EventDeliveryException(bulkResponse.buildFailureMessage());
			}
		} finally {
			bulkRequestBuilder = client.prepareBulk();
		}

	}

}
