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
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.flume.ha.ActiveStandbyElector;
import org.apache.flume.ha.ActiveStandbyElector.ActiveStandbyElectorCallback;
import org.apache.flume.ha.Constants;
import org.apache.flume.ha.StateServiceUtils;
import org.apache.flume.lifecycle.LifecycleAware;
import org.apache.flume.node.Application;
import org.apache.flume.node.PollingZooKeeperConfigurationProvider;
import org.apache.flume.node.StaticZooKeeperConfigurationProvider;
import org.apache.flume.service.FlumeControllerService.Iface;
import org.apache.flume.util.AddressUtils;
import org.apache.flume.util.PropertiesUtil;
import org.apache.flume.util.ZooKeeperSingleton;
import org.apache.thrift.TException;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;

public class FlumeControllerServiceImpl implements Iface {
	private static final Logger logger = LoggerFactory.getLogger(FlumeControllerServiceImpl.class);
	private ConcurrentHashMap<String, Application> map = new ConcurrentHashMap<String, Application>();

	public static final String MDC_AGENTNAME = "agentname";
	public static final String DEFAULT_FLUME_HA_PATH = "/flume/ha/";

	@Override
	public ResponseState startFlumeAgent(FlumeAgent agent) throws TException {
		ResponseState res = new ResponseState();
		res.setStatus(Status.OK);
		res.setMsg("success");
		try {
			String agentName = agent.getAgentName();
			boolean serviceState = checkServiceState(agent.getSourceList());
			if (!serviceState) {
				res.setStatus(Status.FAILED);
				res.setMsg("服务状态不一致! ");
				return res;
			}
			boolean startState = checkHasStarted(agentName);
			if (startState) {
				res.setStatus(Status.FAILED);
				res.setMsg("the agent has started");
				return res;
			}
			MDC.put(MDC_AGENTNAME, agentName);
			Constants c = getServiceState(agent.getSourceList());
			if (c == Constants.NO_STATE) {
				saveOrUpdateConf(agent);
				startNoStateAgent(agentName);
				add2zk(agentName, c);
			} else if (c == Constants.HAS_STATE) {
				saveOrUpdateConf(agent);
				List<ACL> zkAcls = Ids.OPEN_ACL_UNSAFE;
				String zkAddress = PropertiesUtil.readValue("zkAddress");
				if (zkAddress == null) {
					logger.error("the zkAddress is null ");
					res.setMsg("service error , please check the service logs.");
					res.setStatus(Status.FAILED);
					return res;
				}
				ActiveStandbyElector elector = new ActiveStandbyElector(zkAddress, 5 * 1000, getFlumePathHA(agentName),
						zkAcls, new ElectorCallbacks(), 3, true, agentName);
				elector.ensureParentZNode();
				elector.joinElection(AddressUtils.getHostIp().getBytes());
			}

		} catch (Exception e) {
			res.setMsg(e.getMessage());
			res.setStatus(Status.FAILED);
			logger.error("A fatal error occurred while start flume agent. Exception follows.", e);
		} finally {
			MDC.remove(MDC_AGENTNAME);
		}

		return res;
	}

	private String getFlumePathHA(String agentName) {
		String flumeHaPath = PropertiesUtil.readValue("flumeHaPath");
		if (flumeHaPath == null) {
			flumeHaPath = DEFAULT_FLUME_HA_PATH;
		}

		if (!flumeHaPath.endsWith("\\/")) {
			flumeHaPath = flumeHaPath + "/";
		}

		flumeHaPath = flumeHaPath + agentName;
		return flumeHaPath;
	}

	private Constants getServiceState(List<FlumeSource> sourceList) {
		return StateServiceUtils.getState(sourceList.get(0).getSourceType());
	}

	private boolean checkHasStarted(String agentName) {
		if (map.get(agentName) != null) {
			return true;
		}
		return false;
	}

