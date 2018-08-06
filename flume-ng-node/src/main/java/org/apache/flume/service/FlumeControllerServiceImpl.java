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
package org.apache.flume.service;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.flume.lifecycle.LifecycleAware;
import org.apache.flume.node.Application;
import org.apache.flume.node.PollingZooKeeperConfigurationProvider;
import org.apache.flume.service.FlumeControllerService.Iface;
import org.apache.thrift.TException;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;

public class FlumeControllerServiceImpl implements Iface {
	private static final Logger logger = LoggerFactory.getLogger(FlumeControllerServiceImpl.class);
	private ConcurrentHashMap<String, Application> map = new ConcurrentHashMap<String, Application>();

	@Override
	public Status startFlumeAgent(FlumeAgent agent) throws TException {

		try {
			Status res = modifyConf(agent);
			Application application = null;
			// get options
			String zkConnectionStr = "localhost:2181";
			String baseZkPath = "/flume/agent";

			String agentName = agent.getAgentName();

			EventBus eventBus = new EventBus(agentName + "-event-bus");
			List<LifecycleAware> components = Lists.newArrayList();
			PollingZooKeeperConfigurationProvider zookeeperConfigurationProvider = new PollingZooKeeperConfigurationProvider(agentName, zkConnectionStr,
					baseZkPath, eventBus);
			components.add(zookeeperConfigurationProvider);
			application = new Application(components);
			eventBus.register(application);

			application.start();

			map.put(agentName, application);

			final Application appReference = application;
			Runtime.getRuntime().addShutdownHook(new Thread("agent-shutdown-hook") {
				@Override
				public void run() {
					appReference.stop();
				}
			});

		} catch (Exception e) {
			logger.error("A fatal error occurred while running. Exception follows.", e);
		}

		return Status.OK;
	}

	@Override
	public Status stopFlumeAgent(String agentName) throws TException {
		if (map.get(agentName) != null) {
			map.get(agentName).stop();
		}
		return Status.OK;
	}

	@Override
	public Status modifyConf(FlumeAgent agent) throws TException {

		try {

			Properties p = new Properties();
			Configuration cfg = new Configuration();
			// cfg.setDirectoryForTemplateLoading(new
			// File("/Users/user/git/apache-flume-1.8.0-src/flume-ng-node/src/test/resources"));
			// // 模板父路径
			cfg.setObjectWrapper(new DefaultObjectWrapper());

			ClassLoader classLoader = getClass().getClassLoader();
			URL url = classLoader.getResource("flume.ftl");
			// URL url = this.getClass().getResource("/flume.ftl");
			String fileStr = url.getFile();
			File file = new File(fileStr);
			cfg.setDirectoryForTemplateLoading(file.getParentFile());
			Template temp = cfg.getTemplate(file.getName());

			ByteArrayOutputStream bOutput = new ByteArrayOutputStream(2048);
			Writer writer = new BufferedWriter(new OutputStreamWriter(bOutput, "UTF-8"));

			temp.process(agent, writer);
			writer.flush();
			writer.close();

			System.out.println("result  is string \n " + bOutput.toString());

			byte[] data = bOutput.toByteArray();
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

			String agentName = agent.getAgentName();

			// 创建一个目录节点
			Stat stat = zk.exists("/flume/agent/" + agentName, true);
			if (stat == null) {
				zk.create("/flume/agent/" + agentName, data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			} else {
				zk.delete("/flume/agent/" + agentName, stat.getVersion());
				zk.create("/flume/agent/" + agentName, data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return Status.OK;
	}

}
