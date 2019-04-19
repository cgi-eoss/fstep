--Add relations between fstep files

CREATE TYPE fstep_files_relation_type AS ENUM ('VISUALIZATION_OF');

CREATE TABLE fstep_files_relations (
  id                  bigserial           primary key,
  source_file         bigint              NOT NULL REFERENCES fstep_files (id) ON DELETE CASCADE,
  target_file         bigint              NOT NULL REFERENCES fstep_files (id) ON DELETE CASCADE,
  type                fstep_files_relation_type NOT NULL
);

CREATE INDEX fstep_files_relations_source_idx
  ON fstep_files_relations (source_file);
CREATE INDEX fstep_files_relations_target_idx
  ON fstep_files_relations (target_file);
CREATE UNIQUE INDEX fstep_files_source_target_type_idx
  ON fstep_files_relations (source_file,target_file,type);

