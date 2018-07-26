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

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 启动类， 启动thrift服务，用于接受web端的请求，或者其他节点的相关服务,如做主备切换的时候fenceOldActive节点.
 * 
 * @author user
 *
 */
public class App {
	private static final Logger LOG = LoggerFactory.getLogger(App.class);
	private static int SERVER_PORT = 30000;

	public static void main(String[] args) {
		System.out.println("server start ....");
		FlumeControllerService.Processor tprocessor = new FlumeControllerService.Processor(new FlumeControllerServiceImpl());
		TServerSocket serverTransport;
		try {

			serverTransport = new TServerSocket(SERVER_PORT);
			TServer.Args tArgs = new TServer.Args(serverTransport);
			tArgs.processor(tprocessor);
			tArgs.protocolFactory(new TBinaryProtocol.Factory());
			TServer server = new TSimpleServer(tArgs);
			server.serve();
		} catch (TTransportException e) {
			e.printStackTrace();
		}
	}
}
