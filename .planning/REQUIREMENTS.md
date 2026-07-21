# Requirements: Progressive Cancelable Blocking UI

**Defined:** 2026-07-21
**Core Value:** When a long-running frontend action blocks the UI, the user can cancel it and remain in a coherent, usable state without unintended follow-up behavior.

## v1 Requirements

Requirements for the initial progressive-cancellation release. Each maps to exactly one roadmap phase.

### Cancellation Contract

- [x] **CANC-01**: User sees a Cancel control in the global blocking spinner only when the active operation explicitly opts into cancellation
- [x] **CANC-02**: User can activate Cancel to abort that operation's browser request and promptly remove its blocking state
- [x] **CANC-03**: User cancellation completes as a normal outcome without an error toast, success handling, or navigation from the cancelled operation
- [x] **CANC-04**: Cancelling one blocking operation does not clear or abort other concurrent loading operations

### Note Refinement

- [x] **REFN-01**: User sees the global blocking spinner with Cancel while AI generates the initial note-refinement layout
- [x] **REFN-02**: User who cancels initial layout generation remains in the refinement dialog with unchanged note content and an available retry action
- [x] **REFN-03**: User sees the same global blocking spinner with Cancel while AI generates an extraction preview
- [x] **REFN-04**: User who cancels extraction-preview generation keeps the current layout selections, remains before the preview, and can retry
- [ ] **REFN-05**: User sees the shared blocking spinner without Cancel while final extracted-note creation is pending

### Cohesion and Adoption

- [x] **COHE-01**: Frontend cancellation call sites use one shared abort, loading-cleanup, and cancelled-outcome contract while defining only their domain-specific post-cancel behavior
- [ ] **COHE-02**: Every existing whole-UI blocker and long-running note-refinement AI request is classified as cancelable, intentionally noncancelable, or nonblocking, with safe in-scope operations using the shared solution

## v2 Requirements

Deferred capabilities that require broader backend or product work.

### Server Cooperation

- **SERV-01**: User cancellation cooperatively stops long-running server-side AI work rather than only aborting the browser request
- **SERV-02**: User can safely cancel transactional mutations through idempotency, status reconciliation, or another backend-supported cancellation contract

### Broader Adoption

- **ADPT-01**: Users can cancel additional safe long-running operations outside note refinement after their domain-specific outcomes are defined

## Out of Scope

Explicitly excluded from this project to prevent misleading or unsafe cancellation behavior.

| Feature | Reason |
|---------|--------|
| Cancel every wrapped API request | Thin loading-bar and background operations do not all block interaction or have meaningful cancellation behavior |
| Client-only Cancel for extracted-note creation | The server may still commit the mutation, making retry unsafe and potentially creating duplicates |
| Guaranteed backend cancellation | Requires a separate cooperative server-side design and API contract |
| Broad spinner redesign | The existing global blocking modal is the cohesive visual foundation; this project adds behavior and targeted adoption |

## Traceability

Which phases cover which requirements. Populated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| CANC-01 | Phase 2 | Complete |
| CANC-02 | Phase 2 | Complete |
| CANC-03 | Phase 2 | Complete |
| CANC-04 | Phase 2 | Complete |
| REFN-01 | Phase 2 | Complete |
| REFN-02 | Phase 2 | Complete |
| REFN-03 | Phase 3 | Complete |
| REFN-04 | Phase 3 | Complete |
| REFN-05 | Phase 4 | Pending |
| COHE-01 | Phase 1 | Complete |
| COHE-02 | Phase 4 | Pending |

**Coverage:**

- v1 requirements: 11 total
- Mapped to phases: 11
- Unmapped: 0 ✓

---
*Requirements defined: 2026-07-21*
*Last updated: 2026-07-21 after roadmap creation*
