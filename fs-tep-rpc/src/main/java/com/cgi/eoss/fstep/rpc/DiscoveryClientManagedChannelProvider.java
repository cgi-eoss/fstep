package com.cgi.eoss.fstep.rpc;

import com.google.common.collect.Iterables;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

public class DiscoveryClientManagedChannelProvider implements ManagedChannelProvider {

	private final DiscoveryClient discoveryClient;
	private final String fstepServerServiceId;

	public DiscoveryClientManagedChannelProvider(DiscoveryClient discoveryClient, String fstepServerServiceId) {
		this.discoveryClient = discoveryClient;
		this.fstepServerServiceId = fstepServerServiceId;
	}

	@Override
	public ManagedChannel getChannel() {
		ServiceInstance fstepServer = Iterables.getOnlyElement(discoveryClient.getInstances(fstepServerServiceId));

		return ManagedChannelBuilder
				.forAddress(fstepServer.getHost(), Integer.parseInt(fstepServer.getMetadata().get("grpcPort")))
				.usePlaintext(true).build();
	}

}
