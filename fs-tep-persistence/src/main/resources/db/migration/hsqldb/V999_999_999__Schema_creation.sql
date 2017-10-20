-- FS-TEP does not support schema migration when using HSQLDB
-- This 'migration' script is primarily to track the current DB schema for use in tests and test environments

-- Tables & Indexes

CREATE TABLE fstep_users (
  uid  BIGINT IDENTITY PRIMARY KEY,
  mail CHARACTER VARYING(255),
  name CHARACTER VARYING(255)                 NOT NULL,
  role CHARACTER VARYING(255) DEFAULT 'GUEST' NOT NULL CHECK (role IN
                                                              ('GUEST', 'USER', 'EXPERT_USER', 'CONTENT_AUTHORITY', 'ADMIN'))
);
CREATE UNIQUE INDEX fstep_users_name_idx
  ON fstep_users (name);

CREATE TABLE fstep_wallets (
  id      BIGINT IDENTITY PRIMARY KEY,
  owner   BIGINT        NOT NULL FOREIGN KEY REFERENCES fstep_users (uid),
  balance INT DEFAULT 0 NOT NULL
);
CREATE UNIQUE INDEX fstep_wallets_owner_idx
  ON fstep_wallets (owner);

CREATE TABLE fstep_wallet_transactions (
  id               BIGINT IDENTITY PRIMARY KEY,
  wallet           BIGINT                      NOT NULL FOREIGN KEY REFERENCES fstep_wallets (id),
  balance_change   INT                         NOT NULL,
  transaction_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  type             CHARACTER VARYING(255)      NOT NULL CHECK (type IN ('CREDIT', 'JOB', 'DOWNLOAD')),
  associated_id    BIGINT
);
CREATE INDEX fstep_wallet_transactions_wallet_idx
  ON fstep_wallet_transactions (wallet);

CREATE TABLE fstep_services (
  id             BIGINT IDENTITY PRIMARY KEY,
  description    CHARACTER VARYING(255),
  docker_tag     CHARACTER VARYING(255),
  licence        CHARACTER VARYING(255) NOT NULL CHECK (licence IN ('OPEN', 'RESTRICTED')),
  name           CHARACTER VARYING(255) NOT NULL,
  wps_descriptor CLOB,
  status         CHARACTER VARYING(255) NOT NULL CHECK (status IN ('IN_DEVELOPMENT', 'AVAILABLE')),
  type           CHARACTER VARYING(255) NOT NULL CHECK (type IN ('PROCESSOR', 'BULK_PROCESSOR', 'APPLICATION', 'PARALLEL_PROCESSOR')),
  owner          BIGINT                 NOT NULL FOREIGN KEY REFERENCES fstep_users (uid)
);
CREATE UNIQUE INDEX fstep_services_name_idx
  ON fstep_services (name);
CREATE INDEX fstep_services_owner_idx
  ON fstep_services (owner);

CREATE TABLE fstep_credentials (
  id               BIGINT IDENTITY PRIMARY KEY,
  certificate_path CHARACTER VARYING(255),
  host             CHARACTER VARYING(255) NOT NULL,
  password         CHARACTER VARYING(255),
  type             CHARACTER VARYING(255) NOT NULL CHECK (type IN ('BASIC', 'X509')),
  username         CHARACTER VARYING(255)
);
CREATE UNIQUE INDEX fstep_credentials_host_idx
  ON fstep_credentials (host);

CREATE TABLE fstep_groups (
  gid         BIGINT IDENTITY PRIMARY KEY,
  description CHARACTER VARYING(255),
  name        CHARACTER VARYING(255) NOT NULL,
  owner       BIGINT                 NOT NULL FOREIGN KEY REFERENCES fstep_users (uid)
);
CREATE INDEX fstep_groups_name_idx
  ON fstep_groups (name);
CREATE INDEX fstep_groups_owner_idx
  ON fstep_groups (owner);
CREATE UNIQUE INDEX fstep_groups_name_owner_idx
  ON fstep_groups (name, owner);

CREATE TABLE fstep_group_member (
  group_id BIGINT FOREIGN KEY REFERENCES fstep_groups (gid),
  user_id  BIGINT FOREIGN KEY REFERENCES fstep_users (uid)
);
CREATE UNIQUE INDEX fstep_group_member_user_group_idx
  ON fstep_group_member (group_id, user_id);

