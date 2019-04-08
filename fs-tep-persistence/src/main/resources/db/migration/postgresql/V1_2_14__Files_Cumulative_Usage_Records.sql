 --Files Cumulative Usage Records
CREATE TABLE fstep_files_cumulative_usage_records (
  id               BIGSERIAL           PRIMARY KEY,
  owner            BIGINT              NOT NULL REFERENCES fstep_users (uid),
  cumulative_size  BIGINT              NOT NULL,
  record_date      DATE			       NOT NULL,
  file_type        fstep_files_type
);

CREATE INDEX fstep_files_cumulative_usage_records_owner_idx
  ON fstep_files_cumulative_usage_records (owner);


