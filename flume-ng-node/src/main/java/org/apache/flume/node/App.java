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
package org.apache.flume.node;

import org.apache.flume.service.FlumeControllerService;
import org.apache.flume.service.FlumeControllerServiceImpl;
import org.apache.flume.service.FlumeControllerService.Processor;
import org.apache.flume.util.PropertiesUtil;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
	private static final Logger logger = LoggerFactory.getLogger(App.class);
	private static String DEFAULT_SERVER_PORT = "30000";

	public static void main(String[] args) {
		logger.info("server start ....");

		String port = PropertiesUtil.readValue("thrift.service.port");
		if (null == port || port.length() == 0) {
			port = DEFAULT_SERVER_PORT;
		}

		FlumeControllerServiceImpl service = new FlumeControllerServiceImpl();

		FlumeControllerService.Processor tprocessor = new FlumeControllerService.Processor(service);
		TServerSocket serverTransport;
		try {

			serverTransport = new TServerSocket(Integer.parseInt(port));
			TServer.Args tArgs = new TServer.Args(serverTransport);
			tArgs.processor(tprocessor);
			tArgs.protocolFactory(new TBinaryProtocol.Factory());
			TServer server = new TSimpleServer(tArgs);
			server.serve();

			
			service.loadStartedAgent();
			
		} catch (TTransportException e) {
			logger.error("start thrift service error,the msg is ", e);
		}
	}
}
