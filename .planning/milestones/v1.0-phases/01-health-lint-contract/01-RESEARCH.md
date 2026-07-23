# Phase 1: Health lint contract - Research

**Researched:** 2026-07-22
**Domain:** Backend Health lint contract (Spring Boot rule registry + OpenAPI-ready findings DTOs)
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

#### Phase 1 delivery surface
- **D-01:** Backend-only contract in this phase: OpenAPI-ready findings DTOs under `controllers/dto/`, rule interface + runner under `services/health/`, and a thin `NotebookHealthService` orchestration skeleton under `services/`. No controller routes, no frontend, no OpenAPI regen required until Phase 2 exposes an endpoint.
- **D-02:** Runner with zero registered rules returns an empty `NotebookHealthLintReport` (`groups: []`). That is sufficient Phase 1 verification that the contract wires together.
- **D-03:** Fix request/result DTOs and fix applicator are **out of Phase 1** (Phase 7). Lint request DTO may be deferred until Phase 2 if the skeleton does not yet need run options; do not invent option fields beyond what Phase 2 immediately needs.

#### Report and finding shapes
- **D-04:** Top-level report is `NotebookHealthLintReport` with `groups: HealthFindingGroup[]` — one group per rule, shaped for expandable UI later (no frontend in this phase).
- **D-05:** `HealthFindingGroup` includes stable `ruleId`, human `title`, reserved `severity`, reserved `autoFixable`, optional `items` (leaf findings), and optional `children` (nested groups — e.g. Phase 4 dead links by note). Prefer this recursive group shape over a flat findings list so Phase 4 does not rework the shared model.
- **D-06:** `HealthFindingItem` carries optional target identity fields sufficient for later navigation: `folderId`, `noteId`, `label`, optional `message`, optional `wikiLinkToken` (for dead links). Reserve `autoFixable` on items only if cheap; group/rule-level `autoFixable` is enough for v1 gating.
- **D-07:** Severity vocabulary is `error` | `warning` | `info` (API enum / string constants). No severity-settings UI. Structural decay defaults to `warning` or `error` at rule level when rules are added later; Phase 1 only reserves the field.
- **D-08:** Findings are ephemeral DTOs only — **no findings table**, no SARIF, no history persistence.

#### Rule interface and registry
- **D-09:** `HealthRule` (in `services/health/`) exposes at least: `id()`, display title (method or constant), `severity()`, `autoFixable()`, and `evaluate(Notebook notebook, HealthRunContext ctx) → HealthFindingGroup`. Aligns with LM Wiki / research `LintRule` pattern without importing external lint frameworks.
- **D-10:** Rule ids are stable snake_case strings reserved for later rules: `empty_folders`, `readme_only_folders` (or equivalent for Phase 3), `dead_wiki_links`. Phase 1 does not implement those rules; ids are documented in the contract for planner consistency.
- **D-11:** `HealthRuleRunner` discovers rules via Spring `List<HealthRule>` injection (ordered beans). Phase 1 registers **zero** rule beans. Runner never mutates notebook data.
- **D-12:** `HealthRunContext` is a minimal placeholder for future run-scoped options (e.g. which rules / fix prefs). Keep it empty or trivially extensible; do not implement user defaults or fix options here.

#### Package layout and naming
- **D-13:** Package layout follows research: `services/health/` for `HealthRule`, `HealthRuleRunner`, `HealthRunContext`; `NotebookHealthService` at `services/NotebookHealthService.java`; wire DTOs in `controllers/dto/` (`NotebookHealthLintReport`, `HealthFindingGroup`, `HealthFindingItem`, severity type as appropriate).
- **D-14:** Domain language is **readme** / `readmeContent` — never `index` / `indexContent` in new Health code, tests, or docs for this milestone (product rename already landed).

#### Verification
- **D-15:** Prove the contract with focused backend unit tests (runner with no rules → empty report; DTO/group construction). Do not add `@wip` E2E. Existing product tests remain green.
- **D-16:** Do not regenerate the TypeScript API client in Phase 1 (no new endpoints). Phase 2 owns controller + OpenAPI exposure + `pnpm generateTypeScript`.

