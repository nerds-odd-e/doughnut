# Phase 3: Readme-only folder findings - Research

**Researched:** 2026-07-22
**Domain:** Spring Boot notebook Health lint — `readme_only_folders` rule (mutual exclusion with `empty_folders`)
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** A folder is reported under `readme_only_folders` when its **entire subtree has no live notes** (same recursive emptiness as Phase 2: soft-deleted notes do not count) **and** the folder’s **own** `readmeContent` is **non-blank** (not null and not `String.isBlank()`).
- **D-02:** Predicate uses the folder’s **own** readme only — ancestor or descendant readmes do not reclassify this folder. A note-empty child with blank own readme stays under `empty_folders` even if a parent has a readme.
- **D-03:** Mutual exclusion with `empty_folders` must hold: fully empty (note-empty + blank readme) → only `empty_folders`; note-empty + non-blank readme → only `readme_only_folders`; folders with any live note in the subtree → neither group. Do not lump readme-only into `empty_folders`.
- **D-04:** List **every** matching readme-only folder as its own `HealthFindingItem` with `folderId` and `label` = bare folder name (same item shape as Phase 2). No readme preview / path-in-label required for v1.
- **D-05:** Use stable rule id `readme_only_folders` (`HealthRuleIds.README_ONLY_FOLDERS`). Display title: “Readme-only folders”.
- **D-06:** Group severity = `warning` (same structural tier as empty folders).
- **D-07:** Group `autoFixable` = **`false`** — readme-only folders must never be deleted by Phase 7 bulk purge; reserve this flag now so the fix-eligibility boundary is visible in the report.
- **D-08:** Always emit the `readme_only_folders` group (metadata + `items`, possibly empty), consistent with Phase 2’s always-emit `empty_folders` group.
- **D-09:** Add a Spring `HealthRule` bean (e.g. `ReadmeOnlyFolderHealthRule`) discovered by existing `HealthRuleRunner`. Reuse the existing authorized `POST .../health/lint` — no new endpoint, no request-body options, no frontend.
- **D-10:** Prefer a small shared helper for the shared “subtree has live notes” / folder-load scan so `EmptyFolderHealthRule` and the new rule cannot drift on emptiness. Mutual-exclusion tests are required either way. Avoid speculative structure beyond that shared scan.
- **D-11:** No OpenAPI / TypeScript client regen unless a wire schema change appears (report DTOs already support multiple groups). Verify lint response can include both groups via backend tests.
- **D-12:** Prove with focused backend tests: readme-only inclusion; blank/whitespace readme stays in `empty_folders` not this group; live note in subtree excludes; soft-delete ignored; both groups can appear in one lint report; no mutation. No `@wip` E2E / Health UI in this phase.

### Claude's Discretion
- Exact shared-helper name/package (`services/health/` package-private vs package-visible util) — pick the smallest cohesive option.
- Whether to refactor `EmptyFolderHealthRule` in the same change set vs thin wrapper — either is fine if mutual exclusion stays green and duplication is not left to drift.
- Optional `message` on items — omit unless trivially useful; default is label-only.

### Deferred Ideas (OUT OF SCOPE)
- Health tab UI / Run — Phase 5
- Dead wiki-link rule — Phase 4
- User-level lint defaults — Phase 6
- Bulk empty-folder purge — Phase 7 (must never delete `readme_only_folders` items)
- Readme preview in finding `message` — rejected for v1; revisit only if UI needs a cue after Phase 5
- Ancestor-readme inheritance for classification — rejected; own-readme only
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| EFOL-03 | Lint reports note-empty folders that still have non-blank **readme** (`readmeContent`) as a **separate finding type** (not lumped with fully empty folders) | New `ReadmeOnlyFolderHealthRule` with inverted blank-readme gate + shared recursive emptiness scan; mutual-exclusion tests vs `EmptyFolderHealthRule`; `autoFixable=false`; always-emit group |
</phase_requirements>

## Summary

Phase 2 already ships authorized `POST /api/notebooks/{notebook}/health/lint`, `EmptyFolderHealthRule` (fully empty = note-empty + blank own readme), and reserved `HealthRuleIds.README_ONLY_FOLDERS`. Phase 3 is a single **Behavior** slice: add a second `HealthRule` bean that reports note-empty folders with **non-blank** own `readmeContent` under `readme_only_folders`, keep mutual exclusion with `empty_folders`, and prove it with backend tests. No new HTTP surface, no OpenAPI regen (unless schema accidentally changes), no UI, no mutation.

