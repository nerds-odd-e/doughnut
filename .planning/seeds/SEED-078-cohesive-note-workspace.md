---
id: SEED-078
status: dormant
planted: 2026-10-20
planted_during: owner-approved iPhone and iPad UX review and implementation reconciliation
trigger_when: improving everyday note navigation and reading across phone, tablet, and desktop
scope: large
---

# SEED-078: Navigate and read notes through one cohesive workspace

## Why This Matters

For Donut authors and learners, crowded navigation, separate action bars, and
cramped note details should become a quiet, predictable workspace for opening,
reading, and editing knowledge on phones, iPads, and desktop windows, while
preserving authored content, learning behavior, and access rules.

The owner supplied iPhone and iPad screenshots and approved the **Single
header** direction, then its larger-window reconciliation, on 2026-10-20.
The command center is frequently used and must remain directly accessible.
Folder contents are a separate context, with their own persistent toggle.
The date in a note title is authored title text. Editing remains inline.

The read-only implementation assessment at `234f141b41` on 2026-10-20 found
existing measured toolbar overflow, active-tool pinning, global search, global
Undo history, and sidebar-dependent New note placement. It also found
different desktop and smaller-screen menu presentations and note-owned title
and dictation state. These are reuse and reconciliation inputs, not measured
usability results. No product runtime or tests were run for that assessment.
This decomposition starts on synchronized `main` at `a90233a446`.

The owner explicitly selected these stories ahead of the existing queued
work. This supports everyday use of deliberately captured knowledge without
changing the backlog's near-future direction or interrupting Taken work.

## Alternatives and Decision

- **Defer:** Keeps working commands but leaves the reported mobile clutter,
  clipped property values, and different navigation presentations unresolved.
- **Only restyle properties:** The strongest smaller alternative delivers
  useful reading value, and is retained as an independent story below. It
  does not deliver the approved command-center search and shared action bar.
- **Only merge the mobile bars:** Improves phone space, but leaves the owner
  learning a different arrangement when the same window widens. The owner
  explicitly requested cohesion across sizes.
- **Selected direction:** Keep one quiet command-center launcher and shared
  top-bar arrangement across sizes; reuse existing navigation, search, note
  actions, adaptive overflow, and editing. Improve property presentation and
  make secondary details revealable without changing their content model.

The first story tests the highest-risk product assumption: a directly
accessible, quiet command center can replace the always-visible desktop
destinations while remaining easy to find and use. Embedding search is a
separate useful outcome, not a new search capability. Broad navigation
redesign, new note operations, and a new editor are deferred.

## Approved UI Contract and Shared Conceptual Model

This is product and ownership direction for every story, not a technical
design, class model, or executable plan. Implementers refine the smallest
cohesive structure around these concepts rather than adding a model per size.

- **Donut command center — application scope:** App destinations, their active
  state and counts, Resume, account actions, and global note search. The book
  glyph is its launcher; it does not introduce another meaning for the domain
  entity **Book**, which remains an attached reading artifact.
- **Notebook context:** The current notebook/folder ancestry and folder
  contents, including notes, attachments, and child folders. One independent
  contents-toggle state determines a docked pane or a narrow-window drawer.
  New folder and sorting stay with this context. Existing New note placement
  follows the sidebar's visibility and the current creation context.
- **Active note tools:** One ordered set of existing note commands, with the
  same permissions, availability, active states, and measured overflow policy.
  The current note owns editor mode, drafts, title/body dictation intent, and
  Assimilation content even when its controls appear in the shared header.
- **Global history:** Undo addresses the latest available history operation,
  which may belong to another note; it is not reinterpreted as current-note Undo.
- **Note document and presentation:** The authored title, content, frontmatter
  properties, resolved links, and learning identities retain their existing
  meanings. Detail disclosure, wrapping, spacing, and timestamps are views of
  those concepts, not a second metadata or editing model.

The common arrangement is:

```text
Wide:   [Donut menu] [Contents] | [Notebook / folder context] [Note actions ...] | [Undo]
Narrow: [Donut menu] [Contents] | [Note actions ...] [More]                     | [Undo]
        [Notebook / folder context]
```

The book launcher stays a normal, quiet header control. Its menu opens over
the workspace and contains the actual global search surface and existing app
destinations, with counts, active destination, Resume, and account actions.
The folder toggle remains distinct. Wide windows dock the folder pane; narrow
windows use a drawer. More appears when the note tools need overflow. Width
changes context detail and the number of visible actions, not their meaning,
order, or ownership. A single shared header-height rule covers drawers and
safe-area offsets. Parent destinations remain reachable when context is compact.

Below the bar, the note keeps its full authored title and inline editing.
Properties use readable labels and values, comfortable touch controls, and
wrapping rather than a squeezed grid. A compact properties disclosure and
quieter, unambiguous creation/update details give the content visual priority.
Use consistent theme-aware contrast, roughly 17px body text with 1.5–1.6 line
spacing, comfortable phone side margins, and a readable wide-window text
measure as design starting points, not viewport-specific business rules.

