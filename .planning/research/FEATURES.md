# Feature Research

**Domain:** Notebook / wiki content-health linting with optional gated auto-fix
**Researched:** 2026-07-22
**Confidence:** HIGH

## Feature Landscape

### Table Stakes (Users Expect These)

Features users assume exist for a notebook Health / vault-lint product. Missing these = the capability feels incomplete or unsafe.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Dedicated Health surface | Obsidian plugins (Broken Links, Vault Inspector) and LM Wiki lint all expose a **scan + report** entry point, not only inline styling | LOW | Doughnut: Notebook Settings → **Health** tab beside Index/Settings (`WorkspaceIndexSettingsTabs`). No separate `/health` route. |
| Explicit **Run lint** action | Health checks are intentional passes (Karpathy: periodic lint; LM Wiki: “run a lint pass”), not silent background mutation | LOW | On-demand run for this notebook; show progress/empty state. |
| **Dead wiki-link** findings (note body) | Universal structural check: every `[[wiki-link]]` must resolve (LM Wiki, kb-lint `links`, Obsidian Broken Links) | MEDIUM | Doughnut already renders `dead-link` in the editor; Health must **aggregate notebook-wide**, not only note-show. |
| Dead links in **frontmatter / properties** | Property values are first-class link carriers in Doughnut and in wiki frontmatter checks; reporting body-only under-counts rot | MEDIUM | Align with existing property wiki rendering / `NotePropertyIndex` null-target behavior. |
| **Navigable, nested findings** | Users expect group-by-type and drill-down (Obsidian: folder/file/link views; Vault Inspector reports) | MEDIUM | Expandable sections on the tab: e.g. Empty folders → list; Dead links → by note. Click-through to note/folder is expected. |
| **Lint ≠ fix** separation | Ecosystem invariant: report first; never imply scan mutates (LM Wiki: “Do not delete pages. Propose removals separately.”; Vault Inspector default read-only; kb-lint separates `--report` vs `--fix`) | LOW | Always show findings without applying changes. Fix is a second, gated control. |
| **Empty-folder** structural rule | Folder-first notebooks accumulate empty shell trees; users expect folder hygiene in a Health product for Doughnut’s model | MEDIUM | **Recursive:** folder is empty iff entire subtree has **no notes** (nested empty shells count). Leaf-only misses the real rot pattern. |
| Safe, **opt-in** destructive fix only | Production wiki practice: mechanical auto-fix for unambiguous issues; deletes require explicit consent (LM Wiki: human review when fix deletes; Eva-brain: deletions hold for review; team gist comment: lint bot auto-fixes mechanical breakage only) | MEDIUM | v1 fix = **bulk remove empty folders** only. Fix control **active only** when that option is selected. |
| Persist lint/fix **preferences** | Users expect not to re-toggle options every visit | LOW | v1: **user-level defaults** across notebooks (not per-notebook). |

### Differentiators (Competitive Advantage)

Features that set Doughnut Health apart from file-vault plugins and LLM-wiki scripts—aligned with Core Value, not scope bloat.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| **Notebook-scoped** Health in product UI | Vault plugins and CLI linters sit outside the app; Doughnut runs Health where users already manage the notebook | LOW–MEDIUM | Uses existing settings chrome; findings respect notebook authorization boundaries. |
| Reuses **live dead-link semantics** | Same resolve rules as editor/properties (no second, divergent “lint resolver”) | MEDIUM | Competitive vs ad-hoc markdown scanners that miss aliases, notebook-qualified titles, or property-only links. |
| **Recursive empty-folder** definition as a first-class rule | Few wiki linters model folders as containers (file trees ≠ Doughnut folders); recursive “no notes in subtree” is high-signal, low-judgment | MEDIUM | Differentiates from note-only tools (orphans, empty notes) without inventing semantic lint. |
| **Single bulk structural fix**, not a fix menu | One clear action: remove all reported empty folders when opted in—avoids per-item choreography | LOW | Matches “safe fix when unambiguous”; Fix disabled until option selected. |
| **Mechanical-only immune system** in v1 | Instant, deterministic, no LLM cost/latency; matches LM Wiki **structural tier** and kb-lint “No LLM required” | LOW for v1 | Positions Health as always-available hygiene; semantic tier stays a future lane. |
| User defaults that **travel across notebooks** | One preference set for how the user likes to lint/fix | LOW | Fits multi-notebook owners; per-notebook overrides deferred. |

