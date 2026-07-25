# Pitfalls Research

**Domain:** Notebook / wiki health lint and optional auto-fix (PKM, Doughnut)
**Researched:** 2026-07-22
**Confidence:** HIGH (Doughnut code + LM Wiki / Obsidian lint ecosystem patterns)

## Critical Pitfalls

### Pitfall 1: Silent or one-click destructive deletes

**What goes wrong:**
Empty-folder cleanup runs as soon as the user opens Health or clicks “Run lint,” or “Fix” applies deletes without an explicit opt-in for that destructive action. Users lose folder structure (and any folder `indexContent`) with no review surface.

**Why it happens:**
Cleaner plugins and agent scripts optimize for “make the wiki healthy” and treat delete as a mechanical fix. LM Wiki–style guidance and Obsidian File Cleaner Redux both treat removal as a separate, confirmed step—implementations that conflate scan with apply skip that gate.

**How to avoid:**
- **Lint ≠ fix.** Run lint always produces a report only.
- Fix is enabled only when auto-fix is on **and** the bulk “remove empty folders” option is selected for this run.
- Never auto-apply deletes on notebook open, tab focus, or settings load.
- Show the list of folders that will be removed **before** Fix runs (nested findings under Empty folders).
- Prefer explicit confirmation language (“Remove N empty folders”) over a generic “Apply.”

**Warning signs:**
- Fix button active with no auto-fix / remove option selected.
- API that deletes folders from a GET or from a bare “lint” endpoint.
- E2E scenarios that assert folders gone after “run health” without a fix step.

**Phase to address:**
Auto-fix gate phase (after report-only empty-folder findings exist).

---

### Pitfall 2: Wrong empty-folder definition (leaf-only, or “no direct notes”)

**What goes wrong:**
Lint only flags folders with zero *direct* notes, leaving nested empty shells (`A/B/C` all note-free). Or it flags a parent that still has notes in a deep child. Auto-fix then either never cleans real rot or deletes structure users still use.

**Why it happens:**
Filesystem cleaners often implement “directory has no entries” per node without a recursive “subtree note count.” Doughnut folders are a tree (`parent_folder_id`); emptiness is a **subtree** property.

**How to avoid:**
- Define empty folder as: **entire subtree has no non-deleted notes** (project decision: recursive).
- Compute with a single notebook-scoped query (folder id → note counts in self + descendants), not N+1 “list children” walks.
- When reporting, list deepest empty folders and/or roots of empty subtrees consistently; when deleting, remove a whole empty subtree in a safe order (children before parents, or one transactional purge of the empty set).

**Warning signs:**
- Fixture `Parent/Child` both note-free but only `Child` (or neither) appears in findings.
- Fix leaves empty parent shells behind after “remove empty folders.”

**Phase to address:**
Empty-folder lint rule phase (definition + query); auto-fix phase must reuse the same predicate.

---

### Pitfall 3: Treating folder `indexContent` as irrelevant

**What goes wrong:**
A folder with no notes but non-blank `indexContent` (folder index markdown, frontmatter instructions, wiki links) is deleted as “empty.” Users lose container-owned content that is not a `Note` row.

**Why it happens:**
PROJECT text defines empty via “no notes.” Doughnut folders also store `Folder.indexContent` (and notebooks have notebook-level `indexContent`). Existing dissolve deletes the folder row and does not migrate index content to the parent.

**How to avoid:**
- Extend “empty” for **auto-fix eligibility** to: no notes in subtree **and** no meaningful index content on any folder in that subtree (at minimum: blank/null `indexContent` on every folder being removed).
- Still *report* “note-empty but has index” folders as a **separate severity or rule** (e.g. info/warn: structure is note-empty but index exists)—do not auto-delete them in v1.
- Document the predicate in the rule description shown on the Health tab.

**Warning signs:**
- Tests only create folders with names and no `indexContent`.
- User reports: “Health deleted my folder index / title_pattern instructions.”

**Phase to address:**
Empty-folder rule definition phase (predicate); auto-fix phase must refuse folders with index content.

---

### Pitfall 4: Using dissolve as “delete empty folder”

**What goes wrong:**
Health fix calls `DELETE /api/notebooks/{notebook}/folders/{folder}` (`dissolveFolder`), which **promotes** direct notes and subfolders to the parent. On a parent with only empty child folders, dissolve leaves those children at the parent level instead of removing the empty tree. On a non-empty folder, it destroys intended hierarchy.