The emptiness algorithm already exists inside `EmptyFolderHealthRule` (memoized `subtreeHasLiveNotes` over `findLiveNoteFolderIdsByNotebookId`). Locked D-10 requires extracting that shared scan into a small helper so the two rules cannot drift. The only intentional difference between the rules is the readme gate and metadata (`autoFixable`: empty=`true`, readme-only=`false`).

**Primary recommendation:** In one Behavior change set (optionally two ~5‑min plans: extract helper + keep empty tests green, then add readme-only rule + mutual-exclusion tests), add package-private shared scan helpers, `@Service ReadmeOnlyFolderHealthRule`, and focused `*HealthRuleTest` coverage via `NotebookHealthService.lint` — do **not** add endpoints or regenerate the TS client.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Readme-only predicate (note-empty + non-blank own readme) | API / Backend | Database / Storage | Same live MySQL folder tree + note placement as Phase 2; client never owns classification |
| Mutual exclusion vs `empty_folders` | API / Backend | — | Complementary readme gates on shared emptiness; both rules evaluate in one lint run |
| Fix-eligibility flag (`autoFixable=false`) | API / Backend | — | Report metadata only this phase; Phase 7 purge must honor it later |
| Authorized lint transport | API / Backend | — | Existing `NotebookHealthController` + write auth — no new surface |
| Health UI display of both groups | Browser / Client | — | **Out of phase** (Phase 5) |

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot (existing) | repo-managed | Second `@Service` `HealthRule` bean | Runner already injects `List<HealthRule>` [VERIFIED: `HealthRuleRunner.java`] |
| Spring Framework DI | repo-managed | Auto-collect all `HealthRule` implementations | Typed collection autowiring [CITED: docs.spring.io/spring-framework/reference/6.2/core/beans/annotation-config/autowired.html] |
| Spring Data JPA | repo-managed | Folder tree + live note folder ids | Reuse Phase 2 queries — no new repository methods needed [VERIFIED: `FolderRepository`, `NoteRepository`] |
| JUnit 5 + `@SpringBootTest` | repo-managed | Predicate + mutual-exclusion tests | Mirror `EmptyFolderHealthRuleTest` |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Hamcrest | existing | Readable assertions | Match `EmptyFolderHealthRuleTest` |
| MakeMe `FolderBuilder.readmeContent` | existing | Fixtures for blank / non-blank readme | Already used in Phase 2 tests |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Shared package-private scan helper (D-10) | Copy-paste `subtreeHasLiveNotes` into new rule | Faster to type; **rejected** — predicate drift risk on soft-delete / recursion |
| Single rule with two groups | One `HealthRule` emitting both | Breaks one-group-per-rule runner contract and Phase 1 id model |
| Runner-level shared load cache across rules | Evaluate both predicates in one pass inside runner | Speculative structure for perf; not needed at O(folders+notes)×2 |
| OpenAPI regen “just in case” | Skip regen (D-11) | Regen only if DTO/controller annotations change — they should not |

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
Authorized owner
    │
    ▼
POST /api/notebooks/{notebook}/health/lint   (existing; unchanged)
    │
    ▼
NotebookHealthController  →  assertAuthorization(notebook)
    │
    ▼
NotebookHealthService.lint → HealthRuleRunner.run
    │
    ├── EmptyFolderHealthRule
    │     ├── shared: load folders + live folder_ids + subtreeHasLiveNotes
    │     └── if note-empty AND blank own readme → items (autoFixable=true)
    │
    └── ReadmeOnlyFolderHealthRule  (THIS PHASE)
          ├── shared: same emptiness scan (must not drift)
          └── if note-empty AND non-blank own readme → items (autoFixable=false)
    │
    ▼
NotebookHealthLintReport {
  groups: [ empty_folders group, readme_only_folders group ]  // order not relied on; look up by ruleId
}
    │
    └── no DB writes
