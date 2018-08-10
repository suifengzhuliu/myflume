package org.apache.flume.util;

import java.io.IOException;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

public class ZooKeeperSingleton {
	private static ZooKeeper zk;

	public static ZooKeeper getZk() {
		return zk;
	}

	private static class SingletonHolder {

		private static final ZooKeeperSingleton INSTANCE = new ZooKeeperSingleton();
	}

	private ZooKeeperSingleton() {
		try {
			String zkAddress = PropertiesUtil.readValue("zkAddress");
			zk = new ZooKeeper(zkAddress, 300000, new Watcher() {
				// 监控所有被触发的事件
				public void process(WatchedEvent event) {
				}
			});
		} catch (IOException e) {
		}
	}

	public static final ZooKeeperSingleton getInstance() {
		return SingletonHolder.INSTANCE;
	}
}