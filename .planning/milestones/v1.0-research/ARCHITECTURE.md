# Architecture Research

**Domain:** Notebook Health lint + optional auto-fix (Doughnut PKM)
**Researched:** 2026-07-22
**Confidence:** HIGH

## Standard Architecture

### System Overview

Lint is a **notebook-scoped, backend-authoritative audit**. The Vue Health tab is a thin run/review/fix surface; all rule evaluation and the only v1 fix (bulk remove empty folders) execute on Spring Boot against live MySQL note/folder graphs and existing wiki-link resolution.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ Client (Vue SPA)                                                            │
│  NotebookPageView                                                           │
│    WorkspaceIndexSettingsTabs  [Index | Settings | Health]                  │
│    NotebookHealthPanel                                                      │
│      options (run-scoped) · expandable findings · Run / Fix actions         │
└───────────────────────────────┬─────────────────────────────────────────────┘
                                │ OpenAPI SDK + apiCallWithLoading
                                │ GET/PATCH /api/user (lint defaults)
                                │ POST /api/notebooks/{id}/health/lint
                                │ POST /api/notebooks/{id}/health/fix
┌───────────────────────────────▼─────────────────────────────────────────────┐
│ API layer                                                                   │
│  NotebookHealthController (or NotebookController health endpoints)          │
│  AuthorizationService.assertAuthorization(notebook)                         │
│  UserController (user-level Health defaults on User / UserDTO)              │
└───────────────────────────────┬─────────────────────────────────────────────┘
                                │
┌───────────────────────────────▼─────────────────────────────────────────────┐
│ Application services                                                        │
│  NotebookHealthService          — orchestrate run / fix, assemble report    │
│  HealthRuleRunner               — ordered rule registry, collect findings   │
│  EmptyFolderHealthRule          — recursive “no notes in subtree”           │
│  DeadWikiLinkHealthRule         — body + frontmatter tokens vs resolver     │
│  EmptyFolderBulkFixApplicator   — hard-delete empty folder trees only       │
│  HealthUserDefaultsService      — map user prefs ↔ run options              │
└───────┬─────────────────┬─────────────────────┬─────────────────────────────┘
        │                 │                     │
        ▼                 ▼                     ▼
