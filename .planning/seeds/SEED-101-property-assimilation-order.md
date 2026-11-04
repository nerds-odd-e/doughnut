---
id: SEED-101
status: dormant
planted: 2026-11-04
planted_during: discussion of Git-derived property timestamps for assimilation
trigger_when: improving the order in which learners assimilate note properties
scope: medium
---

# SEED-101: Use property change times in assimilation

## Story Decomposition

<a id="story-1"></a>

### Order property assimilation by when values changed

**Identity:** SEED-101#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/003-order-property-assimilation-by-change-time/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"29792be645df8bb02f548b6ff92e4afab2cf2b9dbe37c903246fd6e9c97bdeba","plan":"fc375831059aea872f1e3f9cc1e070ac25c41510e36da1d1fcd30672486d1437"}}
```

#### Goal

A learner (notebook owner or subscriber) walking the assimilation sequence
meets untracked property values in the order those values were last added or
changed, within the same note level, instead of in the order their notes were
created. A property added today to an old note is offered after the older
material at that level. Intake then follows the learner's own authoring
timeline, and additions to old notes stop jumping to the front.

#### Scope

Required behavior:

- **Value date.** Each indexed property value — a scalar per key, and each
  list item individually — carries the committer time of the last accepted
  Git commit that changed its parsed value: the commit that added it, or that
  changed a scalar's content. A list item whose text changes is a new value
  dated at that change. Body edits, frontmatter reformatting, list reordering
  and edits to other properties of the note keep existing dates. Values are
  compared as parsed values, never as text lines.
- **Floor.** A value's date is never earlier than its note's creation time
  (required restriction, see below).
- **Order.** Within a level, units sort by time: a note unit by its creation
  time, a property unit by its value date; ties break by note, then the
  note's own unit before its property units, then property key, then list
  position. Owned and subscribed streams follow the same order.
- **Web save.** Saving a note dates the values that save added or changed at
  the save time, which is the committer time of the commit the save appends.
  Only the saved note's rows are touched.
- **Git publication.** For each note a push changes, the newly accepted range
  is walked commit by commit, so an edit followed by a revert inside one push
  dates the value at the revert commit, and each commit's own committer time
  dates the values it changed. Values unchanged across the range keep their
  dates. Notes the push does not change are not touched.
- **Missing dates.** A value without a date sorts at its note's creation
  time. Donut fills missing dates from retained Git history on its own, in a
  maintenance job (existing scheduling infrastructure, one notebook at a
  time, bounded per run), without a learner or a save waiting for it. A value
  already present in the oldest retained commit — the "Create notebook" root
  or a "Reset" root — has no derivable date and is dated at its note's
  creation time, so a finished rebuild leaves nothing undated. A history
  reset does not change cached dates.
- **Cache.** One optional timestamp per `note_property_index` row. It is
  ordering data only: no event log, no Git content change, no second source
  of truth. Trackers and counting are unaffected: a tracked value never
  re-enters the sequence whatever its date.

Required restriction: the floor. A value cannot have entered the learner's
notebook before its note did; without the floor, a note pushed from the CLI
(created at publish time, committed earlier) would show its properties before
the note itself and before notes created between commit and push.

Scope limits (deferred, not rejected): note-level units keep creation-time
order; no date shown in the UI; author time instead of committer time; clock
normalization for CLI commits (a future-dated or back-dated local commit sorts
where its committer time says); re-dating values whose history was reset
before this story lands (they stay at note creation, by the rule above).

Known consequences of the rule, not restrictions: an untracked scalar edited
for a typo moves to its edit time; a wiki-link rewrite that changes a value's
text (destination renamed) dates that value at the rewrite commit.

#### Key examples

1. Note A created 2024-03 with a note-level tracker; note B created 2025-06,
   untracked → today A gets `example of: "[[X]]"` on the web → the next unit
   is B's note unit; A's new property comes after it. (Today A's property
   comes first.)
2. A has untracked `topic: physics` dated 2024-05 → A's body is edited today →
   `topic` keeps 2024-05 and is still offered before B.
3. `tags: [alpha, beta]` dated 2024-05 and 2024-09 → reordered to
   `[beta, alpha]` and `gamma` added → alpha and beta keep their dates, gamma
   is dated now.
4. Untracked `definition: foo` dated 2024-05 → edited to `definition: bar`
   today → dated today. The same edit on a tracked `definition` re-queues
   nothing.
5. CLI push of three commits: c1 (09:00) adds `topic` to N1, c2 edits N2's
   body, c3 (10:00) adds `topic` to N3 → N1's topic is dated 09:00, N3's
   10:00, N2's values unchanged. A push that removes `alpha` in c1 and adds it
   back in c3 dates alpha at c3.
6. A new note committed locally at 08:00 and published at 09:30 → its values
   are dated 09:30 (the floor); its own unit precedes its property units.
7. A notebook from before this story: every value undated → the sequence is
   ordered as today → the maintenance job runs → values added by later
   commits take those commit times; values present in the root commit stay at
   note creation; the next run finds nothing to do. A notebook whose history
   was reset is treated the same from its reset root.
8. A subscriber's sequence over a subscribed notebook follows the same order.

#### UI

No new interaction or presentation: the learner uses the existing menu
walkthrough and `/next`, and only the order changes. The one interaction choice
examined — whether a note's own unit stays ahead of its property units when a
pushed note's commit precedes its publication — is settled by the floor above.

#### Architecture

- [ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md):
  the accepted commit is authoritative and derived state is committed with the
  accepted head. Value dates are derived ordering state written in the same
  publication transaction (web save or push). Walking the newly accepted range
  reads intermediate commits as evidence and writes the index once with the
  final projection; it replays no mutation, adds no event log, and changes no
  Git content ([synchronization architecture](../../docs/notebook-git-synchronization.md),
  "Domain operation ownership"). The identity walk over a publication range
  already uses this pattern.
- One dating mechanism: a walker that, for one note and one commit range,
  compares the parsed frontmatter of successive commits and dates each
  current value at the newest changing commit. The push uses it over the
  accepted range; the maintenance job uses it over the whole retained
  history; a web save is the one-step case (previous content vs new content).
- The index refresh shared by every save path (`refreshDerivedIndexesForNote`)
  changes from delete-and-reinsert to a keyed refresh that carries dates, and
  the JPQL ORDER BY and the in-memory `AssimilationUnit` comparator must
  change together, since the merge of owned and subscribed streams relies on
  them agreeing.
- Schema: one nullable timestamp column on `note_property_index` (db-migration
  skill). The maintenance job follows the existing prod-only `@Scheduled` plus
  ShedLock pattern and fails loudly through the existing scheduled-job error
  handler ([ADR 0006](../../docs/adrs/0006-failure-handling-accepted.md)); in
  other environments missing dates fall back to note creation.

#### Alternatives considered

- **Server stamp only** (date a value when the index first sees it added or
  changed; no Git reading, no rebuild): simplest, but leaves every value
  already in the learner's notebooks at its note's creation time forever,
  which is the material the story is about. Rejected for that reason.
- **Order property units by the note's `updated_at`**: no new column, but a
  body edit or any property edit moves all of a note's untracked values, which
  the owner's scope excludes.
- **Borrowed mechanism, `git blame`**: attribute each current unit to the last
  commit that changed it; blame follows renames and attributes a revert to the
  revert commit, which is the behavior wanted here. The adaptation is a
  "semantic blame" on parsed values keyed by (property key, value) rather
  than on text lines. Where the analogy breaks: textual blame re-attributes
  lines on list reordering, quoting changes or note moves, which this story
  must not re-date. Adopted as the dating mechanism above.

#### Challenge and investigation

- Strongest doubt: "last changed" re-dates an untracked scalar on a typo fix
  although the material is the same. Kept: a corrected list item is a new
  value dated at the fix anyway, so first-added would make scalars and lists
  behave differently; the owner's wording is "last added or changed".
- Facts (checked 2026-11-04 in this checkout): property units order by level,
  note creation, note id, key, item index
  (`NotePropertyIndexRepository.unassimilatedOrderBy`, `AssimilationUnit.ORDER`);
  the index is deleted and reinserted per save
  (`NotePropertyIndexService.refreshForNote`); scalars are indexed with value
  `""` (`NotePropertyIndexPlanner`), so trackers match scalars by key only;
  web commits are appended with author and committer "Donut System" at the
  save time (`NotebookGitCommitBuilder`, `AcceptedWebChangeService`); notes
  created by a push get the publish time as creation time
  (`NotebookGitProposalNoteAddition`); publication applies a tip diff and
  walks `firstParentRange` for identity; the CLI publishes a linear chain of
  unpublished commits, so multi-commit pushes exist
  (`cli/src/commands/notebook/notebookLocalCandidate.ts`); scheduling is
  prod-only (`SchedulingConfig`) with one ShedLock job.
- Hypotheses left to the plan: retained histories are small enough for the
  whole-history walk per notebook to fit one bounded job run (observable with
  a production notebook commit count; the job's per-run bound makes a large
  history slow, not failing); JGit's rename following suffices for notes
  moved earlier in history (checked by the walker's own tests).

Accepted Git history supplies the evidence and the publication tip supplies the
indexed values. [ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
