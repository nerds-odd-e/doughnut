# Phase 4: Dead-link findings - Research

**Researched:** 2026-07-22
**Domain:** Spring Boot notebook Health lint — dead `[[wiki links]]` (body + frontmatter) via editor resolve semantics
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** Dead = a wiki-link **inner** present in note content that does **not** resolve via the same viewer-readable path as editor/cache (`WikiLinkResolver.resolveWikiLinkToken` / equivalent used by `resolveWikiLinksForCache`), with the authorized lint caller as `viewer`. Do **not** invent a second parser/resolver; do **not** treat `note_wiki_title_cache` absence alone as dead.
- **D-02:** Extract inners with `NoteContentMarkdown.wikiLinkInnersInOccurrenceOrder` so **frontmatter scalar/list values and body** are covered in one pass (FM first, then body — existing algorithm order). One rule covers both DLNK-01 and DLNK-02; do not split into two rule ids for v1.
- **D-03:** Scan only **live** (non-soft-deleted) notes in the notebook as link **sources**. Soft-deleted notes are not audited.
- **D-04:** Match editor semantics for targets: aliases, qualified `Notebook:Title`, `|` display text (resolve on target segment), case rules, and soft-deleted / unreadable targets that fail viewer-readable resolve count as dead. Prefer viewer-readable resolve over `resolveAnyTargetWikiLinkToken` so Health matches render-time dead links for the owner running lint.
- **D-05:** Per note, report each **distinct** unresolved inner **once** (dedupe preserving first-occurrence order), matching `WikiLinkResolver` cache dedupe — not one finding per textual occurrence.
- **D-06:** Each leaf `HealthFindingItem` sets `noteId` (source note), `wikiLinkToken` = the extracted **inner** (no surrounding `[[ ]]`), and `label` = that same inner string. Optional `message` is not required for v1 (no body-vs-frontmatter split in the message).
- **D-07:** Do not set `folderId` on dead-link items. `autoFixable` stays group-level only (`false` on this rule).
- **D-08:** Top group: `ruleId` = `dead_wiki_links` (`HealthRuleIds.DEAD_WIKI_LINKS`), title “Dead wiki links”, severity `warning`, `autoFixable` = **`false`**, top-level `items` empty/null, **`children`** = one child group per source note that has ≥1 dead link.
- **D-09:** Each child group: `title` = note topic/title; `ruleId` = `dead_wiki_links` (same stable id; nesting + `noteId` on items identify the note); severity `warning`; `autoFixable` = `false`; `items` = that note’s dead-link leaf findings; no further nesting.
- **D-10:** Always emit the top `dead_wiki_links` group (metadata + `children`, possibly empty), consistent with Phase 2/3 always-emit groups.
- **D-11:** Add a Spring `HealthRule` bean (e.g. `DeadWikiLinkHealthRule`) discovered by existing `HealthRuleRunner`. Reuse authorized `POST .../health/lint` — no new endpoint, no request-body options, no frontend.
- **D-12:** No OpenAPI / TypeScript client regen unless a wire schema change appears (report DTOs already support `children` + `wikiLinkToken`). Verify lint response can include the nested dead-link group alongside existing folder groups via backend tests.
- **D-13:** Prove with focused backend tests: body dead link; frontmatter/property dead link; alias and qualified `Notebook:Title` resolve as live (not false-positive); soft-deleted source notes excluded; soft-deleted / missing targets reported dead; distinct-token dedupe; nested-by-note children shape; `autoFixable=false`; no mutation. No `@wip` E2E / Health UI in this phase.

### Claude's Discretion
- Exact batching strategy for resolving many notes (per-note resolve vs shared notebook index) — keep correct vs editor; avoid N+1 where a simple batch is obvious.
- Child group field mirroring (whether children copy severity/autoFixable explicitly) — follow DTO constructors used by other rules; keep UI-ready.
- Whether `label` ever shows only the pre-`|` target for display — default is full inner; change only if tests against editor UX strongly prefer target-only.

### Deferred Ideas (OUT OF SCOPE)
- Health tab UI / Run / expandable nesting presentation — Phase 5
- Click-through from dead-link finding to editor focus — v2 (HLTH-11)
- Assisted / automatic dead-link repair — v2 (DLNK-10) / out of v1
- User-level lint defaults — Phase 6
- Bulk empty-folder purge — Phase 7
- Body-vs-frontmatter split in finding `message` — rejected for v1 noise; revisit only if Phase 5 UI needs a cue
- Using `resolveAnyTargetWikiLinkToken` for “exists but unreadable” nuance — rejected; match editor dead-link for the lint caller

