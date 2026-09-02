-- One-time authored_note_reference backfill has completed on production; live saves already
-- write authored_note_reference through Note.replaceContent, so this progress table is no longer
-- read or written by the application.
DROP TABLE IF EXISTS `authored_note_reference_backfill_progress`;
