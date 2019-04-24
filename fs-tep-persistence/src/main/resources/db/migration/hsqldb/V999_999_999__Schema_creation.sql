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
  type             CHARACTER VARYING(255)      NOT NULL CHECK (type IN ('CREDIT', 'JOB', 'JOB_PROCESSING', 'DOWNLOAD', 'SUBSCRIPTION')),
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
  docker_build_info CLOB,
  required_resources CLOB,
  strip_proxy_path BOOLEAN DEFAULT TRUE,
  status         CHARACTER VARYING(255) NOT NULL CHECK (status IN ('IN_DEVELOPMENT', 'AVAILABLE')),
  type           CHARACTER VARYING(255) NOT NULL CHECK (type IN ('PROCESSOR', 'BULK_PROCESSOR', 'APPLICATION', 'PARALLEL_PROCESSOR')),
  owner          BIGINT                 NOT NULL FOREIGN KEY REFERENCES fstep_users (uid)
);
CREATE UNIQUE INDEX fstep_services_name_idx
  ON fstep_services (name);
CREATE INDEX fstep_services_owner_idx
  ON fstep_services (owner);
  
  
CREATE TABLE fstep_service_templates (
  id             BIGINT IDENTITY PRIMARY KEY,
  description    CHARACTER VARYING(255),
  name           CHARACTER VARYING(255) NOT NULL,
  wps_descriptor CLOB,
  required_resources CLOB,
  type           CHARACTER VARYING(255) NOT NULL CHECK (type IN ('PROCESSOR', 'BULK_PROCESSOR', 'APPLICATION', 'PARALLEL_PROCESSOR')),
  owner          BIGINT                 NOT NULL FOREIGN KEY REFERENCES fstep_users (uid)
);

CREATE UNIQUE INDEX fstep_service_templates_name_idx
  ON fstep_service_templates (name);
CREATE INDEX fstep_service_templates_owner_idx
  ON fstep_service_templates (owner);

  CREATE TABLE fstep_default_service_templates (
  id         BIGINT IDENTITY PRIMARY KEY,
  service_template    BIGINT  NOT NULL FOREIGN KEY REFERENCES fstep_service_templates (id),
  type           CHARACTER VARYING(255) NOT NULL CHECK (type IN ('PROCESSOR', 'BULK_PROCESSOR', 'APPLICATION', 'PARALLEL_PROCESSOR')) 
);  

CREATE UNIQUE INDEX fstep_default_service_templates_type_idx
  ON fstep_default_service_templates (type);
  
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
  parent  BIGINT,
  owner   BIGINT NOT NULL FOREIGN KEY REFERENCES fstep_users (uid),
  service BIGINT NOT NULL FOREIGN KEY REFERENCES fstep_services (id),
  systematic_parameter CHARACTER VARYING(255), 
  label   CHARACTER VARYING(255)
  -- WARNING: No unique index on owner-service-inputs-parent-systematic_parameter as hsqldb 2.3.4 cannot index CLOB columns
  -- UNIQUE (owner, service, inputs, parent, systematic_parameter)
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
  gui_endpoint    CHARACTER VARYING(255),
  is_parent  BOOLEAN DEFAULT FALSE,
  outputs    CLOB,
  stage      CHARACTER VARYING(255),
  start_time TIMESTAMP WITHOUT TIME ZONE,
  status     CHARACTER VARYING(255) NOT NULL CHECK (status IN
                                                    ('CREATED', 'RUNNING', 'COMPLETED', 'ERROR', 'CANCELLED', 'PENDING', 'WAITING')),
  job_config BIGINT                 NOT NULL FOREIGN KEY REFERENCES fstep_job_configs (id),
  owner      BIGINT                 NOT NULL FOREIGN KEY REFERENCES fstep_users (uid),
  parent_job_id BIGINT REFERENCES fstep_jobs (id),
  queue_position INTEGER,
  cost_quotation CLOB,
  worker_id CHARACTER VARYING(255)
);
CREATE UNIQUE INDEX fstep_jobs_ext_id_idx
  ON fstep_jobs (ext_id);
CREATE INDEX fstep_jobs_job_config_idx
  ON fstep_jobs (job_config);
CREATE INDEX fstep_jobs_owner_idx
  ON fstep_jobs (owner);
  
--Reference to parent in job config
ALTER TABLE fstep_job_configs ADD FOREIGN KEY (parent) REFERENCES fstep_jobs(id);
  

