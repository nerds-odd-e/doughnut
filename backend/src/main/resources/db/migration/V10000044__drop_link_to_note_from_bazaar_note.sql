ALTER TABLE bazaar_notebook
DROP FOREIGN KEY fk_bazaar_note_note;
ALTER TABLE bazaar_notebook
DROP COLUMN note_id;

ALTER TABLE subscription
DROP FOREIGN KEY fk_subscription_note_id;
ALTER TABLE subscription
DROP COLUMN note_id;

