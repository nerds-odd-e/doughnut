# Skip Memory Tracking — make the glossary true

**Status:** in progress (Phases 1–2 done)  

**Source:** Proposed [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md) — **Skip Memory Tracking** is a notebook setting that opts the notebook out of the **assimilation sequence** and blocks Bazaar subscribe. It does **not** opt the notebook out of recall. Assimilating on a note still creates a memory tracker.

**Decided concept (lock this; do not re-litigate in execute):**

| Does | Does not |
|------|----------|
| Units from the notebook are absent from `/next` and assimilation counts (owned and subscribed) | Hide or disable Assimilate on the note |
| Bazaar Subscribe CTA stays hidden **and** `POST /api/subscriptions/notebooks/{notebook}/subscribe` rejects | Opt existing memory trackers out of recall |
| Settings copy matches the glossary | Rename persisted `skipMemoryTrackingEntirely` / `skip_memory_tracking_entirely` (ADR alignment: existing identifiers until a deliberate rename slice) |
| | Auto-unsubscribe existing subscribers (sequence exclusion is enough) |

Circle subscribe uses the same endpoint: reject whenever the flag is set.

---

## Phase 1: Flagged notebook is absent from the assimilation sequence

**Type:** Behavior  
**Status:** done

**Landed:** `GET /api/assimilation/next` next-unit and due/total-unassimilated counts omit notes and property units from notebooks with Skip Memory Tracking (owned and subscribed). Shared JPQL fragment `NotebookSettings.JPA_NOTEBOOK_NOT_SKIP_MEMORY_TRACKING` on unassimilated note and property queries. Assimilate-on-note and recall queries untouched.

**Learning:** `NoteRepository` unassimilated clause identifiers were named `recall*`; renamed to `unassimilated*` so the filter is not read as a recall opt-out. Phase 2 is E2E-only of this filter.

---

## Phase 2: Walkthrough does not offer a flagged notebook

**Type:** Behavior  
**Status:** done

**Landed:** E2E `Walkthrough does not offer notes from a Skip Memory Tracking notebook` in `assimilation_walkthrough.feature`. Menu walkthrough still offers Note 1 with progress `0/2/5` when a flagged notebook has an unassimilated note. No product-code change; Phase 1 filter was sufficient.

---

## Phase 3: Assimilate-on-note still joins recall

**Type:** Behavior  
**Status:** planned

**Pre-condition:** Note in a **Skip Memory Tracking** notebook has no understanding memory tracker.

**Trigger:** Assimilate from the note (not via `/next`).

**Post-condition:** An understanding memory tracker exists; it can appear in recall. The unit does not re-enter the assimilation sequence.

**Tests:** regression at controller boundary — `AssimilationController.assimilate` on a flagged-notebook note succeeds; a follow-up `/next` does not offer that note; recall/menu due includes the new tracker (extend `AssimilationControllerAssimilateTests` and the existing recall/menu count boundary, e.g. `UserMenuDataControllerTest`). No new E2E unless the unit tests cannot express recall-due.

**Implementation:** none expected if Phase 1 only filtered sequence queries. If assimilate or recall was over-blocked, revert that.

**Verify:** `CURSOR_DEV=true nix develop -c pnpm backend:test_only`

---

## Phase 4: Subscribe API rejects Skip Memory Tracking

**Type:** Behavior  
**Status:** planned

**Pre-condition:** Bazaar notebook has **Skip Memory Tracking** enabled. UI already omits the Subscribe CTA (`BazaarNotebookButtons.vue`; E2E `bazaar_subscription.feature`).

**Trigger:** `POST /api/subscriptions/notebooks/{notebook}/subscribe`.

**Post-condition:** No subscription row is created. Response is a client error (prefer **400**, not 403: the notebook is still readable). Existing subscribe-without-flag behavior unchanged.

**Tests:** extend `SubscriptionControllerTest` (canonical reject). Do not duplicate the Bazaar CTA E2E.

**Implementation:** check the flag in `SubscriptionController.createSubscription` after read authorization.

**Verify:** `CURSOR_DEV=true nix develop -c pnpm backend:test_only`

---

## Phase 5: Copy and glossary match the decided concept

**Type:** Behavior  
**Status:** planned

**Pre-condition:** Phases 1–4 behave as above; Settings still claims notes are left out of “memory tracking and recall sessions.”

**Trigger:** Open notebook Settings (and read ADR 0001).

**Post-condition:** User-facing copy, tests, and the ADR glossary agree: **Skip Memory Tracking** opts out of the **assimilation sequence** and blocks Bazaar subscribe; it does not opt out of recall.

**Tests:** frontend assertion on the Settings help text (`NotebookPageView.settings.spec.ts` or the settings mount). No E2E solely for copy if the unit test covers the string.

**Cleanup in this phase (same observable meaning):**

- `NotebookSettings.vue` help text
- ADR 0001: **Memory tracking** row currently says “opts out of assimilation”; restore a disambiguation bullet; fix the broken Consequences fragment at “notebook **Skip Memory Tracking**”
- Test/story names that say the flag skips “memory tracking entirely” or recall, where they overclaim

Do **not** rename `skipMemoryTrackingEntirely` in OpenAPI/DB in this phase.

**Verify:** `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/NotebookPageView.settings.spec.ts`

---

## Out of scope

- Renaming `skip_memory_tracking_entirely` / generated API field
- Auto-removing existing subscriptions
- Changing **Skip from the assimilation sequence** (per-unit skip)
- Blocking assimilate-on-note or recall
