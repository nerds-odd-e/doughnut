-- Add mcp_notebook column to notebook table
ALTER TABLE notebook
ADD COLUMN mcp_notebook TINYINT(1) DEFAULT 0;
