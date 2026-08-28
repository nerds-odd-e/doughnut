# Pending style for unconfirmed wiki links

**Status:** in progress — slices 1–2 done; slices 3–4 remaining.
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

**Status:** done

Last-saved markdown (`note.content`) is passed through `NoteEditableContent`
→ `RichMarkdownEditor` → `replaceWikiLinksInHtml`. Unresolved tokens only in
current markdown are `pending-wiki-link`; unresolved tokens already in
last-saved stay `dead-wiki-link`; hits in `wikiTitles` stay live.
`quillHtmlToMarkdown` round-trips pending anchors as `[[…]]`. E2E holds real
`PATCH /api/text_content/*/content`, asserts pending, then dead.

### 2. Unconfirmed wiki link to an existing note becomes live after save (Behavior)

**Status:** done

`upgradeUnresolvedWikiAnchors` upgrades pending and dead when `wikiTitles`
has a hit, and runs before confirming leftover pending as dead, so in-flight
HTML goes pending → live. E2E holds real PATCH, asserts pending, then live
(`WikiLinks E2E Tech` → `[[WikiLinks E2E CI]]`).

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
