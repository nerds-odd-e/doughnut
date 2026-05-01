-- Note bodies can exceed MySQL TEXT (65535-byte) limit when adding YAML frontmatter
-- during legacy parent wiki migration or when storing large Markdown.
ALTER TABLE note MODIFY COLUMN details MEDIUMTEXT;
