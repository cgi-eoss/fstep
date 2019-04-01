package com.cgi.eoss.fstep.worker.worker;

import com.cgi.eoss.fstep.worker.jobs.WorkerJob;

public interface JobUpdateListener {

	void jobUpdate(WorkerJob workerJob, Object update);

}