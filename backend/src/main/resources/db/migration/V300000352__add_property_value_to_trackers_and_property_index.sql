-- A list property is tracked per value: a memory tracker and a property index row carry the
-- value text of one list item, or '' for a single (scalar) value, so existing rows keep working.
-- A tracker is unique per (user, note, type, property key, property value).
ALTER TABLE memory_tracker
  ADD COLUMN property_value varchar(255) NOT NULL DEFAULT '' AFTER property_key,
  DROP INDEX user_note_spelling_active,
  ADD UNIQUE KEY uq_memory_tracker_user_note_type_property (user_id, note_id, type, property_key, property_value);

ALTER TABLE note_property_index
  ADD COLUMN property_value varchar(255) NOT NULL DEFAULT '' AFTER item_index;