CREATE TABLE fstep_job_processings (
  id         BIGINT IDENTITY PRIMARY KEY,
  job		 BIGINT NOT NULL FOREIGN KEY REFERENCES fstep_jobs(id),
  sequence_num BIGINT NOT NULL,
  start_processing_time   TIMESTAMP WITH TIME ZONE,
  end_processing_time   TIMESTAMP WITH TIME ZONE,
  last_heartbeat 		TIMESTAMP WITH TIME ZONE
);
CREATE UNIQUE INDEX fstep_job_processings_job_sequence_num_time_idx
  ON fstep_job_processings (job, sequence_num);
CREATE INDEX fstep_job_processings_job_idx
  ON fstep_job_processings (job);

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

 --User collections
 CREATE TABLE fstep_collections (
  id      BIGINT GENERATED BY DEFAULT AS IDENTITY (
  START WITH 100 ) PRIMARY KEY,
  owner   BIGINT    NOT NULL REFERENCES fstep_users (uid),
  name 	  CHARACTER VARYING(255)	    NOT NULL,
  identifier CHARACTER VARYING(255)	NOT NULL,
  description CLOB, 
  products_type    CHARACTER VARYING(255)				
);
  
CREATE UNIQUE INDEX fstep_collections_name_owner_idx
  ON fstep_collections (name, owner);
CREATE UNIQUE INDEX fstep_collections_identifier_idx
  ON fstep_collections (identifier);
CREATE INDEX fstep_collections_name_idx
  ON fstep_collections (name);
CREATE INDEX fstep_collections_owner_idx
  ON fstep_collections (owner); 

-- FstepFile and Databasket tables

