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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
import org.apache.zookeeper.data.ACL;
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
	private int statRetryCount = 0;
	private boolean monitorLockNodePending = false;
	private ZooKeeper monitorLockNodeClient;
	private final List<ACL> zkAcl;
	private String agentName;

	private static enum ConnectionState {
		DISCONNECTED, CONNECTED, TERMINATED
	};

	private Lock sessionReestablishLockForTests = new ReentrantLock();
	private ConnectionState zkConnectionState = ConnectionState.TERMINATED;
	private static final int SLEEP_AFTER_FAILURE_TO_BECOME_ACTIVE = 1000;

	public ActiveStandbyElector(String zookeeperHostPorts, int zookeeperSessionTimeout, String parentZnodeName,
			List<ACL> acl, ActiveStandbyElectorCallback app, int maxRetryNum, boolean failFast, String agentName)
			throws Exception {
		if (agentName == null || app == null || parentZnodeName == null || zookeeperHostPorts == null
				|| zookeeperSessionTimeout <= 0) {
			throw new Exception("Invalid argument");
		}
		this.agentName = agentName;
		zkHostPort = zookeeperHostPorts;
		zkSessionTimeout = zookeeperSessionTimeout;
		appClient = app;
		zkAcl = acl;
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

				// if the listener was asked to move to safe state then it needs to
				// be undone
				ConnectionState prevConnectionState = zkConnectionState;
				zkConnectionState = ConnectionState.CONNECTED;
				if (prevConnectionState == ConnectionState.DISCONNECTED && wantToBeInElection) {
					monitorActiveStatus();
				}
				break;
			case Disconnected:
				LOG.info("Session disconnected. Entering neutral mode...");

				// ask the app to move to safe state because zookeeper connection
				// is not active and we dont know our state
				zkConnectionState = ConnectionState.DISCONNECTED;
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
				if (state == State.ACTIVE) {
					enterNeutralMode();
				}
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

	private void enterNeutralMode() {
		if (state != State.NEUTRAL) {
			state = State.NEUTRAL;
			appClient.enterNeutralMode();
		}
	}

	protected ZooKeeper createZooKeeper() throws IOException {
		return new ZooKeeper(zkHostPort, zkSessionTimeout, watcher);
	}

	private void fatalError(String errorMessage) {
		LOG.error(errorMessage);
		appClient.notifyFatalError(errorMessage);
	}

	private void setAclsWithRetries(final String path) throws KeeperException, InterruptedException {
		final Stat stat = new Stat();
		zkDoWithRetries(new ZKAction<Void>() {
			@Override
			public Void run() throws KeeperException, InterruptedException {
				List<ACL> acl = zkClient.getACL(path, stat);
				if (acl == null || !acl.containsAll(zkAcl) || !zkAcl.containsAll(acl)) {
					zkClient.setACL(path, zkAcl, stat.getAversion());
				}
				return null;
			}
		}, Code.BADVERSION);
	}

	public synchronized void ensureParentZNode() throws IOException, InterruptedException {
		Preconditions.checkState(!wantToBeInElection, "ensureParentZNode() may not be called while in the election");

		String pathParts[] = znodeWorkingDir.split("/");
		Preconditions.checkArgument(pathParts.length >= 1 && pathParts[0].isEmpty(), "Invalid path: %s",
				znodeWorkingDir);

		StringBuilder sb = new StringBuilder();
		for (int i = 1; i < pathParts.length; i++) {
			sb.append("/").append(pathParts[i]);
			String prefixPath = sb.toString();
			LOG.debug("Ensuring existence of " + prefixPath);
			try {
				createWithRetries(prefixPath, new byte[] {}, zkAcl, CreateMode.PERSISTENT);
			} catch (KeeperException e) {
				if (isNodeExists(e.code())) {
					// Set ACLs for parent node, if they do not exist or are different
					try {
						setAclsWithRetries(prefixPath);
					} catch (KeeperException e1) {
						throw new IOException("Couldn't set ACLs on parent ZNode: " + prefixPath, e1);
					}
				} else {
					throw new IOException("Couldn't create " + prefixPath, e);
				}
			}
		}

		LOG.info("Successfully created " + znodeWorkingDir + " in ZK.");
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

	private void monitorActiveStatus() {
		assert wantToBeInElection;
		if (LOG.isDebugEnabled()) {
			LOG.debug("Monitoring active leader for " + this);
		}
		statRetryCount = 0;
		monitorLockNodeAsync();
	}

	private void reJoinElection(int sleepTime) {
		LOG.info("Trying to re-establish ZK session");

		sessionReestablishLockForTests.lock();
		try {
			terminateConnection();
			sleepFor(sleepTime);
			if (appData != null) {
				joinElectionInternal();
			} else {
				LOG.info("Not joining election since service has not yet been " + "reported as healthy.");
			}
		} finally {
			sessionReestablishLockForTests.unlock();
		}
	}

	public synchronized void terminateConnection() {
		if (zkClient == null) {
			return;
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("Terminating ZK connection for " + this);
		}
		ZooKeeper tempZk = zkClient;
		zkClient = null;
		watcher = null;
		try {
			tempZk.close();
		} catch (InterruptedException e) {
			LOG.warn(e.toString());
		}
		zkConnectionState = ConnectionState.TERMINATED;
		wantToBeInElection = false;
	}

	private void reJoinElectionAfterFailureToBecomeActive() {
		reJoinElection(SLEEP_AFTER_FAILURE_TO_BECOME_ACTIVE);
	}

	@Override
	public synchronized void processResult(int rc, String path, Object ctx, String name) {
		Code code = Code.get(rc);
		if (isSuccess(code)) {
			// we successfully created the znode. we are the leader. start monitoring
			if (becomeActive()) {
				monitorActiveStatus();
			} else {
				reJoinElectionAfterFailureToBecomeActive();
			}
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

	private void becomeStandby() {
		if (state != State.STANDBY) {
			state = State.STANDBY;
			appClient.becomeStandby();
		}
	}

	@Override
	public void processResult(int rc, String path, Object ctx, Stat stat) {
		if (isStaleClient(ctx))
			return;
		monitorLockNodePending = false;

		assert wantToBeInElection : "Got a StatNode result after quitting election";

		if (LOG.isDebugEnabled()) {
			LOG.debug("StatNode result: " + rc + " for path: " + path + " connectionState: " + zkConnectionState
					+ " for " + this);
		}

		Code code = Code.get(rc);
		if (isSuccess(code)) {
			// the following owner check completes verification in case the lock znode
			// creation was retried
			if (stat.getEphemeralOwner() == zkClient.getSessionId()) {
				// we own the lock znode. so we are the leader
				if (!becomeActive()) {
					reJoinElectionAfterFailureToBecomeActive();
				}
			} else {
				// we dont own the lock znode. so we are a standby.
				becomeStandby();
			}
			// the watch set by us will notify about changes
			return;
		}

		if (isNodeDoesNotExist(code)) {
			// the lock znode disappeared before we started monitoring it
//			enterNeutralMode();
			joinElectionInternal();
			return;
		}

		String errorMessage = "Received stat error from Zookeeper. code:" + code.toString();
		LOG.debug(errorMessage);

		if (shouldRetry(code)) {
			if (statRetryCount < maxRetryNum) {
				++statRetryCount;
				monitorLockNodeAsync();
				return;
			}
			errorMessage = errorMessage + ". Not retrying further znode monitoring connection errors.";
		} else if (isSessionExpired(code)) {
			// This isn't fatal - the client Watcher will re-join the election
			LOG.warn("Lock monitoring failed because session was lost");
			return;
		}

		fatalError(errorMessage);
	}

	private Stat setDataWithRetries(final String path, final byte[] data, final int version)
			throws InterruptedException, KeeperException {
		return zkDoWithRetries(new ZKAction<Stat>() {
			@Override
			public Stat run() throws KeeperException, InterruptedException {
				return zkClient.setData(path, data, version);
			}
		});
	}

	/**
	 * Write the "ActiveBreadCrumb" node, indicating that this node may need to be
	 * fenced on failover.
	 * 
	 * @param oldBreadcrumbStat
	 */
	private void writeBreadCrumbNode(Stat oldBreadcrumbStat) throws KeeperException, InterruptedException {
		Preconditions.checkState(appData != null, "no appdata");

		LOG.info("Writing znode " + zkBreadCrumbPath + " to indicate that the local node is the most recent active...");
		if (oldBreadcrumbStat == null) {
			// No previous active, just create the node
			createWithRetries(zkBreadCrumbPath, appData, zkAcl, CreateMode.PERSISTENT);
		} else {
			// There was a previous active, update the node
			setDataWithRetries(zkBreadCrumbPath, appData, oldBreadcrumbStat.getVersion());
		}
	}

	private String createWithRetries(final String path, final byte[] data, final List<ACL> acl, final CreateMode mode)
			throws InterruptedException, KeeperException {
		return zkDoWithRetries(new ZKAction<String>() {
			@Override
			public String run() throws KeeperException, InterruptedException {
				return zkClient.create(path, data, acl, mode);
			}
		});
	}

	private boolean becomeActive() {
		assert wantToBeInElection;
		if (state == State.ACTIVE) {
			// already active
			return true;
		}
		try {
			Stat oldBreadcrumbStat = fenceOldActive();
			writeBreadCrumbNode(oldBreadcrumbStat);

			if (LOG.isDebugEnabled()) {
				LOG.debug("Becoming active for " + this);
			}
			appClient.becomeActive(agentName);
			state = State.ACTIVE;
			return true;
		} catch (Exception e) {
			LOG.warn("Exception handling the winning of election", e);
			// Caller will handle quitting and rejoining the election.
			return false;
		}
	}

	private <T> T zkDoWithRetries(ZKAction<T> action) throws KeeperException, InterruptedException {
		return zkDoWithRetries(action, null);
	}

	private <T> T zkDoWithRetries(ZKAction<T> action, Code retryCode) throws KeeperException, InterruptedException {
		int retry = 0;
		while (true) {
			try {
				return action.run();
			} catch (KeeperException ke) {
				if ((shouldRetry(ke.code()) || shouldRetry(ke.code(), retryCode)) && ++retry < maxRetryNum) {
					continue;
				}
				throw ke;
			}
		}
	}

	private interface ZKAction<T> {
		T run() throws KeeperException, InterruptedException;
	}

	private Stat fenceOldActive() throws InterruptedException, KeeperException {
		final Stat stat = new Stat();
		byte[] data;
		LOG.info("Checking for any old active which needs to be fenced...");
		try {
			data = zkDoWithRetries(new ZKAction<byte[]>() {
				@Override
				public byte[] run() throws KeeperException, InterruptedException {
					return zkClient.getData(zkBreadCrumbPath, false, stat);
				}
			});
		} catch (KeeperException ke) {
			if (isNodeDoesNotExist(ke.code())) {
				LOG.info("No old node to fence");
				return null;
			}

			// If we failed to read for any other reason, then likely we lost
			// our session, or we don't have permissions, etc. In any case,
			// we probably shouldn't become active, and failing the whole
			// thing is the best bet.
			throw ke;
		}

		LOG.info("Old node exists: " + new String(data));
		if (Arrays.equals(data, appData)) {
			LOG.info("But old node has our own data, so don't need to fence it.");
		} else {
			appClient.fenceOldActive(data);
		}
		return stat;
	}
}
