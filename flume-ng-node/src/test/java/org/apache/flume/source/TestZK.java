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

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.junit.Test;

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
		System.out.println(new String(zk.getData("/flume/a1", true, null)));
	}

}