**Why it happens:**
The only folder-removal API today is dissolve-with-promote, not “purge empty subtree.” Reusing it for health is the path of least resistance and wrong for cleanup.

**How to avoid:**
- Auto-fix must only target folders that pass the empty predicate (no notes, and no index content if that gate is adopted).
- Implement a dedicated purge path (or dissolve only when the folder has **zero** notes and **zero** subfolders—true leaf empty—and loop until no empty folders remain), never dissolve a folder that still has children unless children are also being deleted in the same transaction.
- Server-side re-check emptiness immediately before delete (TOCTOU).

**Warning signs:**
- After Fix, empty folders appear at notebook root that were previously nested.
- Controller tests for health fix call dissolve without asserting child count is zero.

**Phase to address:**
Auto-fix / remove-empty-folders phase (must not share dissolve semantics blindly).

---

### Pitfall 5: False-positive dead links from a second resolver

**What goes wrong:**
Lint invents its own `[[...]]` regex and title lookup. Links that render live in the note editor are reported dead (or the reverse). Trust in Health collapses.

**Why it happens:**
Ecosystem tools (openclaw wiki lint, markdownlint-obsidian, claude-obsidian wiki-lint) repeatedly ship false positives when resolution diverges from the host app: qualified paths, aliases, fragments, table `\|` escapes, code spans. Doughnut already has `WikiLinkResolver`, `WikiLinkMarkdown`, `NoteContentMarkdown.wikiLinkInnersInOccurrenceOrder` (frontmatter scalars + body), and alias/title case rules.

**How to avoid:**
- Dead = wiki link inner present in content that **does not resolve** via the **same** `WikiLinkResolver` path used for viewing/cache rebuild (target segment after `|`, qualified `Notebook:Title`, unqualified focus-notebook default, alias lookup, case-insensitive title, soft-deleted targets not live).
- Extract inners only through `NoteContentMarkdown.wikiLinkInnersInOccurrenceOrder` (or the shared algorithm it wraps)—not a one-off frontend regex.
- Do **not** invent dead links by “cache row missing” alone without understanding cache semantics: `NoteWikiTitleCache` stores **resolved** rows only; absence means unresolved *or* never refreshed (see Pitfall 6).

**Warning signs:**
- Health lists `[[Other Notebook:Note]]` as dead while the note opens correctly for the owner.
- Findings for `[[Title|display]]` use display text as the target.
- Alias-only titles (`aliases` frontmatter) flagged dead.

**Phase to address:**
Dead-link lint rule phase (must bind to existing resolver).

---

### Pitfall 6: Stale wiki-title cache as source of truth

**What goes wrong:**
Lint reads `note_wiki_title_cache` and treats every `[[token]]` not present as dead—or treats every cached row as live—without re-extracting content. Missed `refreshForNote` call sites (known fragile area) produce phantom dead links or hide real ones.

**Why it happens:**
Cache is an optimization for live rendering and backlinks (`WikiTitleCacheService`), not a complete inventory of link mentions. CONCERNS.md already flags derived-index coherence as a recurring bug class.

**How to avoid:**
- Prefer: extract link inners from current note content → resolve each with `WikiLinkResolver` (batch notebook notes). Cache may be used only as an acceleration **after** proving it matches resolver output in tests.
- If using cache for performance: on Health run, either refresh notebook caches first or detect “content hash / updated_at vs cache” staleness and fall back to live resolve.
- Integration tests: edit content without going through a broken write path, assert Health agrees with editor dead-link styling for the same user.

**Warning signs:**
- Health findings change after opening a note (which triggers refresh) but not after Run alone.
- Divergence between note-show `wikiTitles` / `dead-link` class and Health list.

**Phase to address:**
Dead-link lint phase; performance phase if cache acceleration is added later.

---

### Pitfall 7: Misclassifying qualified / authorization edge cases

**What goes wrong:**
`[[Secret Notebook:Hidden]]` is reported as dead when the target exists but the viewer cannot read that notebook (resolver omits unreadable targets; cache omits them). Cross-notebook links the owner can read are reported dead because lint only searches the **current** notebook’s titles.

**Why it happens:**
Qualified links (`Notebook:Title`) are first-class in `WikiLinkTargetReference` / `WikiLinkResolver`. Naive “title exists in this notebook” checks ignore the notebook prefix and auth.

