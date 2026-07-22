# Phase 4: Dead-link findings - Context

**Gathered:** 2026-07-22
**Status:** Ready for planning

<domain>
## Phase Boundary

**Behavior.** Authorized notebook Health lint reports dead `[[wiki links]]` in note **body** and **frontmatter / properties** under the `dead_wiki_links` findings group, nested **by note**, without mutating the notebook.

Delivers DLNK-01, DLNK-02, and DLNK-03. Lint remains report-only (no rewrite / retarget / create). Reuses existing extract + resolve authority — no second linker. No Health UI (Phase 5). No fix path (Phase 7). Empty-folder and readme-only rules from Phases 2–3 remain unchanged.

</domain>

<decisions>
## Implementation Decisions

### Resolve authority and scan scope (DLNK-01 / DLNK-02)
- **D-01:** Dead = a wiki-link **inner** present in note content that does **not** resolve via the same viewer-readable path as editor/cache (`WikiLinkResolver.resolveWikiLinkToken` / equivalent used by `resolveWikiLinksForCache`), with the authorized lint caller as `viewer`. Do **not** invent a second parser/resolver; do **not** treat `note_wiki_title_cache` absence alone as dead.
- **D-02:** Extract inners with `NoteContentMarkdown.wikiLinkInnersInOccurrenceOrder` so **frontmatter scalar/list values and body** are covered in one pass (FM first, then body — existing algorithm order). One rule covers both DLNK-01 and DLNK-02; do not split into two rule ids for v1.
- **D-03:** Scan only **live** (non-soft-deleted) notes in the notebook as link **sources**. Soft-deleted notes are not audited.
- **D-04:** Match editor semantics for targets: aliases, qualified `Notebook:Title`, `|` display text (resolve on target segment), case rules, and soft-deleted / unreadable targets that fail viewer-readable resolve count as dead. Prefer viewer-readable resolve over `resolveAnyTargetWikiLinkToken` so Health matches render-time dead links for the owner running lint.

### Token uniqueness and finding identity
- **D-05:** Per note, report each **distinct** unresolved inner **once** (dedupe preserving first-occurrence order), matching `WikiLinkResolver` cache dedupe — not one finding per textual occurrence.
- **D-06:** Each leaf `HealthFindingItem` sets `noteId` (source note), `wikiLinkToken` = the extracted **inner** (no surrounding `[[ ]]`), and `label` = that same inner string. Optional `message` is not required for v1 (no body-vs-frontmatter split in the message).
- **D-07:** Do not set `folderId` on dead-link items. `autoFixable` stays group-level only (`false` on this rule).

### Per-note nesting shape
- **D-08:** Top group: `ruleId` = `dead_wiki_links` (`HealthRuleIds.DEAD_WIKI_LINKS`), title “Dead wiki links”, severity `warning`, `autoFixable` = **`false`**, top-level `items` empty/null, **`children`** = one child group per source note that has ≥1 dead link.
- **D-09:** Each child group: `title` = note topic/title; `ruleId` = `dead_wiki_links` (same stable id; nesting + `noteId` on items identify the note); severity `warning`; `autoFixable` = `false`; `items` = that note’s dead-link leaf findings; no further nesting.
- **D-10:** Always emit the top `dead_wiki_links` group (metadata + `children`, possibly empty), consistent with Phase 2/3 always-emit groups.

### Implementation surface
- **D-11:** Add a Spring `HealthRule` bean (e.g. `DeadWikiLinkHealthRule`) discovered by existing `HealthRuleRunner`. Reuse authorized `POST .../health/lint` — no new endpoint, no request-body options, no frontend.
- **D-12:** No OpenAPI / TypeScript client regen unless a wire schema change appears (report DTOs already support `children` + `wikiLinkToken`). Verify lint response can include the nested dead-link group alongside existing folder groups via backend tests.
- **D-13:** Prove with focused backend tests: body dead link; frontmatter/property dead link; alias and qualified `Notebook:Title` resolve as live (not false-positive); soft-deleted source notes excluded; soft-deleted / missing targets reported dead; distinct-token dedupe; nested-by-note children shape; `autoFixable=false`; no mutation. No `@wip` E2E / Health UI in this phase.

### Claude's Discretion
- Exact batching strategy for resolving many notes (per-note resolve vs shared notebook index) — keep correct vs editor; avoid N+1 where a simple batch is obvious.
- Child group field mirroring (whether children copy severity/autoFixable explicitly) — follow DTO constructors used by other rules; keep UI-ready.
- Whether `label` ever shows only the pre-`|` target for display — default is full inner; change only if tests against editor UX strongly prefer target-only.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase scope and requirements
- `.planning/ROADMAP.md` — Phase 4 goal, Behavior type, success criteria (DLNK-01/02/03), stop-safe value after Phase 4
- `.planning/REQUIREMENTS.md` — DLNK-01, DLNK-02, DLNK-03 (this phase); HLTH-03 nesting expectation for later UI
- `.planning/PROJECT.md` — dead links report-only; reuse live resolve semantics; no second client linker

