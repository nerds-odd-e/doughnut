# Recall E2E optimization cleanup

Status: planned

**Execution:** do **not** execute until asked. When executing, use **execute-plan** (commit + push per slice).

## Origin

A post-hoc review of `.planning/quick/042-recall-e2e-test-optimization/` (commits
`5b198b4d99..dddbec79ed` on `main`) found no bugs and no behavior digressions
worth reverting, but did find 4 pieces of dead code and one readability gap
that survived each slice's own `post-change-refactor` pass. This plan fixes
those findings only — it does not reopen plan 042's scope.

**Root cause (context, not a fix):** each slice's post-change-refactor only
re-checked step definitions in files *that slice* touched, not step
definitions a *later* slice's deletion could orphan. Recall.ts, for example,
was touched again in slices 3 and 6, yet two step defs orphaned by slice 2
survived both later passes untouched.

**Verified during planning (no action needed):** the plan-042 review flagged
a low-confidence claim that `recall_pages.feature`'s deleted "Count of recall
and assimilate notes" scenario might have had unique coverage. Traced during
this planning pass: that scenario's `I should see {int} due for assimilation`
step called `assimilationMenu.ts`'s `expectCount()`, which reads the sidebar
`.due-count` badge text with a `startsWith` match. The surviving
`assimilation_walkthrough.feature` exercises the *same* DOM element via
`expectAssimilationNavBadge()` (step: "I should see assimilation progress"),
with a stricter exact-match assertion. So the UI-rendering path this step
exercised is still covered — no gap, no extra slice needed here.

---

### 1. Remove step definitions orphaned by slices 2 and 3 in recall.ts
Type: Structure
Status: done

**Dead code (each confirmed via repo-wide grep against every `.feature` file — zero callers):**
- `e2e_test/step_definitions/recall.ts:30` — `Then('I recall {string}', ...)`. Distinct from the still-live `'On day {int} I recall {string} and assimilate new {string}'`. Its only caller was `accidental_match_scheduling.feature`'s "Unique matched understanding tracker is brought forward when spelling is absent" scenario, deleted in plan 042 slice 3.
- `e2e_test/step_definitions/recall.ts:89` — `When('I am recalling my note on day {int}', ...)`. Orphaned when plan 042 slice 2 deleted `spaced_repetition.feature`'s FSRS-number scenarios.
- `e2e_test/step_definitions/recall.ts:109` — `When('I choose Good', ...)`. Same cause as above. **Do not** delete the page-object method it calls, `chooseGood()` in `e2e_test/start/pageObjects/recallPage.ts` — that method is still used internally by other page-object code.

**Goals:**
- Delete the 3 step definitions above.
- Re-grep after deleting to confirm no other step definition or page-object method becomes newly-orphaned as a side effect (expect none, but verify).

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm biome check e2e_test/step_definitions/recall.ts
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/recall/spaced_repetition.feature,e2e_test/features/recall/accidental_match_scheduling.feature,e2e_test/features/recall/browse_answer_and_notes_while_recalling.feature
```

---

### 2. Remove the dead assimilation-count step and its page-object method
Type: Structure
Status: done

**Dead code (confirmed via repo-wide grep — zero callers in any `.feature` file):**
- `e2e_test/step_definitions/assimilation.ts:15` — `Then('I should see {int} due for assimilation', ...)`.
- `e2e_test/start/pageObjects/assimilationPage/assimilationMenu.ts:22` — `expectCount()`. Its only caller was the step above. Orphaned together when plan 042 slice 6 deleted `recall_pages.feature`'s "Count of recall and assimilate notes" scenario.

**Do not** touch `expectAssimilationNavBadge()` (same file) or `recallPage.ts`'s separate `expectCount()` (the recall-badge check, called from `recall.ts:55`, still live and used by `property_memory_tracker.feature` and others) — both are distinct, still-used methods.

**Goals:**
- Delete the step definition and the now-single-caller `expectCount()` page-object method together.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm biome check e2e_test/step_definitions/assimilation.ts e2e_test/start/pageObjects/assimilationPage/assimilationMenu.ts
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/assimilation/assimilation_walkthrough.feature
```

---

### 3. Document the backdating step in the property-tracker scenario
Type: Structure
Status: done

**Issue:** `e2e_test/features/recall/property_memory_tracker.feature`'s scenario
"Answering a property recall question updates only the property tracker" has
a visibly non-monotonic timeline — `When It's day 2, 9 hour` (line 24) is
immediately followed by `Given It's day 1, 20 hour` (line 26). This is **not**
a bug: the backdated re-assimilation staggers the note-level tracker's next-due
time later than the property tracker's, which is what makes the "1 notes to
recall" assertion on line 25 correct (without it, both trackers would be due
and the count would be 2 — an earlier implementer already hit and fixed this
exact miscount during plan 042 slice 5). But it reads as confusing to anyone
who doesn't already know that, which works against plan 042's own goal that
surviving E2E "document the system's external business purpose" as a coherent
story.

**Goals:**
- Add a short comment immediately above line 26 (`Given It's day 1, 20 hour`)
  explaining *why* the clock moves backward: it backdates the whole-note
  assimilation so the note-level tracker's due window lands after the
  property tracker's, keeping the due-count check on line 25 unambiguous.
- **Do not** reorder or otherwise restructure the steps — reordering
  reintroduces the miscount.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/recall/property_memory_tracker.feature
```

---

E2E groups: 3+ consecutive green runs on touched specs before closing each slice (per `.planning/quick/042-recall-e2e-test-optimization/`'s own convention, kept here).

**Planning cleanup:** prune spent slice detail after the pass; keep this PLAN only while in progress.
