# Phase 7: Gated empty-folder purge - Research

**Researched:** 2026-07-22
**Domain:** Notebook Health gated bulk purge (Spring Boot + Vue + OpenAPI)
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

#### Fix control (AFIX-02, AFIX-03)
- **D-01:** Add a **Fix** control on the Health **action bar** (same bar as Run lint, Remove empty folders checkbox, Save as defaults). No per-folder multi-select and no second fix menu.
- **D-02:** Fix is **enabled only when** the run-scoped **Remove empty folders** checkbox is checked **and** the last successful lint report has **≥1** item in the `empty_folders` group (findings list = preview). Otherwise Fix is **disabled** (visible but not active) so the gate is discoverable.
- **D-03:** Button copy prefers explicit language with count when known: **“Remove N empty folders”** (N = `empty_folders` item count from last report). Fall back to **“Remove empty folders”** if count unavailable. Do **not** use a generic “Apply” / “Fix” alone as the only label.
- **D-04:** No extra confirm dialog in v1 — consent is the checked bulk option plus the findings preview. (Server still re-validates emptiness.)

#### API / server gate (AFIX-03, AFIX-05)
- **D-05:** Dedicated fix endpoint on `NotebookHealthController`: **`POST /api/notebooks/{notebook}/health/fix`** with body requiring **`removeEmptyFolders: true`**. Do **not** overload bodyless `POST .../health/lint` with mutation. Do **not** send user defaults on the fix request — only the explicit run opt-in flag.
- **D-06:** Authorize with **`assertAuthorization(notebook)`** (owner/write), same gate as lint. Foreign/anon callers rejected.
- **D-07:** Server **must** reject the fix when `removeEmptyFolders` is missing or not `true` (client disable is insufficient). Client-only gating is never enough.
- **D-08:** Server **recomputes** the fully-empty set at fix time using the **same predicate** as `empty_folders` / `FolderSubtreeLiveNotes` (no notes in subtree, blank own `readmeContent`; soft-deleted notes do not count as content). Do **not** trust client-supplied folder IDs as authority for what to delete. TOCTOU: folders that gained notes or non-blank readme between lint and fix are not deleted.

#### Purge semantics (AFIX-04, AFIX-05)
- **D-09:** Implement a **dedicated purge applicator** (capability-named, e.g. empty-folder bulk purge on `NotebookHealthService` or a focused collaborator). **Hard-delete** folder rows that match the fully-empty predicate, including nested empty shells, in **one transaction**, children-before-parents (or any equivalent order that never leaves orphan FK issues).
- **D-10:** **Must not** call `dissolveFolder` / promote-children / `DELETE .../folders/{folder}` dissolve semantics. Purge removes empty trees; dissolve promotes contents.
- **D-11:** Folders in **`readme_only_folders`** (note-empty but non-blank `readmeContent`) are **never** deleted by this fix. Dead-link findings are unaffected (report-only).
- **D-12:** After purge, notebook structure must not show promoted empty shells at parent level (the dissolve anti-pattern).

#### Post-fix UX (success criterion 5)
- **D-13:** On successful fix, **automatically re-run lint** (same bodyless lint path) and replace the panel report so the user immediately sees purged fully empty folders gone; readme-only and dead-link groups remain as applicable. User can also manually Run again (same outcome).
- **D-14:** On failure, surface error via `apiCallWithLoading` / existing error handling; leave prior report visible; no partial UI claim of success.

#### Verification
- **D-15:** Backend: unit/service tests for purge predicate + order; reject without opt-in; never deletes readme-only; never invokes dissolve; authorized fix removes empty set; unauthorized rejected. Controller/MVC coverage for the fix endpoint.
- **D-16:** Frontend unit/component: Fix disabled when checkbox off or no empty_folders items; enabled when checkbox on and report has empty folders; Fix calls health/fix then refreshes report.
- **D-17:** Targeted E2E (extend capability-named `notebook_health`): seed fully empty folder(s) + readme-only folder → check Remove empty folders → Run lint → see empty folder finding → Fix → empty folders gone from findings / notebook; readme-only finding still present; no promote-to-parent shells. Tag `@wip` until green; remove `@wip` when scenarios pass. Do not run the full E2E suite unless required.

