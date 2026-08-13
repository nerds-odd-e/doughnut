# Plan: Remove note-level skip memory tracking

**Status:** in progress  
**Type:** ad-hoc (`.planning/quick/`)  
**Goal:** Learners cannot skip memory tracking on a note. Skip remains notebook-wide, or per tracker via Skip recall / Remove from recall / Revive.

Each phase is one commit. Stop after any phase.

**Progress tracking:** [STATE.md](./STATE.md) in this directory only. Do not edit `.planning/STATE.md`.

## Design decisions

1. **Do not touch** `AssimilationRequestDTO.skipMemoryTracking`, assimilation Skip recall UI, Remove from recall, Revive, or notebook skip.
2. **No data migration** for notes already flagged. After the flag is gone they behave like any unassimilated note.
3. **E2E isolation** that today sets `Skip Memory Tracking: true` on inject becomes **skip-recall** (assimilate with request `skipMemoryTracking: true` → skipped tracker). Same sequences; uses the kept mechanism.
4. **Update Proposed ADR 0001** so the glossary no longer describes skip on a note. Status stays **Proposed**. Do not Accept or rename the file.
5. **Do not rename** notebook “Skip Memory Tracking” or the assimilation DTO field.
6. **Do not edit** `V100000000__baseline.sql`. Drop `note.skip_memory_tracking` in a new Flyway version `> 300000246`.
7. **Checkbox absence** → frontend unit test, not an E2E that only asserts a missing control.

## Keep / do not delete

- `NotebookSettings.skipMemoryTrackingEntirely` and `NotebookSettings.vue`
- `e2e_test/start/pageObjects/notebookPage.ts` `skipMemoryTracking()` (notebook settings)
- Bazaar scenario “Notebook with skip recall cannot be subscribed”
- `AssimilationRequestDTO.skipMemoryTracking` and frontend assimilate / `useAssimilateUnit` skip payload
- `MemoryTracker.removedFromTracking`

## Phases

### Phase 1 — Note recall settings have no skip checkbox — Behavior — done

Checkbox removed from `NoteRecallSettingForm`. API field still present. Spec asserts no `skipMemoryTracking` CheckInput.

**Learning:** Spec shares a `checkInputByField` helper (refactor). No change to remaining phases.

---

### Phase 2 — Java fixtures exclude notes via skipped trackers — Structure — done

Callers use `aMemoryTrackerFor(…).removedFromTracking()`. `NoteBuilder.skipMemoryTracking()` deleted. Dropped `shouldNotIncludeNoteThatIsSkippedForRecall` — any note-level tracker already excludes the note from the unassimilated queue.

**Learning:** No remaining Java production tests depend on the note flag. Phase 4 can delete the predicates without rewriting those tests.

---

### Phase 3 — Injected “Skip Memory Tracking” skip-recalls the note — Structure — done

Inject `"Skip Memory Tracking": true` skip-recalls via assimilate and does not set the note flag. Feature tables unchanged. Inject-notes extracted to `InjectNotesWorker` / `NotesTestData` (controller size).

**Learning:** JSON property now lives on `NotesTestData` / `NoteTestData` in `testability/model/`. Phase 6 should remove it there, not only on the old controller inner class.

---

### Phase 4 — Assimilation ignores the note skip flag — Behavior — done

Dropped skip-flag predicates from `NoteRepository.recallWhereClause` and `NotePropertyIndexRepository` (unassimilated properties + wiki-link gate). `recall_pages` E2E still 2 passing.

**Learning:** `RobotsTests.openApiDocsMatchCommittedYaml` failed on a pre-existing `writeOnly` yaml drift, not this JPQL change. Not fixed here.

---

### Phase 5 — Note recall-setting API has no skipMemoryTracking — Behavior — done

Removed `skipMemoryTracking` from `NoteRecallSetting`. Regenerated OpenAPI/client. Assimilation request skip-recall unchanged. DB column remains.

