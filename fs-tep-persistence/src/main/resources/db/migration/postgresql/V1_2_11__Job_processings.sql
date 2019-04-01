 --Job processings
CREATE TABLE fstep_job_processings (
  id         BIGSERIAL PRIMARY KEY,
  job BIGINT  NOT NULL REFERENCES fstep_jobs (id),
  sequence_num BIGSERIAL NOT NULL,
  start_processing_time   TIMESTAMP WITH TIME ZONE,
  end_processing_time   TIMESTAMP WITH TIME ZONE,
  last_heartbeat 		TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX fstep_job_processings_job_sequence_num_idx
  ON fstep_job_processings (job, sequence_num);
CREATE INDEX fstep_job_processings_job_idx
  ON fstep_job_processings (job);