### Claude's Discretion
- Exact Java type name for the applicator (`EmptyFolderBulkFixApplicator` vs method on `NotebookHealthService`) — prefer cohesive package under health services; no phase numbers in product names.
- Exact response DTO for fix (void + client re-lint vs returning purge count) — prefer **void/204 or simple count** and client re-lint per D-13; researcher/planner may choose the smallest OpenAPI surface.
- DaisyUI classes for Fix: secondary/warning/sm is fine; **primary accent stays on Run lint** (Phase 5 UI-SPEC). Destructive styling only if it matches existing delete patterns without fighting the action bar.
- Whether Fix sits left or right of Save as defaults — keep action-bar density and `data-testid`s consistent (`notebook-health-fix` or `notebook-health-remove-empty-folders-fix`).

### Deferred Ideas (OUT OF SCOPE)
- Per-folder multi-select delete — out of v1 (rejected by requirements)
- Auto-fix for dead wiki links or readme-only — out of v1
- Extra confirm modal beyond checkbox + findings preview — not needed for v1
- Dry-run fix mode separate from findings — redundant (findings = preview)
- Undo / recycle bin for purged folders — not in milestone scope
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| AFIX-02 | Only v1 fix action is bulk remove empty folders (no per-folder multi-select) | Single Fix control on action bar; no folder-id picker; server recomputes set |
| AFIX-03 | Fix enabled only when bulk “remove empty folders” option selected | Client computed enablement + server rejects unless `removeEmptyFolders: true` |
| AFIX-04 | Deletes only fully empty trees; readme-only never deleted | Shared predicate + **subtree-safe deletable filter** (CASCADE hazard) |
| AFIX-05 | Dedicated purge path (not dissolve / promote-children) | New `EmptyFolderBulkPurge` using `EntityPersister.remove`; never call `FolderRelocationService.dissolveFolder` |
</phase_requirements>

## Summary

Phase 7 adds the only v1 Health mutation: a gated **bulk hard-delete** of fully empty folder trees. Lint stays report-only on bodyless `POST .../health/lint`. Mutation is a new **`POST .../health/fix`** with `{ removeEmptyFolders: true }`, authorized like lint, recomputing emptiness server-side (no client folder IDs). The Vue Health action bar gains a visible-but-disabled Fix control until the checkbox is on and the last report’s `empty_folders` group has ≥1 item; on success the panel auto re-lints.

**Critical planning constraint:** the lint `empty_folders` predicate checks **own** blank `readmeContent` only. A blank-readme parent with a readme-only child is still listed under `empty_folders`. MySQL `fk_folder_parent` is **`ON DELETE CASCADE`**, so deleting that parent would destroy the readme-only child. Purge must delete only folders whose **entire real subtree** is also in the recomputed empty set (Phase 2 RESEARCH Pitfall 6). That preserves D-08’s recompute of the empty set while satisfying AFIX-04 / D-11.

**Primary recommendation:** Add `EmptyFolderBulkPurge` in `services/health/` reusing `FolderSubtreeLiveNotes` + `EntityPersister.remove` (children-before-parents on the **subtree-safe** deletable set); expose void `fix` on `NotebookHealthController`; regen OpenAPI client; gate Fix in `NotebookHealthPanel` and auto-call `lint` after success.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Fix enablement (checkbox + findings count) | Browser / Client | — | Discoverable gate; not authoritative |
| Opt-in flag enforcement | API / Backend | — | Client disable is insufficient (D-07) |
| Fully-empty predicate + TOCTOU recompute | API / Backend | Database / Storage | Same live graph as lint |
| Hard-delete empty folder rows | API / Backend | Database / Storage | Transactional purge; never dissolve |
| Cascade-safe deletable subset | API / Backend | — | Prevents CASCADE wiping readme-only descendants |
| Auto re-lint after fix | Browser / Client | API / Backend | D-13: bodyless lint refresh |
| Auth (owner/write) | API / Backend | — | `assertAuthorization(notebook)` |

## Current State (code pointers)

### Dissolve ≠ purge (prove wrong for Health)

