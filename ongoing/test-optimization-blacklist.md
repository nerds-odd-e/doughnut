# Test optimization blacklist

Tests listed under **Skip test optimization** are excluded when selecting the top
10% for optimization. **Candidates** are proposals from optimization runs; move
to Skip test optimization only after developer review.

## Skip test optimization

<!-- file path — test/scenario name — reason — added YYYY-MM-DD -->

`e2e_test/features/note_creation_and_update/record_live_audio_with_real_open_ai_service.feature` — Record audio of a live event with real OpenAI service — external OpenAI transcription + 20s content poll; mocked coverage in `record_live_audio.feature` — added 2026-07-10

## Candidates

<!-- file path — test/scenario name — duration — why hard — proposed YYYY-MM-DD -->

`e2e_test/features/wikidata/associate_wikidata.feature` — Associate note to wikipedia via wikidata using real service — ~4201ms — hits live Wikidata network; mocked association scenarios already cover UI; backend unit tests cover Q12345 entity fetch; speeding without mocking is a product/network trade-off — proposed 2026-07-23
`e2e_test/features/recall/browse_answer_and_notes_while_recalling.feature` — Browse notes while recalling and come back — ~3572ms — the UI assimilation (`assimilateWithSpellingOption`) is required to create the pauseable recall session that makes the "Resume" menu item appear; API `assimilateNote` creates only one due tracker so the session completes after the single spelling question and "Resume" never shows; `verifySpelling` is read-only (no tracker creation), so the pauseable state comes from the UI assimilation flow's frontend session state and is not easily replicated via API setup — proposed 2026-07-23
`e2e_test/features/cli/cli_access_token.feature` — Set access token — ~3587ms — PTY-bound: per-scenario PTY spawn + node/Ink startup is the floor (~1s); scenario steps (generate token, `/set-access-token`, `/recall-status`) are already minimal with no redundancy or fixed waits; the CLI bundle only rebuilds when stale (shared infrastructure), not a scenario-level win — proposed 2026-07-23
