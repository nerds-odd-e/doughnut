# Numbered-property migration retirement

The one-time conversion of numbered property keys to list properties completed,
as confirmed by the owner on 2026-10-02. Its startup runner and conversion-only
helpers have been retired. Ordinary startup still runs Flyway and does not scan,
consolidate or normalize authored numbered properties.

List properties continue to support one memory tracker per value through ordinary
editing and learning. Authored legacy keys remain recognizable. Existing
consolidated content, tracker identities, schedules and recall history stay intact;
retirement adds no content conversion and does not rewrite accepted Git history.

Ordinary content edits continue through the existing accepted-change boundary.
See [note content saving](note-content-saving.md) and
[ADR 0002](adrs/0002-git-native-portable-notebook-synchronization-accepted.md).