### Claude's Discretion
- Exact Java type styles (record vs class vs enum) and OpenAPI annotation density — follow existing `controllers/dto/` patterns.
- Whether `NotebookHealthService.lint` is public in Phase 1 or package-visible via runner tests — either is fine if Phase 2 can call it without rework.
- Stable ordering mechanism (`@Order` vs list order) — pick the simplest Spring-idiomatic approach.

### Deferred Ideas (OUT OF SCOPE)
- Concrete `EmptyFolderHealthRule` and authorized lint API — Phase 2
- Readme-only folder finding type — Phase 3
- Dead wiki-link rule and note-nested groups — Phase 4
- Health tab UI / Run — Phase 5
- User-level defaults — Phase 6
- Fix DTOs, purge applicator, controller fix endpoint — Phase 7
- Severity-settings console, SARIF, findings history, LLM lint — out of milestone
</user_constraints>

## Summary

Phase 1 is a **Structure-only** backend contract: OpenAPI-ready findings DTOs, a `HealthRule` interface, Spring `List<HealthRule>` runner, and a thin `NotebookHealthService` skeleton. No HTTP surface, no concrete rules, no TS client regen, no findings persistence. The contract must already support nested groups (`items` + `children`) and reserved `severity` / `autoFixable` so Phase 2 (empty folders) and Phase 4 (dead links by note) plug in without reshaping the model.

The repo already has the exact registry pattern to copy: `AssimilationUnitSource` + `AssimilationServiceFactory(List<AssimilationUnitSource>)`. Recursive DTO nesting already exists (`AttachBookLayoutNodeRequest.children`). Folder domain field is already `readmeContent` (Flyway `V300000231`). Spring Framework 6.2 documents that multi-element injection points resolve to **empty** collections when no matching beans exist — which matches D-02 (zero rules → empty report) without special-casing.

**Primary recommendation:** Add Lombok DTO classes + `HealthSeverity` enum under `controllers/dto/`, put `HealthRule` / `HealthRuleRunner` / `HealthRunContext` (+ optional `HealthRuleIds` constants) under `services/health/`, add public `@Service NotebookHealthService.lint`, unit-test the runner with `List.of()`, and prove Spring still boots with zero `HealthRule` beans via `pnpm backend:test_only`. Do not touch controllers, Flyway, frontend, or codegen.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Findings report / group / item shapes | API / Backend (DTO) | — | Wire contract for later OpenAPI; ephemeral, not persisted |
| `HealthRule` interface + evaluate contract | API / Backend (service) | — | Domain audit logic lives in Spring services |
| Rule registry / ordered runner | API / Backend (service) | — | Spring `List<HealthRule>` injection (same as assimilation unit sources) |
| Lint orchestration skeleton | API / Backend (service) | — | `NotebookHealthService` is the Phase 2 controller call site |
| HTTP lint/fix endpoints | — (deferred Phase 2/7) | — | Explicitly out of Phase 1 (D-01) |
| Health UI / TS client | Browser / Client (deferred) | — | Phase 5 + Phase 2 codegen |
| Findings persistence | Database / Storage | — | **Must not exist** (D-08) |

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot | 4.1.0 | Host `@Service` runner + orchestration | Existing app host `[VERIFIED: backend/build.gradle]` |
| Java | 25 (`sourceCompatibility`) | Interfaces, enums, DTOs | Existing toolchain `[VERIFIED: backend/build.gradle]` |
| springdoc-openapi | 3.0.3 (`springdoc-openapi-starter-webmvc-api`) | `@Schema` on DTOs for later OpenAPI | Existing OpenAPI stack `[VERIFIED: backend/build.gradle]` |
| Lombok | existing (project) | DTO getters/setters | Dominant DTO style in `controllers/dto/` `[VERIFIED: codebase]` |
| JUnit 5 + Spring Boot Test | via `spring-boot-starter-test` | Contract unit tests | Existing backend tests `[VERIFIED: backend/build.gradle]` |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Jackson (`@JsonInclude` optional) | via Spring Boot | Omit null optional fields in JSON | Optional on nested groups/items if matching focus-context style |
| Spring `Ordered` / `@Order` | Spring Framework | Stable rule order when multiple beans exist | Phase 2+ when registering rules; Phase 1 has zero beans |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| In-process Java `HealthRule` | kb-lint / llmwiki / ESLint / SARIF | Wrong data model; forbidden by project research — pattern only |
| `List<HealthRule>` injection | Manual static registry | Loses Spring discovery; contradicts D-11 |
| Flat findings list | Nested `groups`/`children` | Would force Phase 4 rework — locked out by D-05 |
| Findings JPA entity | Ephemeral DTO | Persistence out of scope (D-08) |