### Prior phase decisions
- `.planning/phases/01-health-lint-contract/01-CONTEXT.md` — recursive `children` groups; `wikiLinkToken` on items; reserved rule id `dead_wiki_links`
- `.planning/phases/02-empty-folder-findings/02-CONTEXT.md` — write-auth lint API; always-emit group pattern; backend-tests-only verification style
- `.planning/phases/03-readme-only-folder-findings/03-CONTEXT.md` — multi-rule report coexistence; `autoFixable=false` precedent for non-fixable groups

### Research (resolve semantics and pitfalls)
- `.planning/research/SUMMARY.md` — DeadWikiLinkHealthRule; extract → WikiLinkResolver; no cache-as-truth
- `.planning/research/ARCHITECTURE.md` — nested findings DTO; dead-link data flow; live notes only; distinct tokens
- `.planning/research/STACK.md` — reuse `NoteContentMarkdown` + `WikiLinkResolver`; backend-only Health report
- `.planning/research/PITFALLS.md` — second resolver / cache-as-truth; aliases; qualified links; `|` display; soft-delete; frontmatter parity
- `.planning/research/FEATURES.md` — body + frontmatter dead links; report-only; no schema lint of properties

### Implemented contract / wiki plumbing (must extend, not replace)
- `backend/src/main/java/com/odde/doughnut/services/health/HealthRule.java`
- `backend/src/main/java/com/odde/doughnut/services/health/HealthRuleRunner.java`
- `backend/src/main/java/com/odde/doughnut/services/health/HealthRuleIds.java` — `DEAD_WIKI_LINKS` already reserved
- `backend/src/main/java/com/odde/doughnut/services/NotebookHealthService.java`
- `backend/src/main/java/com/odde/doughnut/controllers/NotebookHealthController.java` — existing lint endpoint
- `backend/src/main/java/com/odde/doughnut/controllers/dto/HealthFindingGroup.java` — `children` for per-note nesting
- `backend/src/main/java/com/odde/doughnut/controllers/dto/HealthFindingItem.java` — `noteId`, `wikiLinkToken`
- `backend/src/main/java/com/odde/doughnut/algorithms/NoteContentMarkdown.java` — `wikiLinkInnersInOccurrenceOrder`
- `backend/src/main/java/com/odde/doughnut/algorithms/WikiLinkMarkdown.java` — inner split / `|` handling
- `backend/src/main/java/com/odde/doughnut/services/WikiLinkResolver.java` — viewer-readable resolve authority

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `HealthRuleIds.DEAD_WIKI_LINKS` — already reserved (`dead_wiki_links`)
- `NoteContentMarkdown.wikiLinkInnersInOccurrenceOrder` — body + frontmatter extraction in document order
- `WikiLinkResolver.resolveWikiLinkToken` / `resolveWikiLinksForCache` — editor-aligned resolve + dedupe pattern
- `HealthFindingGroup.children` + `HealthFindingItem.wikiLinkToken` / `noteId` — Phase 1 reserved for this nesting
- `NotebookHealthController` + `NotebookHealthService.lint` — no new HTTP surface
- Existing empty-folder / readme-only rules — report must continue to include those groups unchanged

### Established Patterns
- Spring `@Service` `HealthRule` beans discovered via `List<HealthRule>` injection
- Always emit group with `ruleId`, `title`, `severity`, `autoFixable`, plus `items` and/or `children`
- Lint is report-only / read-only transaction; owner write auth on controller
- Domain language: **readme** / `readmeContent` elsewhere; wiki links use existing markdown helpers

### Integration Points
- New `DeadWikiLinkHealthRule` bean → injected beside folder rules
- Existing `POST /api/notebooks/{notebook}/health/lint` returns folder groups + nested dead-link group
- Phase 5 UI will expand Dead links → by note → tokens from this shape
- Phase 7 must never treat this group as fixable (`autoFixable=false`)

</code_context>

<specifics>
## Specific Ideas

- `--auto` discussion locked: viewer-readable `WikiLinkResolver`; FM+body via `wikiLinkInnersInOccurrenceOrder`; live source notes only; distinct unresolved inners per note; nest under `children` by note; `wikiLinkToken` = raw inner; `autoFixable=false`; always emit group; backend tests only; no OpenAPI regen unless schema changes.
- Architecture research already specifies distinct tokens and non-deleted source notes — decisions align with that.
- HealthRuleRunnerTest already exercises nested `children` retention — use that shape, not a flat item list for this rule.

</specifics>

<deferred>
## Deferred Ideas

- Health tab UI / Run / expandable nesting presentation — Phase 5
- Click-through from dead-link finding to editor focus — v2 (HLTH-11)
- Assisted / automatic dead-link repair — v2 (DLNK-10) / out of v1
- User-level lint defaults — Phase 6
- Bulk empty-folder purge — Phase 7
- Body-vs-frontmatter split in finding `message` — rejected for v1 noise; revisit only if Phase 5 UI needs a cue
- Using `resolveAnyTargetWikiLinkToken` for “exists but unreadable” nuance — rejected; match editor dead-link for the lint caller

None — discussion stayed within phase scope

</deferred>

---

*Phase: 4-Dead-link findings*
*Context gathered: 2026-07-22*