None — discussion stayed within phase scope
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| DLNK-01 | Lint reports dead `[[wiki links]]` in note **body** using the same resolve semantics as the editor (including aliases and qualified `Notebook:Title` links) | `NoteContentMarkdown.wikiLinkInnersInOccurrenceOrder` extracts body inners; dead = missing from viewer-readable `WikiLinkResolver` path; prove with body fixture + live alias / qualified-link fixtures |
| DLNK-02 | Lint reports dead wiki links in note **frontmatter / properties** with the same resolve semantics | Same extract method covers FM scalars/lists first; one rule id; prove with FM property fixture (no separate rule) |
| DLNK-03 | Dead-link findings are **report-only** in v1 (no auto-fix / rewrite) | Group `autoFixable=false`; no fix endpoint; assert no content/folder mutation after lint |
</phase_requirements>

## Summary

Phases 1–3 already provide the Health lint contract (`HealthFindingGroup.children`, `HealthFindingItem.wikiLinkToken` / `noteId`), reserved `HealthRuleIds.DEAD_WIKI_LINKS`, authorized `POST /api/notebooks/{notebook}/health/lint`, and always-emit folder rules. Phase 4 is a single **Behavior** slice: register `DeadWikiLinkHealthRule` that scans **live** notes, extracts wiki-link inners with the **same** algorithm as the editor cache path, classifies unresolved tokens with **viewer-readable** `WikiLinkResolver`, and nests findings **by note** under `children`. Report-only: `autoFixable=false`, no rewrite, no UI.

The resolve authority already exists end-to-end in `WikiLinkResolver.resolveWikiLinksForCache` (extract → `dedupePreserveOrder` via `FrontmatterAliases.normalizedLookupKey` → `resolveToken` with viewer). Prefer a **single new public method** on `WikiLinkResolver` that returns unresolved inners using that same private loop (do not reimplement extraction or `|` / alias / qualified parsing in the Health rule). Wire the authorized lint caller as `viewer` through `HealthRunContext` (today empty). Load live source notes with one notebook-scoped repository query (`deletedAt IS NULL`); no live-notes-by-notebook query exists yet.

**Primary recommendation:** Add `WikiLinkResolver.unresolvedWikiLinkTokens(Note, User)` (or equivalent name) sharing the cache-resolve loop; extend `HealthRunContext` with `viewer`; add `NoteRepository.findLiveNotesByNotebookIdOrderByIdAsc`; implement `@Service DeadWikiLinkHealthRule`; prove DLNK-01/02/03 with `DeadWikiLinkHealthRuleTest` via `NotebookHealthService.lint` — no OpenAPI regen, no E2E/UI.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Extract `[[…]]` from body + frontmatter | API / Backend | — | Pure algorithm already in `NoteContentMarkdown` / `WikiLinkMarkdown` |
| Resolve token (aliases, qualified, `\|`, readability) | API / Backend | Database / Storage | `WikiLinkResolver` + title/alias indexes; must match editor |
| Classify dead vs live for Health report | API / Backend | — | Invert resolved set; never cache-as-truth |
| Nest findings by source note | API / Backend | — | DTO `children` reserved in Phase 1 for Phase 5 UI |
| Authorized lint transport | API / Backend | — | Existing write-auth controller; pass current user as viewer |
| Health UI expandable dead links | Browser / Client | — | **Out of phase** (Phase 5) |
| Dead-link repair / rewrite | — | — | **Out of scope** (DLNK-10 / Phase 7 N/A) |

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot (existing) | repo-managed | `@Service` `HealthRule` bean | `HealthRuleRunner` injects `List<HealthRule>` [VERIFIED: `HealthRuleRunner.java`] |
| Spring Data JPA | repo-managed | Live notes by notebook | Same pattern as `findLiveNoteFolderIdsByNotebookId` [VERIFIED: `NoteRepository.java`] |
| `WikiLinkResolver` | in-repo | Viewer-readable resolve authority | Editor / cache path [VERIFIED: `WikiLinkResolver.java`] |
| `NoteContentMarkdown` | in-repo | FM + body wiki inners | Single extract for DLNK-01 + DLNK-02 [VERIFIED: `NoteContentMarkdown.java`] |
| JUnit 5 + `@SpringBootTest` | repo-managed | Rule integration tests | Mirror Phase 2/3 `*HealthRuleTest` |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Hamcrest | existing | Readable assertions | Match folder-rule tests |
| MakeMe `NoteBuilder` | existing | Body/FM content, `softDeleted()`, aliases via content + `NoteAliasIndexService` | Same fixtures as `WikiLinkResolverYamlAndBodyIntegrationTest` |
| `FrontmatterAliases.normalizedLookupKey` | in-repo | Dedupe identity (if exposed only via resolver method) | Must match cache dedupe [VERIFIED: `WikiLinkResolver.dedupePreserveOrder`] |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| New method on `WikiLinkResolver` for unresolved tokens | Rule calls `resolveWikiLinksForCache` + re-extract + re-dedupe | Works if dedupe is identical, but **duplicates** extract/dedupe responsibility; drift risk |
| `resolveAnyTargetWikiLinkToken` | Viewer-readable resolve | Rejected (D-04) — would hide “exists but unreadable” as live when editor shows dead |
| Cache row absence = dead | Live resolve | Rejected (D-01 / PITFALLS) — cache stores resolved only; staleness |
| Client-side dead-link CSS scan | Backend rule | Rejected — incomplete notebook graph; second linker |
| Two rule ids (body vs FM) | One `dead_wiki_links` | Rejected (D-02) — one extract pass |