```

### Recommended Project Structure

```
backend/src/main/java/com/odde/doughnut/services/health/
├── HealthRule.java                    # existing
├── HealthRuleRunner.java              # existing — no change expected
├── HealthRuleIds.java                 # README_ONLY_FOLDERS already reserved
├── HealthRunContext.java              # existing
├── EmptyFolderHealthRule.java         # REFACTOR — use shared scan; keep blank-readme gate
├── ReadmeOnlyFolderHealthRule.java    # NEW — non-blank readme gate
└── FolderSubtreeLiveNotes.java        # NEW (name discretionary) — package-private shared scan

backend/src/test/java/com/odde/doughnut/services/health/
├── EmptyFolderHealthRuleTest.java     # KEEP — still green after extract; may add mutual-exclusion assert
└── ReadmeOnlyFolderHealthRuleTest.java # NEW — inclusion + exclusion + both-groups + metadata
```

Discretionary naming: `FolderSubtreeLiveNotes`, `NotebookFolderOccupancy`, or package-private statics on a final util class — pick the smallest cohesive option in `services/health/`.

### Pattern 1: Complementary readme gates on shared emptiness

**What:** Both rules use identical `!subtreeHasLiveNotes(...)`. Empty folders require `isBlankReadme`; readme-only requires `!isBlankReadme`.

**When to use:** Always for EFOL-01/03 mutual exclusion (D-03).

**Example:**

```java
// Source: EmptyFolderHealthRule.java (live) + Phase 3 CONTEXT D-01/D-03
boolean noteEmpty =
    !FolderSubtreeLiveNotes.subtreeHasLiveNotes(
        folder, childrenByParentId, occupiedFolderIds, memo);

if (noteEmpty && isBlankReadme(folder.getReadmeContent())) {
  // empty_folders item
}
if (noteEmpty && !isBlankReadme(folder.getReadmeContent())) {
  // readme_only_folders item
}

private static boolean isBlankReadme(String readmeContent) {
  return readmeContent == null || readmeContent.isBlank();
}
```

Share `isBlankReadme` in the helper too so blank/whitespace thresholds cannot diverge.

### Pattern 2: Spring discovers the second rule automatically

**What:** `@Service class ReadmeOnlyFolderHealthRule implements HealthRule` — no runner edits.

**When to use:** Every new Health rule.

**Example:**

```java
// Source: Spring Framework 6.2 collection injection
// https://docs.spring.io/spring-framework/reference/6.2/core/beans/annotation-config/autowired.html
@Service
public class ReadmeOnlyFolderHealthRule implements HealthRule {
  // id() -> HealthRuleIds.README_ONLY_FOLDERS
  // title() -> "Readme-only folders"
  // severity() -> HealthSeverity.warning
  // autoFixable() -> false   // CRITICAL — not true
  // evaluate(...) -> always emit group; items for note-empty + non-blank own readme
}
```

Tests must look up groups by `ruleId`, not by `groups.get(0)` — Spring list order is not a product contract unless `@Order` is added (do not add Order unless a UI requires stable ordering later).

### Pattern 3: Own-readme only (no inheritance)

**What:** Classify each folder using **only** `folder.getReadmeContent()`. Parent readme does not reclassify a blank child; child readme does not reclassify a blank parent.

**When to use:** Always (D-02; deferred ancestor inheritance).

**Fixture that must pass:**

```java
Folder parent = makeMe.aFolder().notebook(notebook).name("Parent")
    .readmeContent("parent readme").please();
