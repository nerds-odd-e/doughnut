# Quick plan: cancelable follow-ups (bugs, hygiene)

Ad-hoc slice from post-ship review of progressive cancellation
(since `36b810105c49376866fae938b3256958e4da5d93`).

**Goal:** Fix the remove/layout nesting bug, restore Overlay centering
fallback, then delete dead/redundant cancellation tests — stop-safe after
each phase.

**Out of scope (do not do in this plan):**
- Distinct empty/error vs cancel copy for layout (`layoutLoadSettled` shared
  path) — needs a product decision; not clearly higher value than stop.
- Splitting `NoteRefinement.vue` (~461 lines) or rewriting
  `IdentityBoundCancelButton` — speculative Structure with no immediate next
  Behavior in this plan.
- Broader cancel adoption (book layout AI, etc.).

---

## Context / discoveries

1. **Nested cancelable layout inside remove** — fixed in Phase 1:
   `loadRefinementLayout({ blockUi: false })` under remove continuous blocker;
   mount/retry keep cancelable opt-in.
2. **Overlay fallback never landed** — `efc4dbb039` message claimed
   `align-items: center` before `safe center`, but the commit is empty; only
   `safe center` remains.
3. **Test bloat** — type-only LoadingModal control test; AbortError allowlist
   with no product matches; thin “shows Cancel while …” smokes subsumed by
   cancel happy paths; triplicated concurrent-blocker + second-Cancel
   suites; `expect(olderCall).toBeTruthy()` is vacuous.

**Keep as contract suites:** `clientSetup.loading.spec.ts` cancellation
races; one LoadingModal / managedApi concurrent-blocker proof; layout +
extraction cancel *domain* outcomes (late data ignored, retry, create
noncancelable).

---

## Design decisions

| Decision | Rationale |
| --- | --- |
| Post-remove layout refresh must not be a cancelable whole-UI blocker | Continuous remove blocker; mutation follow-up must not expose Cancel or empty-wipe |
| Prefer thin-bar (or non-cancelable) reload when nested under remove; keep mount/retry cancelable | Matches docs; preserves cancel UX for standalone layout generation |
| Prune duplicates toward managedApi / LoadingModal for stack identity; keep NoteRefinement specs for domain post-cancel UI | Minimum tests for same coverage |
| Skip NoteRefinement / Cancel button Structure refactors here | No Behavior phase in this plan depends on them |

---

## Phases

### Phase 1 — Behavior — done

**Observable behavior:** After remove succeeds, nested layout regeneration
stays under the noncancelable remove continuous blocker (no Cancel, no
layout-message takeover / empty-wipe). Mount + empty-retry layout generation
remain cancelable.

**Learnings:** Parameterize `loadRefinementLayout({ blockUi })` — nested
post-remove uses thin-bar; default path keeps `{ cancelable: true }`.
`@click` must wrap `() => loadRefinementLayout()` so PointerEvent is not
passed as options. Inventory: remove row + nonblocking nested reload line in
`frontend-api.mdc`.

---

### Phase 2 — Behavior — planned

**Observable behavior:** Centered Overlay keeps normal vertical centering
when the engine does not understand `safe` keywords (fallback
`align-items: center` then `align-items: safe center`).

**Pre:** Centered Overlay / LoadingModal stack.
**Trigger:** Styles applied (including narrow-viewport path already covered).
**Post:** Centering works with fallback; existing LoadingModal reachability
test still passes.

**Work:**
1. Add CSS fallback in `Overlay.vue` (the change that `efc4dbb039` claimed).
2. Re-run `LoadingModal.spec.ts` narrow/centering cases.

**Done when:** Fallback present; related modal tests green.

---

### Phase 3 — Structure — planned

**Structure change (no product behavior change):** Remove dead and
overlapping cancellation tests so the next reader maintains one contract
suite + domain cancel outcomes only.

**Delete or collapse (capability-preserving):**
- `LoadingModal.spec.ts`: “requires cancel identity and action as one
  component control” (type-only / useless runtime).
- `cancelableAllowlist.spec.ts`: AbortError-name matching describe/it (no
  product hits; docs forbid inventing AbortError matching).
- Layout + extraction: “shows blocking Cancel while … generates” when fully
  subsumed by cancel happy-path tests in the same files.
- Triplicated “older concurrent blocker” / second-Cancel idempotency: keep
  **one** stack-level proof (prefer `LoadingModal.spec.ts` or
  `clientSetup.loading.spec.ts`); drop duplicate copies from NoteRefinement
  cancel specs **unless** they assert domain-unique UI (then keep only the
  domain-unique assertions).
- Vacuous `expect(olderCall).toBeTruthy()` wherever it remains.

**Do not delete:** domain cancel outcomes (late layout/preview ignored,
retry, create-note Cancel-absent, allowlist `cancelable: true` file gate).

**Done when:** Focused cancel/remove/LoadingModal/allowlist tests green;
fewer redundant cases; observable cancel behavior unchanged.

---

## Status

| Phase | Type | Status |
| --- | --- | --- |
| 1 Nested remove must not expose cancelable layout wipe | Behavior | done |
| 2 Overlay `align-items` center fallback | Behavior | planned |
| 3 Prune dead/redundant cancel tests | Structure | planned |

**Next action:** Execute Phase 2 (Overlay centering fallback).
