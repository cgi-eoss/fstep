package com.cgi.eoss.fstep.clouds.ipt;

import static org.awaitility.Awaitility.with;
import static org.awaitility.Duration.FIVE_HUNDRED_MILLISECONDS;
import static org.awaitility.Duration.FIVE_MINUTES;
import static org.awaitility.Duration.FIVE_SECONDS;
import static org.awaitility.Duration.TWO_SECONDS;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.api.client.IOSClientBuilder;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.compute.Address;
import org.openstack4j.model.compute.Addresses;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.FloatingIP;
import org.openstack4j.model.compute.Keypair;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.ServerCreate;
import org.openstack4j.model.compute.ServerUpdateOptions;
import org.openstack4j.model.network.Network;
import com.cgi.eoss.fstep.clouds.service.Node;
import com.cgi.eoss.fstep.clouds.service.NodeFactory;
import com.cgi.eoss.fstep.clouds.service.NodePoolStatus;
import com.cgi.eoss.fstep.clouds.service.NodeProvisioningException;
import com.cgi.eoss.fstep.clouds.service.SSHSession;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

/**
 * <p>This service may be used to provision and tear down FS-TEP compute nodes in the IPT cloud context.</p>
 */
@Log4j2
public class IptNodeFactory implements NodeFactory {

    private static final int DEFAULT_DOCKER_PORT = 2375;
    private static final String SERVER_NAME_PREFIX = "fstep_node_";
    private static final int SERVER_STARTUP_TIMEOUT_MILLIS = Math.toIntExact(Duration.ofMinutes(10).toMillis());

    @Getter
    private final Set<Node> currentNodes = new HashSet<>();

    private final int maxPoolSize;

    private final IOSClientBuilder.V3 osClientBuilder;

    private final ProvisioningConfig provisioningConfig;

    IptNodeFactory(int maxPoolSize, IOSClientBuilder.V3 osClientBuilder, ProvisioningConfig provisioningConfig) {
        this.maxPoolSize = maxPoolSize;
        this.osClientBuilder = osClientBuilder;
        this.provisioningConfig = provisioningConfig;
        currentNodes.addAll(loadExistingNodes());
    }

    private Set<Node> loadExistingNodes() {
        OSClientV3 osClient = osClientBuilder.authenticate();
        Map<String, String> filteringParams = new HashMap<String, String>();
        filteringParams.put("name", SERVER_NAME_PREFIX + "*");
        //TODO Filtering could be performed at API level - not a big issue with few servers
        return osClient.compute().servers().list().stream()
                .filter(server -> server.getName().startsWith(SERVER_NAME_PREFIX))
                .map(server -> Node.builder()
                .id(server.getId())
                .name(server.getName())
                .tag(server.getMetadata().get("tag"))
                .creationEpochSecond(server.getCreated().toInstant().getEpochSecond())
                .ipAddress(server.getAccessIPv4())
                .dockerEngineUrl("tcp://" + server.getAccessIPv4() + ":" + DEFAULT_DOCKER_PORT)
                .build()).collect(Collectors.toSet());
    }

    @Override
    public Node provisionNode(String tag, Path environmentBaseDir, Path dataBaseDir) throws NodeProvisioningException{
        OSClientV3 osClient = osClientBuilder.authenticate();
        if (getCurrentNodes().size() >= maxPoolSize) {
            throw new NodeProvisioningException("Cannot provision node - pool exhausted. Used: " + getCurrentNodes().size() + " Max: " + maxPoolSize);
        }
        return provisionNode(osClient, tag, environmentBaseDir, dataBaseDir, provisioningConfig.getDefaultNodeFlavor());
    }

