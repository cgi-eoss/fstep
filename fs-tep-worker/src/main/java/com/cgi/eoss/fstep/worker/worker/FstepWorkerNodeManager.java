package com.cgi.eoss.fstep.worker.worker;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.cgi.eoss.fstep.clouds.service.Node;
import com.cgi.eoss.fstep.clouds.service.NodeFactory;
import com.cgi.eoss.fstep.clouds.service.NodeProvisioningException;
import com.cgi.eoss.fstep.clouds.service.StorageProvisioningException;
import com.cgi.eoss.fstep.worker.jobs.WorkerJobDataService;
import com.cgi.eoss.fstep.worker.jobs.WorkerNode;
import com.cgi.eoss.fstep.worker.jobs.WorkerNodeDataService;
import com.cgi.eoss.fstep.worker.jobs.WorkerNode.Status;
import com.cgi.eoss.fstep.worker.jobs.WorkerJob;

import lombok.extern.log4j.Log4j2;
@Log4j2
public class FstepWorkerNodeManager {

    private NodeFactory nodeFactory;
   
    private int maxJobsPerNode;

    private Path dataBaseDir;
	
    public static final String POOLED_WORKER_TAG = "pooled-worker-node";
    
    public static final String DEDICATED_WORKER_TAG = "dedicated-worker-node";
    
    private WorkerJobDataService workerJobDataService;

    private WorkerNodeDataService workerNodeDataService;

	private NodePreparer nodePreparer;

    
    public FstepWorkerNodeManager(NodeFactory nodeFactory, Path dataBaseDir, int maxJobsPerNode, WorkerJobDataService workerJobDataService, WorkerNodeDataService workerNodeDataService, NodePreparer nodePreparer ) {
        this.nodeFactory = nodeFactory;
        this.dataBaseDir = dataBaseDir;
        this.maxJobsPerNode = maxJobsPerNode;
        this.workerJobDataService = workerJobDataService;
        this.workerNodeDataService = workerNodeDataService;
        this.nodePreparer = nodePreparer;
    }

    public boolean hasCapacity() {
        return findAvailableNode() != null;
    }

    public boolean reserveNodeForJob(WorkerJob workerJob) {
    	for (Node node : nodeFactory.getCurrentNodes(POOLED_WORKER_TAG)) {
            if (workerJobDataService.assignJobToNode(maxJobsPerNode, workerJob, node.getId())) {
            	return true;
            }
        }
        return false;
    }
    
    private Node findAvailableNode() {
        for (Node node : nodeFactory.getCurrentNodes(POOLED_WORKER_TAG)) {
            if (workerJobDataService.countByWorkerNodeIdAndAssignedToWorkerNodeTrue(node.getId()) < maxJobsPerNode) {
            	WorkerNode workerNode = workerNodeDataService.findOne(node.getId());
                if (workerNode != null && workerNode.getStatus().equals(WorkerNode.Status.INITIALIZED))
            		return node;
            }
        }
        return null;
    }

    public Set<Node> getCurrentNodes(String tag){
        return nodeFactory.getCurrentNodes(tag);
    }
    
    @Deprecated
    public Node provisionNodeForJob(Path jobDir, WorkerJob workerJob) throws NodeProvisioningException{
        Node node = nodeFactory.provisionNode(DEDICATED_WORKER_TAG, jobDir, dataBaseDir);
        this.prepareNode(node);
        workerJobDataService.assignJobToNode(1, workerJob, node.getId());
        return node;
    }
    
    @Deprecated
    public void destroyNode(Node node) throws NodeProvisioningException{
    	nodeFactory.destroyNode(node);
    }
    
    public void releaseJobNode(String jobId) {
        LOG.debug("Releasing node for job {}", jobId);
        WorkerJob workerJob = workerJobDataService.findByJobId(jobId);
        String workerNodeId = workerJob.getWorkerNodeId();
        if (workerNodeId != null) {
        	LOG.debug("Releasing node {} for job {}", workerNodeId, jobId);
            Node jobNode = this.getNodeById(workerNodeId);
        	if (jobNode.getTag().equals(DEDICATED_WORKER_TAG)) {
                nodeFactory.destroyNode(jobNode);
            }
        	else {
        		workerJobDataService.releaseJobFromNode(workerJob);
        	}
        }
    }
    