	/**
	 * 所有的source必须状态一致，要不都是有状态服务，要不全部是无状态服务
	 * 
	 * @param sourceList
	 * @return true:结果正确，false：结果错误
	 */
	private boolean checkServiceState(List<FlumeSource> sourceList) {
		if (sourceList == null || sourceList.size() == 0)
			return false;
		if (sourceList.size() == 1) {
			return true;
		}

		for (int i = 0; i < sourceList.size() - 1; i++) {
			int cur = StateServiceUtils.getState(sourceList.get(i).getSourceType()).getValue();
			int next = StateServiceUtils.getState(sourceList.get(i + 1).getSourceType()).getValue();
			if ((cur ^ next) == 1) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 启动成功的节点保存在zk里 ,flume重启的时候从zk里加载已经启动的agent列表,来决定启动哪些agent 目录节点
	 * /flume/activelist/${agentname}/${host1} /${host2}
	 * 
	 * @param agentName
	 * @param c
	 */
	private void add2zk(String agentName, Constants c) {
		ZooKeeper zk = ZooKeeperSingleton.getInstance().getZk();

		try {
			String activeAgentPath = PropertiesUtil.readValue("zk.agent.activelist");
			Stat stat = zk.exists(activeAgentPath + agentName, true);
			if (stat == null) {
				zk.create(activeAgentPath + agentName, null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			}
			zk.create(activeAgentPath + agentName + "/" + AddressUtils.getHostIp(), null, Ids.OPEN_ACL_UNSAFE,
					CreateMode.PERSISTENT);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public ResponseState stopFlumeAgent(String agentName) throws TException {
		ResponseState res = new ResponseState();
		res.setStatus(Status.OK);
		res.setMsg("success");

		deleteAgentFromZK(agentName);

		try {
			MDC.put(MDC_AGENTNAME, agentName);
			if (map.get(agentName) != null) {
				map.get(agentName).stop();
				return res;
			} else {
				res.setMsg("the flume { " + agentName + " } is not running  ");
				res.setStatus(Status.FAILED);
				logger.error("stop flume agent error,the flume agent {} is not running ", agentName);
			}
		} finally {
			MDC.remove(MDC_AGENTNAME);
		}
		return res;
	}

	private void deleteAgentFromZK(String agentName) {
		try {
			ZooKeeper zk = ZooKeeperSingleton.getInstance().getZk();
			String activeAgentPath = PropertiesUtil.readValue("zk.agent.activelist");
			zk.delete(activeAgentPath + agentName + "/" + AddressUtils.getHostIp(), -1);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public ResponseState saveOrUpdateConf(FlumeAgent agent) throws TException {
		ResponseState res = new ResponseState();
		res.setStatus(Status.OK);
		res.setMsg("success");
		try {
			String agentName = agent.getAgentName();
			Configuration cfg = new Configuration();
			cfg.setObjectWrapper(new DefaultObjectWrapper());

			ClassLoader classLoader = getClass().getClassLoader();
			URL url = classLoader.getResource("flume.ftl");
			String fileStr = url.getFile();
			File file = new File(fileStr);
			cfg.setDirectoryForTemplateLoading(file.getParentFile());
			Template temp = cfg.getTemplate(file.getName());

			ByteArrayOutputStream bOutput = new ByteArrayOutputStream(2048);
			Writer writer = new BufferedWriter(new OutputStreamWriter(bOutput, "UTF-8"));

			temp.process(agent, writer);
			writer.flush();
			writer.close();

			logger.info("saveOrUpdateConf ,the name is {} ,the  conf is {}", agentName, bOutput.toString());

			byte[] data = bOutput.toByteArray();
			ZooKeeper zk = ZooKeeperSingleton.getInstance().getZk();

			String zkAgentPath = getAgentBasePath();
			if (!zkAgentPath.endsWith("\\/")) {
				zkAgentPath = zkAgentPath + "/";
			}
			// 创建一个目录节点
			Stat stat = zk.exists(zkAgentPath + agentName, true);
			if (stat == null) {
				zk.create(zkAgentPath + agentName, data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			} else {
				zk.delete(zkAgentPath + agentName, stat.getVersion());
				zk.create(zkAgentPath + agentName, data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			}

		} catch (Exception e) {
			res.setMsg(e.getMessage());
			res.setStatus(Status.FAILED);
			logger.error("save or update flume conf error ,the error msg is ", e);
		}

		return res;
	}

	private String getAgentBasePath() throws Exception {
		String zkAgentPath = PropertiesUtil.readValue("zkAgentPath");
		if (zkAgentPath.isEmpty()) {
			logger.error("the zkAgentPath is empty");
			throw new Exception("the zkAgentPath is empty");
		}
		return zkAgentPath;
	}

	class ElectorCallbacks implements ActiveStandbyElectorCallback {

		@Override
		public void becomeActive(String agentName) throws Exception {
			logger.info("the agent {} become to active agent.", agentName);
			startHasStateAgent(agentName);
		}

		@Override
		public void becomeStandby() {

		}

		@Override
		public void enterNeutralMode() {

		}

		@Override
		public void notifyFatalError(String errorMessage) {

		}

		@Override
		public void fenceOldActive(byte[] oldActiveData) {

		}

	}

	/**
	 * 启动无服务状态的agent，所有的机器上的agent都要启动成功
	 * 
	 * @param agentName
	 * @throws Exception
	 */
	private void startNoStateAgent(String agentName) throws Exception {
		try {
			Application application = null;
			EventBus eventBus = new EventBus(agentName + "-event-bus");
			List<LifecycleAware> components = Lists.newArrayList();
			String zkAddress = PropertiesUtil.readValue("zkAddress");
			PollingZooKeeperConfigurationProvider zookeeperConfigurationProvider = new PollingZooKeeperConfigurationProvider(
					agentName, zkAddress, getAgentBasePath(), eventBus);
			components.add(zookeeperConfigurationProvider);
			application = new Application(components);
			eventBus.register(application);

			application.start();
			map.put(agentName, application);
		} catch (Exception e) {
			logger.error("startAgent error,the msg is {}", e);
		}
	}

	/*
	 * 启动有服务状态的agent，即全局只能启动成功一个的agent
	 */
	private void startHasStateAgent(String agentName) {
		try {
			Application application = null;
			String zkAddress = PropertiesUtil.readValue("zkAddress");
			StaticZooKeeperConfigurationProvider zookeeperConfigurationProvider = new StaticZooKeeperConfigurationProvider(
					agentName, zkAddress, getAgentBasePath());
			application = new Application();
			application.handleConfigurationEvent(zookeeperConfigurationProvider.getConfiguration());
			application.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void loadStartedAgent() {
		String activeAgentPath = PropertiesUtil.readValue("zk.agent.activelist");
		ZooKeeper zk = ZooKeeperSingleton.getInstance().getZk();
		try {
			if (activeAgentPath != null && activeAgentPath.length() > 1 && activeAgentPath.endsWith("/")) {
				activeAgentPath = activeAgentPath.substring(0, activeAgentPath.length() - 1);
			}
			List<String> list = zk.getChildren(activeAgentPath, false);
			for (String agentName : list) {
				startNoStateAgent(agentName);
			}
		} catch (Exception e) {
			logger.error("loadStartedAgent error,the msg is {}", e);
		}
	}

	@Override
	public ResponseState deleteFlumeAgent(String agentName) throws TException {
		ResponseState res = new ResponseState();
		res.setStatus(Status.OK);
		res.setMsg("success");
		if (map.contains(agentName)) {
			map.get(agentName).stop();
			map.remove(agentName);
		}

		ZooKeeper zk = ZooKeeperSingleton.getInstance().getZk();
		// delete agent conf
		try {
			String zkAgentPath = getAgentBasePath();
			if (!zkAgentPath.endsWith("\\/")) {
				zkAgentPath = zkAgentPath + "/";
			}

			zk.delete(zkAgentPath + agentName, -1);

		} catch (Exception e) {
			e.printStackTrace();
			res.setStatus(Status.FAILED);
			res.setMsg(e.getMessage());
		}

		try {
			String haPath = getFlumePathHA(agentName);
			zk.delete(haPath, -1);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeeperException ke) {
			if (isNodeDoesNotExist(ke.code())) {
				logger.info("No old node to fence");
				return null;
			}
		}

		return res;
	}

	private static boolean isSuccess(Code code) {
		return (code == Code.OK);
	}

	private static boolean isNodeExists(Code code) {
		return (code == Code.NODEEXISTS);
	}

	private static boolean isNodeDoesNotExist(Code code) {
		return (code == Code.NONODE);
	}

	private static boolean isSessionExpired(Code code) {
		return (code == Code.SESSIONEXPIRED);
	}

	private static boolean shouldRetry(Code code) {
		return code == Code.CONNECTIONLOSS || code == Code.OPERATIONTIMEOUT;
	}
}
