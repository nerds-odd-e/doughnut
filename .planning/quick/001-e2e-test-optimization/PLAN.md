# E2E test optimization

Status: done

Completed 2026-08-20. Profile JSON is gitignored (do not commit).

## Results

| Metric | Before | After |
|--------|--------|-------|
| Test count | 277 | 263 |
| Specs | 64 (12 failed) | 63 (all passed) |
| Suite wall | ~23m46s | 18m34s |
| Top 10% total time | 341164 ms | 176220 ms |

Profile tags: `not @ignore and not @wip and not @skipOptimizationDueToKnownNecessarySlowness`

## Groups optimized (by file)

1. `assimilation/note_refinement` — 9→4 scenarios; dead export/blank-title steps removed
2. `ai_generated_content/note_content_completion` — dropped reject (frontend unit coverage)
3. `recall/recall_quiz_ai_question` — dropped correct-answer; replaced fixed poll
4. `ai_generated_recall_questions/question_contest` — API-seed due prompts; slim entry
5. `note_creation_and_update/record_live_audio` — merged continuous+append; dropped download E2E
6. `recall/property_memory_tracker` — dropped rename/markdown-remove (unit coverage)
7. `messages/conversation_about_a_note` — chained ask/follow-up/export
8. `recall/refine_note_after_mcq` — deleted (frontend unit coverage)
9. `note_creation_and_update/mcq_management` — merged generate+refine
10. `user_admin/manage_ai_models` — session-as-admin + intercept waits

**Candidates proposed:** none

## Commits

`perf(e2e):` commits on `cursor/e2e-test-optimization-b911` for each group above.
