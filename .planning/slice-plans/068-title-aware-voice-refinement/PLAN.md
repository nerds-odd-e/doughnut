# Title-aware voice refinement

**Identity:** SEED-066#title-aware-voice-refinement
**Source:** [Refined story](../../seeds/SEED-066-voice-input.md#title-aware-voice-refinement).
**Authority:** Owner accepted the existing-request-first recommendation on
2026-10-11 and requested this plan. This preparation does not start execution.

## Goal and scope

Give note authors faithful, concise dictated title wording with suitable
punctuation in both New note and existing-title editing. Refine only the spoken
fragment; the editor retains the existing title outside its insertion range.
Ordinary phrases omit a terminal period, questions retain their question mark,
and detailed titles retain substantive information without an imposed length cap.
Content dictation retains its existing transcription behavior.

Deferred: toolbar focus targeting (the sibling story), a separate refinement
request unless evidence requires revisiting the approach, model migration,
automatic title generation, recorder redesign, and a numeric latency target.
No additional permanent real-service audio E2E scenario is planned.

## Workspace and decisions

- Reuse `/Users/terryyin/git/doughnut/.worktrees/refine-dictated-titles-with-title-aware-wording`,
  branch `codex/refine-dictated-titles-with-title-aware-wording`, preparation
  Rina-chan, starting revision `96e85acfcfee1ec098d20323413b67484fc37079`.
  Remote target is `origin/main`; integration checkout is
  `/Users/terryyin/git/doughnut`. Preparation start returned `continued`.
- One rule: the target field determines the instruction; title guidance applies
  to the dictated fragment, whether it will replace a whole title or join an
  existing one. Selection and saving remain editor responsibilities. Do not
  create separate prompts for New note, renaming, or caret insertion without
  evidence that the shared rule fails.
- Add optional title intent to the existing multipart contract, retaining
  content behavior for callers that omit it. The backend owns the instruction;
  regenerate the OpenAPI document and client. Keep the current response shape
  and final-transcription model for this first approach.
- [ADR 0005 — Web routes](../../../docs/adrs/0005-web-routes-accepted.md)
  governs `/api` and OpenAPI; [ADR 0006 — Failure handling](../../../docs/adrs/0006-failure-handling-accepted.md)
  governs deliberate recovery; [ADR 0007 — Environments and isolation](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
  governs all runtime observations. No exception is requested.
- Follow [North Star: One real-service audio test](../../NORTH-STAR.md#one-real-service-audio-test).
  Quality observation is a disposable probe, not a second permanent E2E test.
  No new North Star direction is needed.

## Existing solutions and proof boundaries

PFE search covered backend, frontend, CLI, MCP, generated contract consumers,
and E2E support. Reuse `SpeakTitleControl` for both title journeys,
`audioChunkToText` and `AiAudioController` for conversion, and the existing
`OtherAiServices` / `OpenAiApiHandler` provider path for instruction ownership.
The gap is target intent and a transcription prompt, not a missing recorder or
title editor. `SeamlessTextEditor` and `dictatedInsertion` already apply a saved
range and join words; `PathNameEditor` supplies ordinary name handling.
The title generation in `NoteRefinementAiToolFactory` belongs to extracting a
new note from existing content; its summarizing purpose is not a suitable owner
for preserving an explicitly dictated fragment.

Content uses `useNoteAudioProcessing`; its exact request assertion in
`NoteVoiceInputButton.processing.spec.ts` observes omitted title intent.
`AiAudioControllerTests` uses real multipart binding and captures the external
OpenAI request; extend that boundary to prove guidance selection. The shared
E2E transcription stand-in in `e2e_test/start/mock_services/openAiService.ts`
currently returns configured words regardless of prompt. Its supplied wording
cannot prove model quality or instruction routing; require request-sensitive
matching for the title scenario, with controller proof of the full instruction.
CLI and MCP have no production audio-conversion caller in the inspected tree.

| Promise | Owning slice and observable proof |
| --- | --- |
| Concise faithful wording, ordinary-period omission, question punctuation, detailed meaning | 1: actual audio-to-text results for the source examples; 2: outgoing title instruction reflects those rules |
| Whole titles and partial edits receive title intent | 3: mounted New note and existing-title journeys observe the audio request and rendered result |
| Caret/selection preserves surrounding text | 3: mounted placement cases plus one saved partial-edit journey and reload |
| New note saves the dictated title | 3: direct speech-to-submit test and persisted title after reload, with no typed correction supplying the result |
| Content keeps its processing behavior | 2: omitted-intent multipart cases for final text and mid-speech SRT; 3: body request and saved-content regression |
| Both title journeys use the same instruction | 2: controller request capture; 3: both mounted callers plus request-sensitive mocked E2E |

## Observations and remaining premises

All observations below were made in the named checkout on 2026-10-11. Reading
the current call chain establishes its design only; it is not a quality result.

- Ran `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteEditableTitle.spokenTitlePlacement.spec.ts tests/notes/NoteNewForm.spokenTitlePlacement.spec.ts tests/notes/NoteNewForm.spokenTitle.spec.ts`:
  21/21 passed. Temporarily replaced `SpeakTitleControl`'s `heardSegments` emit
  with a no-op and ran the same command: 16 failed, including caret, selection,
  default-title replacement, and rendered transcription. Restored the exact
  source and reran: 21/21 passed (3.34 seconds Vitest time).
  These tests discriminate insertion through the real editor with an external
  API mock. They do not establish model quality or database persistence.
- The New note test that types a correction before submitting passed even with
  delivery disabled. It proves typed submission, not spoken-title persistence;
  slice 3 owns a direct speech-to-submit observation instead.
- Ran `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteVoiceInputButton.processing.spec.ts`:
  4/4 passed (2.88 seconds Vitest time), including the exact content request,
  end timestamp, and originating-realm processing. This is a baseline, not proof
  that a future contract change preserves content.
- Executed the unpaid runner prefix with `CURSOR_DEV=true nix develop -c node
  --input-type=module`, importing `resolveInvocationCheckout`, `resolveSpecs`,
  `worktreeIsolationApplies`, and `runE2eHold` from their repository modules.
  `resolveInvocationCheckout({checkoutRoot:process.cwd(),isIsolatedCheckoutFn:worktreeIsolationApplies})`
  returned `isolated: true`. `resolveSpecs(['--spec',
  'e2e_test/features/note_creation_and_update/record_live_audio.feature'],
  process.cwd(),true)` admitted the private OpenAI mock route. The analogous
  real-service feature selection refused. `runE2eHold` imported successfully.
  This reaches selection only: provisioning, credentials, authenticated audio
  upload, and real title results remain slice 1's state-changing/paid probe.
- Provider documentation and source inspection support prompt transport, but
  actual wording quality remains unobserved. Slice 1 must settle it before
  dependent implementation. Saved-state E2E results also remain unobserved;
  slice 1 validates the disposable runtime route, slice 3 owns their acceptance.

## Ordered slices

### 1. Evaluate faithful title wording in one transcription request
Type: Behavior
Status: planned
Proof: A bounded audio comparison reports actual returned words against the
source examples; failure stops slices 2 and 3.

Behavior: Given recordings of an ordinary title, a question, the fragments
"Autumn" and "harvest", and the detailed orchard/frost title, apply the current
request and a candidate title-guided request to the same audio → observe whether
guidance produces faithful title text, preserves meaningful details and the
question mark, and omits an ordinary ending period. Compare actual output with
the recording, not only a prepared transcript. Baseline success is allowed;
the candidate must satisfy the examples, not manufacture an improvement claim.

- Use the named worktree's disposable held stack via
  `CURSOR_DEV=true nix develop -c pnpm e2e:hold --paid-openai` after obtaining
  the per-run paid-call authorization required by `.agents/agent-map.md`.
  That approval is not supplied by this planning request. Stop before paid
  work if it is unavailable; do not claim the premise settled.
- Establish isolated startup and authenticated multipart `/api/audio/audio-to-text`
  with an attached recording. A temporary prompt-only change to the existing
  provider request is sufficient for the comparison; revert it before retaining
  preparation evidence or starting slice 2. Record the literal commands,
  recording identities, prompt, returned words, and result in this plan.
- No more than the five paired recordings in the first comparison. Retain a
  concise sufficient instruction that names title/fragment use, faithful
  concision, meaningful details, and punctuation. No blanket stripping or
  whole-title rewriting. If quality fails, record which promise failed and
  revise this plan before dependent work; a second model call is not automatic.
- Safe stop: no product change remains; retain only decisive evidence. Runtime
  or credential failure stops here with its owner and missing operation named.
- Size: about 5 minutes active work with prepared recordings; isolated startup,
  obtaining recordings/authorization, and provider waits are explicit external
  wait exceptions. Do not conceal repeated prompt experiments inside that limit.

### 2. Carry title intent through the existing conversion boundary
Type: Structure
Status: planned
Proof: Real multipart controller tests capture the selected provider request;
legacy content responses remain green in the full backend suite.

Internal change: Extend the existing audio request with optional title intent
and pass the probe's instruction through the existing final-transcription path.
Omitted intent retains ordinary content behavior. This prepares only slice 3,
which connects the existing title UI callers; no frontend behavior changes in
this slice. Preserve the response shape, SRT continuation, and save ownership.

- Extend `AiAudioControllerTests` through MockMvc multipart binding. Capture
  the title provider request and assert the behavior-bearing instruction once;
  preserve the ordinary final-text and mid-speech cases with realistic input.
  Provider responses are mocked here; quality evidence belongs to slice 1.
- Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only` (all backend
  unit tests, required by the backend skill), then
  `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`. Generated files
  are regenerated, never hand-edited. Prove the generated optional contract
  accepts existing callers through the frontend typechecks listed below.
- Safe stop: current UI behavior is unchanged and all callers still compile;
  slice 3 activates title guidance. Size: 5–8 minutes active work; required full
  backend verification and API generation may exceed 10 minutes as a named
  test/build wait exception. Reassess implementation if active work exceeds 10.

### 3. Apply title guidance in both spoken-title journeys
Type: Behavior
Status: planned
Proof: Mounted callers prove title intent and insertion; request-sensitive
mocked E2E proves saved titles and unchanged content after reload.

Behavior: When an author speaks a new title or dictates into a whole-title,
caret, or partial selection target, send title intent and apply the returned
fragment through ordinary title insertion and saving → the title retains the
desired words and surrounding text after save/reload. Content dictation still
uses its content request and retains saved content.

- Set intent at the shared title conversion call. If the toolbar sibling has
  landed, follow its actual title caller and reuse the same intent; reconcile
  supported callers rather than restoring the superseded microphone placement.
- Extend mounted title tests to observe request intent and rendered examples.
  Include the question mark and detailed title; keep the existing placement
  cases. Add direct speech-to-submit proof without typing a replacement. Keep
  the body default request/processing regression; do not turn synthetic service
  output into evidence of AI behavior.
- Extend `record_live_audio.feature` under its existing mocked-service tag:
  condition title output on the title prompt at the provider boundary; cover
  one saved partial replacement and New note submission with reload, and retain
  the body saved-content observation. Reuse existing recorder readiness waits;
  the seed's reported early-audio race must not be assumed fixed.
- Run `CURSOR_DEV=true nix develop -c pnpm frontend:test`, both frontend
  typechecks below, and `CURSOR_DEV=true nix develop -c pnpm cy:run --spec
  e2e_test/features/note_creation_and_update/record_live_audio.feature`.
  Verify new assertions fail when title intent or the relevant insertion/save
  behavior is temporarily disabled, then restore and pass them.
- Safe stop: complete title behavior is delivered through both callers, with
  content preservation proved. Size: 5–10 minutes active work; full frontend
  and isolated E2E waits are explicit test exceptions. Reassess if recorder
  synchronization or mock matching reveals additional implementation work.

## Verification and delivery

The repository's [AGENTS.md](../../../AGENTS.md), stack skills, and
[execution wrap-up](../../../.agents/skills/dough-execute-plan/references/wrap-up.md)
own execution gates: Jidoka, a fresh post-change-refactor agent, API generation
when needed, coordinator `./scripts/run.sh pnpm format:changed` once, plan
update, commit with check-only lint hook, publication, and owned asynchronous CI
repair. This plan does not run those delivery operations during preparation.

Frontend acceptance requires both:

```sh
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit
```

Use existing sufficient evidence for unchanged content; hosted CI alone does
not create broader local checks. Record proof locations and results at delivery.
Target roughly five minutes per leaf; scrutinize above five, and stop/redecompose
above ten minutes of active implementation unless the named test/external-wait
exception applies. Do not split the shared title rule into per-example handlers.

## Plan review

The sequence isolates the paid premise, then one necessary conversion-contract
Structure immediately before the cohesive UI Behavior. No independent product
outcome, speculative framework, or duplicate prompt owner is included. A
separate slice-refinement pass is not needed: the boundaries, proof owners, and
test/wait exceptions are explicit. The unpaid route stops at provisioning and
paid access; these are bounded by slice 1, whose failure stops all dependent
work. No other slice-specific planning concern was identified.
