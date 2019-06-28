package com.cgi.eoss.fstep.rpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class InProcessManagedChannelProvider implements ManagedChannelProvider {

	private final ManagedChannelBuilder inProcessChannelBuilder;

	public InProcessManagedChannelProvider(ManagedChannelBuilder inProcessChannelBuilder) {
		this.inProcessChannelBuilder = inProcessChannelBuilder;
	}

	@Override
	public ManagedChannel getChannel() {
		return inProcessChannelBuilder.build();
	}
}
