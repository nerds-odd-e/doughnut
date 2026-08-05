# Stack Research

**Domain:** Accidental-match resolve dialog UX (Doughnut v1.2 subsequent milestone)
**Researched:** 2026-08-05
**Confidence:** HIGH

## Recommended Stack

**Verdict: add zero new runtime libraries.** Implement the resolve dialog by composing existing Vue 3 + DaisyUI + Doughnut popup/breadcrumb/content-update seams. The only “stack” work is wiring and (optionally) a thin API shape tweak—not new packages.

### Core Technologies

| Technology | Version (repo pin) | Purpose | Why Recommended |
|------------|--------------------|---------|-----------------|
| Vue | 3.5.40 | Resolve-dialog UI composition | Already the frontend runtime; `Teleport` + SFC slots power `Modal` / `PopButton` |
| vue-router | 5.2.0 | Clickable matched-note titles; return-and-reopen | `Modal` already closes on `route.fullPath` change; use `noteShowLocation(noteId)` |
| DaisyUI | 5.7.15 | Button / alert / breadcrumb classes | Official DaisyUI 5 modal guidance prefers native `<dialog>.showModal()`—already what `Modal.vue` does; keep `daisy-btn` / `daisy-breadcrumbs` rather than stock `modal`/`modal-box` markup |
| Tailwind CSS | 4.3.3 | Layout spacing for dialog rows | Existing utility classes only |
| Spring / existing OpenAPI client | current backend + `@generated/doughnut-backend-api` | Note realm load + content update | Overlap declaration and path data already flow through these APIs |

### Supporting Libraries (reuse in-repo — do not npm install)

| Library / seam | Version | Purpose | When to Use |
|----------------|---------|---------|-------------|
| `PopButton` → `Modal` | in-repo | Optional “Resolve accidental match” dialog shell | CTA under accidental-match alert; same pattern as current link offer |
| `MatchedNoteLinkOffer` | in-repo | “Build a link” property / relationship | Nested inside resolve dialog (or second `PopButton` per row); already closes via `closer` and skips navigation |
| `BreadcrumbWithCircle` / `Breadcrumb` / `BasicBreadcrumb` | in-repo | Notebook + folder path under each match title | Feed `notebookRealm` + `ancestorFolders` from loaded `NoteRealm` |
| `getNoteRealmRefAndLoadWhenNeeded` | `StoredApiCollection` | Path + notebook name for matches | Preferred path source: `matchedNotes` today is bare `NoteTopology` (id/title only) |
| `buildWikiLinkText` | in-repo util | Wiki-link token for overlap alias | Same helper link-offer uses for property rows |
| `appendAliasToNoteContent` + `updateTextField(..., "edit content", …)` | in-repo | “Add as overlapped note” | Append `[[…]]` wiki-link item to frontmatter `aliases`; backend already treats wiki-link alias items as overlap declarations |
| `noteShowLocation` | in-repo | Title → note page | Clickable title without inventing a new route helper |
| `@lucide/vue` | ^1.28.0 | Folder/book icons in breadcrumb | Already used by breadcrumb components—do not add another icon pack |
| Vitest browser + Cypress | existing | Unit + E2E for dialog flows | Drive `AnsweredSpellingQuestion` / recall page; no new test framework |

### Development Tools

| Tool | Purpose | Notes |
|------|---------|-------|
| Existing frontend Vitest browser suite | Dialog list, link offer, overlap append | Prefer mounting recall answered UI; mock SDK via `mockSdkService` |
| Cypress E2E (`answered question` flows) | Optional resolve CTA; no try-again after dialog overlap | Keep `@wip` only while scenarios fail |
| OpenAPI TS regen (`generate-api-client`) | Only if matched-notes wire shape is enriched | Skip if client-side `NoteRealm` load is enough |

## Installation

```bash
# No new packages for v1.2 resolve dialog UX.
# Keep using the pinned frontend stack:
#   vue@3.5.40  vue-router@5.2.0  daisyui@5.7.15  @lucide/vue@^1.28.0
#
# If OpenAPI matchedNotes shape changes later:
#   CURSOR_DEV=true nix develop -c pnpm <existing generate-api-client workflow>
```

## Alternatives Considered

