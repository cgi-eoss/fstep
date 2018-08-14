 --API keys
 
 CREATE TABLE fstep_api_keys (
  id      BIGSERIAL PRIMARY KEY,
  owner   BIGINT    NOT NULL REFERENCES fstep_users (uid),
  api_key  CHARACTER VARYING(255)
  );  
  
CREATE UNIQUE INDEX fstep_api_keys_owner_idx 
ON fstep_api_keys (owner);
