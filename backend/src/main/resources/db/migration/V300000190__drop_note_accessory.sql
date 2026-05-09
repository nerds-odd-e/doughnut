-- note_accessory is redundant: image / image_mask live in note content frontmatter.
-- Assumes no remaining rows in production (team confirmed); resolve operationally if not.

DROP TABLE IF EXISTS `note_accessory`;
