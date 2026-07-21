# Phase 3: Cancel Extraction Preview Generation - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-21
**Phase:** 3-Cancel Extraction Preview Generation
**Mode:** `--auto` (recommended defaults; no interactive prompts)
**Areas discussed:** Post-cancel landing surface, Retry affordance from preserved selection, Cancel during in-preview regenerate, Blocker message and Cancel adoption

---

## Post-cancel landing surface

| Option | Description | Selected |
|--------|-------------|----------|
| Stay on layout with selections preserved | Never open preview on cancel; keep checkboxes | ✓ |
| Open empty/error preview panel | Enter preview shell without payload | |
| Close the Refine-note dialog | Treat cancel as dismiss | |

**User's choice:** [auto] Stay on layout with selections preserved (recommended default)
**Notes:** Aligns with ROADMAP success criterion 2 and REFN-04 "before the preview" for Extract-from-layout.

[auto] Post-cancel landing surface — Q: "Where should the user land after cancelling extraction-preview generation started from the layout Extract action?" → Selected: "Stay on layout with selections preserved" (recommended default)

---

## Retry affordance from preserved selection

| Option | Description | Selected |
|--------|-------------|----------|
| Reuse Extract button with preserved selection | No new empty-state control | ✓ |
| Add dedicated Ask AI to retry on layout after cancel | Extra control when preview cancelled | |
| Auto-retry once after cancel | Immediate second request | |

**User's choice:** [auto] Reuse Extract button with preserved selection (recommended default)
**Notes:** Unlike Phase 2 layout cancel (empty items), selection remains so Extract stays enabled.

[auto] Retry affordance from preserved selection — Q: "How does the user retry preview generation after cancel?" → Selected: "Reuse Extract button with preserved selection" (recommended default)

---

## Cancel during in-preview regenerate

| Option | Description | Selected |
|--------|-------------|----------|
| Keep prior preview content on the preview panel | Silent cancel; fields unchanged | ✓ |
| Force Back to layout selection | Always leave preview on cancel | |
| Clear preview fields / empty preview shell | Wipe on cancel | |

**User's choice:** [auto] Keep prior preview content on the preview panel (recommended default)
**Notes:** User already crossed into preview; wiping or forcing Back is more disruptive than keeping the last good preview.

[auto] Cancel during in-preview regenerate — Q: "If the user cancels while regenerating from Ask AI to retry on an already-visible preview, what remains on screen?" → Selected: "Keep prior preview content on the preview panel" (recommended default)

---

## Blocker message and Cancel adoption

| Option | Description | Selected |
|--------|-------------|----------|
| Keep AI is generating preview... and Phase 1 Cancel contract | Adopt Cancel visuals unchanged | ✓ |
| Rename message to match layout phrasing | e.g. align wording with layout | |
| Redesign Cancel placement or copy for preview | Visual change | |

**User's choice:** [auto] Keep AI is generating preview... and Phase 1 Cancel contract (recommended default)
**Notes:** Phase 2 UI-SPEC already forbade Cancel redesign when adopting; Phase 3 stays consistent.

[auto] Blocker message and Cancel adoption — Q: "Should the preview blocker change its message or Cancel visuals for Phase 3?" → Selected: "Keep AI is generating preview... and Phase 1 Cancel contract" (recommended default)

---

## Claude's Discretion

- Exact migration off `runWithBlockingApiLoading` onto cancelable `apiCallWithLoading`
- Test helper/file decomposition under recall component tests
- Leaving non-cancel API-error preview display paths unchanged

## Deferred Ideas

- Cancel final extracted-note creation (Phase 4 / REFN-05)
- Full blocker audit (Phase 4 / COHE-02)
- Server-cooperative cancellation (SERV-01)
