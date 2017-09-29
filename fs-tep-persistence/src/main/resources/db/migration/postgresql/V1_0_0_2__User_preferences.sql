 --User preferences KV store
 CREATE TABLE fstep_user_preferences (
  id      BIGSERIAL PRIMARY KEY,
  owner   BIGINT    NOT NULL REFERENCES fstep_users (uid),
  name 	  CHARACTER VARYING(255)	    NOT NULL,
  type    CHARACTER VARYING(255)				,
  preference TEXT   CHECK (length(preference) <= 51200)  
);
  
CREATE UNIQUE INDEX fstep_user_preferences_name_owner_idx
  ON fstep_user_preferences (name, owner);
CREATE INDEX fstep_user_preferences_name_idx
  ON fstep_user_preferences (name);
CREATE INDEX fstep_user_preferences_owner_idx
  ON fstep_user_preferences (owner);