**Installation:**

```bash
# No new packages. Implement in existing backend module only.
# Verify with:
CURSOR_DEV=true nix develop -c pnpm backend:test_only
```

**Version verification:** Spring Boot `4.1.0`, springdoc `3.0.3`, Java `25` confirmed from `backend/build.gradle` on 2026-07-22. No new Maven/npm packages for this phase.

## Package Legitimacy Audit

> No external packages are installed in this phase.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| — | — | — | — | — | — | N/A — reuse existing stack |

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```text
Phase 1 (Structure — no HTTP entry)

                    ┌─────────────────────────────┐
                    │ NotebookHealthService       │
                    │  lint(notebook, ctx?)       │
                    └──────────────┬──────────────┘
                                   │
                                   ▼
                    ┌─────────────────────────────┐
                    │ HealthRuleRunner            │
                    │  List<HealthRule> (0 beans) │
                    └──────────────┬──────────────┘
                                   │ for each rule.evaluate(...)
                                   ▼
                    ┌─────────────────────────────┐
                    │ NotebookHealthLintReport    │
                    │  groups: HealthFindingGroup[]│
                    │    items? / children?       │
                    │    ruleId, severity,        │
                    │    autoFixable              │
                    └─────────────────────────────┘

Phase 2+ (out of scope here):
  Controller ──authorize──▶ NotebookHealthService.lint
  @Service EmptyFolderHealthRule implements HealthRule  ──injected into List──▶
```

### Recommended Project Structure

```text
backend/src/main/java/com/odde/doughnut/
├── controllers/dto/
│   ├── NotebookHealthLintReport.java   # groups: List<HealthFindingGroup>
│   ├── HealthFindingGroup.java         # recursive: items + children
│   ├── HealthFindingItem.java          # folderId/noteId/label/...
│   └── HealthSeverity.java             # enum: error | warning | info
└── services/
    ├── NotebookHealthService.java      # thin lint() orchestration
    └── health/
        ├── HealthRule.java             # interface
        ├── HealthRuleRunner.java       # List<HealthRule> → report
        ├── HealthRunContext.java       # empty placeholder
        └── HealthRuleIds.java          # optional string constants (discretion)

backend/src/test/java/com/odde/doughnut/services/health/
└── HealthRuleRunnerTest.java           # zero rules → empty groups; DTO shape
```

### Pattern 1: Spring multi-bean registry via `List<T>`

**What:** Inject all implementations of an interface as `List<T>`; Phase 1 has zero implementations → empty list → empty report.
**When to use:** Pluggable Health rules (and already used for assimilation).
**Example:**

```java
// Source: backend/.../services/AssimilationServiceFactory.java [VERIFIED: codebase]
@Service
public final class AssimilationServiceFactory {
  private final List<AssimilationUnitSource> unitSources;

  @Autowired
  public AssimilationServiceFactory(
      UserService userService,
      SubscriptionService subscriptionService,
      List<AssimilationUnitSource> unitSources) {
    this.unitSources = unitSources;
  }
}
```

```java
// Conceptual Health adaptation — Source: Spring Framework 6.2 Autowired docs
// [CITED: docs.spring.io/spring-framework/reference/6.2/core/beans/annotation-config/autowired.html]
@Service
public class HealthRuleRunner {
  private final List<HealthRule> rules;

  public HealthRuleRunner(List<HealthRule> rules) {
    this.rules = List.copyOf(rules);
  }

  public NotebookHealthLintReport run(Notebook notebook, HealthRunContext ctx) {
    NotebookHealthLintReport report = new NotebookHealthLintReport();
    report.setGroups(
        rules.stream().map(rule -> rule.evaluate(notebook, ctx)).toList());
    return report;
  }
}
```

Spring 6.2: multi-element injection points resolve to empty instances when no matching beans exist `[CITED: Spring Framework 6.2 Autowired]`. Order later beans with `@Order` / `Ordered` `[CITED: same]`.

