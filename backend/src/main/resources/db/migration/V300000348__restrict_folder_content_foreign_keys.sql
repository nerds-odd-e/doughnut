ALTER TABLE `notebook_attachment`
  DROP FOREIGN KEY `fk_notebook_attachment_folder`;
ALTER TABLE `notebook_attachment`
  ADD CONSTRAINT `fk_notebook_attachment_folder` FOREIGN KEY (`folder_id`) REFERENCES `folder` (`id`) ON DELETE RESTRICT;

ALTER TABLE `folder`
  DROP FOREIGN KEY `fk_folder_parent`;
ALTER TABLE `folder`
  ADD CONSTRAINT `fk_folder_parent` FOREIGN KEY (`parent_folder_id`) REFERENCES `folder` (`id`) ON DELETE RESTRICT;