| Path | Behavior |
|------|----------|
| `NotebookController.dissolveFolder` | `DELETE /api/notebooks/{notebook}/folders/{folder}` — promotes subfolders/notes then `entityPersister.remove(folder)` [VERIFIED: codebase] |
| `FolderRelocationService.dissolveFolder` | Reparents children and **live** notes to parent; does **not** move soft-deleted notes (`findNotesInFolderOrderByIdAsc` filters `deletedAt IS NULL`) [VERIFIED: codebase] |
| Tests | `NotebookFolderManagementControllerTest.DissolveFolder.promotesDirectNotesToParentFolder` proves promote semantics [VERIFIED: codebase] |

### Shared fully-empty predicate

| Path | Role |
|------|------|
| `services/health/FolderSubtreeLiveNotes` | Package-private helper: children map, memoized `subtreeHasLiveNotes`, `isBlankReadme`, `noteEmptyFolderItems` [VERIFIED: codebase] |
| `EmptyFolderHealthRule` | Occupied = `noteRepository.findLiveNoteFolderIdsByNotebookId`; items = note-empty + blank own readme; `autoFixable=true` [VERIFIED: codebase] |
| `ReadmeOnlyFolderHealthRule` | Same occupancy; items = note-empty + **non**-blank own readme; `autoFixable=false` [VERIFIED: codebase] |
| Soft-delete | Live occupancy ignores soft-deleted notes; soft-deleted-only folders appear empty [VERIFIED: `EmptyFolderHealthRuleTest.softDeletedNoteDoesNotOccupyFolder`] |

### Health API / UI today

| Path | Role |
|------|------|
| `NotebookHealthController` | Only `POST .../health/lint` `@Transactional(readOnly=true)` + `assertAuthorization` [VERIFIED: codebase] |
| `NotebookHealthService` | Delegates to `HealthRuleRunner` only — no fix yet [VERIFIED: codebase] |
| `NotebookHealthPanel.vue` | Run lint, checkbox, Save as defaults; **no Fix**; asserts in specs that Fix is absent [VERIFIED: codebase] |
| Generated SDK | `NotebookHealthController.lint` only [VERIFIED: `packages/generated/.../sdk.gen.ts`] |
| E2E | `notebook_health.feature` + `notebookPage` helpers; no fix steps yet [VERIFIED: codebase] |

### FK / delete semantics

| Constraint | Behavior | Purge implication |
|------------|----------|-------------------|
| `fk_folder_parent` | `ON DELETE CASCADE` [VERIFIED: `V100000000__baseline.sql`] | Deleting parent deletes descendants — **must not delete blank parent over readme-only child** |
| `fk_note_folder` | `ON DELETE SET NULL` [VERIFIED: baseline] | Soft-deleted notes in purged folders lose `folder_id` (same leftover path as dissolve) |
| Folder entity | No JPA `@OneToMany` cascade on children [VERIFIED: `Folder.java`] | Prefer explicit children-before-parents `EntityPersister.remove` |

## Standard Stack

### Core

| Library / component | Version / location | Purpose | Why Standard |
|---------------------|-------------------|---------|--------------|
| Spring `@RestController` + `@Transactional` | Existing backend | Fix endpoint + atomic purge | Matches lint/dissolve controllers [VERIFIED: codebase] |
| `EntityPersister.remove` | Existing | Hard-delete folder rows with flush | Same remover dissolve uses after promote — without promote [VERIFIED: codebase] |
| `FolderSubtreeLiveNotes` | Existing health package | Shared emptiness + blank readme | Prevents predicate drift with lint [VERIFIED: codebase] |
| Vue 3 + DaisyUI `daisy-btn` | Existing frontend | Action-bar Fix control | Phase 5/6 Health UI [VERIFIED: codebase] |
| Generated OpenAPI SDK | `pnpm generateTypeScript` | Typed `fix` client | Never hand-edit generated client [VERIFIED: generate-api-client skill] |
| `apiCallWithLoading` | `@/managedApi/clientSetup` | Loading + error toasts | Frontend API rule [VERIFIED: `.cursor/rules/frontend-api.mdc`] |

