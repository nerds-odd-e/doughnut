-- Establish complete deletion ownership for note-dependent rows so a hard-deleted
-- note leaves no orphaned dependent data the product owns. Three note/mcq-subject
-- foreign keys change to ON DELETE CASCADE; no other edges (other notes,
-- notebook containers, users, or inbound reference sources) are affected.
--
-- Each constraint is replaced under a new name: MySQL rejects re-adding a
-- constraint with the same name in the same ALTER statement that drops it.
--
-- conversation.note_id -> note: a conversation about a note is note-dependent
--   chat history; delete it (and its CASCADE conversation_message children) with
--   the note. Mirrors the existing conversation_message.conversation_id CASCADE.
-- image.note_id -> note: an image is uploaded per note (1:1, never shared) and
--   NoteService.deleteOrphanImagesForPersistedContent already treats unreferenced
--   images as note-owned cleanup; delete images with the note.
-- recall_prompt.mcq_id -> mcq: recall_prompt is also cascade-owned by
--   memory_tracker; a child may be CASCADE from both parents. CASCADE removes the
--   NO ACTION blocker that would otherwise prevent mcq deletion when a note is
--   hard-deleted (InnoDB child-table processing order is not guaranteed).

ALTER TABLE conversation
  DROP FOREIGN KEY conversation_ibfk_3,
  ADD CONSTRAINT fk_conversation_note FOREIGN KEY (note_id) REFERENCES note (id) ON DELETE CASCADE;

ALTER TABLE image
  DROP FOREIGN KEY fk_image_note_id,
  ADD CONSTRAINT fk_image_note FOREIGN KEY (note_id) REFERENCES note (id) ON DELETE CASCADE ON UPDATE RESTRICT;

ALTER TABLE recall_prompt
  DROP FOREIGN KEY fk_recall_prompt_mcq,
  ADD CONSTRAINT fk_recall_prompt_mcq_cascade FOREIGN KEY (mcq_id) REFERENCES mcq (id) ON DELETE CASCADE;