Folder child = makeMe.aFolder().parentFolder(parent).name("Child").please(); // blank readme
// parent → readme_only_folders; child → empty_folders; neither in the other group
```

### Anti-Patterns to Avoid

- **Copy-pasting emptiness DFS without a shared helper:** Violates D-10; soft-delete / recursion can diverge.
- **Setting `autoFixable=true` on readme-only:** Phase 7 would treat them as purge-eligible (AFIX-04 risk).
- **Lumping readme-only into `empty_folders`:** Regresses Phase 2 D-02 / EFOL-03.
- **Ancestor-readme inheritance:** Explicitly rejected.
- **New endpoint or OpenAPI regen:** D-09 / D-11 — unused work; UI is Phase 5.
- **E2E / Health UI:** Out of scope (D-12).
- **Sharing one load across rules via runner rewrite:** Speculative structure beyond D-10.
- **Using `indexContent` naming:** Stale research language; product field is `readmeContent` only.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Rule registration | Manual list / static registry | Spring `List<HealthRule>` | Already wired [CITED: Spring 6.2 autowired collections] |
| Emptiness scan | Second ad-hoc DFS | Extract from `EmptyFolderHealthRule` | D-10 drift prevention |
| Blank threshold | Custom trim / regex | `null \|\| String.isBlank()` | Locked Phase 2/3 blank definition |
| Live-note query | N+1 per folder | `findLiveNoteFolderIdsByNotebookId` | Exists from Phase 2 |
| Lint HTTP | New controller method | Existing `NotebookHealthController.lint` | D-09 |
| TS types for second group | Hand-edit generated client | No regen (schema unchanged) | D-11; DTOs already multi-group |
| Auth re-proof as primary work | Duplicate foreign/anon tests | Rely on Phase 2 controller tests; optional smoke that both groups present | Auth unchanged |

**Key insight:** Phase 3 is almost entirely the **inverted readme gate** + **metadata** on infrastructure Phase 2 already proved. Risk is mutual-exclusion bugs and wrong `autoFixable`, not framework choice.

## Common Pitfalls

### Pitfall 1: Predicate drift between the two rules
**What goes wrong:** One rule treats soft-deleted notes or nested shells differently; folders appear in both groups or neither incorrectly.
**Why it happens:** Duplicated DFS / occupied-set construction.
**How to avoid:** Shared helper for children map + memoized `subtreeHasLiveNotes` (+ shared `isBlankReadme`); mutual-exclusion tests.
**Warning signs:** Same `folderId` in both groups’ items; or note-empty+readme folder missing from both.

### Pitfall 2: Double-reporting (same folder in both groups)
**What goes wrong:** UI later shows conflicting fixability; Phase 7 may delete readme content.
**Why it happens:** Overlapping predicates (e.g. empty rule forgets blank-readme gate).
**How to avoid:** Complementary gates on the same `noteEmpty` boolean; assert `disjoint(folderIds)`.
**Warning signs:** `containsInAnyOrder` passes for both groups with overlapping ids.

### Pitfall 3: Wrong `autoFixable` flag
**What goes wrong:** Readme-only group marked `true`; Phase 7 purge becomes eligible by metadata alone.
**Why it happens:** Copy-paste from `EmptyFolderHealthRule` (`autoFixable() { return true; }`).
**How to avoid:** Explicit `return false`; assert in metadata test (D-07).
**Warning signs:** Metadata test expects `true` for `readme_only_folders`.

### Pitfall 4: Whitespace-only treated as “has readme”
**What goes wrong:** `"   "` appears under readme-only instead of empty_folders.
**Why it happens:** Checking `!= null && !isEmpty()` without `isBlank()`.
**How to avoid:** Shared `isBlankReadme`; fixture with `.readmeContent("   ")`.
**Warning signs:** Phase 2’s `nonBlankReadmeExcludesFolderFromEmptyFolders` still passes but Phase 3 inclusion wrongly claims whitespace.

### Pitfall 5: Relying on group list order
**What goes wrong:** Flaky tests / UI assumptions when bean order changes.
**Why it happens:** `report.getGroups().get(0)`.
**How to avoid:** Filter by `HealthRuleIds.*` (as `EmptyFolderHealthRuleTest` already does).
**Warning signs:** Intermittent failures after adding the second `@Service`.

### Pitfall 6: OpenAPI regen / frontend work creep
**What goes wrong:** Scope expands into generate-api-client and Health tab.
**Why it happens:** Habit from Phase 2 (which needed the first lint operation).
**How to avoid:** D-11 / D-12 — backend tests only; no schema change expected.
**Warning signs:** Diff under `packages/generated/` without controller/DTO annotation changes.

## Code Examples

Verified patterns from the live codebase / official Spring docs:

### Shared scan extraction (target shape)

```java
// Source: EmptyFolderHealthRule.java — extract these; do not change algorithm
final class FolderSubtreeLiveNotes {
  private FolderSubtreeLiveNotes() {}

  static Map<Integer, List<Folder>> childrenByParentId(List<Folder> folders) { /* existing */ }

