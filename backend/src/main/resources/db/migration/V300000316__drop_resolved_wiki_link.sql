-- Retire persisted resolved-wiki-link cache; note references live-resolve from authored_note_reference.
DROP TABLE IF EXISTS `resolved_wiki_link`;