### Anti-Features (Commonly Requested, Often Problematic)

Features that seem good in competitor checklists but are wrong for Doughnut **v1** (locked intent + ecosystem failure modes).

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| **LLM / semantic lint** (contradictions, stale claims, weak content, synthesis) | Karpathy lint and LM Wiki semantic tier center on these | Needs models, review UX, false positives, cost; blocks shipping mechanical value | Defer; design rule registry so semantic rules can plug in later |
| **Auto-fix dead links** (create note, retarget, strip link) | Obsidian Link Updater; “heal the graph” | Wrong auto-write is worse than a dead link; needs judgment (LM Wiki: create / update / redirect are human choices) | Report-only + navigate to note; optional retarget later as explicit flows |
| **Per-folder multi-select** delete | Fine-grained cleanup | UI complexity; Fix gating becomes a selection matrix; easy to miss nested shells | Bulk option only for all empty folders in the report |
| **Empty-note** lint | Vault Inspector / kb-lint “thin / empty content” | Ambiguous (draft stubs, placeholders); not in locked v1 rule set | Defer as optional rule after empty folders prove useful |
| **Orphan notes** (no inbound links) | Karpathy / LM Wiki / kb-lint orphans | Orphans are signal, not always defects; auto-linking is judgment | Report-only candidate for v1.x+, never auto-link |
| **Silent auto-fix on open/save** | “Always healthy” | Surprises, undo pain, trust loss; conflicts with lint≠fix | Opt-in Fix after explicit Run |
| **Scheduled / CI lint bots** | Team wiki production patterns | Needs jobs, notifications, PR semantics; not personal-notebook v1 | Future ops surface; v1 is on-demand in UI |
| **Dedicated `/health` route or findings dialog** | Plugin side panels | Extra navigation; loses notebook settings context | Expandable results **on Health tab** |
| **Per-notebook defaults** | Different notebooks, different strictness | Doubles settings surface; v1 locked to user-level | User defaults; notebook overrides only if users demand later |
| **Full severity taxonomy UI** (Blocker/High/Medium/Low) | LM Wiki severity table | Over-designed for two mechanical rules | Simple groupings by rule type; severity model can inform DTO later without UI labels |
| **External URL / HTTP link checks** | Vault Inspector optional | Network, timeouts, false negatives; not wiki structure | Out of scope |
| **Frontmatter schema completeness** (required fields, enums) | LM Wiki / kb-lint frontmatter rules | Doughnut properties are flexible, not a fixed wiki schema | Only **dead wiki links in property values**, not schema enforcement |
| **Dry-run preview of every fix as a separate mode** | kb-lint `--fix --dry-run` | Report already lists what Fix will remove (empty folders); second mode is redundant in v1 | Findings list = preview; Fix applies that set |

## Feature Dependencies

```
Health tab (entry)
    └──requires──> Notebook settings tab shell (existing)
    └──requires──> Run lint (on-demand scan API)

Run lint
    ├──produces──> Nested findings (empty folders + dead links)
    │                 ├──requires──> Empty-folder rule (recursive, no notes in subtree)
    │                 └──requires──> Dead-link rule (body + frontmatter/properties)
    │                                   └──requires──> Shared wiki-link resolve semantics (existing editor/property behavior)
    └──reads──────> User lint/auto-fix defaults

Auto-fix option: “Remove empty folders”
    └──requires──> Empty-folder findings present (or empty set)
    └──enables───> Fix action (disabled unless option selected)

Fix action
    └──requires──> Auto-fix option selected
    └──applies───> Bulk remove empty folders only
    └──conflicts──> Per-folder multi-select (deliberately excluded)

Dead-link findings
    └──conflicts──> Auto-fix dead links (deliberately excluded in v1)

User-level defaults
    └──enhances──> Run lint + Fix gating (remember options across notebooks)
    └──conflicts──> Per-notebook defaults (v1)

LLM semantic rules
    └──enhances──> Future rule registry (not v1)
    └──conflicts──> Mechanical-only v1 delivery
```