### Supporting

| Library | Purpose | When to Use |
|---------|---------|-------------|
| Lombok `@Getter/@Setter` DTOs | `NotebookHealthFixRequest` | Match Phase 1 Health DTO style |
| Vitest + `mockSdkService` | Frontend Fix enablement / call order | Extend `NotebookHealthPanel.spec.ts` |
| Cypress + Cucumber | Targeted E2E | Extend `notebook_health.feature` with `@wip` until green |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Dedicated `EmptyFolderBulkPurge` | Method only on `NotebookHealthService` | Service stays thin; health package already owns rules — prefer collaborator in `services/health/` |
| Void fix response | `NotebookHealthFixResult` with ids/count | Larger OpenAPI; client still re-lints (D-13) — void is smaller |
| Leaf-dissolve loop | Call `dissolveFolder` when child count is 0 | Still wrong API cohesion; easy to misuse; forbidden by D-10 |
| Trust report folder IDs | Client sends ids to delete | TOCTOU + spoofing; forbidden by D-08 |

**Installation:** None — no new npm/Maven packages.

**Version verification:** N/A (no new packages). Stack is in-repo Spring/Vue already running under Nix.

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
┌─────────────────────────────────────────────────────────────┐
│ NotebookHealthPanel                                         │
│  checkbox removeEmptyFolders · findings preview · Fix btn   │
│  enable Fix iff checkbox && empty_folders.items.length ≥ 1  │
└───────────────┬───────────────────────────────┬─────────────┘
                │ POST .../health/fix          │ POST .../health/lint
                │ { removeEmptyFolders: true }  │ (bodyless, after success)
                ▼                               ▼
┌───────────────────────────────────────────────────────────────┐
│ NotebookHealthController                                      │
│  assertAuthorization(notebook)                                │
│  fix: reject unless Boolean.TRUE.equals(removeEmptyFolders)   │
│  lint: readOnly report (unchanged)                            │
└───────────────┬───────────────────────────────────────────────┘
                ▼
┌───────────────────────────────────────────────────────────────┐
│ NotebookHealthService.fix → EmptyFolderBulkPurge.apply       │
│  1. Load folders + live occupied folder ids                   │
│  2. S = note-empty ∩ blank own readme (FolderSubtreeLiveNotes)│
│  3. D = { f ∈ S \| all descendants of f are also ∈ S }        │
│  4. Sort D deepest-first; EntityPersister.remove each         │
│  5. Never dissolveFolder / never trust client ids             │
└───────────────┬───────────────────────────────────────────────┘
                ▼
┌──────────────────────┐     ┌──────────────────────────────────┐
│ folder (hard delete) │     │ note.folder_id SET NULL if soft │
│ parent CASCADE safety│     │ deleted notes remained in folder │
└──────────────────────┘     └──────────────────────────────────┘
```

### Recommended Project Structure

```
backend/src/main/java/com/odde/doughnut/
├── controllers/
│   ├── NotebookHealthController.java      # ADD fix endpoint
│   └── dto/
│       └── NotebookHealthFixRequest.java  # ADD removeEmptyFolders Boolean
├── services/
│   ├── NotebookHealthService.java         # ADD fix(...) orchestration
│   └── health/
│       ├── FolderSubtreeLiveNotes.java    # EXTEND: folder list / subtree-safe helpers
│       └── EmptyFolderBulkPurge.java      # ADD dedicated applicator
frontend/src/components/notebook/
├── NotebookHealthPanel.vue                # ADD gated Fix + post-fix lint
e2e_test/features/notebooks/
└── notebook_health.feature                # EXTEND purge scenario (@wip→green)
```

### Pattern 1: Lint ≠ Fix endpoints

**What:** Separate read-only lint from mutating fix; fix requires explicit body flag.  
**When to use:** Always for Health (locked D-05).  
**Example:**

```java
// Source: codebase pattern — NotebookHealthController.lint + dissolveFolder void style
@PostMapping("/{notebook}/health/fix")
@Transactional
public void fix(
    @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
    @Valid @RequestBody NotebookHealthFixRequest request)
    throws UnexpectedNoAccessRightException {
  authorizationService.assertAuthorization(notebook);
  notebookHealthService.fix(notebook, request);
}
```

### Pattern 2: Subtree-safe deletable set (CASCADE-safe)

**What:** Recompute `S` with the empty_folders predicate; purge only `D ⊆ S` where every real descendant is also in `S`.  
**When to use:** Always for bulk purge (AFIX-04 + Phase 2 Pitfall 6).  
**Example:**

```java
// Conceptual — planner implements in FolderSubtreeLiveNotes or EmptyFolderBulkPurge
Set<Integer> emptyIds = /* folders matching noteEmpty + blank readme */;
List<Folder> deletable =
    folders.stream()
        .filter(f -> emptyIds.contains(f.getId()))
        .filter(f -> allDescendantsIn(f, emptyIds, childrenByParentId))
        .sorted(byDepthDescending)
        .toList();
