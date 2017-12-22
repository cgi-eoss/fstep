 --User endpoints
 CREATE TABLE fstep_user_endpoints (
  id      BIGSERIAL PRIMARY KEY,
  owner   BIGINT    NOT NULL REFERENCES fstep_users (uid),
  name 	  CHARACTER VARYING(255)	    NOT NULL,
  url    CHARACTER VARYING(255)	NOT NULL	 
);
  
CREATE UNIQUE INDEX fstep_user_endpoint_name_owner_idx
  ON fstep_user_endpoints (name, owner);
CREATE INDEX fstep_user_endpoint_name_idx
  ON fstep_user_endpoints (name);
CREATE INDEX fstep_user_endpoint_owner_idx
  ON fstep_user_endpoints (owner);
