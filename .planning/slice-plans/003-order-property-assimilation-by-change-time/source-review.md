# Source review: SEED-101#story-1

Launch: fresh general-purpose subagent on 2026-11-04, no conversation inheritance, told to use only the supplied input, read no project files and change nothing. Input: the verbatim Goal, Scope (required behavior, required restriction, scope limits, known consequences) and Key examples of [SEED-101#story-1](../../seeds/SEED-101-property-assimilation-order.md#story-1) at d04576f870; no plan, slice or proof. (A first launch carried a placeholder instead of the text and was stopped before returning.) The returned commitments are listed as returned.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| 1 | A learner (owner or subscriber) walking the assimilation sequence meets untracked property values in the order those values were last added or changed, within the same note level, not in the order their notes were created. | slice 8 | |
| 1.1 | A property added today to an old note is offered after the older material at that level. | slice 5 | |
| 1.2 | Intake follows the learner's own authoring timeline. | slice 8 | |
| 1.3 | Additions to old notes stop jumping to the front. | slice 5 | |
| 2 | Value date: each indexed property value carries the committer time of the last accepted Git commit that changed its parsed value (the commit that added it or changed a scalar's content). | slice 4 | |
| 2.1 | This applies to a scalar per key. | slice 3 | |
| 2.2 | This applies to each list item individually. | slice 3 | |
| 2.3 | A list item whose text changes is a new value, dated at that change. | slice 3 | |
| 2.4 | Body edits keep existing dates. | slice 5 | |
| 2.5 | Frontmatter reformatting keeps existing dates. | slice 3 | |
| 2.6 | List reordering keeps existing dates. | slice 5 | |
| 2.7 | Edits to other properties of the note keep existing dates. | slice 5 | |
| 2.8 | Values are compared as parsed values, never as text lines. | slice 3 | |
| 3 | Floor: a value's date is never earlier than its note's creation time. | slice 6 | |
| 3.1 | Rationale: a note pushed from the CLI (created at publish time, committed earlier) must not show its properties before the note itself or before notes created between commit and push. | slice 6 | |
| 4 | Order: within a level, units sort by time. | slice 2 | |
| 4.1 | A note unit sorts by its creation time. | slice 2 | |
| 4.2 | A property unit sorts by its value date. | slice 2 | |
| 4.3 | Ties break first by note. | slice 2 | |
| 4.4 | Then the note's own unit comes before its property units. | slice 2 | |
| 4.5 | Then by property key. | slice 2 | |
| 4.6 | Then by list position. | slice 2 | |
| 4.7 | Owned and subscribed streams follow the same order. | slice 2 | |
| 5 | Web save: saving a note dates the values that save added or changed at the save time. | slice 5 | |
| 5.1 | The save time is the committer time of the commit the save appends. | slice 5 | |
| 5.2 | Only the saved note's rows are touched. | slice 5 | |
| 6 | Git publication: for each note a push changes, the newly accepted range is walked commit by commit. | slice 6 | |
| 6.1 | An edit followed by a revert inside one push dates the value at the revert commit. | slice 6 | |
| 6.2 | Each commit's own committer time dates the values it changed. | slice 6 | |
| 6.3 | Values unchanged across the range keep their dates. | slice 6 | |
| 6.4 | Notes the push does not change are not touched. | slice 6 | |
| 7 | Missing dates: a value without a date sorts at its note's creation time. | slice 2 | |
| 7.1 | Donut fills missing dates from retained Git history on its own, in a maintenance job. | slice 8 | |
| 7.1.1 | The job uses existing scheduling infrastructure. | slice 8 | |
| 7.1.2 | It handles one notebook at a time. | slice 7 | |
| 7.1.3 | It is bounded per run. | slice 8 | |
| 7.1.4 | No learner or save waits for it. | slice 8 | |
| 7.2 | A value already present in the oldest retained commit ("Create notebook" or "Reset" root) is dated at its note's creation time. | slice 7 | |
| 7.3 | A finished rebuild leaves nothing undated. | slice 7 | |
| 7.4 | A history reset does not change cached dates. | slice 7 | |
| 8 | Cache: one optional timestamp per `note_property_index` row. | slice 1 | observed: NotePropertyIndex.changedAt (nullable) + V300000356; NotePropertyIndexServiceTest changeDate: date kept on body edit and reorder, null for a new value |
| 8.1 | It is ordering data only. | slice 2 | |
| 8.2 | There is no event log. | slice 8 | |
| 8.3 | There is no Git content change. | slice 8 | |
| 8.4 | There is no second source of truth. | slice 8 | |
| 8.5 | Trackers and counting are unaffected. | slice 2 | |
| 8.6 | A tracked value never re-enters the sequence, whatever its date. | slice 2 | |
| 9.1 | exclusion: note-level units keep creation-time order. | slice 2 | |
| 9.2 | exclusion: no date is shown in the UI. | slice 8 | |
| 9.3 | exclusion: author time is not used instead of committer time. | slice 4 | |
| 9.4 | exclusion: no clock normalization for CLI commits; a future- or back-dated local commit sorts where its committer time says. | slice 6 | |
| 9.5 | exclusion: values whose history was reset before this story lands are not re-dated; they stay at note creation. | slice 7 | |
| 10.1 | An untracked scalar edited for a typo moves to its edit time. | slice 5 | |
| 10.2 | A wiki-link rewrite that changes a value's text (destination renamed) dates that value at the rewrite commit. | slice 5 | |
| E1.1 | A created 2024-03 with a note-level tracker; B created 2025-06 untracked; today A gets `example of: "[[X]]"` on the web → the next unit is B's note unit. | slice 5 | |
| E1.2 | Same setup → A's new property comes after B's note unit. | slice 5 | |
| E2.1 | A has untracked `topic: physics` dated 2024-05; A's body is edited today → `topic` keeps 2024-05. | slice 5 | |
| E2.2 | Same setup → `topic` is still offered before B. | slice 5 | |
| E3.1 | `tags: [alpha, beta]` dated 2024-05 and 2024-09; reordered to `[beta, alpha]` and `gamma` added → alpha and beta keep their dates. | slice 5 | |
| E3.2 | Same setup → gamma is dated now. | slice 5 | |
| E4.1 | Untracked `definition: foo` dated 2024-05 edited to `definition: bar` today → dated today. | slice 5 | |
| E4.2 | The same edit on a tracked `definition` → nothing is re-queued. | slice 2 | |
| E5.1 | CLI push of three commits: c1 (09:00) adds `topic` to N1, c2 edits N2's body, c3 (10:00) adds `topic` to N3 → N1's topic dated 09:00. | slice 6 | |
| E5.2 | Same push → N3's topic dated 10:00. | slice 6 | |
| E5.3 | Same push → N2's values unchanged. | slice 6 | |
| E5.4 | A push that removes `alpha` in c1 and adds it back in c3 → alpha dated at c3. | slice 6 | |
| E6.1 | A new note committed locally at 08:00 and published at 09:30 → its values dated 09:30 (the floor). | slice 6 | |
| E6.2 | Same setup → the note's own unit precedes its property units. | slice 2 | |
| E7.1 | A notebook from before this story, every value undated → the sequence is ordered as today. | slice 2 | |
| E7.2 | After the maintenance job runs → values added by later commits take those commit times. | slice 7 | |
| E7.3 | After the job runs → values present in the root commit stay at note creation. | slice 7 | |
| E7.4 | After the job runs → the next run finds nothing to do. | slice 7 | |
| E7.5 | A notebook whose history was reset → treated the same way from its reset root. | slice 7 | |
| E8.1 | A subscriber's sequence over a subscribed notebook → follows the same order. | slice 2 | |
