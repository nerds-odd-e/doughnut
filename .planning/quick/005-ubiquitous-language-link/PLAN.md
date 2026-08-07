# Ubiquitous language: replace bare “link”

## Goal

Where Doughnut means a **wiki link** and/or **relationship**, stop using bare **link** in user-facing language (then align tests and local identifiers). Cite ADR 0001 disambiguation rules. Stop-safe after any phase: earlier surfaces already speak glossary language.

## Design decisions

1. **ADR status** — Plan follows Proposed ADR 0001; if humans reject or change the glossary, stop and revise. Do not treat Accept as done.
2. **Copy table** — Use recommended strings in `CONTEXT.md` unless Jidoka picks alternatives; do not invent a new umbrella noun outside the glossary.
3. **Shared affordance** — Toolbar and accidental-match CTAs that open a *chooser* for wiki link **or** relationship use the compound phrase **wiki link or relationship**, not bare link.
4. **User copy before identifiers** — Behavior phases change what learners see; Structure phases rename packages/CSS only after copy is stable.
5. **No OpenAPI/MCP rename in this plan** — `outgoingLinks` / `linkText` stay until a deliberate API slice (breaking for clients).
6. **Capability-named artifacts** — Feature files stay domain-named (`link.feature` may be renamed to `wiki_link.feature` only as a Structure/Behavior cleanup for the wiki-link capability — not after phase numbers).
7. **Tests** — Prefer extending existing E2E/unit that assert the old strings; keep at most one intentionally failing test while driving a rename. Targeted Cypress only for touched features.

## Phases

### Phase 1 — Behavior: note toolbar label

**Status:** done
**Type:** Behavior

- Toolbar aria-label/title → `Wiki link or relationship` (+ shortcut); shared constant in component + E2E PO.
- Unit: `NoteToolbar.spec.ts`; Cypress: `note_topology/link.feature` green.

**Done when:** Targeted unit and/or E2E that assert the toolbar name pass; no user-facing bare `Link` on that control. ✅

---

### Phase 2 — Behavior: target-note chooser entry copy

**Status:** done
**Type:** Behavior

- CTA `Use this note`; header `Target:`; dead retarget `Point wiki link "…" at this note`.
- Helpers renamed away from “Add link”; Vitest + `link.feature` green.

**Done when:** Specs asserting the new strings pass. ✅

---

### Phase 3 — Behavior: dead wiki link resolution copy

**Status:** done
**Type:** Behavior

- Modal: `Dead wiki link:` / `Point at an existing note`; identifiers renamed away from bare link.
- Unit + `link.feature` green.

**Done when:** Dead-wiki-link create/retarget scenarios pass with new copy. ✅

---

### Phase 4 — Behavior: accidental-match connect CTA

**Status:** done
**Type:** Behavior

- CTA → `Add wiki link or relationship`; Vitest + `accidental_match_reveal.feature` green.

**Done when:** Accidental-match offer scenarios pass with new CTA text. ✅

---

### Phase 5 — Behavior: delete leaves dead wiki links

**Status:** done  
**Type:** Behavior

- Option → `Leave all references as dead wiki links`; Vitest + `note_deletion.feature` green.

**Done when:** Deletion scenarios that leave dead wiki links pass with new confirmation label. ✅

---

### Phase 6 — Behavior: rename warnings mention wiki links

**Status:** done  
**Type:** Behavior

- Path + notebook rename warnings say **wiki links**; shared confirm constant; unit coverage green.

**Done when:** Specs covering those warnings pass with glossary wording. ✅

---

### Phase 7 — Behavior: relationship assimilation wording in E2E

**Status:** planned  
**Type:** Behavior

- **Pre:** Assimilation/recall E2E uses type `link` and “notes and links” for **relationship** assimilation.
- **Trigger:** Author/run relationship assimilation scenario.
- **Post:** Gherkin and helpers say **relationship** (not bare link); product assimilation UI unchanged unless it also shows bare link (fix only if found).

**Touch:** `recall_pages.feature`, `assimilationFlow.ts` (`case 'link'`), related steps only as needed.

**Done when:** Targeted Cypress for that feature passes with relationship wording.

---

### Phase 8 — Behavior: wiki-link E2E Gherkin uses “wiki link”

**Status:** planned
**Type:** Behavior

- **Pre:** Wiki-link feature/steps say `the link "…"`, `dead link`, `via the link toolbar`, scenario `link and move`, etc.
- **Trigger:** Audit + rewrite Gherkin/steps/POs for wiki-link capability files.
- **Post:** Scenarios speak **wiki link** / **dead wiki link** / toolbar wording aligned with Phase 1; product behavior unchanged.

**Touch:** `e2e_test/features/note_topology/link.feature` (consider rename to `wiki_link.feature` in this phase), `folder_organization.feature` steps that say `the link`, `step_definitions/link.ts` / `note.ts`, related POs. Keep accidental-match “I link the matched note…” verbs only if they still read as actions; prefer “I connect…” or “I add … as wiki property / relationship” when editing those steps.

**Done when:** Targeted Cypress for wiki-link (and any rewritten specs) passes.

---

### Phase 9 — Structure: frontend wiki-link / relationship module names

**Status:** planned
**Type:** Structure

- **Enables:** Immediate consistency for maintainers after Behavior copy is done; no user-visible change.
- Rename `frontend/src/components/links/` and `LinkInsertionChoice` / `MatchedNoteLinkOffer` / `SvgSearchForLink` / shortcut `note-link` toward wiki-link or relationship capability names; update imports and tests.
- Do **not** rename OpenAPI fields in this phase.

**Done when:** Frontend unit tests for touched modules pass; no observable UI string regression.

---

### Phase 10 — Structure: DOM markers for wiki links

**Status:** planned
**Type:** Structure

- **Enables:** Selectors and health testids match glossary (`dead-wiki-link`, etc.) after copy already says dead wiki link.
- Rename CSS classes / data-testids (`dead-link`, `doughnut-link`, `notebook-health-dead-link-*`, `link-to-matched-note-*`) and update Quill/turndown/wiki-property helpers + E2E selectors together.
- Keep behavior identical (live vs dead wiki link rendering unchanged).

**Done when:** Targeted frontend unit + wiki-link / notebook-health / accidental-match Cypress pass.

## Out of scope (explicit)

- Accepting ADR 0001
- Bare **wiki** / Wikidata association copy audit
- `outgoingLinks`, `linkText`, focus-context `## Link targets`, MCP tool prose (API/docs slice)
- `components/svgs/link_types/` mass rename (optional follow-up; relationship-type icons already mostly `RelationType*`)
- Non-domain “link” (URLs, invitation, Quill, DaisyUI)

## Jidoka stops

- Disagree with CONTEXT microcopy table
- Any change that would alter wiki-link resolution or relationship creation behavior (this plan is naming only)
- Desire to split toolbar into two controls
- Desire to include OpenAPI renames in this plan
