package com.cgi.eoss.fstep.clouds.local;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import com.cgi.eoss.fstep.clouds.service.Node;
import com.cgi.eoss.fstep.clouds.service.NodeFactory;
import com.cgi.eoss.fstep.clouds.service.NodePoolStatus;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

/**
 * <p>This service may be used to access Docker 'nodes' in the LOCAL context, i.e. a single Docker Engine running at a
 * configurable URL.</p>
 */
@Log4j2
public class LocalNodeFactory implements NodeFactory {

    @Getter
    private final Set<Node> currentNodes = new HashSet<>();

    private final int maxPoolSize;

    private final String dockerHostUrl;

    public LocalNodeFactory(int maxPoolSize, String dockerHostUrl) {
        this.maxPoolSize = maxPoolSize;
        this.dockerHostUrl = dockerHostUrl;
    }

    @Override
    public Node provisionNode(String tag, Path environmentBaseDir, Path dataBaseDir) {
        // TODO Check against maxPoolSize
        LOG.info("Provisioning LOCAL node");
        Node node = Node.builder()
                .id(UUID.randomUUID().toString())
                .name("LOCAL node")
                .tag(tag)
                .dockerEngineUrl(dockerHostUrl)
                .build();
        currentNodes.add(node);
        return node;
    }

    @Override
    public void destroyNode(Node node) {
        LOG.info("Destroying LOCAL node: {} ({})", node.getId(), node.getName());
        currentNodes.remove(node);
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
