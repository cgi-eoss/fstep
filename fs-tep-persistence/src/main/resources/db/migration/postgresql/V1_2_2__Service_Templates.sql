 --Service Templates
 
 CREATE TABLE fstep_service_templates (
  id             BIGSERIAL PRIMARY KEY,
  description    CHARACTER VARYING(255),
  name           CHARACTER VARYING(255) NOT NULL,
  wps_descriptor TEXT,
  required_resources TEXT,
  type           fstep_services_type NOT NULL,
  owner          BIGINT                 NOT NULL REFERENCES fstep_users (uid)
);

CREATE UNIQUE INDEX fstep_service_templates_name_idx
  ON fstep_service_templates (name);
CREATE INDEX fstep_service_templates_owner_idx
  ON fstep_service_templates (owner);

 
CREATE TABLE fstep_service_template_files (
  id         BIGSERIAL PRIMARY KEY,
  service_template    BIGINT  NOT NULL REFERENCES fstep_service_templates (id),
  filename   CHARACTER VARYING(255),
  executable BOOLEAN DEFAULT FALSE NOT NULL,
  content    TEXT
);
CREATE UNIQUE INDEX fstep_service_template_files_filename_service_idx
  ON fstep_service_template_files (filename, service_template);
CREATE INDEX fstep_service_template_files_filename_idx
  ON fstep_service_template_files (filename);
CREATE INDEX fstep_service_template_files_service_template_idx
  ON fstep_service_template_files (service_template);  
  
CREATE TABLE fstep_default_service_templates (
  id         BIGSERIAL PRIMARY KEY,
  service_template    BIGINT  NOT NULL REFERENCES fstep_service_templates (id),
  type           fstep_services_type NOT NULL  
);  

CREATE UNIQUE INDEX fstep_default_service_templates_type_idx
  ON fstep_default_service_templates (type);
