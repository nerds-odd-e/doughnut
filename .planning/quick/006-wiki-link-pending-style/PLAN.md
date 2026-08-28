# Pending style for unconfirmed wiki links

**Status:** planned — not started.
**Type:** ad-hoc plan (`.planning/quick/`)

## Goal

A wiki link the backend has not yet confirmed is visually distinct from a
**live** link and from a **known-missing** (dead) link. Infer that third
state from data the client already has. No new endpoint or DTO field.

## Origin

Today `wikiTitles` lists only resolved hits. Anything leftover is painted
`dead-wiki-link` (red), including tokens typed or flushed that the last
`NoteRealm` has not confirmed. `wikiTitles ?? []` and
`markUnresolvedAsDeadWikiLinks` collapse “not arrived yet” and “confirmed
missing.”

## Key design decisions

- **No new API.** `GET` / `PATCH` already return `NoteRealm` with
  `wikiTitles` after cache refresh
  (`TextContentController.updateNoteContent` → `refreshForNote` →
  `noteRealmService.build`).
- **Per-token inference** (not “any dirty content → all unmatched pending”):

  | Token vs last persisted snapshot | Style |
  |---|---|
  | In `wikiTitles` | live (`donut-wiki-link`) |
  | Not in `wikiTitles`, **and** present in last-saved note content (the markdown that built this `wikiTitles`) | dead (`dead-wiki-link`) |
  | Not in `wikiTitles`, **and** only in current unsaved / in-flight markdown | pending (`pending-wiki-link`) |

  Reuse `authoredLinkOccurrences` / the same token identity as
  `hasNewWikiLinkTexts`. Last-saved markdown is `note.content` on the
  loaded realm until the PATCH response refreshes it.
- **Pending window is the content PATCH**, not the autosave debounce.
  New wiki tokens already flush immediately
  (`hasNewWikiLinkTexts` → `shouldFlushImmediately`).
- **Third style (locked unless Jidoka):** class `pending-wiki-link` in
  `wikiLinkDomMarkers.ts` + `rich-content-wiki-links.scss`. Use
  `var(--color-warning)` and a **dashed** underline so it is neither
  accent/dotted (live) nor red (dead). Generic `a:not(.donut-wiki-link)`
  rules must also exclude pending.
- **Pending is not a dead link.** Click does not open create / point-at
  existing. Round-trip markdown treats pending anchors like live/dead
  (`quillHtmlToMarkdown`).
- **[ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)**
  constrains authored `[[target]]` spelling, not in-app chrome. No ADR
  conflict.

## Discoveries

- Note show does not render body until `NoteRealm` exists, so
  `wikiTitles === undefined` is not the user-visible wait. The wait is
  **new tokens vs last persisted snapshot** (dirty + PATCH in flight).
- E2E `'live wiki link': 'a:not(.dead-wiki-link)'` would match pending.
  Change it to `a.donut-wiki-link` when pending exists.
- `ScopedReadmeEditor` passes `:wiki-titles="[]"` (readme has no realm
  wiki titles). Out of scope; still looks dead after save.
- Holding the **real** PATCH (delay, then `continue`) avoids stubbing
  `NoteRealm` JSON in Cypress.

## Out of scope

- New wiki-title / unresolved-token payload.
- Notebook health dead-wiki findings.
- Readme wiki-link resolution.

## Slices

### 1. Unconfirmed body wiki link uses the pending style until save confirms missing (Behavior)

**Status:** planned

**Pre:** Loaded note; `wikiTitles` is the last persisted snapshot; body has
no `[[WikiLinks E2E Nowhere]]`.

**Trigger:** Learner adds that wiki link in rich content. Content save is
still in flight (new wiki tokens flush immediately).

**Post:** That token is a `pending-wiki-link`, not red dead. Existing saved
dead links (if any) stay dead. When the PATCH returns and the token is
still unresolved, it becomes `dead-wiki-link`.

Wire last-saved markdown (`note.content`) through
`NoteEditableContent` → `RichMarkdownEditor` → `replaceWikiLinksInHtml`.
Add marker, CSS, and markdown round-trip in this slice (needed for the
observable style; not a leading Structure-only slice).

Tests:

- Unit: `replaceWikiLinksInHtml` — new token vs last-saved → pending;
  token in last-saved and absent from `wikiTitles` → dead; hit in
  `wikiTitles` → live. `quillHtmlToMarkdown` pending → `[[…]]`.
- Unit: mounted note body with delayed `updateNoteContent` — pending
  then dead (`NoteEditableContent` / `NoteTextContent`).
- E2E: extend `e2e_test/features/note_topology/wiki_link.feature`. Hold
  real `PATCH /api/text_content/*/content`, assert pending, release,
  assert dead. Add kind `pending wiki link` in
  `noteContentEditingMethods.ts`. Point `live wiki link` at
  `a.donut-wiki-link`.

`@wip` until the scenario passes. Existing dead-after-save scenarios
must stay green.

### 2. Unconfirmed wiki link to an existing note becomes live after save (Behavior)

**Status:** planned

**Pre:** Target note already exists (e.g. `WikiLinks E2E CI`). Carrier
body has no wiki link to it yet.

**Trigger:** Learner adds `[[WikiLinks E2E CI]]` (or insert-wiki-link);
save in flight then completes.

**Post:** Pending while in flight; `donut-wiki-link` after the returned
realm includes that title. Must not stay dead.

Same inference as slice 1; this slice is the other post-condition.
E2E in `wiki_link.feature` (hold PATCH, then live). Upgrade helpers
(`upgradeDeadWikiAnchors` / pending → live) if in-flight HTML is
already a pending anchor.

Stop after slice 1: missing links no longer flash red; existing-target
links may still flash red then live until this slice.

### 3. Clicking a pending wiki link does not start the missing-note flow (Behavior)

**Status:** planned

**Pre:** Pending wiki link is visible (save held or delayed mock).

**Trigger:** Learner clicks it.

**Post:** Create-note / point-at-existing UI does not open. After the
link becomes dead, the existing click flow still works.

`handleRichContentAnchorClick` / Quill: only `dead-wiki-link` starts
that flow. Unit on mounted editor; E2E can share the held-PATCH setup
from slice 1.

### 4. Frontmatter property wiki links use the same three styles (Behavior)

**Status:** planned

**Pre:** Loaded note; last-saved YAML has no `[[WikiLinks E2E Nowhere]]`
(or equivalent path-markdown token) in a scalar / list value.

**Trigger:** Learner adds that token in a property value; save in flight
then completes missing.

**Post:** Pending then dead (same inference). List `WikiLinkToken` must
not treat default `wikiTitles: []` as “everything dead” for **new**
tokens; pass last-saved property text (or whole last-saved markdown).

Extend existing property wiki-link unit tests
(`propertyValueField.spec.ts`, list `WikiLinkToken`). E2E only if a
property-value scenario already lives in `wiki_link.feature` or a
frontmatter feature; otherwise unit at the property field is the
slice’s user-visible gate plus a small E2E if one feature already
edits property wiki links.

Stop after slice 3: body is correct; properties may still flash dead
until this slice.