| Recommended | Alternative | When to Use Alternative |
|-------------|-------------|-------------------------|
| `PopButton` + existing `Modal` (`<dialog showModal>`) | DaisyUI stock `class="modal"` / `modal-box` markup | Never for this milestone—would fork from Doughnut’s modal stack, ESC handling, and route-close behavior |
| Client `NoteRealm` load for breadcrumb | Enrich `AnsweredQuestion.matchedNotes` with notebook + `ancestorFolders` (RecalledNote-like) | Only if match count / latency makes N realm loads painful; typical accidental-match sets are small |
| Nested `MatchedNoteLinkOffer` in resolve dialog | New dedicated “build link” API | Never—property/relationship flows already exist and stay `navigate-on-success=false` |
| `appendAliasToNoteContent` with `buildWikiLinkText` | New `POST …/declare-overlap` endpoint | Only if content-edit permissions or frontmatter parse failures become a product blocker |
| Stay on ACCIDENTAL_MATCH UI after dialog overlap | Re-answer spelling / flip outcome to OVERLAP | Never for dialog path—product rule: no try-again, no SRS credit reclaim |

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| Headless UI / Radix Vue / `vue-final-modal` / Vuetify dialogs | Duplicate modal stack; fight `modalStack` ESC + Teleport | `PopButton` / `Modal` |
| New breadcrumb or tree-path npm package | Path already modeled as `ancestorFolders` + notebook on `NoteRealm` | `BreadcrumbWithCircle` |
| Widening `NoteTopology` with folder path fields “for convenience” | Topology is intentionally title/id timestamps; other DTOs carry trail | Load `NoteRealm` or add a **separate** matched-note DTO if API enrichment is chosen |
| Stacking `NoteShow` for matches | Milestone goal is full-height reviewed note; bodies are out of scope in the dialog | Compact title + breadcrumb rows |
| Reusing OVERLAP try-again / regrade path for dialog overlap | Declaring overlap from dialog must not prompt try-again or reclaim credit | Content update only; leave answer outcome as `ACCIDENTAL_MATCH` |
| DaisyUI legacy checkbox / hash modals | DaisyUI 5 marks them legacy; SPA-hostile | Native dialog via existing `Modal` |
| New YAML/frontmatter parser | `yaml` + `parseNoteContentMarkdown` / alias helpers already ship | `appendAliasToNoteContent` |

## Stack Patterns by Variant

**If match count stays small (expected):**
- Use client-side `getNoteRealmRefAndLoadWhenNeeded(matched.id)` for notebook name + `ancestorFolders`
- Because `MatchedNoteLinkOffer` already depends on realm load; no OpenAPI change

**If product insists path appear before any extra fetch:**
- Enrich answer payload matched notes to carry `notebookId` / `notebookName` / `ancestorFolders` (mirror `RecalledNote` or `NoteSearchResult` + folders)
- Because bare `NoteTopology` cannot render a path; regenerate the TS client afterward

**If “Build a link” needs a second step inside the resolve dialog:**
- Nest `PopButton` + `MatchedNoteLinkOffer` (or swap dialog body like `LinkInsertionChoice` → `AddRelationshipFinalize`)
- Because `modalStack` already supports stacked modals; avoid a third-party dialog manager

**If user clicks a matched title then returns to recall:**
- Rely on vue-router navigation + existing answered-question state; reopen via the same CTA
- Because `Modal` auto-closes on route change today—do not fight that

## Version Compatibility

| Package A | Compatible With | Notes |
|-----------|-----------------|-------|
| vue@3.5.40 | vue-router@5.2.0 | Pinned together in `frontend/package.json` |
| daisyui@5.7.15 | tailwindcss@4.3.3 | DaisyUI 5 + Tailwind 4 already integrated; Dialog UX uses `daisy-btn` / breadcrumbs, not a DaisyUI major bump |
| DaisyUI 5 native `<dialog>` guidance | Doughnut `Modal.vue` | Confirmed aligned: `showModal()` + Teleport to `body` |
| `@generated/doughnut-backend-api` `matchedNotes: NoteTopology[]` | Path UI | Insufficient alone—pair with `NoteRealm` or enrich DTO |
| Frontmatter wiki-link alias items | Backend `FrontmatterAliases.overlapWikiLinkTokens*` | Overlap declaration is content-shaped; no separate overlap RPC |

## Integration Map (for roadmap)

| Capability | Integration point | Stack action |
|------------|-------------------|--------------|
| Resolve CTA | `AnsweredSpellingQuestion.vue` under accidental-match alert | Replace stacked `NoteShow` section with `PopButton` |
| Dialog rows | New small presentational component under `frontend/src/components/recall/` | Title link + `BreadcrumbWithCircle` + two actions |
| Build a link | Existing `MatchedNoteLinkOffer` | Reuse unchanged |
| Add as overlapped note | `appendAliasToNoteContent` + `storedApi.updateTextField` | Do **not** emit `retry`; do **not** re-call answer spelling |
| Path/breadcrumb | `NoteRealm.notebookRealm` + `ancestorFolders` | Prefer load-on-demand; optional API enrichment later |
| Tests | Vitest recall specs + Cypress answered-question page | Assert no `overlap-try-again` after dialog overlap |

## Sources

- Repo pins: `frontend/package.json` (vue 3.5.40, daisyui 5.7.15, vue-router 5.2.0) — HIGH (local)
- In-repo patterns: `PopButton.vue`, `Modal.vue` (`showModal` + Teleport), `MatchedNoteLinkOffer.vue`, `BreadcrumbWithCircle.vue`, `AnsweredSpellingQuestion.vue`, `noteShowLocation.ts`, `appendAliasToNoteContent` / `buildWikiLinkText` — HIGH (local)
- Wire shapes: `AnsweredQuestion.matchedNotes: NoteTopology[]`; `NoteTopology` lacks path; `NoteRealm.ancestorFolders` / `RecalledNote` carry trail — HIGH (generated OpenAPI + Java DTO)
- Context7 DaisyUI v5.0.50 modal docs — native `dialog.showModal()` recommended over checkbox/anchor legacy — MEDIUM (Context7; verified against repo `Modal.vue`)
- Context7 Vue modal examples — slot/Teleport-style modals remain standard; no need for a Vue dialog package — MEDIUM (Context7; Vue 2-era example snippets; recommendation still holds given in-repo Vue 3 Modal)

---
*Stack research for: accidental-match resolve dialog UX (Doughnut v1.2)*
*Researched: 2026-08-05*
