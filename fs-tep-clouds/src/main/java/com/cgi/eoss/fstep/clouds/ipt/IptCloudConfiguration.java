package com.cgi.eoss.fstep.clouds.ipt;

import org.openstack4j.api.client.IOSClientBuilder;
import org.openstack4j.core.transport.Config;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.openstack.OSFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import net.schmizz.sshj.SSHClient;

@Configuration
@ConditionalOnProperty(value = "fstep.clouds.ipt.enabled", havingValue = "true")
public class IptCloudConfiguration {

    @Value("${fstep.clouds.ipt.maxPoolSize:-1}")
    private int maxPoolSize;

    @Value("${fstep.clouds.ipt.os.identityEndpoint:https://eocloud.eu:5000/v3}")
    private String osIdentityEndpoint;

    @Value("${fstep.clouds.ipt.os.username}")
    private String osUsername;

    @Value("${fstep.clouds.ipt.os.password}")
    private String osPassword;

    @Value("${fstep.clouds.ipt.os.domainName}")
    private String osDomainName;

    @Value("${fstep.clouds.ipt.os.projectWithEoId}")
    private String osProjectWithEoId;

    @Value("${fstep.clouds.ipt.os.projectWithoutEoId}")
    private String osProjectWithoutEoId;

    @Value("${fstep.clouds.ipt.node.flavorName:eo1.large}")
    private String nodeFlavorName;

    @Value("${fstep.clouds.ipt.node.imageId}")
    private String nodeImageId;

    @Value("${fstep.clouds.ipt.node.floatingIpPool:external-network}")
    private String floatingIpPool;

    @Value("${fstep.clouds.ipt.node.securityGroupName:allow_fstep_services}")
    private String securityGroupName;

    @Value("${fstep.clouds.ipt.node.sshUsername:eouser}")
    private String sshUsername;
    
    @Value("${fstep.clouds.ipt.node.networkId}")
    private String networkId;

    @Value("${fstep.clouds.ipt.node.nfsHost}")
    private String nfsHost;

    @Bean
    public IOSClientBuilder.V3 osClientBuilder() {
        return OSFactory.builderV3()
                .withConfig(Config.newConfig()
                        .withConnectionTimeout(60000)
                        .withReadTimeout(60000))
                .endpoint(osIdentityEndpoint)
                .credentials(osUsername, osPassword, Identifier.byName(osDomainName))
                .scopeToProject(Identifier.byId(osProjectWithoutEoId));
    }

    @Bean
    public SSHClient sshClient() {
        return new SSHClient();
    }

    @Bean
    public IptNodeFactory iptNodeFactory(IOSClientBuilder.V3 osClientBuilder) {
        return new IptNodeFactory(maxPoolSize, osClientBuilder,
                ProvisioningConfig.builder()
                        .defaultNodeFlavor(nodeFlavorName)
                        .floatingIpPool(floatingIpPool)
                        .nodeImageId(nodeImageId)
                        .sshUser(sshUsername)
                        .securityGroupName(securityGroupName)
                        .networkId(networkId)
                        .nfsHost(nfsHost)
                        .build()
        );
    }

}
