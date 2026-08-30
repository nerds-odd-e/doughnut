# Property wiki integrity (013 follow-up)

**Status:** in progress (slices 1–2 done).
**Type:** ad-hoc plan (`.planning/quick/`)
**Policy:** [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md) (`#prop:` exact key, one cache). [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md) (**Wiki link**, **Property panel**). Proposed [ADR 0005](../../../docs/adrs/0005-web-routes.md) unchanged.

Shipped [013](../013-note-property-canonical-path/PLAN.md) made `noteProperty` the property location and `#prop:` live. Inspection of that work found two integrity gaps and leftover tests that still treat the **property value dialog** as location chrome.

## Goal

Rewrites that sanitize stored wiki targets keep the encoded `#prop:` suffix. Two property tokens that differ only in key case both stay live when both keys exist. Tests assert the **property panel** / location, not dialog absence.

## Inspection (what we will not change)

These looked like issues and are **not** in this plan:

- Read-only `noteProperty` has no property panel — 013 required focus + visible value only.
- `:close-on-route-change="false"` on the property value dialog — still needed so focused-key **rename** and conversation query `replace` do not dismiss the editor.
- Paste of `noteShow` URLs still using label as identity — Proposed ADR 0005; 013 slice 20 was `noteProperty` only.
- Visit / open-close / assimilate covered by both E2E and mounted-component tests — E2E is the user path; unit uniquely covers scroll, `replace`, and SDK bodies. Keep that split.
- Near-250-line files, inbound-referrer listing without a second property check, shared Java/TS fixture JSON — no observed product failure.

## Design decisions

- OS-invalid sanitization (`DisplayNamePathSeparators.replaceOsInvalidCharsInWikiLinkTarget`) must transform the **note-target** portion only, via `WikiLinkAuthoredTarget` (same `withNoteTarget` / `format` seam as title/folder/notebook rewrite). Do not parse `#prop:` as a notebook qualifier colon.
- Wiki-cache / unresolved **dedupe** must not NFKC+lowercase the encoded `#prop:` suffix. Note-target folding may stay as today; property keys compare case-sensitively (ADR 0004).
- Drop tests whose **unique** claim is that the property value dialog is closed. Do not add URL-unchanged asserts.

## Slices

### 1. OS-invalid sanitization keeps the `#prop:` suffix — **Behavior** — done

`replaceOsInvalidCharsInWikiLinkTarget` maps the note-target via `WikiLinkAuthoredTarget.mapNoteTarget` (same seam as title/folder/notebook rewrite). Encoded `#prop:` is unchanged; notebook-qualified and path-shaped forms keep `/` and `#prop:`. Unqualified `Moon#prop:…` stays a property token.

### 2. Case-distinct property tokens in one note both stay live — **Behavior** — done

`WikiLinkMarkdown.uniqueAuthoredTokensPreserveOrder` folds only the note target; encoded `#prop:` keys stay case-sensitive. Save/load keeps two `link_text` rows when both YAML keys exist; notebook health still lists a missing-case key as dead. Note-only titles still fold.

### 3. Tests assert the property panel, not dialog absence — **Structure** — planned

013 forbade tests whose unique claim is that the property value dialog is closed. Remove that leftover; existing product behavior unchanged.

- E2E `note_property.feature` delete scenario: drop `And the property value dialog should be closed` (location already asserted). Remove the Then, `expectPropertyValueDialogClosed`, and the step if unused.
- Retitle specialized / read-only scenarios away from “without a property value dialog”. Keep the unique positives (panel / focused value / not-found).
- Drop `propertyValueDialogEl() === null` from `propertyFocus.spec.ts` and `propertyDeleteLocation.spec.ts` where it is not the unique claim.
- Drop the href-only compile duplicate in `replaceWikiLinksInHtml.spec.ts` (“compiles a resolved property wiki target to noteProperty”) — keep `wikiLinkResolvedLocation.spec.ts` and the path-Markdown leftover-fragment test.

No new tests. This slice is stop-safe cleanup of the 013 test suite; it does not unlock a further behavior.

## Discoveries

- OS-invalid sanitization now shares `WikiLinkAuthoredTarget.mapNoteTarget` with title/folder/notebook rewrite; the colon-split sanitizer runs on the note-target only.
- Wiki-token uniqueness lives in `WikiLinkMarkdown.uniqueAuthoredTokensPreserveOrder` (note-target fold, case-sensitive `#prop:`). Health dead-property tests split to `NotebookHealthControllerDeadPropertyWikiLinksTest` so files stay under 250 lines.
- `inboundReferrerNotesForViewer` does not re-check property liveness; outgoing read does. Slice 18 already drops stale inbound rows on target refresh. Left out unless a user-visible inbound bug shows up.

```
## SLICE PLAN WRITTEN
```
