# Doughnut CLI Architecture Roadmap

> **Status:** Architectural roadmap only  
> **Purpose:** Organize design direction, boundaries, and sequencing for the CLI application.  
> **Not for direct implementation:** This document is intentionally high-level. It is meant to guide decisions, expose architectural concerns, and help break future work into coherent implementation slices. It is **not** a low-level design or coding specification.

---

## 1. Intent and Scope

The Doughnut CLI is an installable TypeScript command-line application that extends the Doughnut product with a local terminal-based interface.

This roadmap focuses on:

- the architectural shape of the interactive CLI application
- the separation between interactive and non-interactive command paths
- the testing architecture for terminal-based end-to-end behavior
- stage and flow management for modal CLI interactions
- terminal I/O boundaries and dependency direction
- failure diagnostics and observability for PTY-based E2E tests

This roadmap does **not** attempt to define:

- exact class or file names beyond the current direction
- concrete implementation details for every component
- final internal APIs between modules
- detailed UI copy, command syntax, or backend protocol design

---

## 2. Core Architectural Direction

The CLI should be treated as two related but distinct application styles:

### 2.1 Non-interactive command path

This path covers simple commands such as:

- `version`
- `update` / upgrade-related commands
- future simple `help` output

Characteristics:

- one-shot execution
- deterministic stdout-oriented behavior
- no long-lived interactive session
- exits quickly after completing the command

Architectural direction:

- keep this path clearly separated from the interactive application
- optimize for clarity and reliability rather than shared abstraction for its own sake
- allow reuse of shared backend/API code where appropriate, but do not force the non-interactive flow into the same runtime model as the interactive chat-like CLI

### 2.2 Interactive command path

This path is the main TUI application.

Characteristics:

- long-lived terminal session
- message-oriented interaction model
- commands may trigger async work
- commands may move the application into a different stage or modal state
- user can continue interacting with the session over time

Architectural direction:

- implement the interactive experience idiomatically with **Ink + React**
- prefer composition of React components and stage-local state over custom framework-like abstractions
- avoid re-implementing behavior already handled well by Ink

---

## 3. User Interaction Model

The interactive CLI should be modeled as a **message-based session**.

At a high level:

- the user sends a message or command
- the CLI assistant processes it
- the CLI renders assistant responses as session messages
- some commands initiate short operations, while others open deeper staged flows

This model gives the CLI a consistent interaction metaphor and aligns well with a chat-like terminal experience.

### 3.1 Commands as messages

Commands are part of the message flow rather than a separate mental model.

Architectural implication:

- parsing and handling commands should fit naturally into the session model
- command handling should still allow special behavior such as stage transition, cancellation, validation, and API orchestration

### 3.2 Asynchronous operations and cancellation

Some commands will call backend APIs or initiate work that takes time.

Architectural direction:

- the CLI must remain responsive while work is pending
- while an async action is in progress, the user should be able to cancel via **Escape** where appropriate
- cancellation should be treated as part of the interaction contract, not as a late implementation detail

Open design concern for later implementation:

- define clearly which operations are cancellable at the UI layer only and which support real cancellation propagation to the underlying task or network request

---

## 4. Stage-Based Application Structure

The interactive CLI is not a flat conversation only. Some commands move the application into a different **stage**.

Examples:

- recall flow
- confirmation flow
- loading or waiting flow
- future modal or wizard-like interaction patterns

### 4.1 Stage as modal application state

A stage represents a scoped interaction context with its own available UI actions, prompts, and input behavior.

Architectural direction:

- manage stages idiomatically using Ink + React state and composition
- treat each stage as a self-contained interaction scope
- avoid leaking stage-specific behavior into unrelated parts of the app

### 4.2 Parent-child stage isolation

Substages should be able to return control upward, but should **not know the parent stage’s internal details**.

Architectural principle:

- a substage may signal completion, cancellation, or a result
- the parent stage decides what to do next
- the substage should not depend on the parent’s structure, UI, or business assumptions

This keeps stage transitions modular and prevents tight coupling across flows.

### 4.3 Stage boundary concerns to watch early

These concerns should be recognized now even if implemented later:

- how stage-local keyboard handling is scoped
- how prompts/guidance differ by stage
- how shared session history is preserved when changing stages
- how cancellation behaves consistently across stages
- how stage transitions are represented without exposing parent internals

---

## 5. Terminal UI Architecture

### 5.1 Ink + React as the primary UI paradigm

The interactive CLI should follow the **idiomatic Ink/React** model.

Architectural direction:

- represent interactive UI through components
- manage local and shared state using React-friendly patterns
- prefer declarative rendering over imperative terminal manipulation where possible
- only add custom abstractions when Ink’s built-in model is insufficient