┌───────────────┐ ┌───────────────────┐ ┌────────────────────────────┐
│ Folder + Note │ │ WikiLinkResolver  │ │ User entity (defaults)     │
│ repositories  │ │ NoteContentMarkdown│ │ (not NotebookSettings)     │
│ (live graph)  │ │ WikiTitleCache*   │ │                            │
└───────────────┘ └───────────────────┘ └────────────────────────────┘
```

\*Dead-link **reporting** for Health should resolve tokens with the same authority as note show (`WikiLinkResolver` + `NoteContentMarkdown.wikiLinkInnersInOccurrenceOrder`), not by scraping client `dead-link` CSS or loading every `NoteRealm`. The per-note `wikiTitles` cache is an editor performance path; Health is a notebook-wide audit and must not depend on the client having every note open.

### Component Responsibilities

| Component | Responsibility | Typical Implementation |
|-----------|----------------|------------------------|
| **Health UI** (`NotebookHealthPanel` + tab) | Run lint, show expandable nested findings, bind run options, enable Fix only when bulk empty-folder option is selected, save options as user defaults | Vue under `frontend/src/components/notebook/`; tab id on `WorkspaceIndexSettingsTabs` |
| **User defaults** | Persist per-user lint/auto-fix preferences across notebooks | Columns or JSON blob on `User` + `UserDTO` / `PATCH /api/user/{id}` (extend existing user profile update) |
| **Health controller** | Authorize notebook write access; accept run/fix request bodies; return findings DTOs | `NotebookHealthController` or methods on `NotebookController` under `/api/notebooks/{notebook}/health/...` |
| **Rule runner** | Invoke enabled rules in stable order; merge into one nested report; never apply fixes | `NotebookHealthService` + small `HealthRule` interface |
| **Empty-folder rule** | Find folders whose **entire subtree has no notes** (recursive; nested empty shells included) | Service/algorithm using `FolderRepository` + note folder membership (`note.folder_id`); ignore soft-deleted notes |
| **Dead wiki-link rule** | For each note in notebook, extract `[[…]]` from frontmatter values + body; report tokens that do not resolve | Reuse `NoteContentMarkdown.wikiLinkInnersInOccurrenceOrder` + `WikiLinkResolver` |
| **Findings DTO** | Nested, UI-ready groups (e.g. Empty folders → list; Dead links → by note → tokens) | OpenAPI DTOs in `controllers/dto/`; regenerate SDK |
| **Fix applicator** | Apply **only** bulk remove empty folders when request explicitly opts in; refuse otherwise | Dedicated service method — **not** `dissolveFolder` (dissolve promotes children; Health fix deletes empty trees) |

## Recommended Project Structure

```
backend/src/main/java/com/odde/doughnut/
├── controllers/
│   ├── NotebookController.java          # existing folders/notes; health may live here or split
│   ├── UserController.java              # extend for health defaults
│   └── dto/
│       ├── NotebookHealthLintRequest.java
│       ├── NotebookHealthLintReport.java   # top-level findings tree
│       ├── HealthFindingGroup.java         # e.g. rule id + display title + children
│       ├── HealthFindingItem.java          # leaf: folder/note ids, labels, tokens
│       ├── NotebookHealthFixRequest.java   # must include removeEmptyFolders=true
│       └── NotebookHealthFixResult.java    # counts / ids removed
├── services/
│   ├── NotebookHealthService.java         # run + fix orchestration
│   ├── health/                            # cohesive subpackage (preferred)
│   │   ├── HealthRule.java                # interface: id, run(notebook, ctx) → findings
│   │   ├── HealthRuleRunner.java
│   │   ├── EmptyFolderHealthRule.java
│   │   ├── DeadWikiLinkHealthRule.java
│   │   └── EmptyFolderBulkFixApplicator.java
│   ├── WikiLinkResolver.java              # existing — dead-link authority
│   └── FolderRelocationService.java       # existing dissolve — do not overload for Health fix
└── algorithms/
    └── NoteContentMarkdown.java           # existing wiki-link extraction (body + FM)

frontend/src/
├── components/
│   ├── commons/
│   │   └── WorkspaceIndexSettingsTabs.vue # extend tab union: index | settings | health
│   └── notebook/
│       ├── NotebookWorkspaceSettings.vue  # unchanged Settings panel
│       ├── NotebookHealthPanel.vue        # Health tab body
│       └── NotebookHealthFindings.vue     # expandable nested results
└── pages/
    └── NotebookPageView.vue               # switch on activeTab including health
```

### Structure Rationale

- **`services/health/`:** Keeps rule registry and fix applicator cohesive; avoids bloating `NotebookController` / `FolderRelocationService` with audit semantics.
- **Findings as DTOs, not entities:** Reports are ephemeral run results — no findings table in v1.
- **User defaults on `User`, not `NotebookSettings`:** Matches product decision (per-user across notebooks). `NotebookSettings` today is notebook-scoped (`skipMemoryTrackingEntirely`).
- **Third tab, not Settings subsection:** PROJECT requires Health as its own surface with run/results action bar; Index/Settings tabs already exist on `WorkspaceIndexSettingsTabs`.

## Architectural Patterns

### Pattern 1: Backend-authoritative rule runner (lint ≠ fix)

**What:** One orchestrated **lint** path that only produces findings; a separate **fix** path that applies a gated subset of safe mechanical fixes.

**When to use:** Always for notebook Health. Structural rules need the full folder tree, soft-delete filters, alias indexes, and the same resolver as the editor.

**Trade-offs:** Requires API round-trip and OpenAPI regen; prevents client/server drift on “what is empty” / “what is dead.” Client-side scan of currently loaded notes would miss unloaded notes and wrong alias resolution.

**Example (conceptual):**

```java
public interface HealthRule {
  String id(); // "empty_folders" | "dead_wiki_links"
  HealthFindingGroup evaluate(Notebook notebook, HealthRunContext ctx);
}

