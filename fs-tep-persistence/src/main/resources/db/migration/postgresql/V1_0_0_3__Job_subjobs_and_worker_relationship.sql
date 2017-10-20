--Add worker id to job
ALTER TABLE fstep_jobs
ADD COLUMN worker_id CHARACTER VARYING(255);

-- Insert the relation
ALTER TABLE fstep_jobs
  ADD COLUMN parent_job_id BIGINT REFERENCES fstep_jobs (id);
 
