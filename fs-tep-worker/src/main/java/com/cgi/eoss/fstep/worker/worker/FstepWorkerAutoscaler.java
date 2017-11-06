package com.cgi.eoss.fstep.worker.worker;

import java.time.Instant;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.cgi.eoss.fstep.clouds.service.Node;
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
    
    private static final long STATISTICS_WINDOW_MS = 5L * 60L * 1000L;

    private int minWorkerNodes;

    private int maxWorkerNodes;

    private int maxJobsPerNode;

    private QueueMetricsService queueMetricsService;

    private long lastAutoscalingActionTime;

    private long minSecondsBetweenScalingActions;

    @Autowired
    public FstepWorkerAutoscaler(FstepWorkerNodeManager nodeManager, FstepQueueService queueService, QueueMetricsService queueMetricsService,
            JobEnvironmentService jobEnvironmentService, 
            @Qualifier("minWorkerNodes") int minWorkerNodes, 
            @Qualifier("maxWorkerNodes") int maxWorkerNodes, 
            @Qualifier("maxJobsPerNode") int maxJobsPerNode,
            @Qualifier("minSecondsBetweenScalingActions") long minSecondsBetweenScalingActions
            ) {
        this.nodeManager = nodeManager;
        this.queueService = queueService;
        this.queueMetricsService = queueMetricsService;
        this.jobEnvironmentService = jobEnvironmentService;
        this.minWorkerNodes = minWorkerNodes;
        this.maxWorkerNodes = maxWorkerNodes;
        this.maxJobsPerNode = maxJobsPerNode;
        this.minSecondsBetweenScalingActions = minSecondsBetweenScalingActions;
    }

    @Scheduled(fixedRate = QUEUE_CHECK_INTERVAL_MS, initialDelay = 10000L)
    public void getCurrentQueueLength() {
        long queueLength = queueService.getQueueLength(FstepQueueService.jobQueueName);
        queueMetricsService.updateMetric(queueLength, STATISTICS_WINDOW_MS/1000L);
    }

    @Scheduled(fixedRate = AUTOSCALER_INTERVAL_MS, initialDelay = 10000L)
    public void decide() {
        long nowEpoch = Instant.now().getEpochSecond();
        if(lastAutoscalingActionTime != 0L && (nowEpoch - lastAutoscalingActionTime) < minSecondsBetweenScalingActions) {
            return;
        }
        // We check that currentNodes are already equal or greather than minWorkerNodes to be sure that allocation of
        // minNodes has already happened
        Set<Node> currentNodes = nodeManager.getCurrentNodes(FstepWorkerNodeManager.pooledWorkerTag);
        if (currentNodes.size() < minWorkerNodes) {
            return;
        }
        QueueAverage queueAverage = queueMetricsService.getMetrics(STATISTICS_WINDOW_MS/1000L);
        double coverageFactor = 1.0 * QUEUE_CHECK_INTERVAL_MS / STATISTICS_WINDOW_MS;
        double coverage = queueAverage.getCount() * coverageFactor;
        if (coverage > 0.75) {
            LOG.info("Avg queue length is {}", queueAverage.getAverageLength());
            int averageLengthRounded = (int) Math.round(queueAverage.getAverageLength());
            double scaleTarget = 1.0 * averageLengthRounded  / maxJobsPerNode;
            scaleTo((int) Math.round(scaleTarget));
        }
        else {
            LOG.info("Metrics coverage of {} not enough to take scaling decision", coverage);
        }
    }
   

    public void scaleTo(int target) {
        LOG.info("Scaling to {} nodes", target);
        Set<Node> currentNodes = nodeManager.getCurrentNodes(FstepWorkerNodeManager.pooledWorkerTag);
        if (target > currentNodes.size()) {
            lastAutoscalingActionTime = Instant.now().getEpochSecond();
            scaleUp(target - currentNodes.size());
        } else if (target < currentNodes.size()) {
            lastAutoscalingActionTime = Instant.now().getEpochSecond();
            scaleDown(currentNodes.size() - target);
        }
        else {
            LOG.debug("No action needed as current nodes are equal to the target", target);
        }
    }

    public void scaleUp(int numToScaleUp) {
        LOG.info("Evaluating scale up of additional {} nodes", numToScaleUp);
        Set<Node> currentNodes = nodeManager.getCurrentNodes(FstepWorkerNodeManager.pooledWorkerTag);
        int scaleUpTarget = Math.min(currentNodes.size() + numToScaleUp, maxWorkerNodes);
        int actualScaleUp = scaleUpTarget - currentNodes.size();
        LOG.info("Scaling up additional {} nodes. Max worker nodes are {}", actualScaleUp, maxWorkerNodes);
        nodeManager.provisionNodes(actualScaleUp, FstepWorkerNodeManager.pooledWorkerTag, jobEnvironmentService.getBaseDir());
    }

    public void scaleDown(int numToScaleDown) {
        LOG.info("Evaluating scale down of {} nodes", numToScaleDown);
        Set<Node> currentNodes = nodeManager.getCurrentNodes(FstepWorkerNodeManager.pooledWorkerTag);
        int scaleDownTarget = Math.max(currentNodes.size() - numToScaleDown, minWorkerNodes);
        int adjustedScaleDownTarget = currentNodes.size() - scaleDownTarget;
        LOG.info("Scaling down {} nodes. Min worker nodes are {}", adjustedScaleDownTarget, minWorkerNodes);
        int actualScaleDown = nodeManager.destroyNodes(adjustedScaleDownTarget, FstepWorkerNodeManager.pooledWorkerTag, jobEnvironmentService.getBaseDir());
        LOG.info("Scaled down {} nodes of requested {}", actualScaleDown, adjustedScaleDownTarget);
        
    }

}
