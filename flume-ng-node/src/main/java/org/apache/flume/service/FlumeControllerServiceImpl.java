package org.apache.flume.service;

import org.apache.flume.service.FlumeControllerService.Iface;
import org.apache.thrift.TException;

public class FlumeControllerServiceImpl implements Iface {

	@Override
	public Status startFlumeAgent(String name, String prop) throws TException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Status stopFlumeAgent(String name) throws TException {
		// TODO Auto-generated method stub
		return null;
	}

}
