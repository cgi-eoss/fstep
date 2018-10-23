 --Collections Publication Request
  
ALTER TABLE fstep_jobs ADD COLUMN gui_endpoint CHARACTER VARYING(255);

ALTER TABLE fstep_services ADD COLUMN strip_proxy_path BOOLEAN DEFAULT TRUE;