for (Folder folder : deletable) {
  entityPersister.remove(folder);
}
```

### Pattern 3: Client Fix then re-lint

**What:** `apiCallWithLoading(fix)` then on success reuse bodyless `runLint()`.  
**When to use:** Post-fix UX (D-13/D-14).

### Anti-Patterns to Avoid

- **Calling `dissolveFolder` for Health:** Promotes children; leaves empty shells at parent (Pitfall 4) [VERIFIED: codebase + PITFALLS.md].
- **Deleting every `empty_folders` id blindly:** CASCADE can delete readme-only descendants [VERIFIED: baseline FK + Phase 2 Pitfall 6].
- **Sending folder IDs from client as delete authority:** TOCTOU / spoofing (D-08).
- **Mutating on lint or user defaults:** Lint bodyless; defaults only prefill checkbox (Phase 6).
- **Hand-editing generated SDK:** Must `pnpm generateTypeScript` [VERIFIED: generate-api-client skill].

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Emptiness detection | New SQL / DFS | `FolderSubtreeLiveNotes` + `findLiveNoteFolderIdsByNotebookId` | Drift vs lint findings |
| Folder row delete | Raw JDBC / JPQL bulk delete | `EntityPersister.remove` deepest-first | Session flush order; matches dissolve’s remove step |
| Auth | Custom checks | `AuthorizationService.assertAuthorization` | Repo-wide auth footgun |
| API client types | Manual TS interfaces | `pnpm generateTypeScript` | OpenAPI is source of truth |
| Loading/errors | Local LoadingModal | `apiCallWithLoading` | Global loading + toasts |

**Key insight:** The hard part is not “delete folders” — it is **reusing the lint predicate while remaining CASCADE-safe for readme-only descendants**, without inventing dissolve.

## Common Pitfalls

### Pitfall 1: CASCADE deletes readme-only child via blank parent
**What goes wrong:** Parent listed in `empty_folders`; purge deletes parent; child with readme is CASCADE-removed.  
**Why it happens:** Own-readme listing + `ON DELETE CASCADE`.  
**How to avoid:** Subtree-safe deletable filter `D`; test parent(blank)+child(readme) → child survives, parent may remain listed.  
**Warning signs:** Readme-only finding disappears after Fix without user opting to delete readme-only.

### Pitfall 2: Using dissolve
**What goes wrong:** Nested empty shells promoted to parent.  
**How to avoid:** No calls to `FolderRelocationService.dissolveFolder`; assert via code review + tests that folder count drops without promote.  
**Warning signs:** Sidebar shows former children at parent after Fix.

### Pitfall 3: TOCTOU / trusting client IDs
**What goes wrong:** Folder gains a note between lint and fix but still deleted.  
**How to avoid:** Recompute `S` inside the write transaction; ignore client ids.  
**Warning signs:** Fix API accepts a folderId list.

### Pitfall 4: Partial UI success on failure
**What goes wrong:** Optimistic UI clears findings though server failed.  
**How to avoid:** Only replace `report` after successful fix **and** successful re-lint; on error leave prior report (D-14).

### Pitfall 5: Soft-deleted notes + unique title at root
**What goes wrong:** Soft-deleted notes in purged folders get `folder_id` SET NULL; rare clash with `uk_note_notebook_folder_title` rolls back the transaction.  
**Why it happens:** Same SET NULL path dissolve leaves for soft-deleted notes [VERIFIED: FK + dissolve live-only promote].  
**How to avoid:** Keep whole purge in one `@Transactional`; add happy-path test for soft-deleted-only empty folder; treat unique conflict as full failure (no partial deletes). Do not expand scope to hard-delete soft-deleted notes.

### Pitfall 6: Missing server gate
**What goes wrong:** Crafted request deletes without checkbox.  
**How to avoid:** `Boolean.TRUE.equals(removeEmptyFolders)` required; controller tests for false/null/missing.

## Code Examples

### Fix request DTO

```java
// Source: match FolderCreationRequest / UserDTO Lombok DTO style [VERIFIED: codebase]
@Getter
@Setter
public class NotebookHealthFixRequest {
  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description = "Must be true to bulk-purge fully empty folder trees")
  private Boolean removeEmptyFolders;
}
```

### Service gate

```java
public void fix(Notebook notebook, NotebookHealthFixRequest request) {
  if (!Boolean.TRUE.equals(request.getRemoveEmptyFolders())) {
    throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "Fix requires removeEmptyFolders=true");
  }
  emptyFolderBulkPurge.apply(notebook);
}
```

### Frontend enablement + flow

```typescript
// Source: extend NotebookHealthPanel.vue patterns [VERIFIED: codebase]
const emptyFolderCount = computed(() => {
  const group = report.value?.groups?.find((g) => g.ruleId === "empty_folders")
  return group?.items?.length ?? 0
})
const fixEnabled = computed(
  () => removeEmptyFolders.value && emptyFolderCount.value > 0
)
const fixLabel = computed(() =>
  emptyFolderCount.value > 0
    ? `Remove ${emptyFolderCount.value} empty folders`
    : "Remove empty folders"
)

