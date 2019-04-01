package com.cgi.eoss.fstep.worker.jobs;

import java.io.Serializable;
import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "worker_jobs")
@NoArgsConstructor
@EqualsAndHashCode(of = "jobId")
public class WorkerJob implements Serializable{
    /**
	 * 
	 */
	private static final long serialVersionUID = -5714262512135462991L;

	@Id
	@Column(name = "job_id", nullable = false)
    private String jobId;
    
	@Column(name = "int_job_id", nullable = false)
    private String intJobId;
    
	@Column(name = "worker_node_id")
    private String workerNodeId;
	
	@Column(name = "assigned_to_worker_node")
    private boolean assignedToWorkerNode;

	@Column(name = "container_id")
    private String containerId;
    
	@Column(name = "device_id")
    private String deviceId;
    
	@Column(name = "timeout_minutes")
    private int timeoutMinutes;
    
	@Column(name = "exit_code")
    private Integer exitCode;
    
	@Column(name = "output_root_path")
    private String outputRootPath;
    
	@Column(name = "status")
    private Status status = Status.CREATED;
    
	@Column(name = "start")
    private OffsetDateTime start;
    
	@Column(name = "end")
    private OffsetDateTime end;
    
    
    public WorkerJob(String jobId, String intJobId) {
    	this.jobId = jobId;
    	this.intJobId = intJobId;
    }
    
    public enum Status{
    	CREATED, RUNNING, COMPLETED, ERROR
    }

}