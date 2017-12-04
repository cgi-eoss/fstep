 --User mounts
 CREATE TABLE fstep_user_mounts (
  id      BIGSERIAL PRIMARY KEY,
  owner   BIGINT    NOT NULL REFERENCES fstep_users (uid),
  name 	  CHARACTER VARYING(255)	    NOT NULL,
  type 	  CHARACTER VARYING(255)	    NOT NULL,
  mount_path    CHARACTER VARYING(255)	NOT NULL	 
);
  
CREATE UNIQUE INDEX fstep_user_mount_name_owner_idx
  ON fstep_user_mounts (name, owner);
CREATE INDEX fstep_user_mount_name_idx
  ON fstep_user_mounts (name);
CREATE INDEX fstep_user_mount_owner_idx
  ON fstep_user_mounts (owner);
  
 CREATE TABLE fstep_services_mounts (
  fstep_service_id BIGINT NOT NULL REFERENCES fstep_services (id),
  user_mount_id BIGINT NOT NULL REFERENCES fstep_user_mounts (id),
  target_mount_path    CHARACTER VARYING(255)	NOT NULL	 
);