async function applyFix() {
  const { error } = await apiCallWithLoading(() =>
    NotebookHealthController.fix({
      path: { notebook: props.notebookId },
      body: { removeEmptyFolders: true },
    })
  )
  if (!error) {
    await runLint()
  }
}
```

## Recommended Approach (concrete)

### Discretion resolutions (researcher)

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Applicator name | `EmptyFolderBulkPurge` in `services/health/` | Capability-named; cohesive with rules; keeps `NotebookHealthService` thin |
| Fix HTTP body response | `void` (match `dissolveFolder`) | Smallest OpenAPI; D-13 client re-lint supplies fresh report |
| Fix button placement | After checkbox, before Save as defaults | Mirrors “opt-in → act → persist prefs” density |
| `data-testid` | `notebook-health-fix` | Matches existing Phase 5 spec negative assertions expecting this id |
| Button classes | `daisy-btn daisy-btn-secondary daisy-btn-sm` (or `warning` if secondary clashes) | Keep `primary` on Run lint |

### Implementation notes for planner (order of work)

1. **Backend predicate helpers + purge (Behavior core)**  
   - Extend `FolderSubtreeLiveNotes` with helpers needed for purge (e.g. select fully-empty `Folder`s; `allDescendantsInEmptySet`). Keep package-private if only health uses them.  
   - Add `EmptyFolderBulkPurge` (`FolderRepository`, `NoteRepository`, `EntityPersister`).  
   - Wire `NotebookHealthService.fix`.  
   - Tests: nested empty both deleted; readme-only preserved; parent(blank)+child(readme) does **not** delete child (and does not CASCADE-delete); soft-deleted-only empty folder purged when no title clash; never calls dissolve; reject without opt-in at service or controller.

2. **Controller + DTO + OpenAPI**  
   - `NotebookHealthFixRequest`; `POST .../health/fix` with `@Transactional` (writable).  
   - Extend `NotebookHealthControllerTest` (owner success, foreign/anon reject, bad body).  
   - `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`.

3. **Frontend Fix UI**  
   - `NotebookHealthPanel.vue`: computed enablement, label with N, disabled button visible, fix→re-lint.  
   - Update `NotebookHealthPanel.spec.ts` (replace “no Fix control” AFIX-01 assertions with gated Fix behaviors).

4. **E2E**  
   - Extend `notebook_health.feature` + page object (`applyFix` / expect sidebar folder gone / readme-only remains).  
   - `@wip` until green; targeted `pnpm cypress run --spec e2e_test/features/notebooks/notebook_health.feature`.

### Suggested file list

| Action | File |
|--------|------|
| ADD | `backend/.../dto/NotebookHealthFixRequest.java` |
| ADD | `backend/.../health/EmptyFolderBulkPurge.java` |
| ADD | `backend/.../health/EmptyFolderBulkPurgeTest.java` (or service/controller coverage) |
| EDIT | `FolderSubtreeLiveNotes.java` |
| EDIT | `NotebookHealthService.java` |
| EDIT | `NotebookHealthController.java` |
| EDIT | `NotebookHealthControllerTest.java` |
| REGEN | `packages/generated/doughnut-backend-api/**`, `open_api_docs.yaml` |
| EDIT | `NotebookHealthPanel.vue` |
| EDIT | `NotebookHealthPanel.spec.ts` |
| EDIT | `notebook_health.feature`, `notebookPage.ts`, `notebook.ts` steps |

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Only dissolve removes folders | Health adds dedicated empty-tree purge | Phase 7 | Cleanup without promote |
| Fix absent; checkbox UI-only | Checkbox gates Fix + server flag | Phase 5→7 | Lint≠fix preserved |
| PITFALLS `indexContent` wording | Product uses `readmeContent` | Phases 2–3 | Use readme domain language only |

**Deprecated/outdated:** Treating dissolve as “delete empty folder.”

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Void fix response is acceptable to OpenAPI/codegen without `@ResponseStatus(NO_CONTENT)` (same as dissolve) | Recommended Approach | May prefer explicit 204 — low risk; adjust annotation if codegen complains |
| A2 | Soft-deleted title unique clashes on SET NULL are rare enough to handle as full transaction failure | Pitfall 5 | If common in production data, need explicit reparent/conflict strategy (out of v1 unless tests force it) |

## Open Questions

None blocking. Subtree-safe deletable filter resolves the CASCADE vs D-08 tension without reopening CONTEXT: still recompute with the empty_folders predicate; apply an additional safety filter required by AFIX-04 / D-11 / Phase 2 Pitfall 6.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Nix + `CURSOR_DEV=true nix develop` | All tooling | ✓ | local | Cloud VM skill if no Nix |
| Java / Spring Boot test stack | Backend tests | ✓ | OpenJDK 24 observed | — |
| MySQL (via `pnpm sut`) | Integration/E2E | Assume running | — | Start `pnpm sut` |
| `pnpm generateTypeScript` | SDK regen | ✓ via Nix | — | — |
| Cypress targeted run | E2E | ✓ via Nix | — | — |

**Missing dependencies with no fallback:** none identified for planning.

**Step 2.6:** External deps are existing repo tooling only.

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 5 + SpringBootTest (backend); Vitest browser (frontend); Cypress Cucumber (E2E) |
| Config file | Backend Spring test profile; `frontend` Vitest; `e2e_test/config` |
| Quick run command | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` / `pnpm frontend:test tests/components/notebook/NotebookHealthPanel.spec.ts` |
| Full suite command | `CURSOR_DEV=true nix develop -c pnpm backend:verify`; targeted E2E spec only |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| AFIX-02 | Single bulk Fix, no multi-select | frontend + E2E | frontend panel spec; cypress notebook_health | ❌ Wave 0 (extend) |
| AFIX-03 | Fix gated by checkbox (+ findings) | frontend unit | `NotebookHealthPanel.spec.ts` | ❌ Wave 0 (extend) |
| AFIX-03 | Server rejects without flag | controller | `NotebookHealthControllerTest` | ❌ Wave 0 (extend) |
| AFIX-04 | Fully empty only; readme-only kept; CASCADE-safe | service | `EmptyFolderBulkPurgeTest` or health service test | ❌ Wave 0 |
| AFIX-05 | No dissolve / no promote | service | assert no promote; folders removed | ❌ Wave 0 |
| Success 5 | Re-lint clears purged empty folders | frontend + E2E | fix then lint mock/order; feature scenario | ❌ Wave 0 |

### Sampling Rate

- **Per task commit:** targeted backend:test_only and/or frontend panel spec  
- **Per wave merge:** backend:test_only + frontend health panel spec  
- **Phase gate:** targeted `notebook_health.feature` green; remove `@wip`

### Wave 0 Gaps

- [ ] Backend purge service/controller tests covering CASCADE-safe parent/child readme case  
- [ ] Frontend Fix enablement + fix→re-lint specs (replace “no Fix” assertions)  
- [ ] E2E scenario + page-object methods for Fix  
- [ ] OpenAPI regen after controller lands  

*(Existing lint/auth/empty-folder tests remain; they do not cover purge.)*

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes | Session-scoped controllers; anon rejected via auth service |
| V3 Session Management | yes (existing) | Existing session model — no change |
| V4 Access Control | yes | `assertAuthorization(notebook)` write gate (not read/share) |
| V5 Input Validation | yes | Require `removeEmptyFolders == true`; ignore any folder ids |
| V6 Cryptography | no | — |

### Known Threat Patterns for Health fix

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Unauthorized purge | Elevation of privilege | `assertAuthorization` + foreign/anon tests |
| Client spoofs folder ids | Tampering | Server recomputes set; no id list in v1 API |
| Fix without opt-in | Tampering | Reject unless `removeEmptyFolders: true` |
| CASCADE deletes protected readme content | Tampering / Information loss | Subtree-safe deletable filter |
| Over-disclosure in error | Information disclosure | Generic BAD_REQUEST; no note bodies in fix response |

## Project Constraints (from `.cursor/rules/`)

- Run tooling with `CURSOR_DEV=true nix develop -c …`; git without Nix.
- Behavior phase: one observable behavior; stop-safe; no phase numbers in product names.
- Backend: thin controllers; `AuthorizationService.assertAuthorization`; prefer entities/DTOs per existing style; `backend:test_only` / `backend:verify`.
- Frontend API: generated SDK + `apiCallWithLoading`; never hand-edit generated client.
- Frontend components: DaisyUI `daisy-*` + Tailwind; `data-testid` selectors.
- Frontend tests: `mockSdkService`; prefer observable UI; avoid `getByRole`.
- E2E: capability-named `notebook_health`; `@wip` until green; targeted `--spec` only; page objects fluent.
- Planning: commit+push deploy gate is execute-plan wrap-up (planner/executor), not research.
- No new Flyway expected for this phase (folder hard-delete uses existing schema).

## Sources

### Primary (HIGH confidence)

- Codebase: `FolderRelocationService.dissolveFolder`, `FolderSubtreeLiveNotes`, `EmptyFolderHealthRule`, `NotebookHealthController`, `NotebookHealthPanel.vue`, baseline FKs [VERIFIED: codebase]
- `.planning/phases/07-gated-empty-folder-purge/07-CONTEXT.md` — locked decisions
- `.planning/phases/02-empty-folder-findings/02-RESEARCH.md` Pitfall 6 — parent vs readme-only child [VERIFIED: prior research]
- `.planning/research/ARCHITECTURE.md`, `.planning/research/PITFALLS.md` — fix path + dissolve/TOCTOU
- `.cursor/skills/generate-api-client/SKILL.md` — regen steps
- Context7 Spring Framework reference — void controller + `@ResponseStatus` patterns [CITED: spring.io reference 6.2]

### Secondary (MEDIUM confidence)

- WebSearch / Hibernate community — children-before-parents for self-referencing deletes [CITED: community Q&A]; reinforced by Doughnut `ON DELETE CASCADE` + lack of JPA child cascade

### Tertiary (LOW confidence)

- Soft-deleted unique-title clash frequency in production [ASSUMED: rare]

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — existing Doughnut Health stack, no new packages
- Architecture: HIGH — locked CONTEXT + verified FK/predicate code
- Pitfalls: HIGH — dissolve + CASCADE+readme-only documented with code evidence

**Research date:** 2026-07-22  
**Valid until:** 2026-08-22 (stable domain; re-check if folder FK semantics change)
