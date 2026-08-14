# Assimilation-sequence skip

**Status:** in progress (Phase 13 next)  
**Type mix:** Structure then Behavior

Each phase is one commit: **Behavior** (one observable) or **Structure** (only what the **immediate next** behavior needs). Size for ~5 minutes wall-clock including targeted tests.

## Goal

A first-class **assimilation-sequence skip** (table `assimilation_sequence_skip`) takes a unit out of the next-to-assimilate walkthrough. It does **not** block assimilating that unit from the note as understanding, spelling, or commissioned. Remove from recall and Revive stay tracker operations. **Return to sequence** is a different action. Notebook **Skip Memory Tracking** stays the notebook setting; stop calling it skip recall.

## Naming

**Skip assimilation** is rejected. What is skipped is only the **assimilation sequence**. Short UI stays **Skip**.

| Term | Meaning | Short UI |
|---|---|---|
| **Assimilation sequence** | Ordered units offered as next-to-assimilate (menu walkthrough, `/next`). Distinct from assimilating on a note. |
| **Skip from the assimilation sequence** | Mark a unit so it is not offered as next. Does not block assimilating that unit from the note (any tracker type). | **Skip** |
| **Return to sequence** | Delete the sequence-skip row; the unit is pending in the walkthrough again | **Return to sequence** |
| **Remove from recall** | Stop an existing memory tracker from appearing in recall; the unit does not re-enter the sequence | **Remove** / **Remove from recall** |
| **Revive** | Re-enable recall for a tracker that was removed from recall | **Revive** |
| **Skip Memory Tracking** | Notebook setting: notebook does not participate in assimilation or recall (blocks Bazaar subscribe) | full phrase |

Do not use **skip assimilation**, **Unskip**, or **skip recall** for this mark.

## Table

Name: **`assimilation_sequence_skip`**

| Column | Notes |
|---|---|
| `id` | PK |
| `user_id` | FK → `user`, `ON DELETE CASCADE` |
| `note_id` | FK → `note`, `ON DELETE CASCADE` (same as `memory_tracker`) |
| `property_key` | `NOT NULL`, default `''` (empty = note-level) |
| `skipped_at` | `NOT NULL` |

Unique `(user_id, note_id, property_key)`.

## Invariant

For a learner × unit: sequence-skip row **XOR** live understanding tracker (not both).

Spelling and commissioned trackers **may** coexist with a sequence-skip row. They do not satisfy ordinary sequence due and they do **not** clear the skip row.

In the assimilation sequence iff: no sequence-skip row **and** no live understanding tracker at that grain.

Sequence skip does not consume the daily assimilation target. Ordinary assimilate (understanding) from a skipped note **does** consume it, and **clears** the skip row.

## Confirmed product rule — assimilate from a skipped note

A unit marked skip-from-sequence is absent from **next**. The learner can still open that note’s assimilation settings and:

1. **Assimilate** (understanding) — create tracker, delete skip row.
2. **Remember spelling** — create spelling tracker; skip row **remains**.
3. **Assimilate as commissioned** — create commissioned tracker; skip row **remains**.

Each kind is disabled only when that kind of tracker already exists.

## Button matrix (assimilation settings, per unit)

| State | Assimilate / spelling / commissioned | Skip | Return to sequence | Remove from recall | Revive |
|---|---|---|---|---|---|
| Pending | on (each kind if not already present) | on | — | — | — |
| Sequence skip | on (each kind if not already present); understanding assimilate clears skip | — | on | — | — |
| Understanding, active | understanding disabled; spelling/commissioned as today | — | — | on | — |
| Understanding, removed from recall | understanding disabled | — | — | — | on |

Skip and Remove are never both visible. Return to sequence and Revive are never the same control.

Until the matching Behavior phase lands, interim UI is allowed (e.g. Skip still visible and idempotent after a sequence skip). Remove that interim when the later phase adds Return to sequence / Remove from recall.

## API shape (no translation flag)

- **Assimilate** (understanding) creates an understanding tracker and deletes a matching sequence-skip row if present. Spelling / commissioned assimilate do not delete the skip row.
- **Skip from the assimilation sequence** is its own write (`AssimilationSequenceSkip` resource), not `assimilate(skip=true)`. Optional `propertyKey` on that resource from the first API commit (one grain); property **UI** waits for the property Behavior phases.
- **Return to sequence** deletes that row.
- **Remove from recall** / **Revive** keep the existing memory-tracker endpoints.
- Drop `skipMemoryTracking` from `AssimilationRequestDTO` only after property skip no longer uses it.

After any OpenAPI change: `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`. After Flyway: `CURSOR_DEV=true nix develop -c pnpm export:database-erd`.

Permanent artifacts stay capability-named (no phase numbers in product files).

## Phase index