### Pattern 2: Recursive findings DTO (groups with `items` + `children`)

**What:** One top-level group per rule; nest note-level subgroups under `children` later.
**When to use:** Always for Health reports (locked D-04/D-05).
**Example (shape):**

```java
// Follow AttachBookLayoutNodeRequest recursive children [VERIFIED: codebase]
@Getter
@Setter
public class HealthFindingGroup {
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String ruleId;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String title;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private HealthSeverity severity;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean autoFixable;

  private List<HealthFindingItem> items;
  private List<HealthFindingGroup> children;
}
```

### Pattern 3: Capability-named service subpackage

**What:** Cohesive helpers under `services/health/`; façade `NotebookHealthService` at `services/` root (mirrors `BookService` vs `services/book/*`, `NoteSearchService` vs `services/search/*`).
**When to use:** Health milestone layout (D-13).

### Anti-Patterns to Avoid

- **Controller / OpenAPI / `pnpm generateTypeScript` in Phase 1:** Violates D-01/D-16; Structure must not change user-visible API surface.
- **Findings table / SARIF / history:** Violates D-08.
- **Using `index` / `indexContent` in new Health code:** Product rename to `readme` / `readmeContent` already landed `[VERIFIED: Folder.java + V300000231]`.
- **Registering concrete rule beans now:** Phase 1 is zero rules (D-11); empty-folder rule is Phase 2.
- **Coupling runner to mutation / fix applicator:** Lint ≠ fix; fix is Phase 7.
- **Inventing lint-request option fields:** Defer until Phase 2 needs them (D-03).
- **Hand-rolling a plugin host / importing llmwiki:** Pattern only — no new dependency `[CITED: .planning/research/STACK.md]`.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Rule discovery | Custom classpath scanner / static mutable registry | Spring `List<HealthRule>` like `AssimilationUnitSource` | Already proven in-repo; D-11 locks it |
| Nested JSON tree | Ad-hoc Maps / JSONObject | Typed DTO classes with `List` children | OpenAPI + Vue need stable fields |
| Severity strings | Free-form strings | `HealthSeverity` enum (`error`/`warning`/`info`) | Matches D-07 and existing dto enums |
| OpenAPI exposure without endpoint | Dummy controller solely for Phase 1 | Wait for Phase 2 controller return type | D-16; `DummyForGeneratingTypes` is for AI stream types only |
| Empty-folder / dead-link logic | Anything algorithmic | — (Phase 2/4) | Out of Phase 1 |

**Key insight:** The hard part of Health is shared **shape + registry**, not frameworks. Copy assimilation’s `List` injection and book-layout’s recursive DTO; leave algorithms for later phases.

## Common Pitfalls

### Pitfall 1: Shipping behavior under a Structure phase
**What goes wrong:** Endpoint, rule, or UI sneaks into Phase 1.
**Why it happens:** Research docs describe the full system; easy to “just add the empty-folder rule.”
**How to avoid:** Gate on D-01/D-11/D-15 — only DTOs + interface + runner + empty report test.
**Warning signs:** New `@RestController` mappings, `@Service` implementing `HealthRule`, frontend diffs, `generateTypeScript` runs.

### Pitfall 2: Flat findings that Phase 4 must rewrite
**What goes wrong:** Report is `List<HealthFindingItem>` only.
**Why it happens:** Empty-folder rule only needs a flat list.
**How to avoid:** Ship recursive `HealthFindingGroup` with `items` + `children` now (D-05).
**Warning signs:** No `children` field on the group type.

### Pitfall 3: Naming drift (`indexContent` / `index_only_folders`)
**What goes wrong:** Stale research wording leaks into ids/docs/tests.
**Why it happens:** Older research still says `indexContent` in places.
**How to avoid:** Use `readme` / `readmeContent` and rule id `readme_only_folders` (D-10/D-14). Entity field is already `readmeContent` `[VERIFIED: Folder.java]`.
**Warning signs:** Grep hits for `indexContent` under new Health paths.

### Pitfall 4: Assuming OpenAPI/SDK picks up unused DTOs
**What goes wrong:** Expect Phase 1 DTOs in `open_api_docs.yaml` / generated TS without a controller reference.
**Why it happens:** springdoc only documents types reachable from controller operations (or `DummyForGeneratingTypes` via AI dummy endpoint).
**How to avoid:** Treat Phase 1 DTOs as **OpenAPI-ready source**, not published schema. Phase 2 return type publishes them (D-16).
**Warning signs:** Planner tasks for `pnpm generateTypeScript` in Phase 1.