**How to avoid:**
- Always parse qualified targets; resolve against the named notebook’s notes/aliases.
- Scope Health findings to the **same viewer authorization** as the editor for that notebook (notebook owner/editor running Health).
- v1 may label all unresolved tokens as “dead links” to match UI `dead-link` styling—but must not use a weaker resolver than the UI. Optional later: severity distinction `unresolved` vs `forbidden` if product wants it (out of v1 unless cheap).

**Warning signs:**
- Unit tests only use unqualified `[[Title]]` inside one notebook.
- Cross-notebook fixtures missing from dead-link rule tests.

**Phase to address:**
Dead-link lint rule phase.

---

### Pitfall 8: Full-notebook scan performance collapse

**What goes wrong:**
Health loads every note’s full markdown into the app server (or worse, into the SPA), runs regex per note serially, and blocks the UI for large notebooks. Concurrent Health runs or N+1 folder queries lock up MySQL.

**Why it happens:**
PKM linters often start from “walk all files.” Doughnut already pays for full-catalog jobs (`EmbeddingMaintenanceJob` over all notebooks). On-demand Health must not copy that shape carelessly.

**How to avoid:**
- Empty folders: aggregate SQL (notes per folder_id, folder parent map) → compute empty set in memory—O(folders + notes), not O(folders × queries).
- Dead links: stream note id + content (or content length-capped) for **one** notebook; resolve with batched title/alias lookups per notebook name, not per link query to DB.
- Keep work **notebook-scoped** and **request-scoped** (user-triggered), not a global cron in v1.
- Return structured findings; do not ship full note bodies to the client for the report.
- Set an explicit expectation in UI (“Running checks…”) and fail gracefully on timeout rather than hanging the tab.

**Warning signs:**
- Health API latency grows linearly with total note body size with no batching.
- Frontend fetches every note realm to lint client-side.

**Phase to address:**
Structure for lint service (early) + both rule phases; verify with a large-notebook fixture or query plan review before auto-fix.

---

### Pitfall 9: Conflating lint with fix (and fix with “make links valid”)

**What goes wrong:**
One endpoint or button both reports and mutates. Or dead-link findings offer auto-create notes / auto-retarget, writing wrong titles into the graph. User trust and undo story explode.

**Why it happens:**
“Health” product language sounds curative. LM Wiki and maintenance write-ups explicitly separate structural report from rewrite; File Cleaner Redux previews deletions. Dead-link repair needs judgment (create vs retarget vs leave as intentional stub).

**How to avoid:**
- **v1:** dead links = report only; empty-folder remove = only optional fix.
- Separate API operations: `POST .../health/lint` (read-only) vs `POST .../health/fix` (mutates, gated).
- Severity tiers on findings (e.g. empty-folder = fixable when opted in; dead-link = warning/info, not fixable in v1).
- Do not add “create missing notes for all dead links” in v1.

**Warning signs:**
- OpenAPI shows a single “health” POST that returns findings and deletes folders.
- UI copy: “Fix issues” with no per-rule opt-in.

**Phase to address:**
Health tab / lint run phase (report-only contract); auto-fix phase (narrow mutation API).

---

### Pitfall 10: Overbuilding LLM / semantic lint and config early

**What goes wrong:**
Roadmap spends phases on contradiction detection, stale claims, orphan-page LLM review, per-rule severity matrices, per-notebook schemas—before users can run empty-folder + dead-link checks. Mechanical v1 ships late or never; OpenAI cost and latency attach to every Health run.

**Why it happens:**
Karpathy’s LM Wiki “Lint” operation is described mostly as LLM health-check (contradictions, orphans, missing concepts). Implementations that follow the gist literally skip the proven split: **programmatic structural lint first**, semantic/LLM second (`llm-atomic-wiki`, maintenance articles).

**How to avoid:**
- v1 rules are deterministic only: empty folders + dead wiki links (body + frontmatter).
- Config surface v1: user defaults for “enable auto-fix” + “remove empty folders”—not a full rule registry UI.
- Reserve severity / extensible registry as **research-informed internal model** (enums, rule ids in API findings) without building a settings console for every future rule.
- LLM lint is explicit out of scope until mechanical Health is used and trusted.

**Warning signs:**
- Phase plan includes OpenAI calls for Health.
- Settings mockups with per-rule toggles, severity editors, and semantic checks before report-only mechanical lint works.

**Phase to address:**
All v1 phases (scope discipline); defer to a later milestone for semantic lint.

---

### Pitfall 11: Soft-deleted notes and title conflicts