Preserve note/property route-family continuity and conversation query state
under [ADR 0005](../../docs/adrs/0005-web-routes-accepted.md). Use the current
domain vocabulary from [ADR 0001](../../docs/adrs/0001-ubiquitous-language.md):
the speech-bubble note tool is **Conversation** and the upload-shaped tool is
**Export**, not newly invented Comments or Share commands. Authored titles,
properties, and links follow [ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md).

When a presentation is replaced, delete it and everything only it used,
reconcile its consumers, and record a one-time product sweep as an acceptance
reading. Behavioral proof exercises the retained command, navigation, and
editing journeys; it does not assert the absence of replaced UI or names.

## Story Decomposition

S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours, including focused proof,
cleanup, and delivery, following
[SEED-039](SEED-039-faster-ci-feedback.md#story-decomposition). The owner asks
for S stories here. The five hypotheses total roughly 3–4.5 hours; they are
not timing measurements or guarantees. Reassess a story likely to exceed one
hour during refinement and resplit around a useful user outcome before
execution. Do not make separate platform models or technical-only stories to
force the estimate. This seed authorizes no implementation or slice plan.

<a id="story-5"></a>

### Focus on note content while revealing details when needed

**Identity:** SEED-078#story-5
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** A reader can reach the authored content quickly and inspect
  properties or timestamps when useful, without the details dominating the note.
- **Outcome and boundary:** Use one compact properties disclosure showing
  concise existing context, with the current property section revealed on
  demand across sizes. Keep the most useful update date quiet but readable,
  with the creation date available in expanded details and unambiguous date
  formatting. Apply the agreed body spacing, side margins, text measure, and
  theme-aware hierarchy to this reading surface. Keep the exact authored
  title, including any date prefix, and existing inline editing. Disclosure
  must preserve an in-progress property edit and make validation feedback
  discoverable. Keep the existing property row and editor presentation.
- **Evaluation:** Open a note with several properties: its compact summary
  leaves the title and body prominent. Reveal properties and timestamps to
  inspect the same values and dates, then continue inline editing. An authored
  title such as `2026-05-15 Trip notes` remains one unchanged title. A property
  validation error remains discoverable when details are being used.
- **Value / learning:** Tests the approved content-first hierarchy without
  changing the note's data or requiring the header work to finish first.
- **Effort hypothesis:** S, 30–45 minutes, medium confidence; assumes
  presentation/disclosure around existing properties and timestamp values,
  with no alternate editor or persisted metadata model.
- **Depends on:** No blocking supplier story established. Existing property
  presentation is sufficient; improved row layout can be reconciled separately.
- **Safe stopping point:** Compact details improve reading independently of
  the command menu, merged header, or expanded-property restyling. Authorship,
  editable drafts, validation, and access rules remain intact on their own.

## Ordering and Scope Reduction

Place this selected set before the existing queue under the owner's explicit
priority instruction. Within the set, recommend stories 1–5 in this order.
Command-center consistency tests the largest navigation choice first; embedded
search makes that center more useful; the shared note bar then delivers the
main mobile space saving. Complete-value property access precedes optional
detail compaction because it fixes an immediate usability problem. Existing
Taken work and the relative order of unrelated queued stories stay unchanged.

Each story retains useful value if later stories are cancelled; shared
conceptual ownership and ordinary reconciliation avoid artificial supplier
dependencies. Story 5 is first to drop for scope reduction; retain
the selected command-center/search and shared-header outcomes as the core.
If early use challenges menu discoverability or refinement invalidates the
hour-long sizing hypothesis, return to that story's boundary before planning
execution. Do not disguise an overrun as another layer or platform story.

## Open Decisions

No unresolved parent framing or priority decision remains: the owner approved
the UI direction and asked for these bounded stories at the top of the queue.
Individual stories still need refinement and an execution-approach choice.
No implementation, timing run, manual-testing session, or executable plan is
authorized by this decomposition.

## When to Surface

Now, when selecting the next queued product improvement, under the owner's
2026-10-20 instruction to prioritize the approved responsive note workspace.

## Breadcrumbs

- Owner's iPhone and iPad screenshots, corrections, approval of **Single
  header**, larger-window review, and 30–60 minute backlog instruction in this
  conversation, 2026-10-20. The UI contract above retains the design without
  requiring session-local mock files or private example-note content.
- [Current product backlog](../PRODUCT-BACKLOG.md) and
  [short-term architectural direction](../NORTH-STAR.md).
- [Related expanded property-list story](SEED-064-note-properties-fixes.md#story-7).
- [Accepted domain vocabulary](../../docs/adrs/0001-ubiquitous-language.md),
  [Markdown profile](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md),
  and [web routes](../../docs/adrs/0005-web-routes-accepted.md).
