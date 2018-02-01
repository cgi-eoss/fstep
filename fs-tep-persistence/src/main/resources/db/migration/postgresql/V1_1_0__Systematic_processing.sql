 --Systematic processings
 
 CREATE TYPE fstep_systematic_processing_status AS ENUM ('ACTIVE', 'BLOCKED' ,'COMPLETED');
 
 CREATE TABLE fstep_systematic_processings (
  id      BIGSERIAL PRIMARY KEY,
  owner   BIGINT    NOT NULL REFERENCES fstep_users (uid),
  status     fstep_systematic_processing_status,
  parent_job BIGINT NOT NULL REFERENCES fstep_jobs (id) ON DELETE CASCADE,
  last_updated TIMESTAMP WITHOUT TIME ZONE,
  search_parameters TEXT
  );
  
CREATE INDEX fstep_systematic_processing_owner_idx
  ON fstep_systematic_processings (owner);
  
 ALTER TABLE fstep_job_configs add column systematic_parameter character varying(255);
 ALTER TABLE fstep_job_configs drop constraint fstep_job_configs_owner_service_inputs_key;
 ALTER TABLE fstep_job_configs add constraint fstep_job_configs_unique_key UNIQUE (owner, service, inputs, parent, systematic_parameter);
 