**What goes wrong:**
Empty check counts soft-deleted notes as occupying a folder (never cleans) or ignores them inconsistently with placement rules. Bulk delete then interacts badly with soft-deleted title conflict rules when users recreate structure.

**Why it happens:**
Doughnut uses `deletedAt` and folder placement rules for soft-deleted titles (`SoftDeletedTitleConflictMvcTest`, dissolve path checks).

**How to avoid:**
- Empty predicate uses **non-deleted** notes only (`deletedAt IS NULL`), consistently with sidebar listing.
- Auto-fix does not need to hard-delete soft-deleted notes.
- Re-run emptiness check inside the fix transaction.

**Warning signs:**
- Folder with only soft-deleted notes never appears empty (or appears empty in UI but not in Health).

**Phase to address:**
Empty-folder rule phase.

---

### Pitfall 12: Missing authorization on Health endpoints

**What goes wrong:**
New `/api/.../health` endpoints forget `AuthorizationService` (repo-wide footgun: permit-all filter chain). Attackers list another user’s dead links or delete empty folders in notebooks they do not own.

**Why it happens:**
CONCERNS.md: auth is manual per controller method.

**How to avoid:**
- Every Health lint/fix method calls `assertAuthorization(notebook)` (write) / read-equivalent for lint.
- Controller tests: foreign user → 401/403; anonymous → denied.
- Fix endpoint must require edit rights, not mere read share.

**Warning signs:**
- Controller merged without authorization test companions.

**Phase to address:**
First API-bearing phase (Health lint endpoint).

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Client-side lint over fetched notes | No backend work | Wrong auth, huge payloads, resolver drift | Never for v1 |
| Reuse dissolve for cleanup | No new delete path | Promotes children; wrong empty-tree semantics | Only for true leaf empty folders in a loop with re-check |
| Cache-only dead-link detection | Fast | Stale/phantom findings | Only after refresh-on-run or proven coherence |
| Full rule-registry settings UI | Feels “complete” | Blocks shipping mechanical value | Never in v1 |
| Auto-fix dead links by creating notes | Clears the list | Pollutes notebook with stubs | Never in v1 |
| Ignore `indexContent` in empty predicate | Simpler SQL | Silent loss of folder indexes | Never for auto-fix |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| `WikiLinkResolver` / wiki cache | Parallel “lint resolver” | Single resolve path; extract via `NoteContentMarkdown` |
| `DELETE .../folders/{folder}` dissolve | Treat as purge | Empty-only purge or leaf dissolve loop; never promote on health fix |
| `NoteWikiTitleCache` | Equate missing row with dead | Extract mentions → resolve; cache is resolved-only |
| OpenAPI / generated client | Hand-edit client for findings DTO | Regenerate after controller/DTO change |
| AuthorizationService | Skip on read-only lint | Still authorize notebook access |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| N+1 folder emptiness checks | Slow Health on deep trees | One notebook note/folder aggregate query | Hundreds of folders |
| Load all note realms to SPA | Browser memory spike | Server-side lint, compact findings DTO | Large notebooks / mobile |
| Per-link DB resolve | Thousands of queries | Batch candidates by notebook name + title/alias keys | Notes with many links |
| Refresh-all-caches on every lint without need | Write amplification | Live resolve or selective refresh | Write-heavy notebooks |
| Global scheduled Health | Contends with embedding job | User-triggered, notebook-scoped only in v1 | Multi-tenant prod |

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| Unauthenticated Health lint/fix | Data leak / destructive delete | `assertAuthorization` + tests |
| Fix allowed for read-only share | Shared readers delete structure | Require edit/owner authorization on fix |
| Returning full note bodies in findings | Excess disclosure of notebook content | Finding references: note id, title, link text, folder path only |

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| Findings without navigation | Cannot open the offending note/folder | Nest by rule; link/navigate to note or folder |
| “Fix” with no preview | Surprise data loss | List targets; Fix disabled until remove option selected |
| Mixing fixable and report-only in one button | Expects dead links to “heal” | Separate sections; only empty folders show fix affordance |
| No severity cues | Alarm fatigue | Structural fixable vs report-only warning tiers |
| Silent success with zero findings | Unclear if lint ran | Explicit “No issues” / last-run timestamp |

## "Looks Done But Isn't" Checklist

