---
id: SEED-082
status: dormant
planted: 2026-10-21
planted_during: owner review of the deployed note properties and timestamps
trigger_when: restoring the familiar property display and bottom timestamps
scope: small
---

# SEED-082: Read note properties directly and dates at the bottom

## Why This Matters

An author should see a note’s properties as part of its content without an
extra interaction. The owner requests recovery of the earlier presentation:
properties above the body, and Created / Last updated information at the
bottom. Useful improvements to property rows should survive that recovery.

## Alternatives and Decision

Restore the prior always-visible properties and bottom timestamps by
reconciling historical behavior with the current property editor. Delete the
whole-section collapse/expand mechanism and its exclusively used support;
changing its initial state to expanded would leave the unwanted mechanism.
Keep the improved property display and reading layout. The owner explicitly
requests no new navigation and no test that checks the deleted behavior’s
absence.

## Story Decomposition

S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours including delivery.
This seed is non-executable planning input.

<a id="story-1"></a>

### Restore visible note properties and bottom timestamps

**Identity:** SEED-082#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"unselected"}
```

**Goal:** A note reader or author can immediately read and work with the
note’s properties, then find its Created / Last updated information below
the content, as before the details-disclosure change.

**Scope — required:**

- Restore the always-visible property section in its established position
  above the note body for editable and read-only views. Preserve the existing
  rich/Markdown representations: rich mode presents property rows, and raw
  Markdown keeps its frontmatter editing. Existing empty-property behavior
  and Add property availability follow the current permissions.
- Restore Created and Last updated to the note footer after the body and
  displayed images, before references, reconciling the historical footer
  presentation with current code. Use the existing note timestamps and
  localized date formatting; keep the metadata readable and separate from
  the property editor. Preserve the scope of surfaces that displayed footer
  metadata before this change.
- Delete the whole-section collapse/expand control, property-summary strip,
  shared disclosure state, and transitively exclusive wiring, styling,
  helpers, date-only slot forwarding, and test/E2E reveal support. Delete the
  tests/assertions/fixtures used only
  for collapsing, expanding, default-hidden details, auto-revealing details,
  or the disclosure-specific unfinished-edit summary. Reconcile mixed tests
  around their surviving property-editing, focus, timestamps, or layout
  behavior. Do not add a permanent absence test, assertion, guard, or checker
  for this removal.
- Preserve the useful property-display improvements: quiet rows, scalar/list
  wrapping, narrow-screen stacking, accessible row controls, and touch
  targets. Keep current inline editing, add/edit/remove flows, drafts and
  validation, links/images, and existing row-level property actions.
  Removing the section disclosure does not delete row-specific editing or
  learning controls.
- Preserve existing property-focus and assimilation flows, including current
  property locations and missing-property feedback. Reconcile their
  disclosure-specific dependencies with properties being directly present.
  Retain eligible property-panel presentation tracking used by the existing
  active activity control.
  Keep the bounded reading column and body typography added independently
  of the visibility mechanism.

**Scope — constraints and deferred work:** The owner explicitly prohibits
adding navigation for this recovery. Use the existing note/property locations
and controls; remove their unnecessary reveal step. New routes, destination
controls, and replacement visibility preferences are outside the requested
outcome. Navigation/sidebar redesigns, new property capabilities, and changes
to authored titles or note content are deferred.

**Key examples:**

| Context and trigger | Observable result |
| --- | --- |
| Open an editable or read-only note with scalar and list properties | The property rows are directly readable above the body, using the retained wrapping/layout and appropriate editing permissions. |
| Add or edit a property, then encounter validation or save successfully | The current draft/error or saved value stays directly available through the existing editing flow. |
| Use an existing property location or property learning action | The correct property receives its existing focus or learning controls using the current navigation contract. |
| Read a note containing body text and an image | Created / Last updated appear below the content and image, before references, with the note’s actual timestamps. |
| Switch between rich mode and raw Markdown, or resize to a phone | The established property representation remains usable and the footer remains below the content; retained row layout and reading improvements still apply. |

**Implementation cohesion:** Keep frontmatter and the existing property
editor as the source of property data and editing state. Remove the shared
visibility concern outright rather than replacing it with an always-true
flag or a second preference. Keep the footer’s timestamp presentation separate
from property editing, and preserve existing focus/learning ownership while
deleting their disclosure-specific branches. Recover the relevant historical
presentation without reverting unrelated improvements wholesale.

**Evaluation:** Reuse and, where necessary, adapt proof of the surviving
property reading/editing, validation, existing focus/learning, footer dates,
and responsive layout. The removed disclosure tests are deletion scope, not
templates for replacement absence tests. During implementation acceptance,
record a one-time whole-product search for the deleted mechanism’s names and
exclusively used references as a reading, separate from behavioral proof.
Remove obsolete dependent guidance/generated references as needed; Git
history retains the implementation record.

**Effort hypothesis:** S — approximately 45–60 minutes, medium confidence,
assuming reuse of the property editor and historical footer. Untangling the
shared disclosure wiring from retained focus/learning tests is the main
sizing uncertainty.

**Depends on:** None.

**Safe stopping point:** Properties and footer dates are useful independently
of later navigation or sidebar changes, with the existing editor and learning
flows intact.

## Recovery Evidence

Source/history inspection on 2026-10-21 at main
`c10f6c2c288448026a3376861be3c8f93c14a0bc`; no runtime reproduction or product
tests were performed for this story-only request.

- The pre-disclosure presentation is recoverable from the parent of
  `8d30de7ea3`. `NoteTextContent` rendered its title and normal editable
  content; `NoteShow` placed dates after the image and before references.
- `8d30de7ea3` and `89a2bb85d1` introduced shared details visibility and
  automatic revealing; `4b9d3792f7` moved dates into that presentation;
  `f6ba658ab7` added the disclosure-specific unfinished-edit summary.
- `522f3f9f72` added the independent reading-column/body-type improvements.
  Current rich property components retain the row layout, editing, and
  property actions that this recovery preserves.

## Ordering and Scope Reduction

Place this single restoration story first in the queued product backlog, as
the owner requests. Preserve work already Taken and the order of all other
queued items. This instruction creates the story only; implementation and
slice planning remain for a later instruction.

## Open Decisions

None.

## When to Surface

Now, following the owner’s request to recover properties and footer dates.
