-- Add ignored_checklist_topics column to note table
-- The column stores a list of ignored checklist topic IDs as a string
ALTER TABLE note ADD COLUMN ignored_checklist_topics VARCHAR(500) DEFAULT NULL;

