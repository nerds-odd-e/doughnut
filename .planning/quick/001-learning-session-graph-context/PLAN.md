# Learning Session Request — graph context merged across session items

**Status:** in progress (Jidoka confirmed: defaultMaxDepth, `<related_notes>`, `(notebook, title)` dedupe; ADR wording later)

## Goal

A Learning Session Request today gives the Tutor each session item's own note body
only (`RetrievalConfig.focusNoteOnly()`, one `<focus_context>` per item). Give the
Tutor the same graph-structural context that question generation and note
conversation already get: retrieve **per session item** with the normal per-note
budget, then **merge all related notes into one deduped list** in the Request.

Scope stays controlled the way it already is: **per note**. No session-wide token
budget. Request size grows with the number of due items, which is accepted —
the cost is per note.

## Decisions

| Decision | Choice | Rationale / alternative |
|---|---|---|
| Retrieval config per item | `RetrievalConfig.defaultMaxDepth()` (depth 2, 2000 combined content tokens) | Same context depth as question generation and note conversation. Alternative if pastes feel too big: `RetrievalConfig.depth1()` |
| Merged list identity | `(notebook, title)` of the related note | ADR 0005 already makes title-within-notebook the protocol identity; `FocusContextNote` carries no note id, and adding one would change the `GET /notes/{id}/graph` JSON and require TS client regeneration |
| Collision rule | first-seen wins; merged list keeps insertion order (session item order, then retrieval order) | Simplest and reads in the same order as `<session_items>`. Alternative: order by `depth` ascending |
| Merged block element | `<related_notes>` containing the existing `<retrieved_note>` entries | A `<focus_context>` with no focus note inside would misname it |
| Per-item block | `<focus_note>` only (drop the per-item `<focus_context>` wrapper) | `Purpose:` / `Max depth:` are now session-wide and live on `<related_notes>` |

### Target Request shape

```
<session_items>
### Hola
- Tutoring status: not yet tutored
<focus_note>
Title: Hola
Notebook: Spanish conversation
Depth: 0

```doughnut-note-md
Hello. See [[Saludos]]
```
</focus_note>

### Gracias
...
</session_items>

<related_notes>
Purpose: Notes related to the session items, for tutor context.
Max depth: 2

<retrieved_note>
Title: Saludos
Notebook: Spanish conversation
Depth: 1
Path: [[Hola]] -> [[Spanish conversation: Saludos]]

```doughnut-note-md
Greetings
```
</retrieved_note>
</related_notes>

<how_to_report>
...
```

## Slices

### 1. Split focus-note and related-note markdown rendering — Structure
Status: done

Public `renderFocusNote` / `renderRelatedNotes`; `render()` composes them.
`<related_notes>` constants ready for slice 2. Existing focus-context consumers unchanged.

### 2. Related notes reach the Tutor as one merged list — Behavior
Status: done

Per item: `defaultMaxDepth()` + `<focus_note>`; one session-wide `<related_notes>`
via `MergedRelatedNotes` (notebook+title, first-seen). Removed `focusNoteOnly`.
E2E asserts related body; unit test asserts shared link appears once.

### 3. Session-item notes never appear as related notes — Behavior

A session item linked from another session item (Hola → `[[Gracias]]`) must appear
only under `<session_items>` with its full body, never in `<related_notes>`.

- Exclude the session items' `(notebook, title)` keys when merging.
- Test: `LearningSessionRequestTests` — cross-linked "Hola"/"Gracias" produce no
  `<retrieved_note>` for either.

### 4. Document the new Request shape — Docs

- ADR 0005 `docs/adrs/0005-commissioned-learning-session-protocol.md`: Request
  example and the focus-context sentence in the Decision section.
  **Human-owned** — propose the edit, do not self-approve (`architecture-decisions.mdc`).
- `docs/focus-context/focus_context_retrieval_design.md`: note the multi-focus
  merged consumer if the doc's consumer list needs it.

## Jidoka — confirm before slice 1

1. Depth per session item: `defaultMaxDepth()` (recommended) or `depth1()`?
2. `<related_notes>` as the merged block name?
3. Dedupe by `(notebook, title)` rather than threading note ids into
   `FocusContextNote` (which changes the graph API JSON)?
4. ADR 0005 edit in slice 4 — approve wording when it is proposed.