### Pitfall 5: Runner mutates notebook / applies fixes
**What goes wrong:** Lint path deletes folders.
**Why it happens:** Ecosystem tools conflate scan and apply.
**How to avoid:** Runner only maps `evaluate` → groups; no repositories that delete; no fix DTOs (D-03/D-11).
**Warning signs:** Imports of `FolderRelocationService` / dissolve APIs in `services/health/`.

## Code Examples

### HealthRule interface (target)

```java
// Pattern source: .planning/research/ARCHITECTURE.md + llmwiki LintRule
// [CITED: .planning/research/ARCHITECTURE.md]
// [CITED: https://llmwikis.org/operations/lint/] — structural tier; propose removals separately
package com.odde.doughnut.services.health;

import com.odde.doughnut.controllers.dto.HealthFindingGroup;
import com.odde.doughnut.controllers.dto.HealthSeverity;
import com.odde.doughnut.entities.Notebook;

public interface HealthRule {
  String id();

  String title();

  HealthSeverity severity();

  boolean autoFixable();

  HealthFindingGroup evaluate(Notebook notebook, HealthRunContext ctx);
}
```

### Reserved rule ids (documentation constants)

```java
// Discretion: constants class for planner/Phase 2 consistency (D-10)
public final class HealthRuleIds {
  public static final String EMPTY_FOLDERS = "empty_folders";
  public static final String README_ONLY_FOLDERS = "readme_only_folders";
  public static final String DEAD_WIKI_LINKS = "dead_wiki_links";

  private HealthRuleIds() {}
}
```

### Zero-rules unit test (Phase 1 proof)

```java
// Aligns with backend-testing.mdc: test intentional service contract directly
// [VERIFIED: .cursor/rules/backend-testing.mdc]
@Test
void returnsEmptyGroupsWhenNoRulesRegistered() {
  HealthRuleRunner runner = new HealthRuleRunner(List.of());
  NotebookHealthLintReport report =
      runner.run(new Notebook(), new HealthRunContext());
  assertThat(report.getGroups(), empty());
}
```

### Severity enum style

```java
// Prefer lowercase enum names to match API vocabulary (D-07)
// Precedent: Randomization.RandomStrategy { first, last, seed }
// [VERIFIED: controllers/dto/Randomization.java]
public enum HealthSeverity {
  error,
  warning,
  info
}
```

### Discretion recommendations (locked choices for planner)

| Discretion item | Recommendation | Rationale |
|-----------------|----------------|-----------|
| DTO style | Lombok `@Getter`/`@Setter` classes (not records) for report/group/item | Recursive optional lists match `AttachBookLayoutNodeRequest`; easier mutation while building findings |
| Severity | `public enum HealthSeverity { error, warning, info }` | Matches D-07 wire strings; Jackson default enum naming |
| `@Schema` density | Mark required fields with `Schema.RequiredMode.REQUIRED`; leave optional item fields undocumented or lightly described | Matches `FolderCreationRequest` / `AnsweredQuestion` |
| `NotebookHealthService.lint` | **public** method | Phase 2 controller must call it without package hacks |
| Ordering | Rely on injection order for now; add `@Order` on each rule bean when Phase 2+ registers multiple | Simplest; Spring docs support `@Order` when needed `[CITED: Spring 6.2]` |
| `HealthRunContext` | Empty public class (no fields) | D-12; extensible later without interface churn |
| Item-level `autoFixable` | Omit on `HealthFindingItem` in v1 | D-06: group/rule-level is enough |

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Disk-wiki CLI linters (kb-lint, Obsidian plugins) | In-app rule registry over live DB graph | Doughnut Health milestone research 2026-07-22 | No new lint frameworks |
| Flat issue list | Nested groups for expandable UI | Locked in CONTEXT D-05 | Phase 4 dead links nest by note |
| `indexContent` naming | `readmeContent` | Flyway `V300000231` `[VERIFIED]` | Health code must use readme vocabulary |
| LM Wiki severity blocker/high/medium/low | Doughnut `error`/`warning`/`info` | CONTEXT D-07 | Shorter API enum |