    public void provisionNodes(int count, String tag, Path environmentBaseDir) throws NodeProvisioningException{
        for (int i = 0; i < count; i++) {
            Node node = nodeFactory.provisionNode(tag, environmentBaseDir, dataBaseDir);
            workerNodeDataService.save(new WorkerNode(node.getId(), Status.CREATED));
            prepareNode(node);
            workerNodeDataService.save(new WorkerNode(node.getId(), Status.INITIALIZED));
        }
    }
    
    private void prepareNode(Node node) {
    	if (nodePreparer != null) {
    		nodePreparer.prepareNode(node);
    	}
    }

	public int destroyNodes(int count, String tag, Path environmentBaseDir, long minimumHourFractionUptimeSeconds){
        Set<Node> freeWorkerNodes = findNFreeWorkerNodes(count, tag, minimumHourFractionUptimeSeconds);
        int destroyableNodes = freeWorkerNodes.size();
        for (Node scaleDownNode : freeWorkerNodes) {
        	workerNodeDataService.save(new WorkerNode(scaleDownNode.getId(), Status.DESTROYING));
            nodeFactory.destroyNode(scaleDownNode);
            workerNodeDataService.delete(new WorkerNode(scaleDownNode.getId()));
        }
        return destroyableNodes;
    }
    
    private Set<Node> findNFreeWorkerNodes(int n, String tag, long minimumHourFractionUptimeSeconds) {
        Set<Node> freeWorkerNodes = new HashSet<Node>();
        Set<Node> currentNodes = nodeFactory.getCurrentNodes(tag);
        long currentEpochSecond = Instant.now().getEpochSecond();
        for (Node node : currentNodes) {
            if (workerJobDataService.countByWorkerNodeIdAndAssignedToWorkerNodeTrue(node.getId()) == 0 && ((currentEpochSecond - node.getCreationEpochSecond()) % 3600 > minimumHourFractionUptimeSeconds) ) {
            	freeWorkerNodes.add(node);
                if (freeWorkerNodes.size() == n) {
                    return freeWorkerNodes;
                }
            }
        }
        return freeWorkerNodes;
    }

    public String allocateStorageForJob(WorkerJob workerJob, int requiredStorage, String mountPoint) throws StorageProvisioningException{
        Node node = this.getNodeById(workerJob.getWorkerNodeId());
    	String deviceId = nodeFactory.allocateStorageForNode(node, requiredStorage, mountPoint);
    	workerJobDataService.assignDeviceToJob(workerJob, deviceId);
    	return deviceId;
    }

    public void releaseJobStorage(WorkerJob workerJob, String storageId) throws StorageProvisioningException {
    	Node node = this.getNodeById(workerJob.getWorkerNodeId());
    	nodeFactory.removeStorageForNode(node, storageId);
    }

	public int getNumberOfFreeNodes(String tag) {
		return nodeFactory.getCurrentNodes(tag).stream().filter(n-> workerJobDataService.countByWorkerNodeIdAndAssignedToWorkerNodeTrue(n.getId()) == 0).collect(Collectors.toSet()).size();
    }

	public Node getNodeById(String workerNodeId) {
		Optional<Node> pooledNode = nodeFactory.getCurrentNodes(POOLED_WORKER_TAG).stream().filter(n -> n.getId().equals(workerNodeId)).findFirst();
		if (pooledNode.isPresent()) {
			return pooledNode.get();
		}
		Optional<Node> dedicatedNode = nodeFactory.getCurrentNodes(DEDICATED_WORKER_TAG).stream().filter(n -> n.getId().equals(workerNodeId)).findFirst();
		if (dedicatedNode.isPresent()) {
			return dedicatedNode.get();
		}
		return null;
	}

	public void initCurrentNodes(String tag) {
		Set<Node> currentNodes = this.getCurrentNodes(tag);
		for (Node currentNode : currentNodes) {
			WorkerNode workerNode = workerNodeDataService.findOne(currentNode.getId());
			if (workerNode == null) {
				workerNode = workerNodeDataService.save(new WorkerNode(currentNode.getId(), Status.CREATED));
			}
			if (workerNode.getStatus().equals(Status.CREATED)) {
				nodePreparer.prepareNode(currentNode);
				workerNodeDataService.save(new WorkerNode(currentNode.getId(), Status.INITIALIZED));
			}
		}
		
	}

}
