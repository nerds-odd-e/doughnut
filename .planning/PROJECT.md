# Progressive Cancelable Blocking UI

## What This Is

This is a focused brownfield renovation of Doughnut's frontend loading experience. It extends the existing global blocking API spinner into one cohesive, opt-in cancellation mechanism, then proves the mechanism in the AI-assisted note-refinement flow and audits other blocking operations for safe adoption.

The shared layer owns request abortion, loading-state cleanup, and the standard cancelled outcome. Each usage site owns only the small piece of domain behavior that is genuinely specific to that interaction.

## Core Value

When a long-running frontend action blocks the UI, the user can cancel it and remain in a coherent, usable state without unintended follow-up behavior.

## Requirements

### Validated

- ✓ User-initiated frontend API calls can use a central loading and error-handling wrapper — existing
- ✓ Whole-UI loading is rendered through one global blocking modal rather than component-local modals — existing
- ✓ Blocking calls carry per-call state and messages, including correct cleanup for nested and concurrent calls — existing
- ✓ Generated API calls accept standard request options needed to pass an abort signal — existing

### Active

- [ ] A blocking API operation can opt into a shared cancel control that aborts the browser request and clears its loading state
- [ ] User cancellation is a normal outcome: it produces no error toast and triggers no success or navigation behavior
- [ ] Callers customize only their domain-specific post-cancel state while shared cancellation mechanics remain centralized
- [ ] Cancelling note-refinement layout generation leaves the refinement dialog open and makes retry available
- [ ] Cancelling extract-preview generation preserves the selected layout items and leaves the user before the preview
- [ ] Extracted-note creation uses the shared blocking experience without offering a misleading client-only Cancel action
- [ ] Existing whole-UI blockers and long-running AI actions are audited, with safe candidates migrated to the shared opt-in solution

### Out of Scope

- Guaranteed server-side cancellation — the first iteration aborts the browser request; cooperative backend cancellation can be added later where its value justifies the larger API design
- Universal cancellation for every API request or thin loading-bar operation — cancellation is explicitly enabled only for blocking interactions where the caller can define a safe outcome
- Offering cancellation for mutations that may already have committed server-side state — these require separate safety analysis or backend cooperation
- Client-only cancellation for extracted-note creation — retrying after an ambiguous abort could create duplicate notes or apply the original-note update twice
- A broad visual redesign of Doughnut's frontend — this effort is limited to cohesive blocking and cancellation behavior

## Context

- Doughnut is an existing Vue frontend backed by Spring Boot and a generated TypeScript API client.
- `apiCallWithLoading` in `frontend/src/managedApi/clientSetup.ts` already centralizes loading state and API error handling.
- `LoadingModal` is mounted globally from `DoughnutApp.vue`; callers opt into whole-UI blocking with `blockUi: true` or `runWithBlockingApiLoading`.
- The generated client options extend standard `RequestInit`, providing a path to pass `AbortSignal` without editing generated artifacts.
- The current codebase has six explicit whole-UI blocking call sites. These include assimilation, book-layout operations, and AI note-refinement operations.
- In the note-refinement flow, initial layout generation currently uses only the thin loading bar. Extract-preview generation and extracted-note creation already use the global blocking spinner, but none of these interactions is cancelable.
- The refinement flow is the proving ground because it contains several related long-running AI interactions with meaningfully different post-cancel UI states.

## Constraints

- **Delivery**: Introduce cancellation progressively through small, stop-safe slices — the effort should remain smaller than a broad frontend milestone
- **Cohesion**: Abort ownership, loading cleanup, cancelled-result representation, and cancellation error suppression belong in the shared API-loading layer — callers should contain only specialized behavior
- **Safety**: Do not imply that aborting the HTTP request guarantees backend work has stopped — the backend currently has no cooperative cancellation contract
- **Adoption**: Cancellation remains opt-in — a shared Cancel button must not make unsafe mutations appear reversible
- **Generated API**: Do not hand-edit generated SDK files — pass supported request options through generated controller calls
- **Planning**: Each implementation phase must be Behavior or Structure, stop-safe, and limited to one observable behavior; structure may prepare only the immediately following behavior
- **Verification**: Frontend tests should cover shared mechanics and each caller-specific cancelled outcome

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Start with progressive client-side cancellation | Delivers user control quickly without turning a focused frontend effort into a backend job-cancellation system | — Pending |
| Make cancellation opt-in per blocking operation | Not every request is safe to present as cancelable, especially mutations that may finish on the server | — Pending |
| Centralize generic cancellation mechanics | Keeps API abortion, loading cleanup, and cancellation classification consistent across the frontend | — Pending |
| Keep post-cancel UI behavior at each usage site | Layout generation, preview generation, and note creation must preserve different user state | — Pending |
| Prove the solution in the note-refinement flow | It exposes missing and existing spinners around related AI actions and demonstrates reuse across distinct outcomes | — Pending |
| Keep extracted-note creation non-cancelable in the first version | It is a transactional mutation, and aborting the browser request cannot safely promise that the note was not created | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `$gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `$gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-07-21 after initialization*
