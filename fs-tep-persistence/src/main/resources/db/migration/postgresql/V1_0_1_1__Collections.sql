 --User collections
 CREATE TABLE fstep_collections (
  id      BIGSERIAL PRIMARY KEY,
  owner   BIGINT    NOT NULL REFERENCES fstep_users (uid),
  name 	  CHARACTER VARYING(255)	    NOT NULL,
  identifier CHARACTER VARYING(255) UNIQUE NOT NULL,
  description TEXT, 
  products_type    CHARACTER VARYING(255)				
);

-- Insert the relation
ALTER TABLE fstep_files
  ADD COLUMN collection_id BIGINT REFERENCES fstep_collections (id);
  
CREATE UNIQUE INDEX fstep_collections_name_owner_idx
  ON fstep_collections (name, owner);
  CREATE UNIQUE INDEX fstep_collections_identifier_idx
  ON fstep_collections (identifier);
CREATE INDEX fstep_collections_name_idx
  ON fstep_collections (name);
CREATE INDEX fstep_collections_owner_idx
  ON fstep_collections (owner);