    // TODO Expose this overload for workers to provision service-specific flavours
    private Node provisionNode(OSClientV3 osClient, String tag, Path environmentBaseDir, Path dataBaseDir, String flavorName) throws NodeProvisioningException{
        LOG.info("Provisioning IPT node with flavor '{}'", flavorName);
        Server server = null;
        FloatingIP floatingIp = null;
        String keypairName = null;
        try {
            // Generate a random keypair for provisioning
            keypairName = UUID.randomUUID().toString();
            Keypair keypair = osClient.compute().keypairs().create(keypairName, null);
            Flavor flavor = osClient.compute().flavors().list().stream()
                    .filter(f -> f.getName().equals(flavorName))
                    .findFirst().orElseThrow(() -> new NodeProvisioningException("Could not find flavor: " + flavorName));

            HashMap<String, String> metadata = new HashMap<String, String>();
            metadata.put("tag", tag);
            ServerCreate sc = Builders.server()
                    .name(SERVER_NAME_PREFIX + UUID.randomUUID().toString())
                    .flavor(flavor)
                    .image(provisioningConfig.getNodeImageId())
                    .addMetadata(metadata)
                    .keypairName(keypairName)
                    .networks(Arrays.asList(new String[] {provisioningConfig.getNetworkId()}))
                    .addSecurityGroup(provisioningConfig.getSecurityGroupName())
                    .build();

            LOG.info("Provisioning IPT image '{}' to server '{}'", provisioningConfig.getNodeImageId(), sc.getName());
            server = osClient.compute().servers().bootAndWaitActive(sc, SERVER_STARTUP_TIMEOUT_MILLIS);

            if (provisioningConfig.isProvisionFloatingIp()) {
                floatingIp = getFloatingIp(osClient);
                osClient.compute().floatingIps().addFloatingIP(server, floatingIp.getFloatingIpAddress());
                LOG.info("Allocated floating IP to server: {} to {}", floatingIp.getFloatingIpAddress(), server.getId());
                server = osClient.compute().servers().update(server.getId(), ServerUpdateOptions.create().accessIPv4(floatingIp.getFloatingIpAddress()));
            }
            else {
                Addresses addresses = server.getAddresses();
                Map<String, List<? extends Address>> addressesMap = addresses.getAddresses();
                Network network = osClient.networking().network().get(provisioningConfig.getNetworkId());
                List<? extends Address> networkAddresses = addressesMap.get(network.getName());
                Address networkAddress= networkAddresses.get(0);
                server = osClient.compute().servers().update(server.getId(), ServerUpdateOptions.create().accessIPv4(networkAddress.getAddr()));
            }
            
            String serverIP = server.getAccessIPv4();
            LOG.debug("Server access IP: {}", serverIP);
            try (SSHSession ssh = openSshSession(keypair, server)) {
                prepareServer(ssh, environmentBaseDir, dataBaseDir);
            }

            Node node = Node.builder()
                    .id(server.getId())
                    .name(server.getName())
                    .tag(tag)
                    .ipAddress(serverIP)
                    .creationEpochSecond(server.getCreated().toInstant().getEpochSecond())
                    .dockerEngineUrl("tcp://" + server.getAccessIPv4() + ":" + DEFAULT_DOCKER_PORT)
                    .build();
            currentNodes.add(node);
            return node;
        } catch (Exception e) {
            if (server != null) {
                LOG.warn("Tearing down partially-created node {}", server.getId());
                ActionResponse response = osClient.compute().servers().delete(server.getId());
                if (!response.isSuccess()) {
                    LOG.warn("Failed to destroy partially-created node {}", server.getId());
                }
            }
            if (floatingIp != null) {
                osClient.compute().floatingIps().deallocateIP(floatingIp.getId());
            }
            //Remove the keypair
            osClient.compute().keypairs().delete(keypairName);
            throw new NodeProvisioningException(e);
        }
    }

    private FloatingIP getFloatingIp(OSClientV3 osClient) {
        return getUnallocatedFloatingIp(osClient).orElseGet(() -> getNewFloatingIp(osClient));
    }

    private Optional<FloatingIP> getUnallocatedFloatingIp(OSClientV3 osClient) {
        return osClient.compute().floatingIps().list().stream()
                .filter(ip -> Strings.isNullOrEmpty(ip.getInstanceId()))
                .map(ip -> (FloatingIP) ip)
                .findFirst();
    }

    private FloatingIP getNewFloatingIp(OSClientV3 osClient) {
        FloatingIP floatingIP = osClient.compute().floatingIps().allocateIP(provisioningConfig.getFloatingIpPool());
        LOG.debug("Allocated new floating IP: {}", floatingIP);
        return floatingIP;
    }

