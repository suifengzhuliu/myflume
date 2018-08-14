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
package org.apache.flume.source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.flume.service.ESSink;
import org.apache.flume.service.FlumeAgent;
import org.apache.flume.service.FlumeControllerService;
import org.apache.flume.service.FlumeSink;
import org.apache.flume.service.FlumeSource;
import org.apache.flume.service.HDFSSink;
import org.apache.flume.service.HttpSource;
import org.apache.flume.service.Interceptor;
import org.apache.flume.service.KafkaSink;
import org.apache.flume.service.KafkaSource;
import org.apache.flume.service.ResponseState;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.junit.Before;
import org.junit.Test;

public class TestThriftClient {

	public static final String SERVER_IP = "localhost";
	public static final int SERVER_PORT = 30000;
	public static final int TIMEOUT = 30000;
	FlumeAgent fa = new FlumeAgent();

	@Before
	public void init() {

		fa.setAgentName("kafka2es");
//		fa.setSourceType(org.apache.flume.service.SourceType.HTTP);
//		fa.setSinkType(org.apache.flume.service.SinkType.LOGGER);

		org.apache.flume.service.HttpSource hs = new org.apache.flume.service.HttpSource();
		hs.setPort("44444");
//		fa.setHttpSource(hs);
		// KafkaSource kafkaSource = new KafkaSource();
		// kafkaSource.setServers("localhost:9092");
		// kafkaSource.setTopics("test1, test2");
		// kafkaSource.setGroup("custom.g.id");
		// kafkaSource.setTopicsRegex("^topic[0-9]$");
		// kafkaSource.setBatchSize("1000");
		// fa.setKafkaSource(kafkaSource);
		//
		// HDFSSink hs = new HDFSSink();
		// hs.setPath("hdfs://localhost:8020");
		// hs.setFilePrefix("events-");
		// fa.setHdfsSink(hs);
	}

	@Test
	public void testStart() {
		TTransport transport = null;

		FlumeAgent fa = getTest2Agent();

		try {
			transport = new TSocket(SERVER_IP, SERVER_PORT, TIMEOUT);
			TProtocol protocol = new TBinaryProtocol(transport);
			FlumeControllerService.Client client = new FlumeControllerService.Client(protocol);
			transport.open();
			ResponseState result = client.startFlumeAgent(fa);
			System.out.println("thrift remote call : " + result.msg);
		} catch (TTransportException e) {
			e.printStackTrace();
		} catch (TException e) {
			e.printStackTrace();
		} finally {
			if (null != transport) {
				transport.close();
			}
		}
	}

	private FlumeAgent getTest2Agent() {
		FlumeAgent fa = new FlumeAgent();
		fa.setAgentName("mytestagent");

		List<FlumeSource> sourceList = new ArrayList<>();

		FlumeSource fs = new FlumeSource();
		fs.setSourceType(org.apache.flume.service.SourceType.HTTP);
		HttpSource httpSource = new HttpSource();
		httpSource.setPort("1000");
		fs.setHttpSource(httpSource);
		sourceList.add(fs);

		List<FlumeSink> sinkList = new ArrayList<>();

		FlumeSink flumeSink1 = new FlumeSink();
		flumeSink1.setSinkType(org.apache.flume.service.SinkType.ES);
		ESSink essink = new ESSink();
		essink.setClusterName("elasticsearch");
		essink.setHostNames("10.91.66.14:9301");
		essink.setIndexName("logs");
		essink.setIndexType("blog");
		essink.setContentType("json");
		flumeSink1.setEsSink(essink);
		sinkList.add(flumeSink1);

		fa.setSourceList(sourceList);
		fa.setSinkList(sinkList);

		return fa;
	}

	private FlumeAgent getTest1Agent() {
		FlumeAgent fa = new FlumeAgent();
		fa.setAgentName("kafkaes");

		List<FlumeSource> sourceList = new ArrayList<>();

		FlumeSource fs = new FlumeSource();
		fs.setSourceType(org.apache.flume.service.SourceType.KAFKA);
		KafkaSource kafkaSource = new KafkaSource();
		kafkaSource.setServers(
				"10.35.66.127:9092,10.35.66.128:9092,10.35.66.129:9092,10.35.66.130:9092,10.35.66.131:9092,10.35.66.132:9092,10.35.66.133:9092,10.35.66.134:9092,10.35.66.135:9092,10.35.66.136:9092");
		kafkaSource.setTopics("Kafka-savior");
		kafkaSource.setGroup("myconsumer");

		Interceptor i = new Interceptor();
		i.setType("org.apache.flume.interceptor.SubInterceptor$Builder");

		List<Interceptor> interceptorList = new ArrayList<>();
		interceptorList.add(i);

		fs.setInterceptorList(interceptorList);
		fs.setKafkaSource(kafkaSource);
		sourceList.add(fs);

		List<FlumeSink> sinkList = new ArrayList<>();

		FlumeSink flumeSink1 = new FlumeSink();
		flumeSink1.setSinkType(org.apache.flume.service.SinkType.ES);
		ESSink essink = new ESSink();
		essink.setClusterName("elasticsearch");
		essink.setHostNames("10.91.66.14:9301");
		essink.setIndexName("logs");
		essink.setIndexType("blog");
		essink.setContentType("json");
		flumeSink1.setEsSink(essink);
		sinkList.add(flumeSink1);

		fa.setSourceList(sourceList);
		fa.setSinkList(sinkList);

		return fa;
	}

