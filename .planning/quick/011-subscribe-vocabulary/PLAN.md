# PLAN: Subscribe / Subscription vocabulary everywhere

**Status:** planned  
**Type:** ad-hoc quick plan  
**ADR:** Proposed [0001-ubiquitous-language.md](../../../docs/adrs/0001-ubiquitous-language.md) — use **Subscribe** / **Subscription**; reserve **learning** for the learner metaphor; use **daily assimilation target** for the quota.

## Goal

Replace every product and test use of “Add to (my) learning” with **Subscribe** / **Subscription**, and scrub all remaining synonym records (code, E2E, Proposed ADR). No leftover “add to learning” archaeology in the tree.

## Out of scope

- Renaming OpenAPI / Java field `dailyTargetOfNewNotes` (API identifier debt stays until a separate rename slice).
- Accepting ADR 0001 (human process) — this plan only edits the Proposed draft to match reality.
- Broader ADR 0001 glossary items (`space setting`, `outgoingLinks`, etc.).

## Inventory (at planning time)

| Location | Current string / name |
|----------|------------------------|
| `frontend/.../BazaarNotebookButtons.vue` | button `title="Add to my learning"` |
| `frontend/.../SubscribeForm.vue` | dialog title `Add to my learning` |
| `e2e_test/features/bazaar/add_to_learning.feature` | file name; scenario “added to learning”; steps “target of learning” |
| `e2e_test/features/circles/notebooks_in_circles.feature` | step “target of learning” |
| `e2e_test/features/notebooks/notebook_group.feature` | step “target of learning” |
| `e2e_test/step_definitions/{bazaar,circle}.ts` | step text with “learning” synonym |
| `e2e_test/start/pageObjects/BazaarOrCircle.ts` | `addToMyLearning`, `expectCannotAddToMyLearning`, `dailyLearningCount` |
| `docs/adrs/0001-ubiquitous-language.md` | current / problem / debt mentions of “add to learning” |

Already aligned: Subscription APIs, `SubscriptionNoteButtons` (“Edit subscription” / “Unsubscribe”), Feature title “Bazaar subscription”.

---

## Phase 1 — Behavior: Subscribe CTA and dialog

**Status:** done  
**Type:** Behavior

**Observable behavior**

- **Pre:** Shared notebook with memory tracking on (Bazaar or Circle).
- **Trigger:** Learner opens the notebook card actions.
- **Post:** CTA and subscribe dialog use **Subscribe** (not “Add to my learning”). Quota field label is **Daily assimilation target**. When the notebook skips memory tracking entirely, the Subscribe CTA is absent.

**Work**

1. Add/extend a focused frontend unit test (mounted `BazaarNotebookButtons` / `SubscribeForm`) asserting titles **Subscribe** and field label **Daily assimilation target**; confirm it fails for the right reason.
2. Update copy:
   - `BazaarNotebookButtons.vue` button `title` → `Subscribe`
   - `SubscribeForm.vue` card title → `Subscribe`; pass `title="Daily assimilation target"` on the `dailyTargetOfNewNotes` `TextInput`
   - Optionally same field `title` on `SubscriptionEditForm.vue` for consistency
3. Update E2E page object + steps that click / assert the CTA so bazaar subscription scenarios stay green:
   - `BazaarOrCircle.ts`: button label `Subscribe`; rename helpers away from `*Learning*`
   - Steps: “cannot subscribe … from the Bazaar” (replace “add … to my learning”)
4. Run targeted frontend unit test + `pnpm cypress run --spec e2e_test/features/bazaar/add_to_learning.feature`.

**Done when:** UI and that feature’s CTA assertions say Subscribe; no “Add to my learning” in `frontend/src`.

---

## Phase 2 — Structure: E2E + ADR synonym scrub

**Status:** planned  
**Type:** Structure  
**Enables:** Nothing further — finishes vocabulary alignment with zero observable product delta after Phase 1.

**Work**

1. Rename `e2e_test/features/bazaar/add_to_learning.feature` → capability name e.g. `bazaar_subscription.feature` (Feature title already “Bazaar subscription”).
2. Replace Gherkin / step text **target of learning N notes per day** → **daily assimilation target of N notes per day** (or equivalent glossary-faithful wording) in:
   - bazaar subscription feature
   - `notebooks_in_circles.feature`
   - `notebook_group.feature`
   - matching step defs in `bazaar.ts` / `circle.ts`
3. Scrub Proposed ADR 0001:
   - Remove **Add to learning** from “Current vocabulary”
   - Rewrite ambiguous / redundant rows so they do **not** cite the old synonym
   - Drop consequences / cons that treat `add to learning` as known debt or historical metaphor
   - Keep **Learning** only for remaining real senses (overall learner metaphor); point assimilation quota at **daily assimilation target**
4. Repo-wide grep: no `add to learning`, `Add to my learning`, `add_to_learning`, `to my learning`, `target of learning`, or `addToMyLearning` left under the tree (except unrelated CLS `toLearningSessionLite` helper names — leave those; they are Learning Session, not subscription).

**Verify:** Targeted Cypress for renamed bazaar feature + the two other features that share the subscribe step; `rg` clean for the synonym patterns above.

**Done when:** No historical synonym strings remain in product, tests, or Proposed ADR 0001.

---

## Design decisions

1. **Canonical CTA:** short **Subscribe** (matches Unsubscribe / Edit subscription), not “Add to subscription”.
2. **Quota copy:** **Daily assimilation target** in UI label and Gherkin; keep API field `dailyTargetOfNewNotes` unchanged this plan.
3. **No archaeology:** do not document the old phrase as debt in the ADR — delete it.
4. **Capability-named E2E file:** `bazaar_subscription.feature` — phase numbers stay under `.planning/` only.

## Discoveries

- Product path already uses Subscription controllers and “Edit subscription” / “Unsubscribe”; only the Bazaar/Circle *start* CTA and dialog still say “Add to my learning”.
- Circles reuse `BazaarNotebookButtons` via `NotebookButtons.vue` — one UI change covers both surfaces.
- `toLearningSessionLite` in `RecallService` is commissioned Learning Session mapping, not this synonym.