CREATE TABLE fstep_job_configs (
  id      BIGINT IDENTITY PRIMARY KEY,
  inputs  CLOB,
  owner   BIGINT NOT NULL FOREIGN KEY REFERENCES fstep_users (uid),
  service BIGINT NOT NULL FOREIGN KEY REFERENCES fstep_services (id),
  label   CHARACTER VARYING(255)
  -- WARNING: No unique index on owner-service-inputs as hsqldb 2.3.4 cannot index CLOB columns
  -- UNIQUE (owner, service, inputs)
);
CREATE INDEX fstep_job_configs_service_idx
  ON fstep_job_configs (service);
CREATE INDEX fstep_job_configs_owner_idx
  ON fstep_job_configs (owner);
CREATE INDEX fstep_job_configs_label_idx
  ON fstep_job_configs (label);

CREATE TABLE fstep_jobs (
  id         BIGINT IDENTITY PRIMARY KEY,
  end_time   TIMESTAMP WITHOUT TIME ZONE,
  ext_id     CHARACTER VARYING(255) NOT NULL,
  gui_url    CHARACTER VARYING(255),
  is_parent  BOOLEAN DEFAULT FALSE,
  outputs    CLOB,
  stage      CHARACTER VARYING(255),
  start_time TIMESTAMP WITHOUT TIME ZONE,
  status     CHARACTER VARYING(255) NOT NULL CHECK (status IN
                                                    ('CREATED', 'RUNNING', 'COMPLETED', 'ERROR', 'CANCELLED')),
  job_config BIGINT                 NOT NULL FOREIGN KEY REFERENCES fstep_job_configs (id),
  owner      BIGINT                 NOT NULL FOREIGN KEY REFERENCES fstep_users (uid),
  parent_job_id BIGINT REFERENCES fstep_jobs (id),
  worker_id CHARACTER VARYING(255)
);
CREATE UNIQUE INDEX fstep_jobs_ext_id_idx
  ON fstep_jobs (ext_id);
CREATE INDEX fstep_jobs_job_config_idx
  ON fstep_jobs (job_config);
CREATE INDEX fstep_jobs_owner_idx
  ON fstep_jobs (owner);

-- Data sources

CREATE TABLE fstep_data_sources (
  id     BIGINT IDENTITY PRIMARY KEY,
  name   CHARACTER VARYING(255) NOT NULL,
  owner  BIGINT                 NOT NULL FOREIGN KEY REFERENCES fstep_users (uid),
  policy CHARACTER VARYING(255) DEFAULT 'CACHE' NOT NULL CHECK (policy IN ('CACHE', 'MIRROR', 'REMOTE_ONLY'))
);
CREATE UNIQUE INDEX fstep_data_sources_name_idx
  ON fstep_data_sources (name);
CREATE INDEX fstep_data_sources_owner_idx
  ON fstep_data_sources (owner);

-- FstepFile and Databasket tables

CREATE TABLE fstep_files (
  id         BIGINT IDENTITY PRIMARY KEY,
  uri        CHARACTER VARYING(255) NOT NULL,
  resto_id   BINARY(255)            NOT NULL,
  type       CHARACTER VARYING(255) CHECK (type IN ('REFERENCE_DATA', 'OUTPUT_PRODUCT', 'EXTERNAL_PRODUCT')),
  owner      BIGINT FOREIGN KEY REFERENCES fstep_users (uid),
  filename   CHARACTER VARYING(255),
  filesize   BIGINT,
  datasource BIGINT FOREIGN KEY REFERENCES fstep_data_sources (id)
);
CREATE UNIQUE INDEX fstep_files_uri_idx
  ON fstep_files (uri);
CREATE UNIQUE INDEX fstep_files_resto_id_idx
  ON fstep_files (resto_id);
CREATE INDEX fstep_files_owner_idx
  ON fstep_files (owner);

CREATE TABLE fstep_databaskets (
  id          BIGINT IDENTITY PRIMARY KEY,
  name        CHARACTER VARYING(255) NOT NULL,
  description CHARACTER VARYING(255),
  owner       BIGINT FOREIGN KEY REFERENCES fstep_users (uid)
);
CREATE INDEX fstep_databaskets_name_idx
  ON fstep_databaskets (name);
CREATE INDEX fstep_databaskets_owner_idx
  ON fstep_databaskets (owner);
CREATE UNIQUE INDEX fstep_databaskets_name_owner_idx
  ON fstep_databaskets (name, owner);

