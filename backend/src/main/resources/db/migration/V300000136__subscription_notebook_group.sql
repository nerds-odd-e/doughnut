ALTER TABLE subscription
  ADD COLUMN notebook_group_id INT UNSIGNED DEFAULT NULL,
  ADD CONSTRAINT fk_subscription_notebook_group
    FOREIGN KEY (notebook_group_id) REFERENCES notebook_group (id)
    ON DELETE SET NULL ON UPDATE RESTRICT;
