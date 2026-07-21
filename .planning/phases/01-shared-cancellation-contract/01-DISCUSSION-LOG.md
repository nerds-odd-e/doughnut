# Phase 1: Shared Cancellation Contract - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-21
**Phase:** 1-Shared Cancellation Contract
**Areas discussed:** Cancellation outcome, UI release timing, Overlapping blockers, Cancellation feedback

---

## Cancellation outcome

### How should an opted-in caller learn that the operation was cancelled?

| Option | Description | Selected |
|--------|-------------|----------|
| Explicit tagged outcome | Distinct cancelled result prevents accidental success handling | ✓ |
| Empty result | Caller infers cancellation from absent data | |
| Dedicated exception | Caller catches a cancellation-specific exception | |
| You decide | Planner chooses the contract | |

**User's choice:** Explicit tagged outcome.

### How much should the shared layer normalize?

| Option | Description | Selected |
|--------|-------------|----------|
| Two-way result | Completed with existing result, or cancelled; existing error behavior remains | ✓ |
| Three-way result | Normalize success, cancellation, and failure | |
| Add cancellation flag | Add a boolean to the SDK-shaped result | |
| You decide | Planner chooses the narrowest type-safe design | |

**User's choice:** Two-way result.

### What should the cancelled outcome carry?

| Option | Description | Selected |
|--------|-------------|----------|
| Status only | No payload beyond cancelled status | ✓ |
| Stable reason | Include a source such as `user` | |
| Abort details | Expose browser abort error or signal reason | |
| You decide | Planner keeps only proven information | |

**User's choice:** Status only.

### Must callers branch before using completed data?

| Option | Description | Selected |
|--------|-------------|----------|
| Require explicit branch | Completed data is unavailable until status is checked | ✓ |
| Convenience fallback | Permit unwrapping with a default | |
| Optional checking | Preserve flexibility at the risk of accidental success handling | |
| You decide | Planner selects the practical type contract | |

**User's choice:** Require an explicit branch.

---

## UI release timing

### When should the blocking state disappear?

| Option | Description | Selected |
|--------|-------------|----------|
| Immediately | Abort and remove the operation's blocker in the same action | ✓ |
| After abort settles | Keep blocking until the request settles | |
| Show Cancelling | Keep a disabled control and transitional message | |
| You decide | Planner chooses the race-handling behavior | |

**User's choice:** Immediately.

### Which outcome wins a Cancel/completion race?

| Option | Description | Selected |
|--------|-------------|----------|
| Accepted Cancel wins | Suppress any late completion after cancellation is accepted | ✓ |
| First settled wins | Use whichever event completed internally first | |
| Server response wins | Treat any arriving response as completed | |
| You decide | Planner defines a deterministic rule | |

**User's choice:** Accepted Cancel wins.

### When should the caller receive the cancelled outcome?

| Option | Description | Selected |
|--------|-------------|----------|
| Promptly after Cancel | Resolve without waiting for server work; safely consume late results | ✓ |
| After request observes abort | UI unblocks first but caller waits for request settlement | |
| After a short timeout | Give abort a grace period before resolving | |
| You decide | Planner balances promptness and race safety | |

**User's choice:** Promptly after Cancel.

### What should immediate cleanup remove?

| Option | Description | Selected |
|--------|-------------|----------|
| Exactly this operation's state | Remove its contribution from all indicators and preserve others | ✓ |
| Blocking modal state only | Retain its general loading entry until settlement | |
| All loading state | Return the entire interface to idle | |
| You decide | Planner preserves indicators with the smallest change | |

**User's choice:** Exactly this operation's state.

---

## Overlapping blockers

### Which operation does the visible Cancel target?

| Option | Description | Selected |
|--------|-------------|----------|
| Currently displayed blocker | Follow the existing newest-blocker display | ✓ |
| Oldest blocker | Cancel the operation that began first | |
| Every cancelable blocker | One action aborts every eligible operation | |
| You decide | Planner chooses the clearest mapping | |

**User's choice:** Currently displayed blocker.

### What happens when an older blocker remains?

| Option | Description | Selected |
|--------|-------------|----------|
| Reveal it immediately | Show its own message and cancelability | ✓ |
| Close then reopen | Add a visual transition before revealing it | |
| Stay closed | Hide the modal until all operations finish | |
| You decide | Planner preserves the existing stack behavior | |

**User's choice:** Reveal it immediately.

### Can a hidden cancelable blocker expose Cancel through a visible noncancelable blocker?

| Option | Description | Selected |
|--------|-------------|----------|
| Hide Cancel | The control belongs only to the displayed operation | ✓ |
| Cancel hidden operation | Expose a control for the older hidden operation | |
| Operation menu | Let the user choose among cancelable blockers | |
| You decide | Planner chooses | |

**Selection:** Hide Cancel. Auto-selected as the recommended default after the user requested auto mode.

### How do repeated or stale cancel actions behave?

| Option | Description | Selected |
|--------|-------------|----------|
| Idempotent and identity-bound | Repeated input is a no-op and never retargets | ✓ |
| Retarget next blocker | A stale action affects the replacement blocker | |
| Raise an error | Treat repeated cancellation as invalid | |
| You decide | Planner chooses | |

**Selection:** Idempotent and identity-bound. Auto-selected as the recommended default.

---

## Cancellation feedback

### Should shared cancellation show a toast or banner?

| Option | Description | Selected |
|--------|-------------|----------|
| No generic feedback | The immediate UI change is the acknowledgement | ✓ |
| Neutral toast | Show a short `Cancelled` notification | |
| Persistent banner | Keep cancellation feedback visible | |
| You decide | Planner chooses | |

**Selection:** No generic feedback. Auto-selected as the recommended default.

### Should the modal show an interstitial state?

| Option | Description | Selected |
|--------|-------------|----------|
| No interstitial | Disappear or reveal the next blocker immediately | ✓ |
| Cancelling state | Keep the modal while abort is processed | |
| Cancelled state | Briefly confirm cancellation in the modal | |
| You decide | Planner chooses | |

**Selection:** No interstitial. Auto-selected as the recommended default.

### May callers provide cancellation feedback?

| Option | Description | Selected |
|--------|-------------|----------|
| Domain-local state only | Shared code is silent; caller preserves inputs or exposes retry | ✓ |
| Caller-provided message | Shared layer displays a caller-defined cancellation message | |
| Shared default with override | Shared layer owns feedback with customization | |
| You decide | Planner chooses | |

**Selection:** Domain-local state only. Auto-selected as the recommended default.

### Should routine cancellation be logged?

| Option | Description | Selected |
|--------|-------------|----------|
| No routine logging | Rely on typed outcome and focused tests | ✓ |
| Debug console logging | Emit browser diagnostics | |
| Telemetry event | Record cancellation analytics | |
| You decide | Planner chooses | |

**Selection:** No routine logging. Auto-selected as the recommended default.

## the agent's Discretion

- Exact identifiers and internal promise/abort coordination that enforce the locked behavior.
- Test decomposition and helper placement within established frontend patterns.

## Deferred Ideas

None. Server cooperation, mutation-safe cancellation, and product call-site adoption were already outside this phase and were not expanded during discussion.