| # | Type | One outcome |
|---|---|---|
| 1 | Structure | ADR 0001 glossary (done) |
| 2 | Structure | Table `assimilation_sequence_skip` (done) |
| 3 | Structure | POST skip + `/next` excludes skip rows (done) |
| 4 | Behavior | Skip on the panel leaves the sequence with no dummy tracker (done) |
| 5 | Behavior | Assimilate (understanding) a skipped note (done) |
| 6 | Behavior | Remember spelling a skipped note (done) |
| 7 | Behavior | Assimilate as commissioned a skipped note (done) |
| 8 | Behavior | Migrate note-level dummy skips (`recall_count = 0`) (done) |
| 9 | Behavior | Return to sequence (note) (done) |
| 10 | Behavior | Remove from recall on assimilation settings (note) (done) |
| 11 | Behavior | Skip a property in the sequence (done) |
| 12 | Behavior | Assimilate a skipped property (done) |
| 13 | Behavior | Return to sequence (property) |
| 14 | Behavior | Remove from recall on assimilation settings (property) |
| 15 | Behavior | Migrate property-level dummy skips (`recall_count = 0`) |
| 16 | Structure | Drop `skipMemoryTracking` on assimilate + testability wording |
| 17 | Structure | Notebook Skip Memory Tracking E2E language |

---

## Phase 1 — Lock glossary in ADR 0001

- **Type:** Structure  
- **Status:** done  
- **Enables:** Phase 2+ naming

**Done:** Proposed ADR 0001 Decision holds assimilation sequence, sequence skip (UI Skip), Return to sequence, Remove from recall, Revive, Skip Memory Tracking. Context dropped Skip recall / Revive recall. Status stays Proposed.

---

## Phase 2 — `assimilation_sequence_skip` table

- **Type:** Structure  
- **Status:** done  
- **Enables:** Phase 3

**Done:** Flyway `V300000252` empty table (unique user/note/property_key, CASCADE FKs). Entity + repository. ERD regenerated. No product behavior change.

---

## Phase 3 — Sequence-skip write and `/next` exclusion (no UI)

- **Type:** Structure  
- **Status:** done  
- **Enables:** Phase 4

**Done:** `POST /api/assimilation-sequence-skips` (idempotent). `/next` excludes skip rows in SQL for notes and properties. `V300000253` aligned table collation to `utf8mb4_0900_ai_ci` (do not edit V300000252). Skip button still uses dummy-tracker assimilate.

**Learning:** New tables must use `utf8mb4_0900_ai_ci` or JPQL joins on string keys fail.

---

## Phase 4 — Skip on the panel leaves the sequence without a dummy tracker

- **Type:** Behavior  
- **Status:** done

**Done:** Note-level Skip POSTs `/api/assimilation-sequence-skips`, advances next, no dummy tracker, daily cap unchanged. Button **Skip**. Confirm: leave the assimilation sequence. Property skip still uses `skipMemoryTracking`. Old Revive-after-skip E2E rewritten to assert no dummy tracker. Walkthrough E2E green.

---

## Phase 5 — Assimilate a skipped note (understanding)

- **Type:** Behavior  
- **Status:** done

**Done:** Understanding assimilate deletes the matching sequence-skip row (same grain as the tracker). Spelling/commissioned do not. E2E: skip then assimilate from the note; daily cap counts; XOR restored.

---

## Phase 6 — Remember spelling a skipped note

- **Type:** Behavior  
- **Status:** done

**Done:** Controller test at the assimilate boundary: spelling tracker created, skip row remains, note not next. Production already left skip for non-UNDERSTANDING; no production change.

**Learning:** Understanding-only skip deletion from Phase 5 already covers spelling; this phase is the spelling delta test.

---

## Phase 7 — Assimilate as commissioned a skipped note

- **Type:** Behavior  
- **Status:** done

**Done:** Controller test at the assimilate boundary: commissioned tracker created, skip row remains, note not next. Production already left skip for non-UNDERSTANDING; no production change.

**Learning:** Same as Phase 6 — understanding-only skip deletion already covers commissioned.

---

## Phase 8 — Convert note-level dummy skipped trackers (`recall_count = 0`)

- **Type:** Behavior  
- **Status:** done

**Done:** Gated Flyway `V300000254` inserts matching `assimilation_sequence_skip` rows and soft-deletes live note-level `UNDERSTANDING` dummies (`removed_from_tracking`, `recall_count = 0`, empty `property_key`). Does not convert `recall_count > 0` or property-level rows. Placeholder `dummy_note_sequence_skip_convert` defaults to `1=0` in all Flyway profiles (including prod). JDBC harness `NoteLevelDummySequenceSkipConversionTest` covers no-op vs enabled selection.

**Learning:** Production dummy conversion does **not** run until a deliberate deploy sets `dummy_note_sequence_skip_convert=1=1`. Remove the JDBC harness after that production application (gated-migration policy). Phase 9 does not depend on the gate.

---

## Phase 9 — Return to sequence (note)

- **Type:** Behavior  
- **Status:** done