	private FlumeAgent getKafka2esAgent() {
		FlumeAgent fa = new FlumeAgent();
		fa.setAgentName("kafka2es");

		List<FlumeSource> sourceList = new ArrayList<>();

		FlumeSource fs = new FlumeSource();
		fs.setSourceType(org.apache.flume.service.SourceType.KAFKA);
		KafkaSource kafkaSource = new KafkaSource();
		kafkaSource.setServers("localhost:9092");
		kafkaSource.setTopics("test1, test2");
		kafkaSource.setGroup("custom.g.id");

		Interceptor i = new Interceptor();
		i.setType("org.apache.flume.interceptor.HostInterceptor$Builder");
		Map params = new HashMap<>();
		params.put("preserveExisting", "false");
		params.put("hostHeader", "hostname");
		i.setParams(params);

		Interceptor i1 = new Interceptor();
		i1.setType("org.apache.flume.interceptor.TimestampInterceptor$Builder");

		List<Interceptor> interceptorList = new ArrayList<>();
		interceptorList.add(i);
		interceptorList.add(i1);

		fs.setInterceptorList(interceptorList);
		fs.setKafkaSource(kafkaSource);
		sourceList.add(fs);

		FlumeSource fs1 = new FlumeSource();
		fs1.setSourceType(org.apache.flume.service.SourceType.KAFKA);
		KafkaSource kafkaSource1 = new KafkaSource();
		kafkaSource1.setServers("localhost:9093");
		kafkaSource1.setTopics("test3, test4");
		kafkaSource1.setGroup("custom");
		fs1.setKafkaSource(kafkaSource1);
		sourceList.add(fs1);

		List<FlumeSink> sinkList = new ArrayList<>();
		FlumeSink flumeSink = new FlumeSink();
		flumeSink.setSinkType(org.apache.flume.service.SinkType.HDFS);
		HDFSSink hs = new HDFSSink();
		hs.setPath("hdfs://localhost:8020");
		hs.setFilePrefix("events-");
		flumeSink.setHdfsSink(hs);
		sinkList.add(flumeSink);

		FlumeSink flumeSink1 = new FlumeSink();
		flumeSink1.setSinkType(org.apache.flume.service.SinkType.ES);
		ESSink essink = new ESSink();
		essink.setClusterName("elastic");
		essink.setHostNames("localhost:9200");
		essink.setIndexName("indexname");
		flumeSink1.setEsSink(essink);
		sinkList.add(flumeSink1);

		FlumeSink flumeSink2 = new FlumeSink();
		flumeSink2.setSinkType(org.apache.flume.service.SinkType.KAFKA);
		KafkaSink ks = new KafkaSink();
		ks.setTopic("my topic");
		ks.setServers("localhost");
		flumeSink2.setKafkaSink(ks);
		sinkList.add(flumeSink2);

		fa.setSourceList(sourceList);
		fa.setSinkList(sinkList);

		return fa;
	}

	@Test
	public void testModifyConf() {
		TTransport transport = null;
		try {
			transport = new TSocket(SERVER_IP, SERVER_PORT, TIMEOUT);
			TProtocol protocol = new TBinaryProtocol(transport);
			FlumeControllerService.Client client = new FlumeControllerService.Client(protocol);
			transport.open();

			ResponseState result = client.saveOrUpdateConf(fa);
			// System.out.println("thrift remote call : " + result);
		} catch (TTransportException e) {
			e.printStackTrace();
		} catch (TException e) {
			e.printStackTrace();
		} finally {
			if (null != transport) {
				transport.close();
			}
		}
	}

	@Test
	public void testStop() {
		TTransport transport = null;
		try {
			transport = new TSocket(SERVER_IP, SERVER_PORT, TIMEOUT);
			TProtocol protocol = new TBinaryProtocol(transport);
			FlumeControllerService.Client client = new FlumeControllerService.Client(protocol);
			transport.open();
			ResponseState result = client.stopFlumeAgent("kafkaes");
			System.out.println("thrift remote call : " + result);
		} catch (TTransportException e) {
			e.printStackTrace();
		} catch (TException e) {
			e.printStackTrace();
		} finally {
			if (null != transport) {
				transport.close();
			}
		}
	}

}