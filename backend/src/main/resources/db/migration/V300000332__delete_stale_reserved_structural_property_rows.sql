-- One-time cleanup. Structural frontmatter keys (image, url, relationship
-- endpoints, ...) have been excluded from note_property_index at write time
-- since 2026-06-13, but rows indexed before then were only hidden by a
-- per-request filter in the assimilation queue. That filter is gone, so
-- remove the stale rows once. Nothing writes such rows any more.
DELETE FROM note_property_index
WHERE lower(replace(property_key, '_', '')) REGEXP
  '^(image|imagemask|wikidataid|url|titlepattern|questiongenerationinstruction|level|type|relation|source|target)( [0-9]+)?$';
