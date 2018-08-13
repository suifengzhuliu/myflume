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
package org.apache.flume.ha;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.AsyncCallback.StringCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

public class ActiveStandbyElector implements StringCallback, StatCallback {
	public static final Logger LOG = LoggerFactory.getLogger(ActiveStandbyElector.class);

	public interface ActiveStandbyElectorCallback {
		/**
		 * This method is called when the app becomes the active leader. If the service
		 * fails to become active, it should throw ServiceFailedException. This will
		 * cause the elector to sleep for a short period, then re-join the election.
		 * 
		 * Callback implementations are expected to manage their own timeouts (e.g. when
		 * making an RPC to a remote node).
		 */
		void becomeActive(String agentName) throws Exception;

		/**
		 * This method is called when the app becomes a standby
		 */
		void becomeStandby();

		/**
		 * If the elector gets disconnected from Zookeeper and does not know about the
		 * lock state, then it will notify the service via the enterNeutralMode
		 * interface. The service may choose to ignore this or stop doing state changing
		 * operations. Upon reconnection, the elector verifies the leader status and
		 * calls back on the becomeActive and becomeStandby app interfaces. <br/>
		 * Zookeeper disconnects can happen due to network issues or loss of Zookeeper
		 * quorum. Thus enterNeutralMode can be used to guard against split-brain
		 * issues. In such situations it might be prudent to call becomeStandby too.
		 * However, such state change operations might be expensive and enterNeutralMode
		 * can help guard against doing that for transient issues.
		 */
		void enterNeutralMode();

		/**
		 * If there is any fatal error (e.g. wrong ACL's, unexpected Zookeeper errors or
		 * Zookeeper persistent unavailability) then notifyFatalError is called to
		 * notify the app about it.
		 */
		void notifyFatalError(String errorMessage);

		/**
		 * If an old active has failed, rather than exited gracefully, then the new
		 * active may need to take some fencing actions against it before proceeding
		 * with failover.
		 * 
		 * @param oldActiveData the application data provided by the prior active
		 */
		void fenceOldActive(byte[] oldActiveData);
	}

	static enum State {
		INIT, ACTIVE, STANDBY, NEUTRAL
	};

	private State state = State.INIT;

	@VisibleForTesting
	protected static final String LOCK_FILENAME = "ActiveStandbyElectorLock";
	@VisibleForTesting
	protected static final String BREADCRUMB_FILENAME = "ActiveBreadCrumb";
	private boolean wantToBeInElection;
	private ZooKeeper zkClient;
	private WatcherWithClientRef watcher;
	private final ActiveStandbyElectorCallback appClient;
	private final String zkHostPort;
	private final int zkSessionTimeout;
	private byte[] appData;
	private final String zkLockFilePath;
	private final String zkBreadCrumbPath;
	private final String znodeWorkingDir;
	private final int maxRetryNum;
	private int createRetryCount = 0;
	private boolean monitorLockNodePending = false;
	private ZooKeeper monitorLockNodeClient;
	private String agentName;

	public ActiveStandbyElector(String zookeeperHostPorts, int zookeeperSessionTimeout, String parentZnodeName,
			ActiveStandbyElectorCallback app, int maxRetryNum, boolean failFast, String agentName) throws Exception {
		if (agentName == null || app == null || parentZnodeName == null || zookeeperHostPorts == null
				|| zookeeperSessionTimeout <= 0) {
			throw new Exception("Invalid argument");
		}
		agentName = agentName;
		zkHostPort = zookeeperHostPorts;
		zkSessionTimeout = zookeeperSessionTimeout;
		appClient = app;
		znodeWorkingDir = parentZnodeName;
		zkLockFilePath = znodeWorkingDir + "/" + LOCK_FILENAME;
		zkBreadCrumbPath = znodeWorkingDir + "/" + BREADCRUMB_FILENAME;
		this.maxRetryNum = maxRetryNum;

		// establish the ZK Connection for future API calls
		if (failFast) {
			createConnection();
		} else {
			reEstablishSession();
		}
	}

	public synchronized void joinElection(byte[] data) throws Exception {

		if (data == null) {
			throw new Exception("data cannot be null");
		}

		if (wantToBeInElection) {
			LOG.info("Already in election. Not re-connecting.");
			return;
		}

		appData = new byte[data.length];
		System.arraycopy(data, 0, appData, 0, data.length);

		if (LOG.isDebugEnabled()) {
			LOG.debug("Attempting active election for " + this);
		}
		joinElectionInternal();
	}

