ALTER TABLE `conversation`
  DROP FOREIGN KEY `conversation_ibfk_4`;

ALTER TABLE `conversation`
  ADD CONSTRAINT `fk_conversation_recall_prompt` FOREIGN KEY (`recall_prompt_id`) REFERENCES `recall_prompt` (`id`) ON DELETE SET NULL;