### Dependency Notes

- **Health tab requires existing notebook settings shell:** Ship as a third tab; do not invent a new workspace navigation model.
- **Nested findings require both rules in one pass:** Users expect one Run to cover folder rot and link rot (LM Wiki / kb-lint single lint pass).
- **Dead-link rule requires shared resolve semantics:** Health must not invent a second linker; reuse title/path resolution used by `dead-link` rendering and property indexes.
- **Fix requires opt-in bulk option:** Ecosystem lesson—mechanical deletes only with explicit consent; Fix inactive until “remove empty folders” is selected.
- **Dead links conflict with auto-fix in v1:** Same as kb-lint (`links` not auto-fixable) and LM Wiki (propose, don’t invent pages).
- **User defaults enhance, not block, first Run:** Defaults pre-check options; missing defaults still allow a manual run with explicit toggles.

## MVP Definition

### Launch With (v1)

Minimum viable Health capability — validates mechanical immune system + one safe fix.

- [ ] **Health tab** on notebook settings — entry + stay-on-tab results (no route/dialog)
- [ ] **Run lint** — notebook-scoped structural pass
- [ ] **Empty folders rule** — recursive: subtree has no notes
- [ ] **Dead wiki links rule** — body + frontmatter/properties; report-only
- [ ] **Nested expandable findings** — group by rule; drill to note/folder
- [ ] **Optional auto-fix: bulk remove empty folders** — Fix active only when option selected
- [ ] **User-level defaults** for lint/auto-fix options

### Add After Validation (v1.x)

Features to add once core Run → review → Fix loop is proven.

- [ ] **Click-to-resolve UX for dead links** — open note and focus first dead link / prefill create-or-retarget (report still only; no silent rewrite)
- [ ] **Finding counts / health summary chip** — at-a-glance notebook health without opening every section
- [ ] **Empty-note rule** (optional, off by default) — only if users ask after empty folders
- [ ] **Orphan notes report** (info severity) — list only; no auto-link
- [ ] **Per-notebook override** of user defaults — if multi-notebook owners need different strictness
- [ ] **Lightweight severity on findings** — map empty folders / dead links to Medium (LM Wiki) for sorting only

### Future Consideration (v2+)

- [ ] **LLM semantic lint** — contradictions, stale claims, missing provenance, synthesis suggestions (Karpathy / LM Wiki tier 2)
- [ ] **Extensible rule registry** — enable/disable rules, severities, report vs fix capability per rule (LM Wiki / kb-lint model)
- [ ] **Safe auto-fix pack for non-delete mechanical issues** — only if Doughnut gains unambiguous non-destructive fixes
- [ ] **Dead-link repair flows** — explicit retarget-all / create-missing, never default auto
- [ ] **Scheduled lint / MCP or CLI `health` command** — agents and power users
- [ ] **Lint history / log entry** — LM Wiki `log.md` lint entry pattern

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| Health tab + Run lint | HIGH | LOW | P1 |
| Dead wiki links (body + frontmatter), report-only | HIGH | MEDIUM | P1 |
| Empty folders (recursive) | HIGH | MEDIUM | P1 |
| Nested expandable findings | HIGH | MEDIUM | P1 |
| Opt-in bulk remove empty folders; Fix gated | HIGH | MEDIUM | P1 |
| User-level defaults | MEDIUM | LOW | P1 |
| Navigate from finding to note/folder | HIGH | LOW–MEDIUM | P1 |
| Dead-link focus / repair assist (no auto-write) | MEDIUM | MEDIUM | P2 |
| Health summary counts | MEDIUM | LOW | P2 |
| Empty-note rule | LOW–MEDIUM | LOW | P3 |
| Orphan notes report | LOW–MEDIUM | MEDIUM | P3 |
| Severity taxonomy UI | LOW | MEDIUM | P3 |
| LLM semantic lint | HIGH (later) | HIGH | P3 |
| Auto-fix dead links | MEDIUM (risky) | HIGH | — (anti-feature v1) |
| Per-folder multi-select | LOW | MEDIUM | — (anti-feature v1) |
| Silent / scheduled auto-fix | LOW (v1) | HIGH | — (anti-feature v1) |

