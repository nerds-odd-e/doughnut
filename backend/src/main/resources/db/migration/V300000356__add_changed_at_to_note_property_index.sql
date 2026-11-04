-- The time a property value was last added or changed; NULL until dated.
ALTER TABLE note_property_index
  ADD COLUMN changed_at DATETIME NULL;
