---
id: SEED-033
status: dormant
planted: 2026-09-18
planted_during: investigation of embedded NoteShow and owner backlog request
trigger_when: story 1 now; story 2 after higher-priority queued work
scope: medium
---

# SEED-033: Simplify note presentation outside the note page

## Why This Matters

Learners see a full editable note beneath their spelling result even though
the result already links to the note. Removing it makes the result easier to
scan and removes one consumer of the full note display. The owner accepts
this small gain on its own.

Two other embedded consumers remain: Message Center's conversation subject,
and memory-tracker review through Just review. Eventually removing those
embeds should let maintainers simplify NoteShow and NoteShowPage completely
around the note page's needs, with fewer constraints on its menu and layout.
Their replacement interactions are deliberately undecided until later.

## Alternatives and Decision

The owner chose two stories on 2026-09-18: remove the spelling-result embed
first, and queue the remaining embedded-use removal and resulting cleanup
last. Keeping all embeds avoids navigation changes but retains unnecessary
weight on spelling results. Removing all three at once would require deciding
conversation and Just review behavior before realizing the small known gain.
Keeping a hidden or configurable spelling embed would not deliver the removal.

## Story Decomposition

Effort bands follow SEED-018: S = 30–60 minutes, M = 1–2 hours,
L = 2–4 hours. These are hypotheses including delivery, not commitments.

<a id="story-2"></a>

### 2. Use conversations and memory-tracker review without embedding the full note page

Unrefined candidate; the owner explicitly deferred figuring out the replacement
interactions. No executable plan yet.

- **Goal / beneficiaries:** Conversation participants and learners can complete
  conversation and Just review tasks without an embedded full NoteShow;
  maintainers can make the note page and its menu/layout serve one context.
- **Evaluation:** Both workflows have agreed, usable replacements for their
  full-note embeds. No non-note-page production caller uses NoteShow, and the
  page no longer carries props, branches, slots, or wrappers whose only purpose
  was supporting those embeds. Preserve capabilities still needed by the note
  page itself; simplification need not mean flattening every component.
- **Scope direction:** Remove the conversation-subject and MemoryTrackerAsync
  embeds and clean up the resulting unnecessary NoteShow/NoteShowPage machinery.
  Include currently unused options where confirmed unnecessary. Do not replace
  embedding with a duplicate full note-page implementation.
- **Value / learning:** Remove the remaining constraints imposed by embedding
  while learning which context each workflow actually needs.
- **Effort hypothesis:** M–L — low confidence pending the replacement UX;
  reassess and split if refinement discovers independent product outcomes.
- **Depends on:** Story 1 for the final single-context outcome.
- **Safe stopping point:** Both replacement workflows remain usable and full
  note details are reachable; the note page retains its own editing and
  conversation capabilities.
- **Open decisions for later refinement:** What note context and navigation
  should conversations retain? What replaces the note shown during Just review,
  so the learner can still review and grade meaningfully? Which remaining page
  controls and component boundaries become unnecessary after those choices?

## Ordering and Scope Reduction

By explicit owner priority, story 1 is first in the queued backlog and story 2
is last. Existing Taken work and unrelated queue order are unchanged. The
near-future notebook-publication direction remains unchanged. Story 2 is the
first to defer; story 1 delivers useful simplicity independently.

## When to Surface

Story 1 now. Refine story 2 when it reaches priority or the owner selects the
broader note-page cleanup.

## Breadcrumbs

- 2026-09-18 investigation: NoteShow has four production consumers: the note
  page, spelling results, conversation subject, and memory-tracker Just review.
- Owner request: queue the first removal first and refine/plan it; queue removal
  of the other two embeds and complete resulting page simplification last.
- [SEED-032](SEED-032-retry-overlapped-spelling-match.md#story-1) owns the
  separate declared-overlap inline-retry interaction.
