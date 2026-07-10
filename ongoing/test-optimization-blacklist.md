# Test optimization blacklist

Tests listed under **Skip test optimization** are excluded when selecting the top
10% for optimization. **Candidates** are proposals from optimization runs; move
to Skip test optimization only after developer review.

## Skip test optimization

<!-- file path — test/scenario name — reason — added YYYY-MM-DD -->

`e2e_test/features/note_creation_and_update/record_live_audio_with_real_open_ai_service.feature` — Record audio of a live event with real OpenAI service — external OpenAI transcription + 20s content poll; mocked coverage in `record_live_audio.feature` — added 2026-07-10

## Candidates

<!-- file path — test/scenario name — duration — why hard — proposed YYYY-MM-DD -->
