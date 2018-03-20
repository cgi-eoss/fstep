package com.cgi.eoss.fstep.worker.worker;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.cgi.eoss.fstep.clouds.service.Node;
import com.cgi.eoss.fstep.clouds.service.NodeFactory;
import com.cgi.eoss.fstep.clouds.service.NodeProvisioningException;
import com.cgi.eoss.fstep.clouds.service.StorageProvisioningException;
import lombok.Synchronized;
import lombok.extern.log4j.Log4j2;
@Log4j2
public class FstepWorkerNodeManager {

    private NodeFactory nodeFactory;

    // Track how many jobs are running on each node
    private final Map<Node, Integer> jobsPerNode = new HashMap<>();

    // Track which Node is used for each job
    private final Map<String, Node> jobNodes = new HashMap<>();

    private int maxJobsPerNode;

    private Path dataBaseDir;

    public static final String pooledWorkerTag = "pooled-worker-node";
    
    public static final String dedicatedWorkerTag = "dedicated-worker-node";
    
    public FstepWorkerNodeManager(NodeFactory nodeFactory, Path dataBaseDir, int maxJobsPerNode) {
        this.nodeFactory = nodeFactory;
        this.dataBaseDir = dataBaseDir;
        this.maxJobsPerNode = maxJobsPerNode;
    }

    public boolean hasCapacity() {
        return findAvailableNode() != null;
    }

    @Synchronized
    public boolean reserveNodeForJob(String jobId) {
        Node availableNode = findAvailableNode();
        if (availableNode != null) {
            jobNodes.put(jobId, availableNode);
            jobsPerNode.put(availableNode, jobsPerNode.getOrDefault(availableNode, 0) + 1);
            return true;
        } else {
            return false;
        }
    }

    public Node getJobNode(String jobId) {
        return jobNodes.get(jobId);
    }
    
    private Node findAvailableNode() {
        for (Node node : nodeFactory.getCurrentNodes(pooledWorkerTag)) {
            if (jobsPerNode.getOrDefault(node, 0) < maxJobsPerNode) {
                return node;
            }
        }
        return null;
    }

    public Set<Node> getCurrentNodes(String tag){
        return nodeFactory.getCurrentNodes(tag);
    }
    
    @Deprecated
    public Node provisionNodeForJob(Path jobDir, String jobId) throws NodeProvisioningException{
        Node node = nodeFactory.provisionNode(dedicatedWorkerTag, jobDir, dataBaseDir);
        jobNodes.put(jobId, node);
        return node;
    }
    
    public void releaseJobNode(String jobId) {
        LOG.debug("Releasing node for job {}", jobId);
        Node jobNode = jobNodes.remove(jobId);
        if (jobNode != null) {
            LOG.debug("Releasing node {} for job {}", jobNode.getId(), jobId);
            if (jobNode.getTag().equals(dedicatedWorkerTag)) {
                nodeFactory.destroyNode(jobNode);
            } else {
                jobsPerNode.put(jobNode, jobsPerNode.get(jobNode) - 1);
            }
        }
    }
    
    public void provisionNodes(int count, String tag, Path environmentBaseDir) throws NodeProvisioningException{
        for (int i = 0; i < count; i++) {
            nodeFactory.provisionNode(tag, environmentBaseDir, dataBaseDir);
        }
    }
    
    @Synchronized
    public int destroyNodes(int count, String tag, Path environmentBaseDir, long minimumHourFractionUptimeSeconds){
        Set<Node> scaleDownNodes = findNFreeWorkerNodes(count, tag, minimumHourFractionUptimeSeconds);
        int destroyableNodes = scaleDownNodes.size();
        for (Node scaleDownNode : scaleDownNodes) {
            nodeFactory.destroyNode(scaleDownNode);
        }
        return destroyableNodes;
    }
    
    private Set<Node> findNFreeWorkerNodes(int n, String tag, long minimumHourFractionUptimeSeconds) {
        Set<Node> scaleDownNodes = new HashSet<Node>();
        Set<Node> currentNodes = nodeFactory.getCurrentNodes(tag);
        long currentEpochSecond = Instant.now().getEpochSecond();
        for (Node node : currentNodes) {
            if (jobsPerNode.getOrDefault(node, 0) == 0 && ((currentEpochSecond - node.getCreationEpochSecond()) % 3600 > minimumHourFractionUptimeSeconds) ) {
                scaleDownNodes.add(node);
                if (scaleDownNodes.size() == n) {
                    return scaleDownNodes;
                }
            }
        }
        return scaleDownNodes;
    }

    public String allocateStorageForJob(String jobId, int requiredStorage, String mountPoint) throws StorageProvisioningException{
        Node jobNode = jobNodes.get(jobId);
        if (jobNode != null) {
            return nodeFactory.allocateStorageForNode(jobNode, requiredStorage, mountPoint);
        }
        else return null;
    }

    public void releaseStorageForJob(Node jobNode, String jobId, String storageId) throws StorageProvisioningException {
         LOG.info("Removing device {} for job {}", storageId, jobId);
         nodeFactory.removeStorageForNode(jobNode, storageId);
    }

	public int getNumberOfFreeNodes(String tag) {
		return nodeFactory.getCurrentNodes(tag).stream().filter(n -> jobsPerNode.getOrDefault(n, 0) == 0).collect(Collectors.toSet()).size();
    }

}
