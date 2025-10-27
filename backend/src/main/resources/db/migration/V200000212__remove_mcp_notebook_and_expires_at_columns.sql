-- Remove mcp_notebook column from notebook table (undo V200000210)
ALTER TABLE notebook
DROP COLUMN mcp_notebook;

-- Remove expires_at column from user_token table (undo V200000211)
ALTER TABLE user_token
DROP COLUMN expires_at;

