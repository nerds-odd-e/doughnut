# Phase 8: Match path and clickable titles - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-08-05
**Phase:** 8-match-path-and-clickable-titles
**Mode:** `--auto`
**Areas discussed:** Path data source, Path chrome & row layout, Title navigation boundary, Hydrate timing & loading UX

---

## Path data source

| Option | Description | Selected |
|--------|-------------|----------|
| Client `NoteRealm` hydrate via `getNoteRealmRefAndLoadWhenNeeded` | Keep `NoteTopology[]`; load path on demand (research default) | ✓ |
| Enrich `AnsweredQuestion.matchedNotes` with notebook/ancestors | Backend/OpenAPI Structure work on critical path | |

**User's choice:** [auto] Client `NoteRealm` hydrate (recommended default)
**Notes:** Matches STACK/ARCHITECTURE preference; avoids widening `NoteTopology`.

---

## Path chrome & row layout

| Option | Description | Selected |
|--------|-------------|----------|
| Title (`NoteTitleWithLink`) above `BreadcrumbWithCircle`; thin row component | Identity-first; reuse existing chrome; row owns hydrate | ✓ |
| Plain notebook name string only | Weaker PKM disambiguation; skips folder trail | |
| Mount compact `NoteShow` header in dialog | Risks body/peek regression | |

**User's choice:** [auto] Title above BreadcrumbWithCircle + thin row component (recommended default)
**Notes:** No body peek; row extract prepares Phase 9 actions without nested PopButton.

---

## Title navigation boundary

| Option | Description | Selected |
|--------|-------------|----------|
| `NoteTitleWithLink` → leave recall; Modal route-close; AMR-05 reopen = Phase 12 | Ship clickability now; reopen polish later | ✓ |
| Open match in new tab (`target=_blank`) | Unusual for in-app note navigation | |
| Block navigation / in-dialog peek | Conflicts with locked no-peek rule | |

**User's choice:** [auto] NoteTitleWithLink navigate away; AMR-05 deferred to Phase 12 (recommended default)
**Notes:** Pitfall 6 reopen guarantee is Phase 12 success criteria, not Phase 8.

---

## Hydrate timing & loading UX

| Option | Description | Selected |
|--------|-------------|----------|
| Hydrate on dialog mount; title immediate; path fills per row | Avoid CTA-click waterfall; no whole-list block | ✓ |
| Block dialog open until all realms loaded | Spinners before any identity | |
| Eager hydrate before Resolve CTA | Wastes fetches when user never opens dialog | |

**User's choice:** [auto] Hydrate on dialog mount; progressive path fill (recommended default)
**Notes:** Aligns with PITFALLS “load on dialog open”; title remains usable without path.

---

## Claude's Discretion

- Exact row component / `data-testid` naming
- Visual density of breadcrumb inside dialog rows
- Whether folder segments stay clickable via stock `BreadcrumbWithCircle`

## Deferred Ideas

- AMR-05 reopen-after-navigate — Phase 12
- Build a link / overlap declare — Phases 9–11
- API path enrichment — only if later product needs it
