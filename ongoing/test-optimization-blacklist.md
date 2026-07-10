# Test optimization blacklist

Tests listed under **Ignored** are skipped when selecting the top 10% for
optimization. **Candidates** are proposals from optimization runs; promote to
Ignored only after developer review.

## Ignored

<!-- file path — test/scenario name — reason — added YYYY-MM-DD -->

## Candidates

<!-- file path — test/scenario name — duration — why hard — proposed YYYY-MM-DD -->

`e2e_test/features/note_creation_and_update/record_live_audio_with_real_open_ai_service.feature` — Record audio of a live event with real OpenAI service — ~7592ms — external OpenAI transcription + 20s content poll; mocked coverage in `record_live_audio.feature` — proposed 2026-07-10

`e2e_test/features/recall/browse_answer_and_notes_while_recalling.feature` — View last answered question when the quiz answer was correct — ~4351ms — shares Background with Resume scenario; API spelling assimilate/answer breaks pause/Resume; split file or Phase 4 merge with remove/revive — proposed 2026-07-10