**Installation:** None — no new packages.

**Version verification:** N/A (no new dependencies). Stack is the existing Doughnut backend.

## Package Legitimacy Audit

> No external packages are installed in this phase.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| — | — | — | — | — | — | N/A |

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```
Authorized owner (lint caller = viewer)
    │
    ▼
POST /api/notebooks/{notebook}/health/lint   (existing; unchanged path)
    │
    ▼
NotebookHealthController
    ├── assertAuthorization(notebook)
    └── HealthRunContext(viewer = authorizationService.getCurrentUser())
    │
    ▼
NotebookHealthService.lint → HealthRuleRunner.run
    │
    ├── EmptyFolderHealthRule          (unchanged)
    ├── ReadmeOnlyFolderHealthRule     (unchanged)
    └── DeadWikiLinkHealthRule         (THIS PHASE)
          ├── load live notes (deletedAt IS NULL) for notebook
          ├── for each note:
          │     WikiLinkResolver.unresolvedWikiLinkTokens(note, viewer)
          │       = wikiLinkInnersInOccurrenceOrder
          │         → dedupePreserveOrder(normalizedLookupKey)
          │         → resolveToken(viewer) == null ⇒ dead
          └── build top group + children (one per note with ≥1 dead token)
    │
    ▼
NotebookHealthLintReport {
  groups: [
    empty_folders…,
    readme_only_folders…,
    dead_wiki_links { children: [ { title: noteTitle, items: [{noteId, wikiLinkToken, label}] } ] }
  ]
}
    │
    └── no DB writes / no content rewrite
```

### Recommended Project Structure

```
backend/src/main/java/com/odde/doughnut/
├── entities/repositories/
│   └── NoteRepository.java              # ADD findLiveNotesByNotebookIdOrderByIdAsc
├── services/
│   ├── WikiLinkResolver.java            # ADD unresolvedWikiLinkTokens(Note, User)
│   ├── NotebookHealthService.java       # unchanged API
│   └── health/
│       ├── HealthRunContext.java        # ADD viewer User
│       ├── HealthRuleIds.java           # DEAD_WIKI_LINKS already reserved
│       ├── EmptyFolderHealthRule.java   # unchanged (ignores viewer)
│       ├── ReadmeOnlyFolderHealthRule.java  # unchanged
│       └── DeadWikiLinkHealthRule.java  # NEW
└── controllers/
    └── NotebookHealthController.java    # pass getCurrentUser() into HealthRunContext

backend/src/test/java/com/odde/doughnut/
├── services/health/
│   └── DeadWikiLinkHealthRuleTest.java  # NEW — DLNK-01/02/03 via NotebookHealthService.lint
└── controllers/
    └── NotebookHealthControllerTest.java  # optional: assert dead_wiki_links group present; auth tests stay
```

Capability names in product code: `DeadWikiLinkHealthRule`, `dead_wiki_links` — **no phase numbers**.

### Pattern 1: Unresolved tokens from WikiLinkResolver (no second linker)

**What:** Add a public method that mirrors `resolveWikiLinksForCache` but collects tokens where `resolveToken` returns null, using the same extract + `dedupePreserveOrder` private helpers.

**When to use:** Always for Health dead-link classification (D-01, D-05).

**Example:**

```java
// Source: WikiLinkResolver.java (live resolveWikiLinksForCache loop — invert)
public List<String> unresolvedWikiLinkTokens(Note focusNote, User viewer) {
  String content = focusNote.getContent();
  if (content == null || content.isBlank()) {
    return List.of();
  }
  List<String> linkTitlesOrdered = NoteContentMarkdown.wikiLinkInnersInOccurrenceOrder(content);
  if (linkTitlesOrdered.isEmpty()) {
    return List.of();
  }
  List<String> dead = new ArrayList<>();
  for (String token : dedupePreserveOrder(linkTitlesOrdered)) {
    if (resolveToken(token, viewer, focusNote) == null) {
      dead.add(token);
    }
  }
  return List.copyOf(dead);
}
```

`wikiLinkToken` / `label` = full inner string (e.g. `Alpha|friendly alias`), matching `ResolvedWikiLink.linkText` — not target-only (D-06 + discretion default).

`WikiLinkTargetReference.forToken` already applies `WikiLinkMarkdown.splitInner(token).target()` before qualified/unqualified parse — `|` display text is handled inside resolve, not in the Health rule. [VERIFIED: `WikiLinkTargetReference.java`]

