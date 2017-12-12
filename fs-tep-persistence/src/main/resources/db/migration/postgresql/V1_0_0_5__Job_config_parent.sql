 --Reference to parent in job config
ALTER TABLE fstep_job_configs ADD COLUMN parent BIGINT REFERENCES fstep_jobs(id);
  