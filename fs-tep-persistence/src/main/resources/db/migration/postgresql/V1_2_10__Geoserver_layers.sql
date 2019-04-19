--Fstep geoserver layers

CREATE TYPE geoserver_store_type AS ENUM ('MOSAIC', 'GEOTIFF', 'POSTGIS');

CREATE TABLE fstep_geoserver_layers (
  id          bigserial           primary key,
  owner         BIGINT                      NOT NULL REFERENCES fstep_users (uid),
  workspace        CHARACTER VARYING(255) NOT NULL,
  layer CHARACTER VARYING(255) NOT NULL,
  store CHARACTER VARYING(255),
  store_type  geoserver_store_type
);

CREATE INDEX fstep_geoserver_layers_owner_idx
  ON fstep_geoserver_layers (owner); 
  
CREATE UNIQUE INDEX fstep_geoserver_layers_workspace_datastore_layer_idx
  ON fstep_geoserver_layers (workspace, layer);

CREATE TABLE fstep_geoserver_layer_files (
  geoserver_layer_id bigint NOT NULL REFERENCES fstep_geoserver_layers (id),
  file_id       bigint NOT NULL REFERENCES fstep_files (id),
 PRIMARY KEY (geoserver_layer_id, file_id)
);

