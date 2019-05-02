package com.cgi.eoss.fstep.worker.jobs;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "worker_nodes")
@NoArgsConstructor
@EqualsAndHashCode(of = "nodeId")
public class WorkerNode implements Serializable{
    
	/**
	 * 
	 */
	private static final long serialVersionUID = -8556474797788696543L;


	@Id
	@Column(name = "node_id", nullable = false)
    private String nodeId;
    
	
	@Column(name = "status")
    private Status status = Status.CREATED;
    
    public WorkerNode(String nodeId) {
    	this.nodeId = nodeId;
    }
    
    public WorkerNode(String nodeId, Status status) {
		this.nodeId = nodeId;
		this.status = status;
	}

	public enum Status{
    	CREATED, INITIALIZED, DESTROYING
    }

}