CREATE TABLE fstep_databasket_files (
  databasket_id BIGINT FOREIGN KEY REFERENCES fstep_databaskets (id),
  file_id       BIGINT FOREIGN KEY REFERENCES fstep_files (id)
);
CREATE UNIQUE INDEX fstep_databasket_files_basket_file_idx
  ON fstep_databasket_files (databasket_id, file_id);

CREATE TABLE fstep_projects (
  id          BIGINT IDENTITY PRIMARY KEY,
  name        CHARACTER VARYING(255) NOT NULL,
  description CHARACTER VARYING(255),
  owner       BIGINT FOREIGN KEY REFERENCES fstep_users (uid)
);
CREATE INDEX fstep_projects_name_idx
  ON fstep_projects (name);
CREATE INDEX fstep_projects_owner_idx
  ON fstep_projects (owner);
CREATE UNIQUE INDEX fstep_projects_name_owner_idx
  ON fstep_projects (name, owner);

CREATE TABLE fstep_project_databaskets (
  project_id    BIGINT FOREIGN KEY REFERENCES fstep_projects (id),
  databasket_id BIGINT FOREIGN KEY REFERENCES fstep_databaskets (id)
);
CREATE UNIQUE INDEX fstep_project_databaskets_ids_idx
  ON fstep_project_databaskets (project_id, databasket_id);

CREATE TABLE fstep_project_services (
  project_id BIGINT FOREIGN KEY REFERENCES fstep_projects (id),
  service_id BIGINT FOREIGN KEY REFERENCES fstep_services (id)
);
CREATE UNIQUE INDEX fstep_project_services_ids_idx
  ON fstep_project_services (project_id, service_id);

CREATE TABLE fstep_project_job_configs (
  project_id    BIGINT FOREIGN KEY REFERENCES fstep_projects (id),
  job_config_id BIGINT FOREIGN KEY REFERENCES fstep_job_configs (id)
);
CREATE UNIQUE INDEX fstep_project_job_configs_ids_idx
  ON fstep_project_job_configs (project_id, job_config_id);

-- FstepServiceContextFile table

CREATE TABLE fstep_service_files (
  id         BIGINT IDENTITY PRIMARY KEY,
  service    BIGINT                NOT NULL FOREIGN KEY REFERENCES fstep_services (id),
  filename   CHARACTER VARYING(255),
  executable BOOLEAN DEFAULT FALSE NOT NULL,
  content    CLOB
);
CREATE UNIQUE INDEX fstep_service_files_filename_service_idx
  ON fstep_service_files (filename, service);
CREATE INDEX fstep_service_files_filename_idx
  ON fstep_service_files (filename);
CREATE INDEX fstep_service_files_service_idx
  ON fstep_service_files (service);

-- Cost expressions

CREATE TABLE fstep_costing_expressions (
  id                        BIGINT IDENTITY PRIMARY KEY,
  type                      CHARACTER VARYING(255) NOT NULL CHECK (type IN ('SERVICE', 'DOWNLOAD')),
  associated_id             BIGINT                 NOT NULL,
  cost_expression           CHARACTER VARYING(255) NOT NULL,
  estimated_cost_expression CHARACTER VARYING(255)
);
CREATE UNIQUE INDEX fstep_costing_expressions_type_associated_id_idx
  ON fstep_costing_expressions (type, associated_id);

-- Worker expressions

CREATE TABLE fstep_worker_locator_expressions (
  id         BIGINT IDENTITY PRIMARY KEY,
  service    BIGINT                 NOT NULL FOREIGN KEY REFERENCES fstep_services (id),
  expression CHARACTER VARYING(255) NOT NULL
);
CREATE UNIQUE INDEX fstep_worker_locator_expressions_service_idx
  ON fstep_worker_locator_expressions (service);

-- Publishing requests

CREATE TABLE fstep_publishing_requests (
  id            BIGINT IDENTITY PRIMARY KEY,
  owner         BIGINT                 NOT NULL FOREIGN KEY REFERENCES fstep_users (uid),
  request_time  TIMESTAMP WITHOUT TIME ZONE,
  updated_time  TIMESTAMP WITHOUT TIME ZONE,
  status        CHARACTER VARYING(255) NOT NULL CHECK (status IN
                                                       ('REQUESTED', 'GRANTED', 'NEEDS_INFO', 'REJECTED')),
  type          CHARACTER VARYING(255) NOT NULL CHECK (type IN
                                                       ('DATABASKET', 'DATASOURCE', 'FILE', 'SERVICE', 'GROUP', 'JOB_CONFIG', 'PROJECT')),
  associated_id BIGINT                 NOT NULL
);
CREATE INDEX fstep_publishing_requests_owner_idx
  ON fstep_publishing_requests (owner);
