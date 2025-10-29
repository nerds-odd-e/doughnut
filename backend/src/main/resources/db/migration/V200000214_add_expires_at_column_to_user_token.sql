-- Add mcp_notebook column to notebook table
ALTER TABLE user_token
  ADD COLUMN expires_at TIMESTAMP;
