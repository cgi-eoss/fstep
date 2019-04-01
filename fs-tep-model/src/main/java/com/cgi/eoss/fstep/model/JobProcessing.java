package com.cgi.eoss.fstep.model;

import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.google.common.collect.ComparisonChain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * <p>A job processing, i.e. an execution of the related job.</p>
 */
@Data
@EqualsAndHashCode(exclude = {"id"})
@Table(name = "fstep_job_processings",
        indexes = {@Index(name = "fstep_job_processings_job_idx", columnList = "job, sequence_num")}
)
@NoArgsConstructor
@Entity
public class JobProcessing implements FstepEntityWithOwner<JobProcessing> {

    /**
     * <p>Unique identifier of the job processing.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    
    /**
     * <p>The job this processing refers to </p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "job")
    private Job job;
    
    @Column(name = "sequence_num")
    private Long sequenceNum;
    
    /**
     * <p>The date and time this job started processing</p>
     */
    @Column(name = "start_processing_time")
    private OffsetDateTime startProcessingTime;

    /**
     * <p>The date and time this job ended processing</p>
     */
    @Column(name = "end_processing_time")
    private OffsetDateTime endProcessingTime;

    /**
     * <p>Last heartbeat received for this job processing</p>
     */
    @Column(name = "last_heartbeat")
    private OffsetDateTime lastHeartbeat;
    /**
     * <p>Create a new JobProcessing instance with the minimum required parameters.</p>
     *
     * @param job The user who owns the job
     * @param startProcessingTime The time this processing started
     */
    public JobProcessing(Job job, long sequenceNum) {
        this.job = job;
        this.sequenceNum = sequenceNum;
    }

    @Override
    public int compareTo(JobProcessing o) {
        return ComparisonChain.start().compare(job, o.job).compare(sequenceNum, o.getSequenceNum()).result();
    }

	@Override
	public User getOwner() {
		return job.getOwner();
	}

	@Override
	public void setOwner(User owner) {
		// no-op; job processings cannot change their owner
		
	}

}
