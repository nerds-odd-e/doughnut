---
phase: 1
slug: shared-cancellation-contract
status: verified
threats_open: 0
asvs_level: 1
created: 2026-07-21
---

# Phase 1 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Global modal action → shared loading state | A user action may affect only the exact operation represented by its rendered state identity. | Cancel click → `ApiLoadingState.cancel` for one id |
| Shared wrapper → generated browser request | AbortSignal affects browser request consumption; it does not prove server work stopped. | `AbortSignal` on generated request options |
| Rendered Cancel control → selected loading state | The event must remain bound to the state represented when the button subtree was rendered. | Keyed child + captured cancel closure |
| Browser cancellation UI → user interpretation | The control abandons the browser wait and must not imply cooperative server termination. | Fixed `"Cancel"` label only |
| Viewport → fixed loading overlay | Browser dimensions and content length determine whether the blocker endpoints are reachable. | Overlay layout CSS |
| Layout surface → identity-bound Cancel control | Scrolling may expose the existing action but must not replace, duplicate, or retarget its callback. | Same identity-bound control after scroll |

---

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-01-01 | Denial of Service | `ApiLoadingState.cancel` / `LoadingModal` action binding | high | mitigate | Idempotent cancel closure bound to one state id; keyed child captures original action; stale/repeated-action tests. | closed |
| T-01-02 | Tampering / Denial of Service | `finishLoading` / `currentBlockingApiState` | high | mitigate | Exact-id removal; message/id/action derived from one selected object; concurrent/nested survivors remain. | closed |
| T-01-03 | Tampering | `apiCallWithLoading` terminal result | high | mitigate | Accepted latch wins same-turn races; late settlement returns cancelled / is silent; UI never reclassifies. | closed |
| T-01-04 | Repudiation / integrity ambiguity | Browser abort semantics / Cancel label | medium | mitigate | Result named `cancelled` only; fixed `"Cancel"` copy; no Stop/server-stop claim; no product `cancelable: true` callers. | closed |
| T-01-05 | Denial of Service | `Overlay.vue` narrow-viewport layout | high | mitigate | `align-items: safe center` + `overflow-y: auto`; Chromium 320×568 endpoint reachability regression. | closed |
| T-01-06 | Tampering | `LoadingModal` control/state binding | medium | mitigate | LoadingModal and managed cancellation semantics untouched by overflow fix; same identity-bound control after scroll. | closed |
| T-01-07 | Repudiation / integrity ambiguity | Existing Cancel presentation | low | accept | No copy/feedback change; fixed `Cancel` continues to mean abandoning the browser wait only. | closed |

*Status: open · closed · open — below high threshold (non-blocking)*
*Severity: critical > high > medium > low — only open threats at or above `workflow.security_block_on` (`high`) count toward `threats_open`*
*Disposition: mitigate · accept · transfer*

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-01 | T-01-07 | Fixed Cancel label and layout-only gap fix intentionally leave presentation semantics unchanged; Cancel still means browser-wait abandon only. | plan 01-03 threat model | 2026-07-21 |

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-07-21 | 7 | 7 | 0 | gsd-verify-work --auto (ASVS L1, plan-time register) |

### L1 evidence (grep-depth)

- `frontend/src/managedApi/ApiStatusHandler.ts` — `cancel?`, `finishLoading(loadingState)` exact-id removal
- `frontend/src/managedApi/clientSetup.ts` — per-call `AbortController`, `{ status: "cancelled" }`, accepted-latch gate
- `frontend/src/components/commons/LoadingModal.vue` — `IdentityBoundCancelButton` hardcoded `"Cancel"`
- `frontend/src/components/commons/Overlay.vue` — `align-items: safe center`; `overflow-y: auto`
- Product callers: only type-literal `cancelable: true` in `clientSetup.ts`; no product opt-in

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-07-21
