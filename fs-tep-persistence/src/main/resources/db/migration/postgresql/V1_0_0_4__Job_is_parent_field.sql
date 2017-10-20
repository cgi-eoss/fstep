--Add isParent boolean to job
ALTER TABLE fstep_jobs
ADD COLUMN is_parent BOOLEAN DEFAULT FALSE;

 
