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

import org.apache.flume.service.FlumeAgent;
import org.apache.flume.service.FlumeControllerService;
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

		FlumeAgent fa = getKafka2esAgent();

		try {
			transport = new TSocket(SERVER_IP, SERVER_PORT, TIMEOUT);
			TProtocol protocol = new TBinaryProtocol(transport);
			FlumeControllerService.Client client = new FlumeControllerService.Client(protocol);
			transport.open();
			ResponseState result = client.startFlumeAgent(fa);
			System.out.println("thrift remote call : " + result.status);
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

	private FlumeAgent getKafka2esAgent() {
		FlumeAgent fa = new FlumeAgent();
		fa.setAgentName("kafka2es");
//		fa.setSourceType(org.apache.flume.service.SourceType.KAFKA);
//		fa.setSinkType(org.apache.flume.service.SinkType.ES);

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
			ResponseState result = client.stopFlumeAgent("kafka2es");
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