// Lint: never mutates
public NotebookHealthLintReport lint(Notebook notebook, NotebookHealthLintRequest req) {
  return healthRuleRunner.run(notebook, HealthRunContext.from(req));
}

// Fix: explicit opt-in only
public NotebookHealthFixResult fix(Notebook notebook, NotebookHealthFixRequest req) {
  if (!req.isRemoveEmptyFolders()) {
    throw badRequest("Fix requires removeEmptyFolders");
  }
  return emptyFolderBulkFixApplicator.apply(notebook);
}
```

### Pattern 2: Nested findings DTO shaped for expandable UI

**What:** Report is a list of **groups** (one per rule), each with stable `ruleId`, human title, severity (optional v1; reserve field), and **items** or **child groups** (e.g. dead links grouped by note).

**When to use:** Health tab expandable results (Empty folders → path list; Dead links → note → link tokens). No dedicated route/dialog.

**Trade-offs:** Slightly richer DTO than a flat list, but avoids frontend re-grouping and keeps E2E assertions stable (`ruleId` + ids).

**Example shape:**

```typescript
// Conceptual OpenAPI types after codegen
type NotebookHealthLintReport = {
  groups: HealthFindingGroup[]
}

type HealthFindingGroup = {
  ruleId: "empty_folders" | "dead_wiki_links"
  title: string
  items?: HealthFindingItem[]      // empty folders
  children?: HealthFindingGroup[]  // dead links by note
}

type HealthFindingItem = {
  folderId?: number
  noteId?: number
  label: string           // folder path or note title
  wikiLinkToken?: string  // for dead links
}
```

### Pattern 3: User defaults seed run options (not notebook config)

**What:** Health panel loads user profile defaults into local run options; “Save as defaults” (or auto-save on toggle) PATCHes user. Lint/fix requests send the **run-scoped** options explicitly so a one-off change does not require persist.

**When to use:** Per-user defaults across notebooks (v1).

**Trade-offs:** Users cannot pin different defaults per notebook in v1 (accepted). Defaults affect **suggestion / prefill**, not silent background mutation.

### Pattern 4: Reuse domain algorithms; extend folder lifecycle carefully

**What:** Dead links: `NoteContentMarkdown` + `WikiLinkResolver`. Empty folders: query folder tree + notes with `deleted_at IS NULL`. Fix: **new** hard-delete of folders whose subtree has zero notes — deepest-first or parent-after-children — **not** `DELETE .../folders/{folder}` dissolve.

**When to use:** Any Health rule that overlaps existing graph/wiki behavior.

**Trade-offs:** Must document that dissolve (promote notes/subfolders) and Health empty-folder delete are different operations. Auto-fix must refuse folders that gained notes between lint and fix (re-check emptiness transactionally).

## Data Flow

### Request Flow — Run lint

```
User opens Notebook Settings → Health
    ↓
Panel loads User profile (defaults) → seeds checkboxes
    ↓
User clicks Run (options: which rules / auto-fix prefs for UI gating)
    ↓
POST /api/notebooks/{notebook}/health/lint
    ↓
assertAuthorization(notebook)
    ↓
HealthRuleRunner evaluates EmptyFolderHealthRule + DeadWikiLinkHealthRule
    ↓
NotebookHealthLintReport (nested groups)
    ↓
Health UI renders expandable sections (no navigation away from tab)
```

### Request Flow — Optional fix

```
User selects “Remove empty folders” option (bulk; no multi-select)
    ↓
Fix button enabled only when that option is selected
    ↓
POST /api/notebooks/{notebook}/health/fix  { removeEmptyFolders: true }
    ↓
assertAuthorization(notebook)
    ↓
EmptyFolderBulkFixApplicator re-evaluates emptiness, deletes only empty trees
    ↓
NotebookHealthFixResult (e.g. deletedFolderIds / count)
    ↓
UI refreshes by re-running lint (or returns updated report in fix response)
```

### State Management

```
User (persisted defaults)
    ↓ load once / after save
NotebookHealthPanel local state (run options, last report, busy)
    ↓ Run / Fix
