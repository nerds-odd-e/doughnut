ALTER TABLE `notebook_attachment`
  ADD COLUMN `folder_id` int unsigned DEFAULT NULL AFTER `notebook_id`,
  DROP INDEX `uk_notebook_attachment_notebook_filename`,
  ADD UNIQUE KEY `uk_notebook_attachment_notebook_folder_filename` (`notebook_id`, (ifnull(`folder_id`, 0)), `filename`),
  ADD CONSTRAINT `fk_notebook_attachment_folder` FOREIGN KEY (`folder_id`) REFERENCES `folder` (`id`) ON DELETE CASCADE;
