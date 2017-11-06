package com.cgi.eoss.fstep.worker.metrics;

import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class QueueMetricsService {

    private QueueMetricsRepository queueMetricsRepository;

    @Autowired
    public QueueMetricsService(QueueMetricsRepository queueMetricsRepository) {
        this.queueMetricsRepository = queueMetricsRepository;
    }

    public void updateMetric(long queueLength, long duration) {
        long now = Instant.now().getEpochSecond();
        int removed = queueMetricsRepository.removeOlderThan(now, duration);
        LOG.debug("{} metrics removed", removed);
        saveMetric(now, queueLength);
    }
    
    private void saveMetric(long epoch, long queueLength) {
        QueueMetric qm = new QueueMetric(epoch, queueLength);
        queueMetricsRepository.save(qm);
        
    }
    
    public Iterable<QueueMetric> getAllMetrics() {
        return queueMetricsRepository.findAll();
    }
    
    public long getMetricsCount() {
        return queueMetricsRepository.count();
    }
    
    public QueueAverage getMetrics(long duration) {
        long now = Instant.now().getEpochSecond();
        List<Object[]> metrics = queueMetricsRepository.getMetrics(now, duration);
        Object[] metric = metrics.get(0);
        Long count = (Long) metric[0];
        Double average = (Double) metric[1];
        return new QueueAverage(count, average);
    }
    
}
