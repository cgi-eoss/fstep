--Persistent folders

CREATE TYPE fstep_persistent_folders_type AS ENUM ('LOCAL_TO_WORKER');
CREATE TYPE fstep_persistent_folders_status AS ENUM ('CREATED', 'ACTIVE');

 CREATE TABLE fstep_persistent_folders (
  id      BIGSERIAL           PRIMARY KEY,
  owner   BIGINT    NOT NULL REFERENCES fstep_users (uid),
  name 	  CHARACTER VARYING(255)	    NOT NULL,
  locator CHARACTER VARYING(255)	    NOT NULL,
  type 	  fstep_persistent_folders_type,
  status  fstep_persistent_folders_status	 
);

CREATE UNIQUE INDEX fstep_persistent_folders_name_owner_idx
  ON fstep_persistent_folders (name, owner);
CREATE INDEX fstep_persistent_folders_name_idx
  ON fstep_persistent_folders (name);
CREATE INDEX fstep_persistent_folders_owner_idx
  ON fstep_persistent_folders (owner);
  



