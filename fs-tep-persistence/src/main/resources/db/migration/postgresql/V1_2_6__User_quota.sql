 --User quota
  
CREATE TABLE fstep_quota (
  id      BIGSERIAL PRIMARY KEY,
  owner   BIGINT    NOT NULL REFERENCES fstep_users (uid),
  usage_type  CHARACTER VARYING(255) NOT NULL, 
  value BIGINT NOT NULL
);  

CREATE INDEX fstep_quota_owner_idx
  ON fstep_quota (owner);
  
CREATE UNIQUE INDEX fstep_quota_type_owner_idx
  ON fstep_quota (usage_type, owner); 