    private void prepareServer(SSHSession ssh, Path environmentBaseDir, Path dataBaseDir) throws IOException {
        try {
            LOG.debug("IPT node reports hostname: {}", ssh.exec("hostname").getOutput());

            String baseDir = environmentBaseDir.toString();
            String dataBaseDirStr = dataBaseDir.toString();
            LOG.info("Mounting job environment base directory: {}", baseDir);
            ssh.exec("sudo mkdir -p " + baseDir);
            ssh.exec("sudo mkdir -p " + dataBaseDirStr);
            ssh.exec("sudo mount -t nfs " + provisioningConfig.getNfsHost() + ":" + baseDir + " " + baseDir);
            ssh.exec("sudo mount -t nfs " + provisioningConfig.getNfsHost() + ":" + dataBaseDirStr + " " + dataBaseDirStr);
            String additionalNfsMountsStr = provisioningConfig.getAdditionalNfsMounts();
            if (additionalNfsMountsStr != null) {
                String[] additionalNfsMounts = additionalNfsMountsStr.split(",");
                for (String additionalNfsMount: additionalNfsMounts) {
                    ssh.exec("sudo mkdir -p " + additionalNfsMount);
                    ssh.exec("sudo mount -t nfs " + provisioningConfig.getNfsHost() + ":" + additionalNfsMount + " " + additionalNfsMount);
                }
                
            }
            // TODO Use/create a certificate authority for secure docker communication
            LOG.info("Launching dockerd listening on tcp://0.0.0.0:{}", DEFAULT_DOCKER_PORT);
            with().pollInterval(FIVE_HUNDRED_MILLISECONDS)
                    .and().atMost(FIVE_SECONDS)
                    .await("Successfully launched Dockerd")
                    .until(() -> {
                        try {
                            StringBuffer dockerConf = new StringBuffer();
                            dockerConf.append("{");
                            String dockerHost = "\"hosts\":[\"tcp://0.0.0.0:" + DEFAULT_DOCKER_PORT + "\"]";
                            dockerConf.append(dockerHost);
                            if (provisioningConfig.getInsecureRegistries() != null){
                                dockerConf.append(",");
                                dockerConf.append("\"insecure-registries\":[");
                                String[] insecureRegistriesList = provisioningConfig.getInsecureRegistries().split(",");
                                String elems = Arrays.stream(insecureRegistriesList).map(s -> "\""+ s + "\"").collect(Collectors.joining(", "));
                                dockerConf.append(elems);
                                dockerConf.append("]");
                            }
                            dockerConf.append("}");
                            
                            ssh.exec("echo '"+ dockerConf + "'" + "| sudo tee /etc/docker/daemon.json");
                            ssh.exec("sudo systemctl restart docker.service");
                            return ssh.exec("sudo systemctl status docker.service | grep 'API listen on \\[::\\]:2375'").getExitStatus() == 0;
                        } catch (Exception e) {
                            return false;
                        }
                    });
        } catch (Exception e) {
            LOG.error("Failed to prepare server", e);
            throw e;
        }
    }

    private SSHSession openSshSession(Keypair keypair, Server server) throws IOException {
        // Wait until port 22 is open on the server...
        with().pollInterval(TWO_SECONDS)
                .and().atMost(FIVE_MINUTES)
                .await("SSH socket open")
                .until(() -> {
                    try (SSHSession ssh = new SSHSession(server.getAccessIPv4(), provisioningConfig.getSshUser(), keypair.getPrivateKey(), keypair.getPublicKey())) {
                        return true;
                    } catch (Exception e) {
                        LOG.debug("SSH connection not available for server {}", server.getId(), e);
                        return false;
                    }
                });
        // ...then make the SSH connection
        return new SSHSession(server.getAccessIPv4(), provisioningConfig.getSshUser(), keypair.getPrivateKey(), keypair.getPublicKey());
    }

    @Override
    public void destroyNode(Node node) {
        OSClientV3 osClient = osClientBuilder.authenticate();

        LOG.info("Destroying IPT node: {} ({})", node.getId(), node.getName());
        Server server = osClient.compute().servers().get(node.getId());
        String keyname = server.getKeyName();
        ActionResponse response = osClient.compute().servers().delete(server.getId());
        if (response.isSuccess()) {
            LOG.info("Destroyed IPT node: {}", node.getId());
            currentNodes.remove(node);
        } else {
            LOG.warn("Failed to destroy IPT node {}: [{}] {}", node.getId(), response.getCode(), response.getFault());
        }
        // Check for floating IP
        Optional<? extends FloatingIP> floatingIP = osClient.compute().floatingIps().list().stream().filter(ip -> ip.getFloatingIpAddress().equals(server.getAccessIPv4())).findFirst();
        floatingIP.ifPresent(ip -> osClient.compute().floatingIps().deallocateIP(ip.getId()));
        //Remove the keypair
        osClient.compute().keypairs().delete(keyname);
        
    }

    @Override
    public NodePoolStatus getNodePoolStatus() {
        return NodePoolStatus.builder()
                .maxPoolSize(maxPoolSize)
                .used(currentNodes.size())
                .build();
    }
    
    @Override
    public Set<Node> getCurrentNodes(String tag) {
       return currentNodes.stream().filter(node -> node.getTag().equals(tag)).collect(Collectors.toSet());
    }

}
