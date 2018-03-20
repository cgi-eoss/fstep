package com.cgi.eoss.fstep.worker.worker;

import java.time.Instant;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.cgi.eoss.fstep.clouds.service.Node;
import com.cgi.eoss.fstep.clouds.service.NodeProvisioningException;
import com.cgi.eoss.fstep.queues.service.FstepQueueService;
import com.cgi.eoss.fstep.worker.metrics.QueueAverage;
import com.cgi.eoss.fstep.worker.metrics.QueueMetricsService;
import lombok.extern.log4j.Log4j2;

/**
 * <p>
 * Service for autoscaling the number of worker nodes based on queue length
 * </p>
 */
@Log4j2
@Service
@ConditionalOnProperty(name="fstep.worker.autoscaler.enabled", havingValue="true", matchIfMissing = false)
public class FstepWorkerAutoscaler {

    private FstepWorkerNodeManager nodeManager;

    private FstepQueueService queueService;

    private JobEnvironmentService jobEnvironmentService;

    // TODO move this to configuration
    private static final long QUEUE_CHECK_INTERVAL_MS = 5L * 1000L;

    private static final long AUTOSCALER_INTERVAL_MS = 1L * 60L * 1000L;
    
    private static final long STATISTICS_WINDOW_MS = 2L * 60L * 1000L;

    private int minWorkerNodes;

    private int maxWorkerNodes;

    private int maxJobsPerNode;

    private QueueMetricsService queueMetricsService;

    private long lastAutoscalingActionTime;

    private long minSecondsBetweenScalingActions;

    private long minimumHourFractionUptimeSeconds;

    private boolean autoscalingOngoing;

    @Autowired
    public FstepWorkerAutoscaler(FstepWorkerNodeManager nodeManager, FstepQueueService queueService, QueueMetricsService queueMetricsService,
            JobEnvironmentService jobEnvironmentService, 
            @Qualifier("minWorkerNodes") int minWorkerNodes, 
            @Qualifier("maxWorkerNodes") int maxWorkerNodes, 
            @Qualifier("maxJobsPerNode") int maxJobsPerNode,
            @Qualifier("minSecondsBetweenScalingActions") long minSecondsBetweenScalingActions,
            @Qualifier("minimumHourFractionUptimeSeconds") long minimumHourFractionUptimeSeconds
            ) {
        this.nodeManager = nodeManager;
        this.queueService = queueService;
        this.queueMetricsService = queueMetricsService;
        this.jobEnvironmentService = jobEnvironmentService;
        this.minWorkerNodes = minWorkerNodes;
        this.maxWorkerNodes = maxWorkerNodes;
        this.maxJobsPerNode = maxJobsPerNode;
        this.minSecondsBetweenScalingActions = minSecondsBetweenScalingActions;
        this.minimumHourFractionUptimeSeconds = minimumHourFractionUptimeSeconds;
    }

    @Scheduled(fixedRate = QUEUE_CHECK_INTERVAL_MS, initialDelay = 10000L)
    public void getCurrentQueueLength() {
        long queueLength = queueService.getQueueLength(FstepQueueService.jobQueueName);
        queueMetricsService.updateMetric(queueLength, STATISTICS_WINDOW_MS/1000L);
    }

    @Scheduled(fixedRate = AUTOSCALER_INTERVAL_MS, initialDelay = 10000L)
    public void decide() {
        long nowEpoch = Instant.now().getEpochSecond();
        if(autoscalingOngoing || lastAutoscalingActionTime != 0L && (nowEpoch - lastAutoscalingActionTime) < minSecondsBetweenScalingActions) {
            return;
        }
        autoscalingOngoing = true;
        // We check that currentNodes are already equal or greather than minWorkerNodes to be sure that allocation of
        // minNodes has already happened
        try {
	        Set<Node> currentNodes = nodeManager.getCurrentNodes(FstepWorkerNodeManager.pooledWorkerTag);
	        if (currentNodes.size() < minWorkerNodes) {
	            return;
	        }
	        QueueAverage queueAverage = queueMetricsService.getMetrics(STATISTICS_WINDOW_MS/1000L);
	        double coverageFactor = 1.0 * QUEUE_CHECK_INTERVAL_MS / STATISTICS_WINDOW_MS;
	        double coverage = queueAverage.getCount() * coverageFactor;
	        if (coverage > 0.75) {
	            LOG.info("Avg queue length is {}", queueAverage.getAverageLength());
	            int averageLengthRounded = (int) Math.ceil(queueAverage.getAverageLength());
	            double scaleTarget = 1.0 * averageLengthRounded  / maxJobsPerNode;
	            scaleTo((int) Math.round(scaleTarget));
	        }
	        else {
	            LOG.info("Metrics coverage of {} not enough to take scaling decision", coverage);
	        }
	        autoscalingOngoing = false;

        }
        catch (RuntimeException e) {
            autoscalingOngoing = false;
            throw e;
        }
    }
   
    public void scaleTo(int target) {
        LOG.info("Scaling to {} nodes", target);
        int freeNodes = nodeManager.getNumberOfFreeNodes(FstepWorkerNodeManager.pooledWorkerTag);
        if (target > freeNodes) {
            long previousAutoScalingActionTime = lastAutoscalingActionTime;
            try {
                scaleUp(target - freeNodes);
                lastAutoscalingActionTime = Instant.now().getEpochSecond();
            } catch (NodeProvisioningException e) {
                LOG.debug("Autoscaling failed because of node provisioning exception");
                lastAutoscalingActionTime = previousAutoScalingActionTime;
            }
        } else if (target < freeNodes) {
            scaleDown(freeNodes - target);
            lastAutoscalingActionTime = Instant.now().getEpochSecond();
        }
        else {
            LOG.debug("No action needed as current nodes are equal to the target", target);
        }
    }

    public int scaleUp(int numToScaleUp) throws NodeProvisioningException {
        LOG.info("Evaluating scale up of additional {} nodes", numToScaleUp);
        Set<Node> currentNodes = nodeManager.getCurrentNodes(FstepWorkerNodeManager.pooledWorkerTag);
        int scaleUpTarget = Math.min(currentNodes.size() + numToScaleUp, maxWorkerNodes);
        int actualScaleUp = scaleUpTarget - currentNodes.size();
        LOG.info("Scaling up additional {} nodes. Max worker nodes are {}", actualScaleUp, maxWorkerNodes);
        nodeManager.provisionNodes(actualScaleUp, FstepWorkerNodeManager.pooledWorkerTag, jobEnvironmentService.getBaseDir());
        return actualScaleUp;
    }

    public int scaleDown(int numToScaleDown) {
        LOG.info("Evaluating scale down of {} nodes", numToScaleDown);
        Set<Node> currentNodes = nodeManager.getCurrentNodes(FstepWorkerNodeManager.pooledWorkerTag);
        int scaleDownTarget = Math.max(currentNodes.size() - numToScaleDown, minWorkerNodes);
        int adjustedScaleDownTarget = currentNodes.size() - scaleDownTarget;
        LOG.info("Scaling down {} nodes. Min worker nodes are {}", adjustedScaleDownTarget, minWorkerNodes);
        int actualScaleDown = nodeManager.destroyNodes(adjustedScaleDownTarget, FstepWorkerNodeManager.pooledWorkerTag, jobEnvironmentService.getBaseDir(), minimumHourFractionUptimeSeconds);
        LOG.info("Scaled down {} nodes of requested {}", actualScaleDown, adjustedScaleDownTarget);
        return actualScaleDown;
    }

}
