/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.flume.source;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.flume.service.ESSink;
import org.apache.flume.service.FlumeAgent;
import org.apache.flume.service.FlumeControllerServiceImpl;
import org.apache.flume.service.FlumeSink;
import org.apache.flume.service.FlumeSource;
import org.apache.flume.service.HDFSSink;
import org.apache.flume.service.Interceptor;
import org.apache.flume.service.KafkaSink;
import org.apache.flume.service.KafkaSource;
import org.apache.flume.util.ZooKeeperSingleton;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;

public class TestZK {
	private static final Logger LOG = LoggerFactory.getLogger(FlumeControllerServiceImpl.class);
	private ZooKeeper zkClient;
	private final int zkSessionTimeout = 30000;

	@Test
	public void testNodeDelete() {
		ZooKeeper zk = null;
		try {
			zk = new ZooKeeper("localhost:2181", 300000, new Watcher() {
				// 监控所有被触发的事件
				public void process(WatchedEvent event) {
					System.out.println("已经触发了" + event.getType() + "事件！");
				}
			});

			zk.create("/nodedeletetest", "test".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);

			Thread.sleep(1000 * 60);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void uploadFileToZK() throws KeeperException, InterruptedException {
		String propFilePath = "/Users/user/work/flume/conf/kafka2es.conf";

		ZooKeeper zk = null;
		try {
			zk = new ZooKeeper("localhost:2181", 300000, new Watcher() {
				// 监控所有被触发的事件
				public void process(WatchedEvent event) {
					System.out.println("已经触发了" + event.getType() + "事件！");
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (zk.exists("/flume", true) == null) {
			zk.create("/flume", null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
		}
		InputStream is = null;
		ByteArrayOutputStream bytestream = null;
		byte[] data = null;
		try {
			is = new FileInputStream(propFilePath);
			bytestream = new ByteArrayOutputStream();
			int ch;
			while ((ch = is.read()) != -1) {
				bytestream.write(ch);
			}
			data = bytestream.toByteArray();
			System.out.println(new String(data));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				bytestream.close();
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// 创建一个目录节点
		Stat stat = zk.exists("/flume/agent/kafka2es", true);
		if (stat == null) {
			zk.create("/flume/agent/kafka2es", data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
		} else {
			zk.delete("/flume/agent/kafka2es", stat.getVersion());
			zk.create("/flume/agent/kafka2es", data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
		}
	}

	@Test
	public void testParseConfFile() {
		try {
			Configuration cfg = new Configuration();
			cfg.setDirectoryForTemplateLoading(
					new File("/Users/user/git/apache-flume-1.8.0-src/flume-ng-node/src/main/resources")); // 模板父路径
			cfg.setObjectWrapper(new DefaultObjectWrapper());

			Template temp = cfg.getTemplate("flume.ftl"); // 模板文件，相对于setDirectoryForTemplateLoading设置的路径

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

			ByteArrayOutputStream bOutput = new ByteArrayOutputStream(1024);
			Writer writer = new BufferedWriter(new OutputStreamWriter(bOutput, "UTF-8"));

			temp.process(fa, writer);
			writer.flush();
			writer.close();

			System.out.println("result  is string \n " + bOutput.toString());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void get() throws KeeperException, InterruptedException {
		ZooKeeper zk = null;
		try {
			zk = new ZooKeeper("localhost:2181", 300000, new Watcher() {
				// 监控所有被触发的事件
				public void process(WatchedEvent event) {
					System.out.println("已经触发了" + event.getType() + "事件！");
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(new String(zk.getData("/flume/a2", true, null)));
	}

	@Test
	public void testGetChild() throws KeeperException, InterruptedException {
		ZooKeeper zk = ZooKeeperSingleton.getInstance().getZk();
		String path = "/flume/activelist/";
		if (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		List<String> list = zk.getChildren(path, false);
		for (String string : list) {
			System.out.println(string);
		}
	}
	
	@Test
	public void test1() {
		int a = 1^0;
		System.out.println();
	}
	
}