**Deprecated/outdated:**
- Research mentions of `indexContent` in empty-folder eligibility — treat as **readmeContent** for this milestone.
- STACK.md suggestion to reuse folder dissolve DELETE for Health fix — superseded by ARCHITECTURE/PITFALLS (dedicated purge in Phase 7); irrelevant to Phase 1.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Item-level `autoFixable` can be omitted without Phase 2–7 rework | Discretion | Tiny additive field later if UI needs per-item gating |
| A2 | Empty `HealthRunContext` class is enough until Phase 2/6 options exist | D-12 / Discretion | May add fields later; avoid inventing unused option DTOs now |
| A3 | Publishing Health DTOs into OpenAPI only in Phase 2 is acceptable (no DummyForGeneratingTypes entry) | Pitfall 4 | If someone expects schema in Phase 1 CI OpenAPI snapshot — do not regenerate; Robots/OpenAPI snapshot should stay unchanged |

**If this table is empty:** All claims verified — not applicable; three low-risk discretion assumptions remain.

## Open Questions

1. **Should a registered rule that finds nothing still emit a group with empty `items`?**
   - What we know: D-02 only specifies zero rules → `groups: []`. Phase 2 empty-folder UX may want an empty group or omit it.
   - What's unclear: Product preference for “no issues” presentation.
   - Recommendation: Runner always appends whatever `evaluate` returns; Phase 2 rule decides empty vs omit. Do not invent filtering in Phase 1.

2. **Exact display titles for reserved rule ids**
   - What we know: ids are `empty_folders`, `readme_only_folders`, `dead_wiki_links`.
   - What's unclear: Human title strings (“Empty folders” vs longer copy).
   - Recommendation: Document ids in `HealthRuleIds`; titles live on each rule bean in later phases.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Nix + `CURSOR_DEV=true nix develop` | All backend commands | ✓ | flake present | Cloud VM skill if no Nix |
| Java toolchain (via Nix/Gradle) | Compile/tests | ✓ (host also has OpenJDK 24; Nix supplies project Java 25) | project Java 25 | Use Nix wrapper always |
| MySQL (test profile) | `backend:test_only` | Assume via `pnpm sut` / process-compose | MySQL 8.x | Inspect `mysql/mysql.log` |
| New npm/Maven packages | — | N/A | — | None required |

**Missing dependencies with no fallback:** none identified for Phase 1.

**Missing dependencies with fallback:** none.

Step 2.6 note: Phase 1 is code/config in the existing backend; no new external services.

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 5 via `spring-boot-starter-test` (Spring Boot 4.1.0) |
| Config file | Spring `test` profile; Gradle `backend/build.gradle` |
| Quick run command | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` |
| Full suite command | `CURSOR_DEV=true nix develop -c pnpm backend:verify` (when migrations involved; else `backend:test_only`) |

### Phase Requirements → Test Map

Phase 1 has **no REQUIREMENTS.md IDs** (enables Phase 2). Map success criteria:

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| SC-1 | Existing product behavior unchanged | backend suite | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` | ✅ existing suite |
| SC-2 | `HealthRule` + runner + DTO shapes exist | unit (new) | same suite (includes new test class) | ❌ Wave 0 |
| SC-3 | Zero rules → empty `groups` | unit (new) | same | ❌ Wave 0 |
| SC-3b | Nested group fields present (`items`, `children`) | unit (DTO construction) | same | ❌ Wave 0 |

### Sampling Rate

