# Isolated OpenAI browser mocks

Source: [SEED-015 story 3](../../seeds/SEED-015-concurrent-worktree-environments.md#story-3).
Status: done.

## Goal and scope

Two local worktrees can accept different mocked OpenAI note-content suggestions
without sharing mock responses or request recordings. Ordinary `pnpm sut` then
`pnpm cypress run --spec e2e_test/features/ai_generated_content/note_content_completion.feature`
(and the no-mock note-editing spec). One runner lease per checkout.

## Outside-in proof

| Promise | Leaf | Evidence |
|---|---|---|
| Ordinary isolated OpenAI run; private context for stubs/recordings | 2 | Cypress note_content_completion 2/2 |
| Shared defaults / no-mock without Mountebank | 1–2 | Unit + follow-up note-editing after mocked run |
| No persistent mock allocation; non-colliding endpoints | 2 | Identity unchanged; private ports |
| Only the two named single-spec selections | 2 | Guard unit tests |
| Foreign mock refused before mutation | 3 | Foreign management/serving unit proofs |
| Normal completion releases mock before lease | 2 | Follow-up runner acquires lease |
| Background mock failure fails run and cleans up | 4 | Spawned-runner kill-mock proof |
| Cancellation releases owned mock | 5 | SIGINT/SIGTERM cancel proofs |
| Peer survives reset/reconfigure both ways | 6 | Paired harness `--mode openai-mock` both directions |

## Ordered slices

### 1. Bind OpenAI mock operations to one endpoint context
Type: Structure — Status: done
Proof: `openAiMockEndpointContext.test.ts`

### 2. Accept a content suggestion through a private mock run
Type: Behavior — Status: done
Proof: Cypress `note_content_completion.feature`; follow-up `worktree_note_editing.feature`

### 3. Refuse a foreign mock before changing test state
Type: Behavior — Status: done
Proof: `isolated-cypress-openai-mock.test.mjs` foreign management/serving cases

### 4. Stop the affected runner when its mock fails
Type: Behavior — Status: done
Proof: `isolated-cypress-openai-mock-failure.test.mjs`

### 5. Cancel a mocked run without blocking its next run
Type: Behavior — Status: done
Proof: `isolated-cypress-openai-mock-cancel.test.mjs` (SIGINT/SIGTERM)

### 6. Preserve a peer's content suggestion across mock resets
Type: Behavior — Status: done
Proof: `node scripts/worktree-reset-isolation-harness.mjs --mode openai-mock`
both directions; peer/resetter markers and suggestions distinct; recording
rejects shared 2525/5001. Cypress 16: detect `--spec` from parent Cypress.app
argv so private MB injects into `expose` at setup.

## Learnings

Runner-scoped temporary Mountebank ports avoid persistent mock allocation.
Isolated SUT health treats Gradle-forked backends as owned via PGID or PPID
ancestry with `bootRunE2E --no-daemon`. Cypress 16 uses `expose` (not
`Cypress.env`) and may strip `--spec` from the config process argv.