### Pattern 2: Viewer on HealthRunContext

**What:** `HealthRunContext` currently has no fields. Dead-link resolve requires `User viewer` = authorized lint caller (D-01).

**When to use:** Controller constructs context after `assertAuthorization`.

**Example:**

```java
// Source: NotebookController pattern for getCurrentUser + Phase 4 CONTEXT D-01
authorizationService.assertAuthorization(notebook);
return notebookHealthService.lint(
    notebook, new HealthRunContext(authorizationService.getCurrentUser()));
```

```java
public final class HealthRunContext {
  private final User viewer;

  public HealthRunContext(User viewer) {
    this.viewer = viewer;
  }

  public User viewer() {
    return viewer;
  }
}
```

Update existing call sites (`EmptyFolderHealthRuleTest`, `ReadmeOnlyFolderHealthRuleTest`, `HealthRuleRunnerTest`) to pass a `User` (notebook owner is fine for folder-only tests). Folder rules continue to ignore `viewer`.

Do **not** implement viewer as “notebook ownership user only” in the rule while the controller session user differs — for v1 write-auth, they are the same actor, but context must carry the **caller**.

### Pattern 3: Live source notes only

**What:** One query for notes with `n.notebook.id = :notebookId AND n.deletedAt IS NULL ORDER BY n.id ASC`. Soft-deleted sources are never scanned (D-03).

**When to use:** Always.

**Example:**

```java
// Source: NoteRepository live-note patterns (findLiveNoteFolderIdsByNotebookId)
@Query(
    """
    SELECT n FROM Note n
    WHERE n.notebook.id = :notebookId
      AND n.deletedAt IS NULL
    ORDER BY n.id ASC
    """)
List<Note> findLiveNotesByNotebookIdOrderByIdAsc(@Param("notebookId") Integer notebookId);
```

Do **not** walk folders and call `findNotesInFolderOrderByIdAsc` per folder (N+1). Do **not** use `findNotesInNotebookRootFolderScopeByNotebookId` (root-only, misses notes in folders).

### Pattern 4: Nested report shape (always-emit top group)

**What:** Top group metadata + `children`; leaf items only under children.

**When to use:** Always (D-08..D-10).

**Example:**

```java
// Source: HealthFindingGroup / Phase 4 CONTEXT D-08..D-10
HealthFindingGroup top = new HealthFindingGroup();
top.setRuleId(HealthRuleIds.DEAD_WIKI_LINKS);
top.setTitle("Dead wiki links");
top.setSeverity(HealthSeverity.warning);
top.setAutoFixable(false);
// items: leave null or empty list — never put leaves on the top group
List<HealthFindingGroup> children = new ArrayList<>();
for (Note note : liveNotes) {
  List<String> deadTokens = wikiLinkResolver.unresolvedWikiLinkTokens(note, context.viewer());
  if (deadTokens.isEmpty()) {
    continue;
  }
  HealthFindingGroup child = new HealthFindingGroup();
  child.setRuleId(HealthRuleIds.DEAD_WIKI_LINKS);
  child.setTitle(note.getTitle());
  child.setSeverity(HealthSeverity.warning);
  child.setAutoFixable(false);
  child.setItems(
      deadTokens.stream()
          .map(
              token -> {
                HealthFindingItem item = new HealthFindingItem();
                item.setNoteId(note.getId());
                item.setWikiLinkToken(token);
                item.setLabel(token);
                // no folderId, no message required
                return item;
              })
          .toList());
  children.add(child);
}
top.setChildren(children);
return top;
```

Child group field mirroring: set `ruleId`, `severity`, `autoFixable` on children explicitly (UI-ready; matches `HealthRuleRunnerTest` nested fixture style).

### Pattern 5: Spring discovers the rule automatically

**What:** `@Service class DeadWikiLinkHealthRule implements HealthRule` — no `HealthRuleRunner` edits.

**When to use:** Every new Health rule (same as Phase 2/3).

Tests look up groups by `HealthRuleIds.DEAD_WIKI_LINKS`, never `groups.get(N)`.

### Anti-Patterns to Avoid