- [ ] **Lint ≠ fix:** Run lint never mutates; verified by API and E2E.
- [ ] **Empty = recursive subtree notes:** Nested empty shells detected and removable as a set.
- [ ] **Index content gate:** Folders with non-blank `indexContent` are not auto-deleted.
- [ ] **Dissolve semantics:** Fix does not promote children or leave empty shells at parent.
- [ ] **Shared wiki resolver:** Qualified links, aliases, frontmatter, `|` display, case rules match editor.
- [ ] **Cache coherence:** Health agrees with editor dead-link styling without requiring a note open first.
- [ ] **Auth:** Foreign user cannot lint or fix.
- [ ] **Performance shape:** No per-folder or per-link query pattern in hot path.
- [ ] **Dead links report-only:** No auto-create / auto-retarget in v1.
- [ ] **No LLM on Health path:** Mechanical rules only.
- [ ] **TOCTOU:** Fix re-validates emptiness inside the transaction.
- [ ] **User defaults:** Opt-in auto-fix defaults do not enable Fix without the bulk remove option selected for the run.

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Silent empty-folder delete | HIGH | No undo for dissolved/removed folders today—prevent with gates; if shipped, stop fix, restore from DB backup if any; do not add more delete paths until preview+opt-in exist |
| Wrong empty definition / leftover shells | LOW | Re-run lint with corrected predicate; fix remaining empty set |
| False-positive dead links | MEDIUM | Switch to shared resolver; clear user distrust with release notes; no data loss |
| Accidental dissolve promote | MEDIUM | Manual folder recreation; avoid by not calling dissolve on non-leaves |
| Stale cache phantoms | LOW | Refresh notebook wiki caches or live-resolve; re-run Health |
| Unauthorized endpoint | HIGH | Patch auth immediately; audit access logs |

## Pitfall-to-Phase Mapping

Suggested v1 phase order (capability names; numbers assigned at roadmap time):

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| Lint≠fix conflation; LLM overbuild | Health tab + report-only lint shell | Run produces findings only; no OpenAI; no delete |
| Missing auth | First Health API | Foreign/anon denied |
| Wrong empty definition; soft-delete | Empty-folder rule (report) | Nested empty shells listed; soft-deleted-only folders count as empty |
| `indexContent` ignored | Empty-folder rule (report + fix eligibility) | Index-bearing note-empty folder reported differently / not fix-eligible |
| Dead-link false positives; qualified links; cache | Dead-link rule (report) | Cross-notebook + alias + frontmatter fixtures match editor |
| Full-scan performance | Lint service structure (with both rules) | Aggregate queries; compact DTO; no client-side full-notebook fetch |
| Silent deletes; dissolve misuse; TOCTOU | Optional auto-fix: bulk remove empty folders | Fix disabled without options; preview list; only empty set removed; no promote |
| Overbuilt config | User defaults (narrow) | Only auto-fix + remove-empty-folders defaults; no rule-registry UI |

**Phase ordering rationale:** Report-only shell and auth first (stop-safe). Empty-folder report before dead links or either order among reports is fine; **both reports before any fix**. Auto-fix last so delete predicates and previews are proven. Defaults after the options exist. Semantic/LLM never in this milestone.

## Sources

- Doughnut `.planning/PROJECT.md` — Health scope, lint≠fix, recursive empty folders, dead links report-only, LM Wiki inspiration
- Doughnut `.planning/codebase/CONCERNS.md` — auth footgun, wiki cache coherence, full-notebook scan jobs
- Doughnut `WikiLinkResolver`, `WikiLinkMarkdown`, `NoteContentMarkdown`, `WikiTitleCacheService`, `WikiTitleCacheServiceTest` (qualified links, aliases, unreadable notebooks)
- Doughnut `Folder` entity (`indexContent`), `FolderRelocationService.dissolveFolder`, `NotebookController` dissolve DELETE
- Karpathy LLM Wiki gist — Lint as health-check; pattern description (semantic-leaning; not a delete-safe checklist alone)
- `dr-data/llm-atomic-wiki` — programmatic structural lint before LLM lint
- DEV: “LLM Wiki Maintenance” (rosgluk) — structural checks first; semantic checks produce reports, not auto-rewrites
- Obsidian ecosystem: File Cleaner Redux (preview before delete; optional recursive empty folders); wiki-lint false positives (qualified/title resolution, code spans, `\|` escapes) — AgriciDaniel/claude-obsidian#69, openclaw#73574, markdownlint-obsidian PR #91

---
*Pitfalls research for: Notebook Health lint / auto-fix (Doughnut PKM)*
*Researched: 2026-07-22*
