package com.cgi.eoss.fstep.rpc;

import io.grpc.ManagedChannel;

public interface ManagedChannelProvider {

	public ManagedChannel getChannel();
	
}
