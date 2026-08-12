# Remove re-assimilate — decisions & open issues

Status: planned (refined for small Behavior/Structure phases)
Updated: 2026-08-12

## Goal

Remove the unclear **re-assimilate** product concept. Keep the useful **frequent wrong-answer rule**; surface it as a warning only. Do not delete memory trackers because of that rule.

## Current product snapshot (as of analysis)

| Concept | Mechanism | Effect |
|---------|-----------|--------|
| **Delete note** | `note.deletedAt` | Note hidden; trackers usually get matching `deletedAt`; undo-delete restores both |
| **Soft-delete tracker (user-facing)** | `memoryTracker.deletedAt` via `/soft-delete` or bulk wipe | Tracker gone from recall **and** from “has tracker” checks → note can re-enter assimilation |
| **Remove from recall** | `removedFromTracking = true` (`/remove`) | Tracker inactive for recall; still exists; **revive** via `/re-enable`; note stays assimilated |
| **Skip recall (setting)** | `note.recallSetting.skipMemoryTracking` / notebook `skipMemoryTrackingEntirely` | Note/notebook opted out of memory tracking / assimilation queue |

Frequent-failure rule today: **≥ 5 wrong answers within 14 days** (per memory tracker). Overlap does not count. Frontend confirms “re-assimilate?” then soft-deletes the tracker.

Unassimilated units today:

- **Notes:** no live **note-level** tracker (`propertyKey` empty — covers normal **and** spelling).
- **Properties:** separate unit source; can be due even when the note is already assimilated.
- **Missing spelling while a normal tracker exists:** **not** counted. Enabling remember spelling later currently **wipes** trackers so the note re-enters the note queue — that wipe/re-queue behavior is being **removed**, not replaced with a spelling queue rule.

---

## Decisions (agreed)

1. **Drop re-assimilate as a concept** — Remove name and confirm→soft-delete flow from UI, E2E, code identifiers, and glossary. No prompt offering to reset learning after failures.
2. **Keep the frequent-failure rule** — Still detect the threshold; do **not** remove/soft-delete the tracker because of it.
3. **Warning instead of confirm** — On every wrong answer while still over threshold (not only first crossing), show an alert. Alert-only (OK).
4. **Threshold API returns details (option B)** — `thresholdExceeded`, `wrongCount`, `threshold`, `periodDays`. Frontend must not hardcode 5/14.
5. **Warning copy (O5 decided)** —
   - Note-level: `You've answered incorrectly {wrongCount} times within the last {periodDays} days.`
   - Property: `You've answered the "{propertyKey}" property incorrectly {wrongCount} times within the last {periodDays} days.`
   - Use **live** `wrongCount` from the API (may be > threshold).
6. **Remember spelling later: remove wipe/re-queue only** — Enabling remember spelling must **not** soft-delete trackers and must **not** change assimilation queue or due count. **Delete** E2E *“Already assimilated note reappears for assimilation when remember spelling is added later”* and the unit test that asserts re-queue. **Do not** add a missing-spelling queue rule. **Do not** add a negation E2E/unit test that the count stays unchanged. (Add-spelling-only when already assimilating may remain elsewhere.)
7. **ADRs** — Update Proposed ADR **0001** (drop Re-assimilate). ADR **0003**: concise frequent-failure **warning** policy; fix overlap bullet away from “re-assimilation.”
8. **Leave alone** — Tracker-page remove/revive, note/notebook skip recall, **note soft-delete/restore cascade** onto trackers (`deletedAt` for that lifecycle only).
9. **Remove user-facing soft-delete of memory trackers** — Drop `/soft-delete`, bulk wipe, and learner flows that soft-delete a tracker independently.
10. **Property removal → hard-delete tracker** — Cancellable warning in rich **and** markdown (guard already exists); action becomes hard-delete, not soft-delete.
11. **Data cleanup** — Hard-delete `memory_tracker` rows with `deletedAt IS NOT NULL` whose **note is not** soft-deleted. Keep trackers on soft-deleted notes for restore.
12. **No dead leftovers** — Remove unused endpoints, methods, tests, E2E, copy, docs; regenerate API client.

---

## Clarifications

### O2 — Note→tracker cascade

No redesign. Note delete/restore continues to stamp/clear tracker `deletedAt`.

### O4 — Remember spelling vs queue (final)

Today wipe forces re-queue. **Final product:** turning on remember spelling does not wipe trackers and does not affect queue/count. Remove the reappears E2E and related unit test; no replacement queue feature; no negation counter test.

### O7 — Out of scope

- Changing the numeric rule (5 / 14) itself
- Skip/revive UX redesign
- Redesigning note soft-delete
- Dropping `memory_tracker.deletedAt` column
- Assimilation queue rule for “missing spelling tracker”
- Negation tests that count is unchanged after enabling remember spelling

---

## Implementation notes

- Hard-delete memory tracker API for property removal; then remove user-facing soft-delete.
- Flyway data migration for orphan soft-deleted trackers on live notes.
- Remove remember-spelling wipe + reappears E2E/unit test (no queue replacement).
- Threshold DTO enrichment + recall alert.
- Scrub re-assimilate; regenerate OpenAPI/TS client.

See `PLAN.md` for phases.
