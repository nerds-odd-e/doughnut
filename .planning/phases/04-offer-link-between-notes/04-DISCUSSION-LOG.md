# Phase 4: Offer link between notes - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-24
**Phase:** 4-offer-link-between-notes
**Mode:** `--auto`
**Areas discussed:** CTA placement & multi-match targeting, Preselection entry into add-link UI, Link type options on recall result, Permissions & post-link behavior

---

## CTA placement & multi-match targeting

| Option | Description | Selected |
|--------|-------------|----------|
| Per matched note CTA under each NoteShow | One "Link to this note" control per `matchedNotes` row | ✓ |
| Single CTA for primary `matchedNoteId` only | One offer; user cannot pick other matches without search | |
| CTA only in alert banner | Global offer, then pick target | |

**User's choice:** [auto] Per matched note CTA under each NoteShow (recommended default)
**Notes:** Aligns with Phase 3 multi-match reveal; user picks which confusion to connect.

| Option | Description | Selected |
|--------|-------------|----------|
| Reviewed → matched (source = reviewed) | Matches ROADMAP "connect the reviewed note to a matched note" | ✓ |
| Matched → reviewed | Reverse direction | |
| User chooses direction | Extra step before existing UI | |

**User's choice:** [auto] Reviewed → matched (recommended default)
**Notes:** Mirrors NoteToolbar SearchForm source-note pattern.

---

## Preselection entry into add-link UI

| Option | Description | Selected |
|--------|-------------|----------|
| Skip search; land on LinkInsertionChoice with matched target pre-set | Minimal effort; user still confirms type | ✓ |
| Open SearchForm with search prefilled by title | Still requires selecting the result | |
| Auto-create a default relationship | Forbidden by PROJECT out-of-scope | |

**User's choice:** [auto] Skip search; land on LinkInsertionChoice with matched target pre-set (recommended default)
**Notes:** Never auto-submit. Prefer smallest adapter around existing SearchForm stack; avoid AnsweredQuestion contract change if possible (D-04).

---

## Link type options on recall result

| Option | Description | Selected |
|--------|-------------|----------|
| Property wiki + relationship; hide bare wiki-insert without cursor | Matches AM-04 / PROJECT wording; avoids dead wiki-insert on recall | ✓ |
| Full LinkInsertionChoice including "Insert as a wiki link" | Wiki insert needs content cursor — poor fit on answer result | |
| Relationship note only | Narrower than AM-04 | |

**User's choice:** [auto] Property wiki + relationship; hide bare wiki-insert without cursor (recommended default)
**Notes:** Property path may use API content/property update if cursor inserter unavailable.

---

## Permissions & post-link behavior

| Option | Description | Selected |
|--------|-------------|----------|
| CTA only when user can write/link from reviewed note | Same class of gate as NoteToolbar link | ✓ |
| Always show CTA; fail on submit | Worse UX | |

**User's choice:** [auto] CTA only when user can write/link from reviewed note (recommended default)

| Option | Description | Selected |
|--------|-------------|----------|
| Close dialog; stay on recall result | Keep both notes visible; allow linking another match | ✓ |
| Auto-advance to next recall | Discards learning context | |

**User's choice:** [auto] Close dialog; stay on recall result (recommended default)

---

## Claude's Discretion

- CTA label/styling; SearchForm prop vs thin wrapper; how to fetch/build source Note + target NoteSearchResult; exact write-permission probe; unit vs E2E split (extend Phase 3 accidental-match coverage).

## Deferred Ideas

- Phase 5 alias-as-wiki-link; Phase 6 overlap try-again; bare wiki-insert from recall; v2 MCQ/fuzzy/qualified titles.
