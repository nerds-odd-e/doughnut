ALTER TABLE bazaar_notebook
    ADD CONSTRAINT `fk_bazaar_notebook_notebook_id`
        FOREIGN KEY (notebook_id) REFERENCES notebook (id)
        ON UPDATE RESTRICT;

ALTER TABLE subscription
    ADD CONSTRAINT `fk_subscription_notebook_id`
        FOREIGN KEY (notebook_id) REFERENCES notebook (id)
        ON UPDATE RESTRICT;