**Done:** DELETE `/api/assimilation-sequence-skips` (same grain as POST). `NoteRecallInfo.skippedFromAssimilationSequence` drives **Return to sequence** on assimilation settings; Skip hidden while sequence-skipped. E2E: return restores next-eligibility; Revive stays a different control.

**Learning:** OpenAPI operationId is `deleteAssimilationSequenceSkip` so it does not collide with memory-tracker delete.

---

## Phase 10 — Remove from recall on assimilation settings (note)

- **Type:** Behavior  
- **Status:** done

**Done:** Assimilated notes show **Remove from recall** (existing `removeFromRepeating`), not Skip. After remove: Revive shown; unit stays out of the sequence. Tracker-page Remove unchanged; shared confirm copy.

**Learning:** Property rows still show Skip until Phase 11.

---

## Phase 11 — Skip a property in the sequence

- **Type:** Behavior  
- **Status:** done

**Done:** Property Skip POSTs `/api/assimilation-sequence-skips` with `propertyKey`; no dummy tracker. Unassimilated-property queries already excluded skip rows (Phase 3); service test locks that. E2E in `property_memory_tracker.feature`.

**Learning:** Confirm copy still says “this note” for property Skip (shared `SEQUENCE_SKIP_CONFIRM`). Unassimilated-property exclusion was already in `/next` SQL.

---

## Phase 12 — Assimilate a skipped property

- **Type:** Behavior  
- **Status:** done

**Done:** Controller test + E2E: assimilating a skipped property creates the understanding tracker and deletes the matching skip row. Production already deleted skip at tracker grain (Phase 5); no production change.

**Learning:** Skip can still show on an assimilated property row until Phases 13–14.

---

## Phase 13 — Return to sequence (property)

- **Type:** Behavior  
- **Status:** planned

**Pre-condition:** Property skip row, no live property understanding tracker.  
**Trigger:** **Return to sequence** on that property row.  
**Post-condition:** Skip row gone; property pending in the sequence again.

**Tests:** Same property feature.

**Done when:** Property Return to sequence is not Revive.

---

## Phase 14 — Remove from recall on assimilation settings (property)

- **Type:** Behavior  
- **Status:** planned

**Pre-condition:** Active property understanding tracker.  
**Trigger:** **Remove from recall** on that property row.  
**Post-condition:** Property tracker removed from tracking; **Revive** on that row; **Skip** hidden.

**Tests:** Same property feature.

**Done when:** Assimilated properties show Remove from recall, not Skip.

---

## Phase 15 — Convert property-level dummy skipped trackers (`recall_count = 0`)

- **Type:** Behavior  
- **Status:** planned

Same conversion as Phase 8, `property_key <> ''`. Leave `recall_count > 0` as removed from recall.

**Done when:** Those production dummy property skips are sequence-skip rows.

---

## Phase 16 — Drop assimilate `skipMemoryTracking` and testability “skip-recalled”

- **Type:** Structure  
- **Status:** planned  
- **Enables:** nothing further (cleanup after Phase 11 removed the last dummy-skip writer)

**Change:** Remove `skipMemoryTracking` from `AssimilationRequestDTO` and callers. Testability `the notes "X" are skip-recalled` / `assimilateNoteSkippingRecall` insert sequence-skip rows (wording → sequence skip). generateTypeScript. Existing tests still pass (same observables).

**Tests:** `CURSOR_DEV=true nix develop -c pnpm backend:test_only` plus a recall spec that uses that testability step (e.g. `e2e_test/features/recall/recall_quiz_spelling_question.feature`).

**Done when:** Assimilate cannot create a dummy skipped tracker. No production translation flag.

---

## Phase 17 — Notebook Skip Memory Tracking language

- **Type:** Structure  
- **Status:** planned

**Change:** E2E/steps that call the notebook setting “skip recall” use **Skip Memory Tracking** (`e2e_test/features/bazaar/bazaar_subscription.feature` and `When I change notebook {string} to skip recall`). UI title already correct.

**Command:** `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/bazaar/bazaar_subscription.feature`

**Done when:** No E2E/step describes the notebook setting as skip recall.

---

## Out of scope

- Changing **Skip Memory Tracking** as a product concept or column (`skip_memory_tracking_entirely`).
- FSRS / ADR 0003.
- Fourth memory-tracker type.
- Mass-rename unrelated “skip” strings outside this glossary.

## Stop-safe

| Stop after | User value | Waste if later phases never happen |
|---|---|---|
| 1 | Glossary is clear | None |
| 2 | — | Empty table (only justified because 3 is next) |
| 3 | — | Unused skip API (only justified because 4 is next) |
| 4 | Skip no longer creates dummy trackers | Assimilate-from-skipped-note may still be incomplete until 5 |
| 5 | Can assimilate a skipped note | Spelling/commissioned-on-skipped not yet proven |
| 9 | Can put a note back in the walkthrough | Properties still dummy-skip |
| 11 | Property skip is healthy | Property migrate leftover dummies until 15 |

Do not stop after 2 or 3 except mid-execution; they exist only to make 4 a small commit.
