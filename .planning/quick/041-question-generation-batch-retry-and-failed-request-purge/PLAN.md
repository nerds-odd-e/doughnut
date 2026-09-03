# Remaining issues

Harvested while deleting spent quick plans 038–043. Outcomes of those plans live in code and permanent docs. This is not a milestone — park until `/gsd-new-milestone` or an ad-hoc plan picks one up.

Standing tech debt (auth footguns, oversized modules, `@ignore` E2E, Quill selection, embedding cron, …) stays in [CONCERNS.md](../../codebase/CONCERNS.md).

## From spent plans

### Note references (038 / 039 / 040)

- Large-notebook deploy check never run: time a title rename and a new note; edit body while a title save is in flight — content PATCH should not lock-timeout (live resolution, no notebook-wide wiki cache).
- Reconsider only if measured:
  - `UNIQUE(source_note_id, document_order)` on `authored_note_reference` — the write path already removed the duplicate-row hazard.
  - Indexed `wiki_note_title_key` column — only if the key-matched inbound query is a bottleneck.
  - Batching resolution in `UnassimilatedPropertyService.isGated` — only if assimilation streaming is slow.
  - Extending the frontend mutation barrier past same-note body autosave to title/property edits.

### Question generation batch (041)

None. Latest-only OpenAI retry, unconditional `FAILED` request purge (`V300000318`), and the ops-doc purge notes are shipped.

### Recall E2E (042 / 043)

None. Suite cut to 12 scenarios; orphaned steps and the property-tracker backdating comment shipped.

## Parked seeds

- [SEED-001](../../seeds/SEED-001-mcq-fuzzy-notebook-title-spelling-match.md) — MCQ / fuzzy / `Notebook:Title` spelling match
- [SEED-002](../../seeds/SEED-002-host-mcp-over-https.md) — host MCP over HTTPS
- [SEED-006](../../seeds/SEED-006-remove-mysql-timestamp-2038.md) — remaining MySQL `TIMESTAMP` → `DATETIME`
- [SEED-007](../../seeds/SEED-007-per-concept-prompt-splitting-single-note-qg.md) — per-concept QG prompt splitting (spike; may not pay off at single-note scale)
- [SEED-008](../../seeds/SEED-008-prod-secrets-visible-in-process-args.md) — prod secrets visible in `ps aux`

QG research companion (not a seed): summarization may be a stronger QG lever than splitting — [qg-retrieval-summarization-highest-leverage.md](../../notes/qg-retrieval-summarization-highest-leverage.md).

## Deferred product

- ADR 0002 Level 1 (git-native notebooks)
- Commissioned trackers for properties in UI (TRK-04)
- Commissioned assimilation first intake via Tutor only (TRK-05)
- Smart request generator, in-app Tutor, machine transport
- Feedback recommendations of what to study next
- Post-deploy API paging; normalising legacy `*_ibfk_*` constraints; re-attempting orphan cleanup (from the 2026-08-12 hard-delete incident)

## Not remaining

Locked anti-features / superseded: `.doughnut-sync`, stacked accidental-match bodies, content peek in resolve dialog, forced resolve / try-again after overlap declare.