Backend authoritative graph (folders, notes, wiki resolution)
    ↓ report DTO
Expandable findings (ephemeral; not stored)
```

No global Pinia/Vuex store required. Follow existing page/composable patterns (`NotebookPageView` local refs).

### Key Data Flows

1. **Empty folder detection:** Load all folders for notebook → build parent→children map → for each folder, walk subtree; if no non-deleted note has `folder_id` in that set, folder is empty. Report roots or all empty folders as product chooses (recommend listing each empty folder path; fix removes entire empty trees).

2. **Dead wiki links:** For each non-deleted note in notebook, `wikiLinkInnersInOccurrenceOrder(content)` → resolve each distinct token via `WikiLinkResolver` (viewer = notebook owner / current user with write auth). Unresolved tokens become findings under that note. Covers body and frontmatter property values (existing algorithm order: FM first, then body).

3. **Editor dead-link UI vs Health:** Note show uses `wikiTitles` (resolved only) so the editor paints `dead-link` for unresolved tokens. Health **does not** reuse client HTML; it reports the same conceptual set at notebook scale.

4. **User defaults:** Extend `User` / `UserDTO` / `UserController.updateUser` the same way as `spaceIntervals` and `dailyAssimilationCount` — preference columns, not a new microservice.

## Authority decision: backend vs client-side

| Concern | Authority | Why |
|---------|-----------|-----|
| Empty folders (recursive) | **Backend** | Needs full folder tree + note placement; client never loads whole notebook graph for settings |
| Dead wiki links (body + FM) | **Backend** | Must use `WikiLinkResolver` (titles, aliases, notebook-qualified tokens, soft-delete) |
| Bulk delete empty folders | **Backend** | Authorization + transactional re-check; must not use dissolve semantics |
| Expandable presentation | **Client** | Pure view of report DTO |
| Run options UI gating (Fix enabled) | **Client + server** | Client disables Fix; server **must** reject fix without `removeEmptyFolders` |
| Defaults storage | **Backend (User)** | Cross-notebook, logged-in profile |

**Verdict: lint runs backend-authoritative.** Client-side lint is an anti-pattern here.

## Suggested build order (dependencies)

Order is stop-safe and Behavior/Structure friendly:

1. **Findings DTO + `HealthRule` interface + runner skeleton** (Structure) — contract for UI and rules; no behavior yet.
2. **Empty-folder rule (report only)** (Behavior) — first observable Health signal; no UI required if exercised via API/controller tests.
3. **Dead wiki-link rule (report only)** (Behavior) — reuses resolver; independent of empty folders.
4. **Lint API endpoint** (Behavior) — authorized `POST .../health/lint` returns full report.
5. **Health tab + expandable results** (Behavior) — extend `WorkspaceIndexSettingsTabs` + `NotebookPageView`; Run button.
6. **User defaults load/save** (Behavior) — prefill options; PATCH user.
7. **Fix applicator + fix API + gated Fix UI** (Behavior) — only after report path works; Fix enabled only when bulk option selected; server enforces gate.

**Do not** implement fix before empty-folder rule is trustworthy. **Do not** store findings in DB in v1.

## Scaling Considerations

| Scale | Architecture Adjustments |
|-------|--------------------------|
| Small notebooks (typical) | Single-request lint; load folders + notes once per run |
| Large notebooks (10k+ notes) | Batch note content reads; stream or chunk dead-link scan; still one API from UI (server times out only if extreme) |
| Concurrent edits during fix | Re-check emptiness inside transaction; skip non-empty; return what was deleted |

### Scaling Priorities

1. **First bottleneck:** Dead-link rule scanning all note contents — mitigate with single query for note ids/content, in-memory resolve with existing title/alias repos (same as cache refresh patterns).
2. **Second bottleneck:** Premature persistence of findings or background jobs — avoid until UX demands history.

## Anti-Patterns

### Anti-Pattern 1: Client-side notebook lint

**What people do:** Walk notes already in frontend cache / Index content and mark dead links locally.
**Why it's wrong:** Incomplete coverage; diverges from `WikiLinkResolver` / aliases; cannot safely delete folders.
**Do this instead:** Backend `HealthRuleRunner` + report DTO.

### Anti-Pattern 2: Reuse dissolve for empty-folder “delete”

**What people do:** Call existing `DELETE /api/notebooks/{nb}/folders/{folder}` (dissolve) for Health fix.
**Why it's wrong:** Dissolve **promotes** notes and subfolders; it is not “remove empty folder trees.”
**Do this instead:** Dedicated applicator that only removes folders with zero notes in the entire subtree (children folders that are also empty included).

### Anti-Pattern 3: Auto-fix without explicit opt-in

**What people do:** Lint endpoint also deletes when user defaults say auto-fix.
**Why it's wrong:** Violates lint ≠ fix and silent destructive behavior.
**Do this instead:** Separate fix endpoint; Fix control enabled only when bulk option selected for **this run**; defaults only prefill options.

### Anti-Pattern 4: Per-note Health via N× NoteRealm loads

**What people do:** Frontend fetches every `NoteRealm` and diffs tokens vs `wikiTitles`.
**Why it's wrong:** Expensive, races cache refresh, poor UX.
**Do this instead:** One notebook health lint call.

### Anti-Pattern 5: NotebookSettings for user defaults

**What people do:** Add health flags to `NotebookSettings`.
**Why it's wrong:** Product requires per-user defaults across notebooks.
**Do this instead:** Extend `User` / `UserDTO` like other profile preferences.

### Anti-Pattern 6: Findings persistence / `/health` route

**What people do:** Store findings rows or add a dedicated route/dialog.
**Why it's wrong:** Out of scope; ephemeral report on Health tab is enough for v1.
**Do this instead:** Expandable in-tab results from last run in component state.

## Integration Points

### External Services

| Service | Integration Pattern | Notes |
|---------|---------------------|-------|
| None (v1) | — | Mechanical only; no OpenAI / LLM lint |

### Internal Boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| Health UI ↔ Health API | Generated SDK (`apiCallWithLoading`) | User-initiated Run/Fix |
| Health UI ↔ User API | Existing `UserController` profile | Defaults only |
| Rule runner ↔ WikiLinkResolver | Direct Spring injection | Same resolution as editor authority |
| Rule runner ↔ Folder/Note repos | Direct | Soft-delete aware |
| Fix applicator ↔ Folder graph | Direct delete of empty folders | Not dissolve; not note soft-delete |
| Health rules ↔ Note show dead-link UI | Conceptual only | Shared definition of “unresolved token”; separate code paths |
| Health tab ↔ Index/Settings tabs | Shared `WorkspaceIndexSettingsTabs` | Extend tab union; Health panel sibling of settings |

## Sources

- `.planning/PROJECT.md` — Health tab, v1 rules, lint≠fix, per-user defaults, no `/health` route
- `.planning/codebase/ARCHITECTURE.md` — controller → service → entity layers, OpenAPI SDK, auth pattern
- `.planning/codebase/STRUCTURE.md` — where to add controllers/services/Vue components
- `backend/.../services/WikiLinkResolver.java` — authoritative wiki-link resolution
- `backend/.../algorithms/NoteContentMarkdown.java` — frontmatter + body wiki token extraction
- `backend/.../services/WikiTitleCacheService.java` — editor path (resolved titles only); not Health transport
- `backend/.../controllers/NotebookController.java` — folder dissolve semantics (`dissolveFolder`)
- `backend/.../services/FolderRelocationService.java` — folder lifecycle (move/rename/dissolve)
- `backend/.../entities/User.java`, `UserController.java`, `UserDTO.java` — per-user preference pattern
- `frontend/src/pages/NotebookPageView.vue` — Index/Settings tab host
- `frontend/src/components/commons/WorkspaceIndexSettingsTabs.vue` — tab model to extend with `health`
- `frontend/src/components/form/replaceWikiLinksInHtml.ts` / note show — client dead-link rendering (presentation only)

---
*Architecture research for: Notebook Health lint + optional auto-fix*
*Researched: 2026-07-22*