CREATE UNIQUE INDEX fstep_publishing_requests_owner_object_idx
  ON fstep_publishing_requests (owner, type, associated_id);

-- Job-output file relationships

CREATE TABLE fstep_job_output_files (
  job_id  BIGINT NOT NULL FOREIGN KEY REFERENCES fstep_jobs (id),
  file_id BIGINT NOT NULL FOREIGN KEY REFERENCES fstep_files (id)
);
CREATE UNIQUE INDEX fstep_job_output_files_job_file_idx
  ON fstep_job_output_files (job_id, file_id);

-- ACL schema from spring-security-acl

CREATE TABLE acl_sid (
  id        BIGINT GENERATED BY DEFAULT AS IDENTITY (
  START WITH 100 )                  NOT NULL PRIMARY KEY,
  principal BOOLEAN                 NOT NULL,
  sid       VARCHAR_IGNORECASE(100) NOT NULL,
  UNIQUE (sid, principal)
);

CREATE TABLE acl_class (
  id    BIGINT GENERATED BY DEFAULT AS IDENTITY (
  START WITH 100 )              NOT NULL PRIMARY KEY,
  class VARCHAR_IGNORECASE(100) NOT NULL,
  UNIQUE (class)
);

CREATE TABLE acl_object_identity (
  id                 BIGINT GENERATED BY DEFAULT AS IDENTITY (
  START WITH 100 )           NOT NULL PRIMARY KEY,
  object_id_class    BIGINT  NOT NULL FOREIGN KEY REFERENCES acl_class (id),
  object_id_identity BIGINT  NOT NULL,
  parent_object      BIGINT FOREIGN KEY REFERENCES acl_object_identity (id),
  owner_sid          BIGINT FOREIGN KEY REFERENCES acl_sid (id),
  entries_inheriting BOOLEAN NOT NULL,
  UNIQUE (object_id_class, object_id_identity)
);

CREATE TABLE acl_entry (
  id                  BIGINT GENERATED BY DEFAULT AS IDENTITY (
  START WITH 100 )            NOT NULL PRIMARY KEY,
  acl_object_identity BIGINT  NOT NULL FOREIGN KEY REFERENCES acl_object_identity (id),
  ace_order           INT     NOT NULL,
  sid                 BIGINT  NOT NULL FOREIGN KEY REFERENCES acl_sid (id),
  mask                INTEGER NOT NULL,
  granting            BOOLEAN NOT NULL,
  audit_success       BOOLEAN NOT NULL,
  audit_failure       BOOLEAN NOT NULL,
  UNIQUE (acl_object_identity, ace_order)
);

--User preferences KV store
 CREATE TABLE fstep_user_preferences (
  id      BIGINT GENERATED BY DEFAULT AS IDENTITY (
  START WITH 100 ) PRIMARY KEY,
  owner   BIGINT    NOT NULL REFERENCES fstep_users (uid),
  name 	  CHARACTER VARYING(255)	    NOT NULL,
  type    CHARACTER VARYING(255)				,
  preference CLOB   CHECK (length(preference) <= 51200)  
);
  
CREATE UNIQUE INDEX fstep_user_preferences_name_owner_idx
  ON fstep_user_preferences (name, owner);
CREATE INDEX fstep_user_preferences_name_idx
  ON fstep_user_preferences (name);
CREATE INDEX fstep_user_preferences_owner_idx
  ON fstep_user_preferences (owner);

-- Initial data

-- Fallback internal user
INSERT INTO fstep_users (name, mail) VALUES ('fstep', 'foodsecurity-tep@esa.int');

-- Default project
INSERT INTO fstep_projects (name, owner) VALUES ('Default Project', (SELECT uid
                                                                    FROM fstep_users
                                                                    WHERE name = 'fstep'));

-- FS-TEP datasource
INSERT INTO fstep_data_sources (name, owner) VALUES ('FS-TEP', (SELECT uid
                                                              FROM fstep_users
                                                              WHERE name = 'fstep'));
