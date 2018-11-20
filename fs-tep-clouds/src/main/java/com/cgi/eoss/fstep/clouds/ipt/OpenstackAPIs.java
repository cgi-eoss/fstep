package com.cgi.eoss.fstep.clouds.ipt;

import org.jclouds.openstack.neutron.v2.NeutronApi;
import org.jclouds.openstack.nova.v2_0.NovaApi;

import lombok.Data;

@Data
public class OpenstackAPIs {

	private final NeutronApi neutronApi;
	
	private final NovaApi novaApi;
}
