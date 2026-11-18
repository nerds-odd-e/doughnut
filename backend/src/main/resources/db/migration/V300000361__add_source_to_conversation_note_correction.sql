-- The passage quoted from an outside source for a content correction, and the web address of its page; NULL when no source was found.
ALTER TABLE conversation_note_correction
  ADD COLUMN source_quote TEXT NULL,
  ADD COLUMN source_url TEXT NULL;