CREATE TABLE fstep_files (
  id         BIGINT IDENTITY PRIMARY KEY,
  uri        CHARACTER VARYING(255) NOT NULL,
  resto_id   BINARY(255)            NOT NULL,
  collection_id BIGINT REFERENCES fstep_collections (id),
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

CREATE TABLE fstep_files_relations (
  id         BIGINT IDENTITY PRIMARY KEY,
  source_file      BIGINT FOREIGN KEY REFERENCES fstep_files (id) ON DELETE CASCADE,
  target_file      BIGINT FOREIGN KEY REFERENCES fstep_files (id) ON DELETE CASCADE,
  type       CHARACTER VARYING(255) CHECK (type IN ('VISUALIZATION_OF'))
);
CREATE INDEX fstep_files_relations_source_idx
  ON fstep_files_relations (source_file);
CREATE INDEX fstep_files_relations_target_idx
  ON fstep_files_relations (target_file);
CREATE UNIQUE INDEX fstep_files_source_target_type_idx
  ON fstep_files_relations (source_file,target_file,type);


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

CREATE TABLE fstep_geoserver_layers (
  id          BIGINT IDENTITY PRIMARY KEY,
  workspace        CHARACTER VARYING(255) NOT NULL,
  layer CHARACTER VARYING(255) NOT NULL,
  store CHARACTER VARYING(255),
  store_type  CHARACTER VARYING(255) NOT NULL CHECK (store_type IN('MOSAIC', 'GEOTIFF', 'POSTGIS')),
  owner       BIGINT NOT NULL FOREIGN KEY REFERENCES fstep_users (uid)
);

CREATE INDEX fstep_geoserver_layers_owner_idx
  ON fstep_geoserver_layers (owner);
CREATE UNIQUE INDEX fstep_geoserver_layers_workspace_datastore_layer_idx
  ON fstep_geoserver_layers (workspace, layer);

CREATE TABLE fstep_geoserver_layer_files (
  geoserver_layer_id BIGINT FOREIGN KEY REFERENCES fstep_geoserver_layers (id),
  file_id       BIGINT FOREIGN KEY REFERENCES fstep_files (id)
);

CREATE UNIQUE INDEX fstep_geoserver_layer_files_layer_file_idx
  ON fstep_geoserver_layer_files (geoserver_layer_id, file_id);



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

-- FstepServiceContextFile table

CREATE TABLE fstep_service_template_files (
  id         BIGINT IDENTITY PRIMARY KEY,
  service_template    BIGINT  NOT NULL FOREIGN KEY REFERENCES fstep_service_templates (id),
  filename   CHARACTER VARYING(255),
  executable BOOLEAN DEFAULT FALSE NOT NULL,
  content    CLOB
);
CREATE UNIQUE INDEX fstep_service_template_files_filename_service_idx
  ON fstep_service_template_files (filename, service_template);
CREATE INDEX fstep_service_template_files_filename_idx
  ON fstep_service_template_files (filename);
CREATE INDEX fstep_service_template_files_service_template_idx
  ON fstep_service_template_files (service_template);  
  
-- Cost expressions

CREATE TABLE fstep_costing_expressions (
  id                        BIGINT IDENTITY PRIMARY KEY,
  type                      CHARACTER VARYING(255) NOT NULL CHECK (type IN ('SERVICE', 'DOWNLOAD', 'COLLECTION')),
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
                                                       ('DATABASKET', 'DATASOURCE', 'FILE', 'SERVICE', 'SERVICE_TEMPLATE', 'GROUP', 'JOB_CONFIG', 'PROJECT', 'COLLECTION')),
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

 --User mounts
 CREATE TABLE fstep_user_mounts (
  id      BIGINT GENERATED BY DEFAULT AS IDENTITY (
  START WITH 100 ) PRIMARY KEY,
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

 --Mounts in services

 CREATE TABLE fstep_services_mounts (
  fstep_service_id BIGINT NOT NULL FOREIGN KEY REFERENCES fstep_services (id),
  user_mount_id BIGINT NOT NULL FOREIGN KEY REFERENCES fstep_user_mounts (id),
  target_mount_path    CHARACTER VARYING(255)	NOT NULL	 
);

CREATE UNIQUE INDEX fstep_services_mounts_service_mount_idx
  ON fstep_services_mounts (fstep_service_id, user_mount_id);
    
 --User endpoints
 CREATE TABLE fstep_user_endpoints (
  id      BIGINT GENERATED BY DEFAULT AS IDENTITY (
  START WITH 100 ) PRIMARY KEY,
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

 --Systematic processings
 
 CREATE TABLE fstep_systematic_processings (
  id      BIGINT GENERATED BY DEFAULT AS IDENTITY (
  START WITH 100 ) PRIMARY KEY,
  owner   BIGINT    NOT NULL REFERENCES fstep_users (uid),
  status  CHARACTER VARYING(255) NOT NULL CHECK (status IN
                                                    ('ACTIVE', 'BLOCKED', 'COMPLETED')),
  parent_job BIGINT NOT NULL REFERENCES fstep_jobs (id),
  last_updated TIMESTAMP WITHOUT TIME ZONE,
  search_parameters CLOB
  );  
  
  
 --API keys
 
 CREATE TABLE fstep_api_keys (
  id      BIGINT GENERATED BY DEFAULT AS IDENTITY (
  START WITH 100 ) PRIMARY KEY,
  owner   BIGINT    NOT NULL REFERENCES fstep_users (uid),
  api_key  CHARACTER VARYING(255)
  );  
  
CREATE UNIQUE INDEX fstep_api_keys_owner_idx
  ON fstep_api_keys (owner);
  
 --User quota
 
 CREATE TABLE fstep_quota (
  id      BIGINT GENERATED BY DEFAULT AS IDENTITY (
  START WITH 100 ) PRIMARY KEY,
  owner   BIGINT    NOT NULL REFERENCES fstep_users (uid),
  usage_type  CHARACTER VARYING(255) NOT NULL, 
  value BIGINT NOT NULL
  );  

CREATE INDEX fstep_quota_owner_idx
  ON fstep_quota (owner); 
CREATE UNIQUE INDEX fstep_quota_type_owner_idx
  ON fstep_quota (usage_type, owner); 


CREATE TABLE fstep_files_cumulative_usage_records (
  id               BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  owner            BIGINT                      NOT NULL REFERENCES fstep_users (uid),
  cumulative_size  BIGINT                      NOT NULL,
  record_date      DATE					       NOT NULL,
  file_type        CHARACTER VARYING(255)      CHECK (file_type IN ('REFERENCE_DATA', 'OUTPUT_PRODUCT', 'EXTERNAL_PRODUCT'))
);

CREATE INDEX fstep_files_cumulative_usage_records_owner_idx
  ON fstep_files_cumulative_usage_records (owner);
  
--Persistent folders
 CREATE TABLE fstep_persistent_folders (
  id      BIGINT GENERATED BY DEFAULT AS IDENTITY (
  START WITH 100 ) PRIMARY KEY,
  owner   BIGINT    NOT NULL REFERENCES fstep_users (uid),
  name 	  CHARACTER VARYING(255)	    NOT NULL,
  locator CHARACTER VARYING(255)	    NOT NULL,
  type 	  CHARACTER VARYING(255)	    CHECK (type IN ('LOCAL_TO_WORKER')),
  status    CHARACTER VARYING(255)	    CHECK (status IN ('CREATED', 'ACTIVE'))	 
);

CREATE UNIQUE INDEX fstep_persistent_folders_name_owner_idx
  ON fstep_persistent_folders (name, owner);
CREATE INDEX fstep_persistent_folders_name_idx
  ON fstep_persistent_folders (name);
CREATE INDEX fstep_persistent_folders_owner_idx
  ON fstep_persistent_folders (owner);
 

CREATE TABLE fstep_subscription_plans (
  id             BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  name           CHARACTER VARYING(255) NOT NULL,
  description    CHARACTER VARYING(255),
  usage_type  CHARACTER VARYING(255) NOT NULL, 
  unit		 BIGINT NOT NULL,
  min_quantity INTEGER NOT NULL,
  max_quantity INTEGER NOT NULL,
  billing_scheme  CHARACTER VARYING(255) NOT NULL, 
  cost_quotation CLOB NOT NULL
); 

CREATE UNIQUE INDEX fstep_subscription_plans_name_idx
  ON fstep_subscription_plans (name);

CREATE TABLE fstep_subscriptions (
  id               BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  owner            BIGINT              NOT NULL REFERENCES fstep_users (uid),
  subscription_plan  BIGINT         NOT NULL REFERENCES fstep_subscription_plans (id),
  quantity 		   INTEGER NOT NULL,
  created	       TIMESTAMP WITH TIME ZONE NOT NULL,
  ended   	       TIMESTAMP WITH TIME ZONE,
  current_start    TIMESTAMP WITH TIME ZONE,
  current_end      TIMESTAMP WITH TIME ZONE,
  status		   CHARACTER VARYING(255) NOT NULL,
  quota			   BIGINT REFERENCES fstep_quota (id),
  renew	           BOOLEAN NOT NULL,
  downgrade_plan   BIGINT REFERENCES fstep_subscription_plans (id),
  downgrade_quantity  INTEGER
  	
);

CREATE INDEX fstep_subscriptions_owner_idx
  ON fstep_subscriptions (owner);

CREATE INDEX fstep_subscriptions_subscription_plan_idx
ON fstep_subscriptions (subscription_plan);

CREATE UNIQUE INDEX fstep_subscriptions_subscription_plan_owner_created_idx
ON fstep_subscriptions (subscription_plan, owner, created);

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
                                                              
-- Quartz schema creation

DROP TABLE qrtz_locks IF EXISTS;
DROP TABLE qrtz_scheduler_state IF EXISTS;
DROP TABLE qrtz_fired_triggers IF EXISTS;
DROP TABLE qrtz_paused_trigger_grps IF EXISTS;
DROP TABLE qrtz_calendars IF EXISTS;
DROP TABLE qrtz_blob_triggers IF EXISTS;
DROP TABLE qrtz_cron_triggers IF EXISTS;
DROP TABLE qrtz_simple_triggers IF EXISTS;
DROP TABLE qrtz_simprop_triggers IF EXISTS;
DROP TABLE qrtz_triggers IF EXISTS;
DROP TABLE qrtz_job_details IF EXISTS;

CREATE TABLE qrtz_job_details
(
SCHED_NAME VARCHAR(120) NOT NULL,
JOB_NAME VARCHAR(200) NOT NULL,
JOB_GROUP VARCHAR(200) NOT NULL,
DESCRIPTION VARCHAR(250) NULL,
JOB_CLASS_NAME VARCHAR(250) NOT NULL,
IS_DURABLE BOOLEAN NOT NULL,
IS_NONCONCURRENT BOOLEAN NOT NULL,
IS_UPDATE_DATA BOOLEAN NOT NULL,
REQUESTS_RECOVERY BOOLEAN NOT NULL,
JOB_DATA BLOB NULL,
PRIMARY KEY (SCHED_NAME,JOB_NAME,JOB_GROUP)
);

CREATE TABLE qrtz_triggers
(
SCHED_NAME VARCHAR(120) NOT NULL,
TRIGGER_NAME VARCHAR(200) NOT NULL,
TRIGGER_GROUP VARCHAR(200) NOT NULL,
JOB_NAME VARCHAR(200) NOT NULL,
JOB_GROUP VARCHAR(200) NOT NULL,
DESCRIPTION VARCHAR(250) NULL,
NEXT_FIRE_TIME NUMERIC(13) NULL,
PREV_FIRE_TIME NUMERIC(13) NULL,
PRIORITY INTEGER NULL,
TRIGGER_STATE VARCHAR(16) NOT NULL,
TRIGGER_TYPE VARCHAR(8) NOT NULL,
START_TIME NUMERIC(13) NOT NULL,
END_TIME NUMERIC(13) NULL,
CALENDAR_NAME VARCHAR(200) NULL,
MISFIRE_INSTR NUMERIC(2) NULL,
JOB_DATA BLOB NULL,
PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
FOREIGN KEY (SCHED_NAME,JOB_NAME,JOB_GROUP)
REFERENCES QRTZ_JOB_DETAILS(SCHED_NAME,JOB_NAME,JOB_GROUP)
);

CREATE TABLE qrtz_simple_triggers
(
SCHED_NAME VARCHAR(120) NOT NULL,
TRIGGER_NAME VARCHAR(200) NOT NULL,
TRIGGER_GROUP VARCHAR(200) NOT NULL,
REPEAT_COUNT NUMERIC(7) NOT NULL,
REPEAT_INTERVAL NUMERIC(12) NOT NULL,
TIMES_TRIGGERED NUMERIC(10) NOT NULL,
PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE qrtz_cron_triggers
(
SCHED_NAME VARCHAR(120) NOT NULL,
TRIGGER_NAME VARCHAR(200) NOT NULL,
TRIGGER_GROUP VARCHAR(200) NOT NULL,
CRON_EXPRESSION VARCHAR(120) NOT NULL,
TIME_ZONE_ID VARCHAR(80),
PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE qrtz_simprop_triggers
  (          
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    STR_PROP_1 VARCHAR(512) NULL,
    STR_PROP_2 VARCHAR(512) NULL,
    STR_PROP_3 VARCHAR(512) NULL,
    INT_PROP_1 NUMERIC(9) NULL,
    INT_PROP_2 NUMERIC(9) NULL,
    LONG_PROP_1 NUMERIC(13) NULL,
    LONG_PROP_2 NUMERIC(13) NULL,
    DEC_PROP_1 NUMERIC(13,4) NULL,
    DEC_PROP_2 NUMERIC(13,4) NULL,
    BOOL_PROP_1 BOOLEAN NULL,
    BOOL_PROP_2 BOOLEAN NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) 
    REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE qrtz_blob_triggers
(
SCHED_NAME VARCHAR(120) NOT NULL,
TRIGGER_NAME VARCHAR(200) NOT NULL,
TRIGGER_GROUP VARCHAR(200) NOT NULL,
BLOB_DATA BLOB NULL,
PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE qrtz_calendars
(
SCHED_NAME VARCHAR(120) NOT NULL,
CALENDAR_NAME VARCHAR(200) NOT NULL,
CALENDAR BLOB NOT NULL,
PRIMARY KEY (SCHED_NAME,CALENDAR_NAME)
);

CREATE TABLE qrtz_paused_trigger_grps
(
SCHED_NAME VARCHAR(120) NOT NULL,
TRIGGER_GROUP VARCHAR(200) NOT NULL,
PRIMARY KEY (SCHED_NAME,TRIGGER_GROUP)
);

CREATE TABLE qrtz_fired_triggers
(
SCHED_NAME VARCHAR(120) NOT NULL,
ENTRY_ID VARCHAR(95) NOT NULL,
TRIGGER_NAME VARCHAR(200) NOT NULL,
TRIGGER_GROUP VARCHAR(200) NOT NULL,
INSTANCE_NAME VARCHAR(200) NOT NULL,
FIRED_TIME NUMERIC(13) NOT NULL,
SCHED_TIME NUMERIC(13) NOT NULL,
PRIORITY INTEGER NOT NULL,
STATE VARCHAR(16) NOT NULL,
JOB_NAME VARCHAR(200) NULL,
JOB_GROUP VARCHAR(200) NULL,
IS_NONCONCURRENT BOOLEAN NULL,
REQUESTS_RECOVERY BOOLEAN NULL,
PRIMARY KEY (SCHED_NAME,ENTRY_ID)
);

CREATE TABLE qrtz_scheduler_state
(
SCHED_NAME VARCHAR(120) NOT NULL,
INSTANCE_NAME VARCHAR(200) NOT NULL,
LAST_CHECKIN_TIME NUMERIC(13) NOT NULL,
CHECKIN_INTERVAL NUMERIC(13) NOT NULL,
PRIMARY KEY (SCHED_NAME,INSTANCE_NAME)
);

CREATE TABLE qrtz_locks
(
SCHED_NAME VARCHAR(120) NOT NULL,
LOCK_NAME VARCHAR(40) NOT NULL,
PRIMARY KEY (SCHED_NAME,LOCK_NAME)
);