- **Per task commit:** `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
- **Per wave merge:** same (Phase 1 is backend-only)
- **Phase gate:** Backend suite green; **do not** run E2E or `generateTypeScript`

### Wave 0 Gaps

- [ ] `backend/src/test/java/com/odde/doughnut/services/health/HealthRuleRunnerTest.java` — covers SC-2/SC-3 (empty report)
- [ ] Optional: small test asserting `HealthFindingGroup` can hold both `items` and nested `children` without NPEs
- [ ] Framework install: none — JUnit already present

None of the gaps require new test frameworks. Prefer direct construction (`new HealthRuleRunner(List.of())`) over `@SpringBootTest` for the empty-rules case; full suite still boots Spring and will fail if `List<HealthRule>` wiring breaks.

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no (Phase 1) | Phase 2 controller must stay behind existing session/auth |
| V3 Session Management | no | — |
| V4 Access Control | no (no endpoint yet) | Phase 2: `AuthorizationService.assertAuthorization(notebook)` before lint `[CITED: .planning/research/ARCHITECTURE.md]` |
| V5 Input Validation | partial | DTOs reserved; no request body in Phase 1; later validate fix flags server-side |
| V6 Cryptography | no | — |

### Known Threat Patterns for Health lint contract

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Unauthenticated lint/fix API (future) | Elevation of privilege | Do not add permit-all Health routes; Phase 2 must authorize |
| Findings persistence leaking notebook content | Information disclosure | Ephemeral DTOs only (D-08) |
| Lint path performing deletes | Tampering / repudiation | Runner never mutates; separate fix phase |
| Client-only lint as authority | Spoofing | Backend-authoritative design (contract prepares for this) |

Phase 1 introduces **no new attack surface** if D-01 is honored (no routes).

## Project Constraints (from .cursor/rules/)

Actionable directives the planner must honor:

| Rule | Directive |
|------|-----------|
| `planning.mdc` | Phase is **Structure** only; stop-safe; no user-facing behavior change; capability naming (no phase numbers in product code); targeted tests; Jidoka / post-change-refactor / commit+push at phase wrap-up |
| `gsd-coexistence.mdc` | Local Behavior/Structure grammar + Nix prefix + commit/push wrap-up override plain GSD defaults |
| `general.mdc` | `CURSOR_DEV=true nix develop -c …` for tooling; git without Nix; high cohesion; no past-implementation comments |
| `backend-code.mdc` | Prefer entities when shape fits; introduce DTOs when wire shape differs (Health findings qualify); top-of-file imports; `pnpm backend:test_only` / `backend:verify` |
| `backend-testing.mdc` | Test behavior; services/algorithms directly only for intentional contracts (Health runner qualifies); one behavior per test; `makeMe` when DB fixtures needed (not required for empty-rules unit test); run full backend unit suite locally for agents |
| `frontend-api.mdc` / E2E rules | N/A for Phase 1 (no frontend/E2E) |
| `db-migration.mdc` | N/A — no schema / findings table |

## Sources

### Primary (HIGH confidence)

- `.planning/phases/01-health-lint-contract/01-CONTEXT.md` — locked decisions D-01…D-16
- `.planning/research/ARCHITECTURE.md`, `SUMMARY.md`, `STACK.md`, `PITFALLS.md` — contract sketch and anti-patterns
- `backend/src/main/java/com/odde/doughnut/services/AssimilationServiceFactory.java` + `AssimilationUnitSource` — `List<>` registry precedent
- `backend/src/main/java/com/odde/doughnut/controllers/dto/AttachBookLayoutNodeRequest.java` — recursive DTO precedent
- `backend/src/main/java/com/odde/doughnut/entities/Folder.java` + `V300000231__rename_index_content_to_readme_content.sql` — `readmeContent`
- `backend/build.gradle` — Spring Boot 4.1.0, Java 25, springdoc 3.0.3
- Spring Framework 6.2 Autowired reference — collection injection + `@Order` `[CITED: docs.spring.io/spring-framework/reference/6.2/...]` via Context7
- https://llmwikis.org/operations/lint/ — structural lint; propose removals separately `[CITED]`

### Secondary (MEDIUM confidence)

- Context7 `/websites/spring_io_spring-framework_reference_6_2` — empty multi-element injection + ordering (classify-confidence: MEDIUM even with `--verified`)
- `.planning/codebase/ARCHITECTURE.md`, `STRUCTURE.md`, `CONVENTIONS.md` — placement and naming

### Tertiary (LOW confidence)

- Whether Phase 2 prefers empty finding groups vs omitted groups for zero-hit rules (product UX)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — no new packages; versions verified in `build.gradle`
- Architecture: HIGH — CONTEXT locks layout; in-repo precedents for List registry + recursive DTOs
- Pitfalls: HIGH — Structure leakage, naming drift, premature OpenAPI regen are the main risks

**Research date:** 2026-07-22
**Valid until:** 2026-08-21 (30 days — stable Spring/Java patterns)