### 5.2 Do not re-implement what Ink already solves

This is an explicit design constraint.

Architectural rule:

- if Ink already provides an appropriate solution for rendering, focus, input handling, or lifecycle behavior, use it
- custom infrastructure should exist only where the CLI’s needs exceed Ink’s built-in capabilities

This reduces accidental complexity and keeps the application aligned with the framework it depends on.

### 5.3 Terminal styling

The CLI uses **Chalk** for color and visual terminal formatting.

Architectural implication:

- styling concerns should remain simple and terminal-oriented
- avoid mixing styling policy with domain logic
- keep style usage consistent across prompts, status output, warnings, and error feedback

---

## 6. Terminal I/O Adapter Boundary

There should be a dedicated **TTY I/O adapter** boundary between the application and the real terminal.

### 6.1 Purpose of the adapter

The adapter exists to isolate low-level terminal concerns from the application.

It should handle concerns such as:

- reading terminal input
- writing terminal output
- dealing with terminal capabilities or terminal-specific behavior
- mediating between the real terminal and the higher-level interactive application

### 6.2 What the adapter must not know

The adapter should remain deliberately ignorant of:

- Doughnut business/domain concepts
- application use cases
- stage semantics
- UI meaning beyond generic terminal I/O responsibilities

This is a strict separation-of-concerns decision.

### 6.3 Dependency direction

The adapter should be passed in or wired by higher-level facilitating code.

Architectural direction:

- application logic depends on an abstraction of terminal I/O
- the concrete terminal adapter is supplied from outside
- keep dependency direction flowing from framework/infrastructure toward the application boundary, not the reverse

This preserves testability and prevents terminal details from leaking into domain/application logic.

---

## 7. Shared Backend/API Code

When the CLI calls backend APIs, it should reuse the existing shared code where that code already fits the need.

Architectural direction:

- do not duplicate backend communication logic in the CLI without a reason
- keep backend/API integration code distinct from terminal presentation concerns
- allow application-level orchestration to coordinate API calls, cancellation, and rendering state

This is not a roadmap item for redesigning shared API code. The current direction is reuse, not reinvention.

---

## 8. End-to-End Testing Architecture

The CLI introduces a terminal-centric E2E testing model.

### 8.1 PTY-based execution model

E2E tests run the CLI inside a PTY-backed TTY simulation so the test can observe and drive terminal interaction.

Architectural direction:

- terminal behavior is tested through PTY interaction rather than through internal implementation hooks
- this preserves realistic end-to-end behavior for interactive flows

### 8.2 Existing orchestration direction

The current direction already includes:

- Cypress as the E2E test runner
- Cucumber preprocessor for scenario structure
- a hook/tag mechanism that ensures the bundled TypeScript CLI is up to date before execution
- launching the bundled CLI process and connecting it to the PTY within the test environment

**Install + interactive E2E (narrow slice):** Prefer **reusing the PTY started by the first interactive step** for later steps in the **same** scenario, without introducing **extra** scenario-wide session hooks solely for that flow; rely on normal step/scenario timeouts to tear down a still-running child when clean exit is not yet implemented.

This should remain part of the test architecture baseline.

### 8.3 Thin step definitions

Cucumber step definitions should remain thin.

Architectural direction:

- keep step definitions as scenario-language glue only
- move behavior complexity into page objects and fluent interfaces
- continue aligning with the broader project testing style

This supports readability, reuse, and maintainability of terminal-oriented scenarios.

---

## 9. Centralized Terminal Assertion Layer

This is one of the most important architectural decisions for the CLI test system.

### 9.1 Why centralization matters

All assertions about what is present in the terminal should be centralized in one place.

Reasons:

- PTY output is raw terminal output, not naturally human-readable
- terminal assertions require shared parsing and rendering logic
- consistent diagnostics depend on consistent assertion behavior
- Cypress teardown is not reliable enough to depend on post-failure cleanup or artifact generation

### 9.2 Architectural rule

All test assertions about terminal-visible content should go through a single assertion layer.

Do **not** scatter assertions across unrelated helpers or alternative mechanisms.

### 9.3 Responsibilities of the assertion layer

The centralized layer should own:

- interpreting PTY output relevant to visible terminal state
- matching expected terminal-visible content
- presenting meaningful assertion failures
- generating failure artifacts before throwing the assertion failure

This makes the assertion layer not just a matcher, but also the main diagnostic gateway.

---

## 10. Failure Diagnostics and Artifact Strategy

Failure handling is part of the architecture, not merely test polish.

### 10.1 Problem to solve

Raw terminal output may be a stream of control sequences and characters that humans cannot quickly interpret.

When a terminal assertion fails, the failure should answer two questions clearly:

- what was expected?
- what was actually visible or materially different?