- **Second `[[…]]` regex or title-only lookup in the Health package:** False positives on aliases / qualified / `|` (PITFALLS #5).
- **`note_wiki_title_cache` as truth:** Stale or never-refreshed rows (PITFALLS #6).
- **`resolveAnyTargetWikiLinkToken`:** Diverges from editor dead-link for unreadable targets (D-04).
- **Auditing soft-deleted source notes:** Violates D-03.
- **One finding per occurrence without normalized dedupe:** Violates D-05 / cache semantics.
- **Top-level flat `items` only:** Phase 5 expects per-note `children` (D-08).
- **`autoFixable=true`:** Phase 7 / fix UI must never treat dead links as fixable (DLNK-03).
- **OpenAPI regen / Health UI / E2E:** Out of scope (D-11, D-13).
- **Phase numbers in class/test/feature names:** Capability naming only (`DeadWikiLink…`).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Wiki-link extraction | Custom regex over content | `NoteContentMarkdown.wikiLinkInnersInOccurrenceOrder` | FM scalars/lists + body order already correct [VERIFIED: `NoteContentMarkdown.java`] |
| Token resolve | Title-in-notebook map | `WikiLinkResolver` viewer-readable path | Aliases, qualified names, auth, soft-delete targets [VERIFIED: `WikiLinkResolver.java`] |
| `\|` display handling | Manual split in Health rule | `WikiLinkTargetReference.forToken` via resolver | Already splits on target segment [VERIFIED: `WikiLinkTargetReference.java`] |
| Dedupe | `HashSet` of raw strings | Resolver `dedupePreserveOrder` / `FrontmatterAliases.normalizedLookupKey` | Case/NFKC-normalized identity matches cache [VERIFIED: `WikiLinkResolver.java`] |
| Dead = cache miss | Query `note_wiki_title_cache` | Live unresolved list | Cache is resolved-only optimization [VERIFIED: milestone PITFALLS] |
| Rule registration | Manual runner list edit | Spring `@Service` + `List<HealthRule>` | Already wired |
| Live notes enumeration | Folder walk / root-only query | New `findLiveNotesByNotebookIdOrderByIdAsc` | Single notebook-scoped live set |
| Lint HTTP | New endpoint | Existing `NotebookHealthController.lint` | D-11 |
| TS client update | Hand-edit / regen “just in case” | Skip unless DTO annotations change | D-12 |

**Key insight:** Phase 4 risk is **semantic drift from the editor**, not DTO plumbing. Keep extract + resolve + dedupe inside `WikiLinkResolver`; Health only maps unresolved tokens into nested findings.

## Common Pitfalls

### Pitfall 1: Second resolver / cache-as-truth
**What goes wrong:** Health flags live alias or qualified links as dead (or misses real dead links).
**Why it happens:** Ad-hoc title scan or “token not in `wikiTitles` cache.”
**How to avoid:** Only `WikiLinkResolver` viewer-readable unresolved list; never invent a second path (D-01).
**Warning signs:** Tests pass only for unqualified titles; alias/qualified fixtures fail.

### Pitfall 2: Soft-deleted sources audited
**What goes wrong:** Soft-deleted notes contribute dead-link noise.
**Why it happens:** Loading all notes for notebook without `deletedAt IS NULL`.
**How to avoid:** Live-notes-only query (D-03); fixture soft-deletes a linker and asserts no child for it.
**Warning signs:** Child group for a soft-deleted note id.

### Pitfall 3: Soft-deleted / missing targets not reported
**What goes wrong:** Links to deleted or absent titles appear healthy.
**Why it happens:** Resolver already excludes `deletedAt IS NULL` targets — but Health might short-circuit if it treats “any historical title” as live.
**How to avoid:** Rely on live resolve; fixture a link to a soft-deleted target and assert dead.
**Warning signs:** `findByNotebookNameAndNoteTitleOrderByIdAsc` filters deleted notes [VERIFIED: `NoteRepository.java`].

### Pitfall 4: Alias / qualified false positives
**What goes wrong:** `[[color]]` alias or `[[Other Notebook:Title]]` reported dead for owner.
**Why it happens:** Current-notebook title-only lookup.
**How to avoid:** Resolver alias index + qualified parse (reuse existing integration fixtures patterns from `WikiLinkResolverYamlAndBodyIntegrationTest`).
**Warning signs:** Health fails where `resolveWikiLinksForCache` succeeds for same viewer.

### Pitfall 5: `|` display text used as resolve key incorrectly
**What goes wrong:** `[[Alpha|friendly]]` reported dead because display text looked up, or `wikiLinkToken` is only `Alpha`.
**Why it happens:** Hand-rolled split.
**How to avoid:** Resolver owns `|` via `WikiLinkTargetReference`; store **full inner** as `wikiLinkToken` / `label` (D-06).
**Warning signs:** Token mismatches `ResolvedWikiLink.linkText` for pipe links.

### Pitfall 6: Frontmatter links missed
**What goes wrong:** Body-only scan implements DLNK-01 but not DLNK-02.
**Why it happens:** Using `WikiLinkMarkdown.innerTitlesInOccurrenceOrder(body)` alone.
**How to avoid:** Always `NoteContentMarkdown.wikiLinkInnersInOccurrenceOrder` (D-02).
**Warning signs:** FM `parent: "[[Missing]]"` never appears in findings.

### Pitfall 7: Occurrence spam without dedupe
**What goes wrong:** `[[Missing]]` three times → three leaf items.
**Why it happens:** Iterating every occurrence.
**How to avoid:** Same `dedupePreserveOrder` as cache (D-05); first-occurrence string wins.
**Warning signs:** Item count > distinct normalized tokens.

### Pitfall 8: Missing viewer on context
**What goes wrong:** NPE or wrong readability decisions.
**Why it happens:** `HealthRunContext` is empty today.
**How to avoid:** Required `viewer` on context; controller passes `getCurrentUser()`.
**Warning signs:** Rule tests that never set a user.

### Pitfall 9: Flat items instead of children
**What goes wrong:** Phase 5 UI cannot nest by note.
**Why it happens:** Copy-paste from folder rules (`setItems` on top group).
**How to avoid:** D-08/D-09; assert `children` non-null and top `items` empty/null.
**Warning signs:** Dead tokens only on top-level `items`.

### Pitfall 10: Performance N+1 (discretion)
**What goes wrong:** Large notebooks time out on Health run.
**Why it happens:** Per-link or per-folder queries without reuse.
**How to avoid (v1):** Load all live notes once; per note call resolver (same cost class as notebook-wide cache rebuild). Do **not** build a speculative shared title index unless profiling shows need — correctness first (discretion). Optional later: batch candidates by notebook name.
**Warning signs:** Per-token custom repository calls outside `WikiLinkResolver`.

## Code Examples

Verified patterns from the live codebase:

### Extract authority (body + frontmatter)

```java
// Source: NoteContentMarkdown.java
public static List<String> wikiLinkInnersInOccurrenceOrder(String content) {
  // frontmatter supportedValueStringsInInsertionOrder → WikiLinkMarkdown.innerTitles…
  // then body WikiLinkMarkdown.innerTitlesInOccurrenceOrder
}
```

### Resolve authority (viewer-readable; not any-target)

```java
// Source: WikiLinkResolver.java
public Optional<Note> resolveWikiLinkToken(String token, Note focusNote, User viewer) {
  return Optional.ofNullable(resolveToken(token, viewer, focusNote));
}

// DO NOT use for Health dead-link classification:
// public Optional<Note> resolveAnyTargetWikiLinkToken(String token, Note focusNote)
```

### Cache loop to invert (dedupe + resolve)

```java
// Source: WikiLinkResolver.resolveWikiLinksForCache
for (String token : dedupePreserveOrder(linkTitlesOrdered)) {
  Note target = resolveToken(token, viewer, focusNote);
  if (target != null) {
    out.add(new ResolvedWikiLink(token, target));
  }
}
// Dead = same loop, collect when target == null
```

### Existing folder rule always-emit pattern

```java
// Source: EmptyFolderHealthRule.java / ReadmeOnlyFolderHealthRule.java
HealthFindingGroup group = new HealthFindingGroup();
group.setRuleId(id());
group.setTitle(title());
group.setSeverity(severity());
group.setAutoFixable(autoFixable());
// folder rules set items; dead links set children instead
```

### Service-level test entry (Phase 2/3 style)

```java
// Source: EmptyFolderHealthRuleTest.java
NotebookHealthLintReport report =
    notebookHealthService.lint(notebook, new HealthRunContext(owner));
HealthFindingGroup group =
    report.getGroups().stream()
        .filter(g -> HealthRuleIds.DEAD_WIKI_LINKS.equals(g.getRuleId()))
        .findFirst()
        .orElseThrow();
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Client-only dead-link CSS on loaded notes | Backend notebook-wide Health rule | Phase 4 | Full notebook audit matching editor resolve |
| Cache absence as dead | Live resolve unresolved tokens | Phase 4 decisions | No phantom dead links from stale cache |
| Flat findings list | Nested `children` by note | Phase 1 contract | Phase 5 expandable UI ready |

**Deprecated/outdated:**
- Treating `indexContent` language from early research as product field — use **readme** / `readmeContent` for folders; wiki links live in **note `content`** only.
- Hand-rolled wiki linkers in Health — forbidden.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| — | *(none material)* | — | All critical APIs verified in-repo |

Notes:
- Batching optimization beyond per-note resolver calls is discretionary and not required for correctness [ASSUMED: acceptable latency for typical notebook sizes on on-demand lint].
- Spring `List<HealthRule>` order is undefined without `@Order` — tests must filter by `ruleId` [VERIFIED: Phase 2/3 practice].

## Open Questions

1. **Should `WikiLinkResolver` gain `unresolvedWikiLinkTokens` vs invert `resolveWikiLinksForCache` in the rule?**
   - What we know: Both can be correct if extract/dedupe match.
   - What's unclear: Naming preference only.
   - Recommendation: **Add `unresolvedWikiLinkTokens`** (or `unresolvedWikiLinkInners`) on `WikiLinkResolver` so Health cannot drift.

2. **HealthRunContext constructor breakage for existing tests**
   - What we know: All current call sites use `new HealthRunContext()`.
   - What's unclear: None — mechanical update.
   - Recommendation: Require `User viewer`; update folder-rule tests to pass notebook owner.

## Environment Availability

> Phase depends on existing backend/MySQL test stack only (no new external tools).

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| MySQL (test profile) | Live notes + resolve queries | ✓ (repo Nix/`pnpm sut`) | repo-managed | — |
| Spring Boot test context | `@SpringBootTest` rule tests | ✓ | repo-managed | — |
| `NoteAliasIndexService` | Alias fixtures in tests | ✓ | in-repo | — |

**Missing dependencies with no fallback:** none

**Missing dependencies with fallback:** none

Step 2.6: no new external CLIs; use `CURSOR_DEV=true nix develop -c pnpm backend:test_only` for verification.

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Spring Boot Test (`@SpringBootTest`, `@ActiveProfiles("test")`, `@Transactional`) |
| Config file | Spring test profile (existing) |
| Quick run command | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` |
| Full suite command | `CURSOR_DEV=true nix develop -c pnpm backend:verify` (only if migration/format gates needed; prefer `backend:test_only` for this phase) |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| DLNK-01 | Body dead link reported; live alias / qualified `Notebook:Title` not reported | integration (service) | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` | ❌ Wave 0 — `DeadWikiLinkHealthRuleTest` |
| DLNK-02 | Frontmatter/property dead link reported | integration (service) | same | ❌ Wave 0 — same class |
| DLNK-03 | `autoFixable=false`; no mutation (content/folder counts unchanged) | integration (service) | same | ❌ Wave 0 — same class |
| (contract) | Nested children by note; distinct-token dedupe; soft-deleted source excluded; soft-deleted/missing targets dead; always-emit empty children | integration (service) | same | ❌ Wave 0 |
| (coexist) | Folder groups still present alongside dead_wiki_links | integration (service) | same | ❌ Wave 0 — assert by ruleId |
| (auth) | Foreign/anon still rejected | controller | existing `NotebookHealthControllerTest` | ✅ |

### Sampling Rate

- **Per task commit:** `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
- **Per wave merge:** same (backend-only phase)
- **Phase gate:** All backend tests green; no `@wip` E2E required

### Wave 0 Gaps

- [ ] `backend/src/test/java/com/odde/doughnut/services/health/DeadWikiLinkHealthRuleTest.java` — covers DLNK-01, DLNK-02, DLNK-03 and D-13 scenarios
- [ ] Update call sites of `new HealthRunContext()` to pass `User` once context requires viewer
- [ ] Optional: extend `NotebookHealthControllerTest` with one smoke that `dead_wiki_links` group appears for owner (not required if service tests cover shape; auth already covered)

*(No new test framework install.)*

### Recommended test cases (D-13 checklist)

| Test | Setup | Assert |
|------|-------|--------|
| Body dead link | Live note content `See [[Missing]]` | Child for note; item `wikiLinkToken=Missing`, `noteId` set |
| FM dead link | Content `---\nparent: "[[Missing]]"\n---\n\nbody` | Same token reported (DLNK-02) |
| Live body link not reported | Target note exists; linker `[[Alpha]]` | No dead item for `Alpha` |
| Alias live | Target with `aliases: [color]`; linker `[[color]]`; refresh alias index | Not dead |
| Qualified live | Target in other notebook owner can read; `[[Other Notebook:Title]]` | Not dead |
| Soft-deleted source excluded | Soft-deleted note with `[[Missing]]` | No child for that note |
| Soft-deleted target dead | Linker to soft-deleted title | Dead |
| Missing target dead | No matching title/alias | Dead |
| Dedupe | `[[Missing]]` twice | One leaf item |
| Nested shape | Two notes with different dead tokens | Two children; top `autoFixable=false`; top items empty/null |
| Always emit | Notebook with no wiki links | Top group present, `children` empty |
| No mutation | Count notes/folders/content hashes before/after lint | Unchanged |
| Coexistence | Empty folder + dead link in same notebook | Both `empty_folders` and `dead_wiki_links` groups present |

Fixture patterns: copy from `WikiLinkResolverYamlAndBodyIntegrationTest` for alias/qualified/`|` cases; use `makeMe.aNote().softDeleted()` for soft-delete; use `NoteAliasIndexService.refreshForNote` after alias frontmatter.

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes (session) | Existing session-scoped controller + `AuthorizationService` |
| V3 Session Management | no new surface | Existing Spring session |
| V4 Access Control | yes | `assertAuthorization(notebook)` write auth on lint; viewer-readable resolve for targets |
| V5 Input Validation | limited | Notebook id path binding only; wiki tokens extracted from owned note content, not request body |
| V6 Cryptography | no | — |

### Known Threat Patterns for Health dead-link lint

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Foreign user enumerates dead links in another notebook | Information disclosure | Existing write-auth gate (Phase 2 controller tests) |
| Unreadable target treated as “exists” / leak title | Information disclosure | Viewer-readable resolve only (D-04); do not use `resolveAnyTargetWikiLinkToken` |
| Lint mutates content under report-only API | Tampering | `@Transactional(readOnly = true)` on controller; no rewrite service calls |
| Malicious wiki tokens in note content | — | Tokens are author-owned content; resolve uses parameterized JPA queries (no SQL concat of token) |

`security_enforcement`: enabled in `.planning/config.json` — no elevated risk beyond existing notebook write APIs.

## Project Constraints (from .cursor/rules/)

| Rule | Directive for this phase |
|------|---------------------------|
| `planning.mdc` | Behavior phase; one observable behavior; stop-safe; capability names (no phase numbers in product code/tests); targeted tests; commit gate is orchestrator/execute-plan concern |
| `gsd-coexistence.mdc` | Local Behavior/Structure grammar + Nix prefix for tooling |
| `backend-code.mdc` | Imports at top; no new DTO if existing shapes suffice; run `pnpm backend:test_only` (no migration ⇒ no `backend:verify` required) |
| `backend-testing.mdc` | Behavior via stable entry (`NotebookHealthService.lint` / controller); MakeMe fixtures; `@Transactional`; do not mirror one test class per trivial method; prefer descriptive behavior names |
| `general.mdc` | `CURSOR_DEV=true nix develop -c …` for tooling; git without Nix |
| `agent-map.mdc` | No OpenAPI regen unless controller/DTO signatures change; `packages/generated/**` not hand-edited |

## Implementation Approach (planner-ready)

### Minimal file change set

1. **`HealthRunContext`** — hold `User viewer` (required).
2. **`NotebookHealthController`** — `new HealthRunContext(authorizationService.getCurrentUser())`.
3. **`NoteRepository`** — `findLiveNotesByNotebookIdOrderByIdAsc`.
4. **`WikiLinkResolver`** — `unresolvedWikiLinkTokens(Note, User)` sharing private extract/dedupe/resolve loop with `resolveWikiLinksForCache`.
5. **`DeadWikiLinkHealthRule`** — `@Service` implementing `HealthRule`; build nested report per D-08..D-10.
6. **Tests** — `DeadWikiLinkHealthRuleTest` + update existing `HealthRunContext` constructions.

### What not to touch

- `HealthRuleRunner` (auto-discovers beans)
- Folder rules / `FolderSubtreeLiveNotes`
- OpenAPI / frontend / E2E
- `WikiLinkRewriteService` / fix endpoints
- `NoteWikiTitleCache` / `WikiTitleCacheService` as classification authority

### Suggested plan granularity (~5 min slices)

| Slice | Behavior delivered |
|-------|-------------------|
| A | `HealthRunContext(viewer)` + call-site updates; tests compile / folder rules still green |
| B | `unresolvedWikiLinkTokens` + focused resolver unit/integration assert (optional if rule tests cover) |
| C | Live-notes query + `DeadWikiLinkHealthRule` + full `DeadWikiLinkHealthRuleTest` (DLNK-01/02/03) |

Prefer **one Behavior plan** if execution stays under time budget; split only if WIP exceeds ~10 minutes.

## Sources

### Primary (HIGH confidence)

- `backend/src/main/java/com/odde/doughnut/services/WikiLinkResolver.java` — resolve / cache / dedupe
- `backend/src/main/java/com/odde/doughnut/algorithms/NoteContentMarkdown.java` — FM + body extract
- `backend/src/main/java/com/odde/doughnut/algorithms/WikiLinkTargetReference.java` — `|` + qualified parse
- `backend/src/main/java/com/odde/doughnut/services/health/*` — rule registry + folder rule patterns
- `backend/src/test/java/com/odde/doughnut/services/WikiLinkResolverYamlAndBodyIntegrationTest.java` — alias / qualified / pipe fixtures
- `.planning/phases/04-dead-link-findings/04-CONTEXT.md` — locked decisions
- `.planning/research/ARCHITECTURE.md`, `PITFALLS.md`, `STACK.md` — milestone guidance

### Secondary (MEDIUM confidence)

- Spring Framework 6.2 autowired collections — [CITED: docs.spring.io/spring-framework/reference/6.2/core/beans/annotation-config/autowired.html]

### Tertiary (LOW confidence)

- Notebook-size latency of per-note resolve without extra batching — acceptable for on-demand v1 [ASSUMED]

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — no new libraries; all APIs in-repo
- Architecture: HIGH — mirrors Phase 2/3 rule bean + existing resolver
- Pitfalls: HIGH — milestone PITFALLS + verified code paths

**Research date:** 2026-07-22
**Valid until:** 2026-08-22 (stable domain; invalidate if `WikiLinkResolver` or Health DTO contract changes)

## RESEARCH COMPLETE
