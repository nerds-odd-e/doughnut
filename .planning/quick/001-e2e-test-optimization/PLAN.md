# E2E test optimization

Status: in-progress

**Execution:** run via **execute-plan** (commit + push per slice).
**Cloud VM:** no Nix prefix; `source /workspace/scripts/cloud_agent_setup.sh` before Cypress/Gradle; verify with `xvfb-run pnpm cypress run --spec … --config-file e2e_test/config/ci.ts`.

## Profiling baseline (2026-08-20)

Command:

```bash
source /workspace/scripts/cloud_agent_setup.sh
xvfb-run -a pnpm cy:run-on-sut --reporter json --env tags='not @ignore and not @wip and not @skipOptimizationDueToKnownNecessarySlowness'
```

- **277 tests**, suite wall ~**23m46s** (real ~25m12s)
- Eligible: **277** (profiled with skip-optimization + ignore + wip excluded)
- Cypress reported **12 of 64 specs failed** (34 scenario failures) during baseline — durations still used for ranking; none of the top-10% scenarios were in the parsed failed set
- Raw profile: `.planning/quick/e2e-profile-results.json` (+ `/tmp/e2e-profile.log`) — **do not commit**

### Top 10% slowest (n = ceil(277 × 0.10) = 28)

| # | ms | file / spec | test / scenario |
|---|-----|-------------|-----------------|
| 1 | 15341 | assimilation/note_refinement.feature | Retry extraction preview before creating note |
| 2 | 15335 | assimilation/note_refinement.feature | Switch between content and diff for updated original note |
| 3 | 15328 | assimilation/note_refinement.feature | Export extract request shows AI request JSON |
| 4 | 15294 | assimilation/note_refinement.feature | Cannot create note with blank title from extraction preview |
| 5 | 15283 | assimilation/note_refinement.feature | Remove selected refinement layout items |
| 6 | 15162 | ai_generated_content/note_content_completion.feature | Content completion fails when OpenAI is unavailable |
| 7 | 14275 | assimilation/note_refinement.feature | Extract selected refinement layout items to one new note |
| 8 | 14264 | assimilation/note_refinement.feature | Export breakdown request shows AI request JSON |
| 9 | 14263 | assimilation/note_refinement.feature | Save edited extraction preview content |
| 10 | 13910 | recall/recall_quiz_ai_question.feature | AI question generation includes wiki-linked, depth-two wiki path, and folder-sibling focus context |
| 11 | 12407 | ai_generated_recall_questions/question_contest.feature | Internally contested MCQs are replaced before recall (example #1) |
| 12 | 11316 | note_creation_and_update/record_live_audio.feature | Continuous transcription while recording |
| 13 | 11282 | assimilation/note_refinement.feature | Generate a refinement layout for a note |
| 14 | 11088 | recall/property_memory_tracker.feature | Removing tracked property in markdown mode deletes property memory tracker |
| 15 | 10947 | note_creation_and_update/record_live_audio.feature | Append live recording transcription to note |
| 16 | 10766 | ai_generated_content/note_content_completion.feature | Rejecting a suggested note content completion |
| 17 | 10739 | ai_generated_content/note_content_completion.feature | Accepting a suggested note content completion |
| 18 | 10710 | messages/conversation_about_a_note.feature | Asking AI about a note returns a reply with focus context |
| 19 | 10690 | recall/recall_quiz_ai_question.feature | AI generated question - incorrect answer |
| 20 | 10600 | messages/conversation_about_a_note.feature | Follow-up question continues the AI conversation |
| 21 | 10578 | recall/refine_note_after_mcq.feature | Question-led refinement layout items are preselected when refining after MCQ |
| 22 | 10576 | note_creation_and_update/mcq_management.feature | Generate a question with AI |
| 23 | 10498 | ai_generated_recall_questions/question_contest.feature | Learner contests an MCQ and gets a replacement |
| 24 | 10384 | user_admin/manage_ai_models.feature | Admin chooses a default model |
| 25 | 10350 | ai_generated_recall_questions/question_contest.feature | Internally contested MCQs are replaced before recall (example #2) |
| 26 | 10210 | recall/property_memory_tracker.feature | Renaming tracked property key updates property memory tracker |
| 27 | 9989 | note_creation_and_update/mcq_management.feature | Refine a question with AI |
| 28 | 9579 | messages/conversation_about_a_note.feature | Exporting an AI conversation includes the conversation for external tools |

Top 10% total time: **341164 ms** (~341s)

### Grouping

- By file: **10** groups
- Batches of 3: **10** groups
- **Chosen:** by file (tie → prefer by file)

## Optimization rules

1. Remove or simplify redundant tests first.
2. Strictly no fixed-time waits.
3. Flaky = failure.

Hard-to-improve tests: propose under **Candidates** in
`ongoing/test-optimization-blacklist.md`. Permanent skip (developer Jidoka only):
tag Scenario or Feature `@skipOptimizationDueToKnownNecessarySlowness`.

---

### Optimize assimilation note_refinement
Type: Structure
Status: done

**Done:** 9→4 scenarios (~20s warm). Background assimilate; merged generate→remove and content/diff→extract; dropped blank-title + export (frontend unit coverage). Removed dead step defs / page-object methods.

---

### Optimize note_content_completion
Type: Structure
Status: done

**Done:** Dropped reject scenario (frontend unit coverage); removed dead cancel step/PO; redundant app-busy wait; `{delay:0}` typing. 3→2 scenarios (~8–9s warm).

---

### Optimize recall_quiz_ai_question
Type: Structure
Status: done

**Done:** Dropped correct-answer scenario; batch assimilate + wait for 3 recall-prompt GETs (no cy.wait(250)); dropped restartImposter before focus stubs; dead PO/helpers cleaned. 3→2 scenarios (~8s warm).

---

### Optimize question_contest
Type: Structure
Status: done

**Done:** Shared Background + API-seed `dueRecallPrompt`; visit recall without reload; evaluation stub helpers consolidated; dead steps removed. ~15s→~11s warm.

---

### Optimize record_live_audio
Type: Structure
Status: done

**Done:** Merged continuous+append into one scenario; dropped download E2E (frontend unit coverage); removed dead helpers. ~9.4s→~5.4s warm.

---

### Optimize property_memory_tracker
Type: Structure
Status: done

**Done:** Dropped rename + markdown-remove E2E (backend/frontend unit coverage); slimmed rich remove path; removed dead steps/POs. Slow pair gone (~21s cold).

---

### Optimize conversation_about_a_note
Type: Structure
Status: done

**Done:** 3→1 chained ask/follow-up/export; Background OpenAI stubs; slim follow-up asserts. ~12s→~5.5s warm.

---

### Optimize refine_note_after_mcq
Type: Structure
Status: done

**Done:** Deleted feature + E2E helpers; covered by frontend NoteRefinement/AnsweredQuestion unit tests and assimilation refine E2E.

---

### Optimize mcq_management
Type: Structure
Status: done

**Done:** Merged generate+refine into one scenario; router.push; consolidated MCQ stub helpers. ~9s→~6s warm AI path.

---

### Optimize manage_ai_models
Type: Structure
Status: done

**Done:** Session-as-admin (skip notebooks); direct Manage Models tab + intercept waits; save via alias. ~4.9s→~3.6s warm.

---

### Re-profile and close
Type: Structure
Status: planned

Re-run the same baseline profile command. Record metrics below. If full suite is red (e.g. Bad Gateway), document and use per-spec timings — do not fake green wall time. Clean up spent plan history per `planning.mdc`.

| Metric | Before | After |
|--------|--------|-------|
| Test count | 277 | |
| Suite wall | ~23m46s | |
| Top 10% total time | 341164 ms | |

**Candidates proposed this run:** (none / list)

**Commits:**