### 10.2 Required diagnostic behavior

Before throwing an assertion failure, the assertion layer should generate diagnostic output.

Preferred direction:

1. **Best case:** produce an image-like screenshot of the visible terminal state
2. **Acceptable fallback:** produce a human-readable rendering of the visible terminal state, likely HTML and/or text
3. include the raw terminal content as supporting material when useful

### 10.3 CI integration goal

Failure artifacts should be integrated with the Cypress failure artifact flow as much as reasonably possible so they are downloadable in CI.

Architectural goal:

- terminal failures should be inspectable in CI with comparable convenience to browser screenshot failures
- the diagnostic path must not rely on a teardown step that may never execute

### 10.4 Diagnostic representation roadmap

This roadmap does not force the exact rendering technology yet, but it does establish the direction:

- translate terminal output into a readable visible-state representation
- expose the difference between expected and actual in a human-friendly way
- prefer a representation that helps debugging without requiring engineers to decode escape sequences manually

---

## 11. Recommended Architectural Concerns to Keep Visible Early

Beyond the areas already discussed, these are high-level concerns worth tracking now in the roadmap.

### 11.1 Session state model

Because the CLI is message-based and stage-based, define a clear distinction between:

- persistent session history
- current input buffer
- current stage-specific state
- transient async operation state

This does not need full design now, but the separation should remain visible.

### 11.2 Error handling model

The CLI will need a consistent model for:

- user-facing recoverable errors
- infrastructure/API failures
- cancellation outcomes
- unexpected internal failures

The roadmap should preserve the expectation that these are rendered intentionally, not ad hoc.

### 11.3 Keyboard/input ownership

Interactive CLIs often become messy when ownership of keys is unclear.

Track early:

- who handles Escape
- which keys are stage-specific
- how focus/input routing is decided
- how conflicts are resolved between session-level and stage-level input behavior

### 11.4 Rendering consistency

As the application grows, consistency of prompt regions, guidance regions, and message rendering will matter.

This is especially important because the project already has CLI vocabulary around prompts, guidance, stage indicators, and user/past message distinctions.

### 11.5 Distribution and installation boundary

Since the CLI is installable and updatable, it is worth preserving a clear architectural boundary between:

- product runtime behavior
- installation/update mechanics
- release/distribution concerns

This matters even if update behavior is initially simple.

---

## 12. Suggested Roadmap Sequence

This is a **sequencing proposal**, not an implementation checklist.

### Phase 1 — Preserve the core structural boundaries

Clarify and protect:

- interactive vs non-interactive command separation
- Ink/React as the TUI foundation
- TTY I/O adapter boundary
- stage isolation principles

### Phase 2 — Stabilize terminal testing foundations

Strengthen and consolidate:

- PTY-based execution path
- thin Cucumber steps + page object/fluent style
- centralized terminal assertion layer
- baseline human-readable failure artifacts

### Phase 3 — Mature stageful interactive behavior

Grow the architecture around:

- stage transitions
- async operation handling
- Escape-based cancellation
- consistent session and stage rendering patterns

### Phase 4 — Improve diagnostics and operational quality

Advance toward:

- richer screenshot-like terminal failure artifacts
- stronger CI artifact integration
- better diff-oriented failure messages
- more robust observability of interactive failures

### Phase 5 — Expand command surface safely

Add more capability while preserving the architectural shape:

- more staged flows
- additional non-interactive commands
- improved help/update experiences
- future command families without collapsing the separation of concerns

---

## 13. Explicit Non-Goals for This Roadmap

To avoid accidental over-commitment, this roadmap does **not** yet decide:

- exact component hierarchy for all Ink screens
- exact state-management library choices beyond idiomatic React direction
- final parser strategy for terminal output assertions
- whether terminal screenshots are rendered as PNG, HTML snapshots, or another format
- concrete cancellation primitives for every async task
- final command taxonomy for all future CLI commands

Those belong to later design and implementation work.

---

## 14. Summary

The CLI architecture should move forward with a few strong ideas kept stable:

- separate **non-interactive commands** from the **interactive TUI application**
- build the interactive experience idiomatically with **Ink + React**
- treat the interactive CLI as a **message-based, stage-aware session**
- keep substages **self-contained** and independent from parent internals
- isolate terminal mechanics behind a **TTY I/O adapter**
- reuse shared backend/API code where it already fits
- keep E2E tests **PTY-based**, with **thin Cucumber steps** and page-object-driven behavior
- centralize all terminal assertions in one diagnostic-capable assertion layer
- make failure output **human-readable and CI-downloadable**, without relying on teardown

Most importantly, this document is a **roadmap for architectural direction**. It is intended to help future implementation stay coherent, not to prescribe code directly.
