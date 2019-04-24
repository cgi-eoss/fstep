--Subscription downgrade information
ALTER TABLE fstep_subscriptions
ADD COLUMN downgrade_plan BIGINT REFERENCES fstep_subscription_plans (id);

ALTER TABLE fstep_subscriptions
ADD COLUMN downgrade_quantity INTEGER;