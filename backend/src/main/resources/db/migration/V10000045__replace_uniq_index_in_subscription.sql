ALTER TABLE subscription DROP FOREIGN KEY fk_subscription_user_id;

ALTER TABLE subscription DROP INDEX user_id;

ALTER TABLE subscription
 ADD CONSTRAINT `fk_subscription_user_id`
  FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT;

ALTER TABLE subscription
    ADD UNIQUE INDEX subscription_user_notebook (user_id, notebook_id);
