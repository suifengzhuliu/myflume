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
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class TestZK {
	@Test
	public void uploadFileToZK() throws KeeperException, InterruptedException {
		String propFilePath = "/Users/user/work/flume/conf/example.conf";

		ZooKeeper zk = null;
		try {
			zk = new ZooKeeper("localhost:2181", 300000, new Watcher() {
				// 监控所有被触发的事件
				public void process(WatchedEvent event) {
					System.out.println("已经触发了" + event.getType() + "事件！");
				}
			});
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
		Stat stat = zk.exists("/flume/a1", true);
		if (stat == null) {
			zk.create("/flume/a1", data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
		} else {
			zk.delete("/flume/a1", stat.getVersion());
			zk.create("/flume/a1", data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
		}
	}

	@Test
	public void testWriteToZK() {
		try {
			Configuration cfg = new Configuration();
			cfg.setDirectoryForTemplateLoading(new File("/Users/user/git/apache-flume-1.8.0-src/flume-ng-node/src/test/resources")); // 模板父路径
			cfg.setObjectWrapper(new DefaultObjectWrapper());

			Template temp = cfg.getTemplate("config.properties"); // 模板文件，相对于setDirectoryForTemplateLoading设置的路径

			Map<String, Object> root = new HashMap<String, Object>(); // 注意必须有一个根结点
			root.put("agentName", "a1");
			root.put("port", "1213");

			ByteArrayOutputStream bOutput = new ByteArrayOutputStream(1024);
			Writer writer = new BufferedWriter(new OutputStreamWriter(bOutput, "UTF-8"));

			temp.process(root, writer);
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// 创建一个目录节点
			Stat stat = zk.exists("/flume/a2", true);
			if (stat == null) {
				zk.create("/flume/a2", data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			} else {
				zk.delete("/flume/a2", stat.getVersion());
				zk.create("/flume/a2", data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			}

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

}