	private void joinElectionInternal() {
		Preconditions.checkState(appData != null, "trying to join election without any app data");
		if (zkClient == null) {
			if (!reEstablishSession()) {
				LOG.error("Failed to reEstablish connection with ZooKeeper");
				return;
			}
		}

		createRetryCount = 0;
		wantToBeInElection = true;
		createLockNodeAsync();
	}

	private void createLockNodeAsync() {
		try {
			zkClient.create(zkLockFilePath, appData, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL, this, zkClient);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@VisibleForTesting
	protected void sleepFor(int sleepMs) {
		if (sleepMs > 0) {
			try {
				Thread.sleep(sleepMs);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private void monitorLockNodeAsync() {
		if (monitorLockNodePending && monitorLockNodeClient == zkClient) {
			LOG.info("Ignore duplicate monitor lock-node request.");
			return;
		}
		monitorLockNodePending = true;
		monitorLockNodeClient = zkClient;
		zkClient.exists(zkLockFilePath, watcher, this, zkClient);

	}

	private synchronized boolean isStaleClient(Object ctx) {
		Preconditions.checkNotNull(ctx);
		if (zkClient != (ZooKeeper) ctx) {
			LOG.warn("Ignoring stale result from old client with sessionId "
					+ String.format("0x%08x", ((ZooKeeper) ctx).getSessionId()));
			return true;
		}
		return false;
	}

	private boolean reEstablishSession() {
		int connectionRetryCount = 0;
		boolean success = false;
		while (!success && connectionRetryCount < maxRetryNum) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Establishing zookeeper connection for " + this);
			}
			try {
				createConnection();
				success = true;
			} catch (IOException e) {
				LOG.warn(e.toString());
				sleepFor(5000);
			} catch (KeeperException e) {
				LOG.warn(e.toString());
				sleepFor(5000);
			}
			++connectionRetryCount;
		}
		return success;
	}

	private final class WatcherWithClientRef implements Watcher {
		private ZooKeeper zk;

		/**
		 * Latch fired whenever any event arrives. This is used in order to wait for the
		 * Connected event when the client is first created.
		 */
		private CountDownLatch hasReceivedEvent = new CountDownLatch(1);

		/**
		 * Latch used to wait until the reference to ZooKeeper is set.
		 */
		private CountDownLatch hasSetZooKeeper = new CountDownLatch(1);

		@Override
		public void process(WatchedEvent event) {
			hasReceivedEvent.countDown();
			try {
				if (!hasSetZooKeeper.await(zkSessionTimeout, TimeUnit.MILLISECONDS)) {
					LOG.debug("Event received with stale zk");
				}
				ActiveStandbyElector.this.processWatchEvent(zk, event);
			} catch (Throwable t) {
				LOG.error("Failed to process watcher event " + event + ": " + t);
			}
		}

		private void setZooKeeperRef(ZooKeeper zk) {
			Preconditions.checkState(this.zk == null, "zk already set -- must be set exactly once");
			this.zk = zk;
			hasSetZooKeeper.countDown();
		}

		private void waitForZKConnectionEvent(int connectionTimeoutMs) throws KeeperException, IOException {
			try {
				if (!hasReceivedEvent.await(connectionTimeoutMs, TimeUnit.MILLISECONDS)) {
					LOG.error("Connection timed out: couldn't connect to ZooKeeper in " + connectionTimeoutMs
							+ " milliseconds");
					zk.close();
					throw KeeperException.create(Code.CONNECTIONLOSS);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IOException("Interrupted when connecting to zookeeper server", e);
			}
		}

	}

	synchronized void processWatchEvent(ZooKeeper zk, WatchedEvent event) {
		Event.EventType eventType = event.getType();

		if (eventType == Event.EventType.None) {
			// the connection state has changed
			switch (event.getState()) {
			case SyncConnected:
				LOG.info("Session connected.");

				monitorLockNodeAsync();

				// if the listener was asked to move to safe state then it needs to
				// be undone
//				ConnectionState prevConnectionState = zkConnectionState;
//				zkConnectionState = ConnectionState.CONNECTED;
//				if (prevConnectionState == ConnectionState.DISCONNECTED && wantToBeInElection) {
//					monitorActiveStatus();
//				}
				break;
			case Disconnected:
				LOG.info("Session disconnected. Entering neutral mode...");

				// ask the app to move to safe state because zookeeper connection
				// is not active and we dont know our state
//				zkConnectionState = ConnectionState.DISCONNECTED;
//				enterNeutralMode();
				break;
			case Expired:
				// the connection got terminated because of session timeout
				// call listener to reconnect
//				LOG.info("Session expired. Entering neutral mode and rejoining...");
//				enterNeutralMode();
//				reJoinElection(0);
				break;
			case SaslAuthenticated:
				LOG.info("Successfully authenticated to ZooKeeper using SASL.");
				break;
			default:
				fatalError("Unexpected Zookeeper watch event state: " + event.getState());
				break;
			}

			return;
		}

		// a watch on lock path in zookeeper has fired. so something has changed on
		// the lock. ideally we should check that the path is the same as the lock
		// path but trusting zookeeper for now
		String path = event.getPath();
		if (path != null) {
			switch (eventType) {
			case NodeDeleted:
//				if (state == State.ACTIVE) {
//					enterNeutralMode();
//				}
				joinElectionInternal();
				break;
			case NodeDataChanged:
//				monitorActiveStatus();
				break;
			default:
				if (LOG.isDebugEnabled()) {
					LOG.debug("Unexpected node event: " + eventType + " for path: " + path);
				}
//				monitorActiveStatus();
			}

			return;
		}

		// some unexpected error has occurred
		fatalError("Unexpected watch error from Zookeeper");
	}

	protected ZooKeeper createZooKeeper() throws IOException {
		return new ZooKeeper(zkHostPort, zkSessionTimeout, watcher);
	}

	private void fatalError(String errorMessage) {
		LOG.error(errorMessage);
		appClient.notifyFatalError(errorMessage);
	}

	protected synchronized ZooKeeper connectToZooKeeper() throws IOException, KeeperException {

		// Unfortunately, the ZooKeeper constructor connects to ZooKeeper and
		// may trigger the Connected event immediately. So, if we register the
		// watcher after constructing ZooKeeper, we may miss that event.
		// Instead,
		// we construct the watcher first, and have it block any events it
		// receives
		// before we can set its ZooKeeper reference.
		watcher = new WatcherWithClientRef();
		ZooKeeper zk = createZooKeeper();
		watcher.setZooKeeperRef(zk);

		// Wait for the asynchronous success/failure. This may throw an
		// exception
		// if we don't connect within the session timeout.
		watcher.waitForZKConnectionEvent(zkSessionTimeout);

		return zk;
	}

	private void createConnection() throws IOException, KeeperException {
		if (zkClient != null) {
			try {
				zkClient.close();
			} catch (InterruptedException e) {
				throw new IOException("Interrupted while closing ZK", e);
			}
			zkClient = null;
			watcher = null;
		}
		zkClient = connectToZooKeeper();
		if (LOG.isDebugEnabled()) {
			LOG.debug("Created new connection for " + this);
		}
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

	private static boolean shouldRetry(Code code, Code retryIfCode) {
		return (retryIfCode == null ? false : retryIfCode == code);
	}

	@Override
	public synchronized void processResult(int rc, String path, Object ctx, String name) {
		Code code = Code.get(rc);
		if (isSuccess(code)) {

			monitorLockNodeAsync();

			// we successfully created the znode. we are the leader. start monitoring
//			if (becomeActive()) {
//				monitorActiveStatus();
//			} else {
//				reJoinElectionAfterFailureToBecomeActive();
//			}
			return;
		}
//
//	    if (isNodeExists(code)) {
//	      if (createRetryCount == 0) {
//	        // znode exists and we did not retry the operation. so a different
//	        // instance has created it. become standby and monitor lock.
//	        becomeStandby();
//	      }
//	      // if we had retried then the znode could have been created by our first
//	      // attempt to the server (that we lost) and this node exists response is
//	      // for the second attempt. verify this case via ephemeral node owner. this
//	      // will happen on the callback for monitoring the lock.
//	      monitorActiveStatus();
//	      return;
//	    }
	}

	@Override
	public void processResult(int rc, String path, Object ctx, Stat stat) {
		if (isStaleClient(ctx))
			return;
		monitorLockNodePending = false;
	}

}
