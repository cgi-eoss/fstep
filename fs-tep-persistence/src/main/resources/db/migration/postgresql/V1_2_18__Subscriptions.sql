 --Subscriptions

CREATE TABLE fstep_subscription_plans (
  id               BIGSERIAL           PRIMARY KEY,
  name           CHARACTER VARYING(255) NOT NULL,
  description    CHARACTER VARYING(255),
  usage_type  CHARACTER VARYING(255) NOT NULL, 
  unit		 BIGINT NOT NULL,
  min_quantity INTEGER NOT NULL,
  max_quantity INTEGER NOT NULL,
  billing_scheme  CHARACTER VARYING(255) NOT NULL, 
  cost_quotation TEXT
);

CREATE UNIQUE INDEX fstep_subscription_plans_name_idx
  ON fstep_subscription_plans (name);

CREATE TABLE fstep_subscriptions (
  id               BIGSERIAL           PRIMARY KEY,
  owner            BIGINT              NOT NULL REFERENCES fstep_users (uid),
  subscription_plan  BIGINT         NOT NULL REFERENCES fstep_subscription_plans (id),
  quantity INTEGER NOT NULL,
  created	       TIMESTAMP WITH TIME ZONE NOT NULL,
  ended   	       TIMESTAMP WITH TIME ZONE,
  current_start    TIMESTAMP WITH TIME ZONE,
  current_end      TIMESTAMP WITH TIME ZONE,
  status 		   CHARACTER VARYING(255) NOT NULL,
  quota			  BIGINT REFERENCES fstep_quota (id),
  renew           BOOLEAN NOT NULL
);

CREATE INDEX fstep_subscriptions_owner_idx
  ON fstep_subscriptions (owner);

CREATE INDEX fstep_subscriptions_subscription_plan_idx
ON fstep_subscriptions (subscription_plan);

CREATE UNIQUE INDEX fstep_subscriptions_subscription_plan_owner_created_idx
ON fstep_subscriptions (subscription_plan, owner, created);