**Learning:** Regen also dropped incidental `NotesTestData` `writeOnly` yaml that had been failing `RobotsTests`. Left as generator output. Phase 4’s OpenAPI-drift note is resolved.

---

### Phase 6 — Note inject no longer has Skip Memory Tracking — Behavior — done

Inject column removed. E2E uses `the notes "…" are skip-recalled` (assimilate-with-skip). Notebook skip scenarios unchanged. Shared `assimilateInjectedNote` helper.

**Learning:** Bazaar Shape skip-recall must run as the notebook owner, then log out, so later scenarios keep a clean session. `cli_recall.feature` is `@ignore`; tables still converted.

---

### Phase 7 — `note.skip_memory_tracking` column dropped — Behavior — planned

**Observable**

- Pre: `note.skip_memory_tracking` still exists.
- Trigger: Migrate the database.
- Post: The column is gone. Notebook `skip_memory_tracking_entirely` remains.

**Production**

- New Flyway `V300000247__drop_note_skip_memory_tracking.sql` (next free version if 247 is taken): `ALTER TABLE note DROP COLUMN skip_memory_tracking;`
- Do not edit `V100000000__baseline.sql`.
- Regenerate `docs/database-erd.md` (`database-erd` skill) if note columns are listed.

**Verify**

```bash
CURSOR_DEV=true nix develop -c pnpm backend:test_only
```

**Stop-safe:** Schema matches the removed concept.

---

### Phase 8 — Proposed ADR 0001 no longer describes skip on a note — Behavior — planned

**Observable**

- Pre: ADR 0001 says skip recall opts a **note or notebook** out, and treats skip recall / Skip Memory Tracking as two names for one concept.
- Trigger: Read Proposed ADR 0001.
- Post: Glossary matches remaining product: **skip recall** = tracker action (panel / Remove from recall); **Skip Memory Tracking** = notebook setting; **revive recall** unchanged. No skip-on-a-note term.

**Work (draft hygiene only — Status stays Proposed)**

Edit [docs/adrs/0001-ubiquitous-language.md](../../../docs/adrs/0001-ubiquitous-language.md):

- Current vocabulary **Skip recall**: opt a **memory tracker** out of recall (assimilation Skip recall or Remove from recall). Not a note setting.
- Add **Skip Memory Tracking**: notebook setting that opts the whole notebook out of assimilation and recall (blocks Bazaar subscribe).
- Redundant table: remove “Turning off recall for a note or notebook | skip recall, Skip Memory Tracking” (they are different layers). Do not list skip-on-a-note.
- Canonical **Memory tracking**: whether a **notebook** participates in assimilation and recall (notebook setting). Tracker-level opt-out is skip recall.
- Disambiguation “Prefer **memory tracking** for the setting; **skip recall** for the action”: the setting is **notebook-only**; do not skip memory tracking on a note.

Do **not** change Status, filename, or Accept the ADR.

**Verify:** Read the edited sections; grep the ADR for “note or notebook” / skip-on-note wording.

**Stop-safe:** Product already shipped the removal; glossary matches.

## Jidoka

- **Before Phase 4:** Grep — only the two repository predicates (plus embeddable) still read the note flag.
- **Before Phase 5:** Do not drop `AssimilationRequestDTO.skipMemoryTracking`.
- **Before Phase 8:** Product phases 1–7 done so the draft describes shipped language, not intent.
- **Value fork:** If skip-recall-as-E2E-setup changes scenario counts, stop rather than rewriting product behavior.

## Discoveries

- Checking the note box never removed existing trackers from recall.
- Many E2E tables skip a notebook-root note so assimilation starts on content notes — isolation must move to skip-recall.
- OpenAPI lists `skipMemoryTracking` on both `NoteRecallSetting` (remove) and `AssimilationRequestDTO` (keep).
- Highest Flyway at plan time: `V300000246`.
- Proposed ADR 0001 is the naming draft agents will keep citing; leaving skip-on-a-note there would invite the concept back.