**Priority key:**
- P1: Must have for launch
- P2: Should have, add when possible
- P3: Nice to have, future consideration
- —: Explicitly do not build in v1

## Competitor Feature Analysis

| Feature | LM Wiki / Karpathy | Obsidian ecosystem | kb-lint / CLI wiki linters | Doughnut v1 approach |
|---------|--------------------|--------------------|----------------------------|----------------------|
| Broken / dead wiki links | Core structural check; Medium severity; fix = create/update/redirect (human) | Broken Links + Vault Inspector: report, navigate, group by folder/file/link | `links` rule, **not** auto-fixable | Report-only aggregate; body + frontmatter; reuse existing resolve/`dead-link` semantics |
| Empty folders | Not a primary concept (file trees / indexes) | Partial via folder views of other issues | N/A / structure is filenames | **First-class rule**, recursive “no notes in subtree” |
| Empty / thin notes | Content quality / semantic tier | Vault Inspector empty notes; kb-lint thin articles | Report, not auto-fix | **Out of v1** |
| Orphan pages | Karpathy + LM Wiki core semantic/structural signal | Find unlinked files plugins | Warning, not auto-fix | **Out of v1** |
| Frontmatter health | Required fields, provenance | Frontmatter type drift (Vault Inspector) | Missing/invalid YAML; some auto-fix defaults | **Only dead links in property values**, not schema enforcement |
| Auto-fix | Scripts for safe structural; **no silent deletes**; “propose removals separately” | Usually read-only reports; separate tools for retarget; trash for batch delete | Auto-fix only unambiguous (frontmatter defaults, renames, index); **not** links | **One** fix: bulk remove empty folders when opted in |
| Lint entry point | Prompt / agent command / period check | Plugin view / command palette | CLI `--report` / `--fix` / `--ci` | Notebook Settings → **Health** tab |
| Semantic / LLM lint | Karpathy centerpiece; LM Wiki tier 2 | Rare / external agents | Explicitly no LLM | **Deferred** |
| Preferences | Schema / severity in docs | Plugin settings | CLI flags | **User-level defaults** for options |

### Lessons distilled (for requirements)

1. **Immune system, not editor chrome alone** — Inline dead links are necessary but not sufficient; users need a notebook-wide pass (LM Wiki, Obsidian Broken Links).
2. **Structural first** — Ship deterministic rules before semantic/LLM (LM Wiki two-tier; kb-lint).
3. **Report before mutate; deletes are consentful** — Empty-folder removal is the only v1 mutate path and must be opt-in with Fix gated.
4. **Links stay report-only** — Industry default (kb-lint; LM Wiki human choices for create/retarget).
5. **One pass, multiple rule groups** — Nested results by rule type match Obsidian multi-view reports without full severity UI.
6. **Do not over-build config** — Severity registry and rule packs inspire architecture, not v1 settings sprawl.

## Sources

- [LM Wiki — Lint: Keep the Knowledge Graph Healthy](https://llmwikis.org/operations/lint/) — checks, two-tier model, severity, “do not delete / propose removals”
- [Karpathy llm-wiki gist](https://gist.github.com/karpathy/442a6bf555914893e9891c11519de94f) — Lint as periodic immune system (contradictions, orphans, stale claims, missing cross-refs); production comments on mechanical auto-fix vs content review gates
- [Obsidian Broken Links plugin](https://www.obsidianstats.com/plugins/broken-links) — folder/file/link views, navigate to containing note, counts
- [Obsidian Vault Inspector](https://github.com/rogerdigital/vault-inspector) — multi-rule health scan, broken links incl. headings, empty notes, default read-only
- [Obsidian Link Updater](https://github.com/felixscherz/obsidian-link-updater) — explicit dangling-link retarget (separate from report-only scan)
- [kb-lint](https://github.com/SingggggYee/kb-lint) — mechanical KB lint; links not auto-fixable; `--report` vs `--fix`
- Doughnut codebase — existing `dead-link` rendering (body + properties), notebook `WorkspaceIndexSettingsTabs`, folder-as-container model
- `.planning/PROJECT.md` — locked v1 intent and out-of-scope list

---
*Feature research for: notebook/wiki content-health linting and optional auto-fix*
*Researched: 2026-07-22*