  static boolean subtreeHasLiveNotes(
      Folder folder,
      Map<Integer, List<Folder>> childrenByParentId,
      Set<Integer> occupiedFolderIds,
      Map<Integer, Boolean> memo) { /* existing memoized DFS */ }

  static boolean isBlankReadme(String readmeContent) {
    return readmeContent == null || readmeContent.isBlank();
  }
}
```

### Readme-only evaluate loop

```java
// Source: Phase 3 CONTEXT D-01/D-04/D-08
List<HealthFindingItem> items = new ArrayList<>();
for (Folder folder : folders) {
  if (!FolderSubtreeLiveNotes.subtreeHasLiveNotes(
          folder, childrenByParentId, occupiedFolderIds, memo)
      && !FolderSubtreeLiveNotes.isBlankReadme(folder.getReadmeContent())) {
    HealthFindingItem item = new HealthFindingItem();
    item.setFolderId(folder.getId());
    item.setLabel(folder.getName());
    items.add(item);
  }
}
HealthFindingGroup group = new HealthFindingGroup();
group.setRuleId(HealthRuleIds.README_ONLY_FOLDERS);
group.setTitle("Readme-only folders");
group.setSeverity(HealthSeverity.warning);
group.setAutoFixable(false);
group.setItems(items);
return group;
```

### Mutual-exclusion test skeleton

```java
// Source: EmptyFolderHealthRuleTest patterns + CONTEXT D-03/D-12
@Test
void partitionsNoteEmptyFoldersByOwnReadme() {
  Folder withReadme =
      makeMe.aFolder().notebook(notebook).name("HasReadme").readmeContent("keep me").please();
  Folder whitespace =
      makeMe.aFolder().notebook(notebook).name("Blankish").readmeContent("   ").please();
  Folder nullReadme = makeMe.aFolder().notebook(notebook).name("NullReadme").please();

  NotebookHealthLintReport report = notebookHealthService.lint(notebook, new HealthRunContext());
  HealthFindingGroup empty = group(report, HealthRuleIds.EMPTY_FOLDERS);
  HealthFindingGroup readmeOnly = group(report, HealthRuleIds.README_ONLY_FOLDERS);

  assertThat(ids(empty), containsInAnyOrder(whitespace.getId(), nullReadme.getId()));
  assertThat(ids(readmeOnly), containsInAnyOrder(withReadme.getId()));
  assertThat(ids(empty), not(hasItem(withReadme.getId())));
  assertThat(ids(readmeOnly), not(hasItem(whitespace.getId())));
  assertThat(ids(readmeOnly), not(hasItem(nullReadme.getId())));
}
```

### Always-emit metadata

```java
@Test
void alwaysEmitsReadmeOnlyGroupWithMetadata() {
  HealthFindingGroup group = readmeOnlyGroup(); // from lint with no matching folders
  assertThat(group.getRuleId(), equalTo(HealthRuleIds.README_ONLY_FOLDERS));
  assertThat(group.getTitle(), equalTo("Readme-only folders"));
  assertThat(group.getSeverity(), equalTo(HealthSeverity.warning));
  assertThat(group.isAutoFixable(), equalTo(false));
  assertThat(group.getItems(), empty());
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Note-empty + readme left unreported (Phase 2 D-02) | Separate `readme_only_folders` group | Phase 3 | Fix-eligibility boundary visible before Phase 7 |
| Research docs saying `indexContent` | Product `readmeContent` | Phase 1–2 | Use `readmeContent` only |
| Single Health rule bean | Two complementary folder rules | Phase 3 | Lint report has ≥2 groups |

**Deprecated/outdated:**
- Milestone research text using `indexContent` for folders — treat as stale; use **`readmeContent`** [VERIFIED: `Folder.java`, Phase 2/3 CONTEXT].

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| — | *(none material)* | — | Implementation claims verified against live Phase 2 code; Spring collection injection cited from official docs |

Blank-readme uses `String.isBlank()` (Java standard); already used in `EmptyFolderHealthRule` [VERIFIED: codebase].

## Open Questions

1. **One plan vs two plans (extract helper then rule)?**
   - What we know: D-10 wants the shared helper; EmptyFolderHealthRule is ~111 lines; extract is small.
   - What's unclear: Whether the planner prefers a tiny Structure wave first.
   - Recommendation: Prefer **one Behavior plan** that extracts helper + adds rule in the same change set (discretion allows same change set). Split only if execution exceeds ~5–10 min time budget.

2. **Should controller tests be extended?**
   - What we know: Auth + no-mutate already covered for lint; D-12 lists service-level proofs.
   - Recommendation: Optional one controller/service assertion that both `ruleId`s appear after lint; **not** required to re-test foreign/anon. Prefer `NotebookHealthService.lint` / rule tests as primary.

3. **Stable group order for UI?**
   - What we know: Phase 5 will render expandable groups; order is unspecified today.
   - Recommendation: Do **not** add `@Order` in Phase 3 unless UI work starts; tests filter by id.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Nix + `CURSOR_DEV=true nix develop -c …` | Backend tests | ✓ (repo contract) | — | cloud-vm-setup skill |
| JDK | Compile/tests | ✓ | openjdk 24.0.2 (probe) | Nix shell |
| MySQL (test profile) | `@SpringBootTest` | ✓ via migrateTestDB / process-compose | — | `pnpm backend:test` migrates |
| OpenAPI regen toolchain | Only if schema changes | ✓ | — | **Skip** this phase (D-11) |

**Missing dependencies with no fallback:** none

**Missing dependencies with fallback:** none

Step 2.6: Code/config-only phase on existing backend test stack — no new external services.

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Spring Boot Test (`@SpringBootTest`, `@ActiveProfiles("test")`, `@Transactional`) |
| Config file | `backend/` Gradle + Spring `test` profile |
| Quick run command | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` |
| Full suite command | `CURSOR_DEV=true nix develop -c pnpm backend:verify` |

Per `backend-testing.mdc`: run **all** backend unit tests when verifying (not a single class).

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| EFOL-03 | Note-empty + non-blank own readme listed under `readme_only_folders` | service | `pnpm backend:test_only` | ❌ Wave 0 — `ReadmeOnlyFolderHealthRuleTest` |
| EFOL-03 | Blank / whitespace / null readme stay only in `empty_folders` | service | `pnpm backend:test_only` | ❌ Wave 0 (mutual exclusion) |
| EFOL-03 | Live note in subtree excludes from both groups | service | `pnpm backend:test_only` | ❌ Wave 0 |
| EFOL-03 | Soft-deleted notes ignored (readme-only still reports if non-blank) | service | `pnpm backend:test_only` | ❌ Wave 0 |
| EFOL-03 | Own-readme only (parent/child different groups) | service | `pnpm backend:test_only` | ❌ Wave 0 |
| EFOL-03 | Always emit group; title/severity/`autoFixable=false` | service | `pnpm backend:test_only` | ❌ Wave 0 |
| SC-3 | Lint does not mutate folder count | service/controller | `pnpm backend:test_only` | ✅ extend existing pattern / light assert |
| EFOL-01 regression | Empty folders still listed for blank-readme note-empty | service | `pnpm backend:test_only` | ✅ `EmptyFolderHealthRuleTest` must stay green after extract |

### Sampling Rate

- **Per task commit:** `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
- **Per wave merge:** `CURSOR_DEV=true nix develop -c pnpm backend:verify`
- **Phase gate:** Backend green; no E2E; no OpenAPI regen unless schema changed

### Wave 0 Gaps

- [ ] `backend/src/test/java/com/odde/doughnut/services/health/ReadmeOnlyFolderHealthRuleTest.java` — inclusion, blank exclusion, live-note exclusion, soft-delete, own-readme partitioning, always-emit metadata (`autoFixable=false`), both groups in one report
- [ ] Keep `EmptyFolderHealthRuleTest` green after shared-helper extract (regression gate)
- [ ] Optional: one assert in controller or service test that lint returns both rule ids — not a substitute for mutual-exclusion unit coverage

*(Framework already installed — no new test infra.)*

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no (unchanged) | Existing session auth on API |
| V3 Session Management | no | — |
| V4 Access Control | yes (reuse) | Existing `assertAuthorization(notebook)` on lint — no new endpoint |
| V5 Input Validation | no new inputs | Lint body remains empty; rule reads persisted folder fields |
| V6 Cryptography | no | — |

### Known Threat Patterns for notebook Health lint

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Cross-notebook lint / data disclosure | Information Disclosure | Write auth already on controller (Phase 2) — do not weaken |
| Accidental delete via wrong `autoFixable` | Tampering / Elevation | `autoFixable=false` on readme-only; Phase 7 must re-check emptiness + blank readme |
| Soft-delete confusion hiding/showing folders | Tampering | Shared live-note query (`deletedAt IS NULL`) |

No new attack surface if D-09 is followed (reuse lint endpoint only).

## Project Constraints (from .cursor/rules/)

| Source | Directive for planner/executor |
|--------|--------------------------------|
| `planning.mdc` | Phase is **Behavior** (one observable: separate readme-only findings). Shared helper is allowed Structure **inside** this phase because it enables this immediate behavior (D-10) — not prep for Phase 4+. |
| `planning.mdc` | ~5 min fuzzy slice; >10 min → finer decompose. Prefer small plans. |
| `planning.mdc` | Capability names in product code (`ReadmeOnlyFolderHealthRule`), never phase numbers. |
| `planning.mdc` | No failing tests at phase boundary; no `@wip` E2E this phase. |
| `gsd-coexistence.mdc` | After phase: Jidoka → post-change-refactor → plan update → commit+push. |
| `backend-code.mdc` | Tooling: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` / `backend:verify`; import statements at top. |
| `backend-testing.mdc` | Prefer behavior through stable entry (`NotebookHealthService.lint`); MakeMe builders; run **all** backend unit tests; Hamcrest `assertThat`. |
| `post-change-refactor` skill | Before commit: eliminate duplicated emptiness DFS; keep files cohesive; ≤250 lines; no speculative Phase 7 purge code. |

## Exact Code Touch Points

| File | Action | Notes |
|------|--------|-------|
| `services/health/FolderSubtreeLiveNotes.java` (or similar) | **ADD** | Package-private shared children map + subtree DFS + `isBlankReadme` |
| `services/health/EmptyFolderHealthRule.java` | **EDIT** | Call shared helper; keep blank-readme + `autoFixable=true` |
| `services/health/ReadmeOnlyFolderHealthRule.java` | **ADD** | Non-blank gate; title “Readme-only folders”; `autoFixable=false` |
| `services/health/HealthRuleIds.java` | **NONE** | `README_ONLY_FOLDERS` already reserved |
| `HealthRuleRunner.java` / `NotebookHealthService.java` / `NotebookHealthController.java` | **NONE** expected | Discovery + lint path already work |
| `controllers/dto/*` | **NONE** expected | Multi-group report already supported |
| `ReadmeOnlyFolderHealthRuleTest.java` | **ADD** | Primary EFOL-03 proof |
| `EmptyFolderHealthRuleTest.java` | **KEEP / light extend** | Must stay green; optional mutual-exclusion assert |
| OpenAPI / `packages/generated/**` | **NONE** unless accidental schema change | D-11 |

## Sources

### Primary (HIGH confidence)
- Live codebase: `EmptyFolderHealthRule.java`, `HealthRuleIds.java`, `EmptyFolderHealthRuleTest.java`, `NotebookHealthController.java`, `NoteRepository.findLiveNoteFolderIdsByNotebookId`
- Phase 3 CONTEXT.md (locked decisions D-01–D-12)
- Phase 2 CONTEXT.md + `02-RESEARCH.md` (predicate and test patterns)

### Secondary (MEDIUM confidence)
- Spring Framework 6.2 autowired collections — [CITED: docs.spring.io/spring-framework/reference/6.2/core/beans/annotation-config/autowired.html]
- Milestone `.planning/research/PITFALLS.md` — separate readme-bearing folders; ignore soft-deletes (note: docs still say `indexContent` in places — treat as stale naming)

### Tertiary (LOW confidence)
- None material for implementation

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — no new libraries; Spring DI pattern already in production
- Architecture: HIGH — touch points and complementary predicates verified in code + CONTEXT
- Pitfalls: HIGH — mutual exclusion / autoFixable / blank threshold drawn from Phase 2 research + locked D-03/D-07

**Research date:** 2026-07-22
**Valid until:** 2026-08-21 (30 days — stable internal API; refresh if HealthRule contract changes)
