ALTER TABLE note ADD `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
update note set updated_at = created_at;