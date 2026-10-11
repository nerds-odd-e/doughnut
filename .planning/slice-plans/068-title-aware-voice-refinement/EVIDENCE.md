# Title-aware voice refinement evidence

Execution source and proof ownership remain in [PLAN.md](PLAN.md).

## Historical preparation observations

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

## Execution stop and resume

Slice 1 preparation only: five Samantha synthetic recordings of the seed's
exact examples are retained outside the product at
`/var/folders/65/16p4k5qj42qg7l46k2j0nhj40000gn/T/title-quality-jjemqm3o`.
`manifest.json` holds exact generation commands, hashes and durations;
`candidate-prompt.txt` holds the proposed shared instruction.
Credential availability was confirmed without exposing its value. No stack,
paid call, temporary provider edit, or product implementation started.
Owner authorized this bounded paid probe on 2026-10-11 ("yes, go ahead").
The existing claim and slice 1 resumed; its accepted result is recorded below.
The prior observer was stopped with confirmed receipt, zero unread events,
and confirmed local process exit. Setup and command readiness passed again;
the resumed observer is recorded in PLAN.md.

## Slice 1 paid comparison

Accepted 2026-10-11 at `32b131f18e779989c8f7e2651cb482c3485269f1` with
only the temporary provider prompt changed for the candidate half. The owner
explicitly authorized this run. Exactly ten authenticated multipart requests
returned HTTP 200; no repeat experiment. Existing `gpt-4o-mini-transcribe`.
Isolated database `doughnut_e2e_wt_3c3e00a263d449bfbd34c0eccb74d542`,
origin `http://127.0.0.1:62058`; startup healthy after seven polls.

Instruction accepted for slice 2:

> Transcribe the spoken words as a note title or title fragment. Use concise, faithful wording; preserve the speaker's meaning and every substantive detail. Omit an ordinary sentence-ending period, but retain appropriate punctuation, including a question mark for a question. Return only the dictated title fragment; do not invent or rewrite surrounding title text.

| Recording | Baseline words | Candidate words |
| --- | --- | --- |
| ordinary | Orchard harvest planning. | Orchard harvest planning |
| question | Why do apples ripen unevenly? | Why do apples ripen unevenly? |
| autumn | Autumn | Autumn |
| harvest | Harvest | Harvest |
| detailed | Autumn apple harvest planning for the north orchard after the September frost. | Autumn apple harvest planning for the north orchard after the September frost |

Same synthetic WAV bytes in each pair, generated with Samantha at rate 165,
mono 24 kHz PCM. Recording SHA256 identities:

- ordinary: `f9e2ea87a8ab84f5e9d9ef5b6f2a5af2aab35d7dc6ed4029fd1f08d8a58f0d80`
- question: `aae11be37b0500dfc585644621578c7d1aca4509d06c8b3c28fb9d32bcc3700e`
- autumn: `b02e8272e54fe9e28f62a45271a0950c2b2a0804e9f8fcdd9afae8b15b747f0e`
- harvest: `c2ecf2bfef0644113c31844d680ca88019bd645a77901bade0682525d1df0b9e`
- detailed: `fc31757e0335f8ccedfb942dbe64065bdf1b0e3c70662cef32f0aaffa3d1d500`

Literal commands (the script stores every literal curl invocation and response):

```sh
CURSOR_DEV=true nix develop -c pnpm e2e:hold --paid-openai
python3 /var/folders/65/16p4k5qj42qg7l46k2j0nhj40000gn/T/title-quality-jjemqm3o/probe.py baseline
python3 /var/folders/65/16p4k5qj42qg7l46k2j0nhj40000gn/T/title-quality-jjemqm3o/probe.py candidate --name ordinary
python3 /var/folders/65/16p4k5qj42qg7l46k2j0nhj40000gn/T/title-quality-jjemqm3o/probe.py candidate --name question
python3 /var/folders/65/16p4k5qj42qg7l46k2j0nhj40000gn/T/title-quality-jjemqm3o/probe.py candidate --name autumn
python3 /var/folders/65/16p4k5qj42qg7l46k2j0nhj40000gn/T/title-quality-jjemqm3o/probe.py candidate --name harvest
python3 /var/folders/65/16p4k5qj42qg7l46k2j0nhj40000gn/T/title-quality-jjemqm3o/probe.py candidate --name detailed
```

The script uses Basic `old_learner:password`, upload field `uploadAudioFile`
and `midSpeech=false` at `/api/audio/audio-to-text`. Coordinator inspected
`manifest.json`, `probe.py`, all ten `*-evidence.json` actual response bodies,
`probe-summary.json`, and the restored source diff. Ordinary/detailed periods
were omitted, question punctuation and every substantive detailed word retained.
Both variants capitalized Harvest; exact mid-title casing is not established.
This bounded result supports guidance in the existing request, without a claim
about arbitrary speech, general reliability, UI saving, or latency.

Provider restored byte-for-byte SHA256
`3eb2ffb04056468cf3c1db681eac1ec541354e67d1d56f79e51ce57d3bd505d5`.
Held session `81979` ended on Ctrl-C with exit 0, runtime owner absent.
Fresh refactor agent split historical observations into this file; no product
edits or invalidated probe proof, whitespace check passed, no tests needed.
Coordinator selective format passed (planning-only no-op).

## Slice 2 contract and consumers

Slice 1 published `76aec3c8e1641bb5779f5f2c6b3b4136229a7f29` on the
execution branch, suffix base `32b131f18e779989c8f7e2651cb482c3485269f1`;
managed delivery reused the recorded observer. Slice 2 adds optional boolean
`titleIntent`, default false, through real multipart binding to final-text
transcription. OtherAiServices owns the accepted instruction; the existing
provider accepts optional prompt. SRT and response shape stay intact.

Terminal passes in this worktree:

```sh
CURSOR_DEV=true nix develop -c pnpm generateTypeScript
CURSOR_DEV=true nix develop -c pnpm backend:test_only
CURSOR_DEV=true nix develop -c pnpm frontend:test
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm openapi:lint
```

Generation precedes backend tests to satisfy RobotsTests YAML synchronization.
SDK 4.80 prompt setter accepts String, so transport uses Optional.ifPresent.
Full backend passed, AiAudioControllerTests XML: 12 cases, zero failures/errors.
Real MockMvc/external OpenAI capture in titleIntentAsksForFaithfulTitleFragmentWording
observes true field binding and full accepted instruction. stopAsksTheFastModelForPlainText
and midSpeechAsksWhisperForSrt observe omitted-intent model, response format and
empty prompt. stopWritesTheWholeTranscriptionAsOneSegmentWithoutEndTimestamp
and midSpeechWritesTheTranscriptionOfAllButTheLastSegment prove retained results.
RobotsTests.openApiDocsMatchCommittedYaml passed.
Frontend: 338 files, 2146 cases passed, including body exact omitted-intent
request in NoteVoiceInputButton.processing.spec.ts and supported title callers.
Both typechecks prove existing callers accept generated titleIntent?: boolean.
Consumer search across backend/frontend/CLI/MCP/packages/E2E found only shared
frontend production audio calls; no CLI/MCP conversion caller. E2E mock remains
compatible; activation and persisted observations stay with slice 3 (G2).
Coordinator inspected handwritten diffs, generated optional contract, controller
XML and body request assertion. Fresh refactor: already clean, no edits/tests;
whitespace check passed. Selective format passed with no semantic change.

## Slice 3 question-save discovery

The first E2E run passed body reload, partial selected-title save/reload, retry,
and linked rename, but New note question submission returned HTTP 400.
NoteUpdateTitleDTO inherits NOTE_TITLE_REGEXP prohibiting ASCII `?`;
PathNameEditor only mapped backslash/slash/colon. The provider and editor
retained the question punctuation, so mocks were not its cause.
Coordinator selected the existing normal-name solution: backend
DisplayNamePathSeparators.normalizeNoteTitle/toFullwidthOrSpace already maps
ASCII `?` to fullwidth `？`. Extend the existing frontend normalizer to the
same mark; retain question semantics/punctuation and backend name constraint.
Do not claim raw ASCII persistence or add title-only punctuation stripping.
This is necessary repair at the existing title naming seam, no new provider
operation, backend rejection change, or architectural exception.

## Execution context

- Execution established 2026-10-11: Akiho-chan, publisher
  `dashboard-territory.local-doughnut`, Story Branch Mode; owned execution
  checkout is the named worktree below, integration/default checkout is
  `/Users/terryyin/git/doughnut`, remote trunk `origin/main`, increment target
  `origin/codex/refine-dictated-titles-with-title-aware-wording`.
  Claim/base `32b131f18e779989c8f7e2651cb482c3485269f1`, execution starting
  revision `67a193879089732f7093db75f9b27cd1321da76f`.
  Changed-since-readiness indication retained: toolbar targeting has landed;
  use its actual supported title caller. Existing scope and Ready still apply.
  Existing planning authority permits refining remaining work on an overrun.
  Checkout setup and `./scripts/run.sh pnpm exec biome --version` succeeded.
  CI source GitHub Actions, verified selector `ci.yml`; Codex observer
  coordinator `akiho-title`, directory `/tmp/dough-ci-501/watch-4QRYW9`,
  PID `7600`, yielded cell `3`, session `98491`, observing the execution
  branch from this checkout. The claim's trunk CI is unobserved.

## Slice 3 title journeys and persistence

Shared useTitleVoiceInput sends titleIntent true; body calls omit intent.
PathNameEditor uses ordinary fullwidth question-mark normalization, preserving
question punctuation and the existing backend name constraint. Mounted New note
and toolbar tests assert literal question/detailed wording, the actual request,
and direct submit/debounced saving. Caret and partial selection cases retain
surrounding words. Provider-sensitive mocked E2E proves New note question
save/reload, selected-title replacement/reload, and body content reload;
retry and linked rename remain covered. Synthetic output is not quality proof.

Terminal passes in the execution checkout:

```sh
CURSOR_DEV=true nix develop -c pnpm frontend:test
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/record_live_audio.feature
```

Full frontend: 338 files, 2151 cases; selected E2E: all 5 scenarios. Logs:
/tmp/title-full-frontend-normalized.log and /tmp/title-e2e-normalized.log.
Final focused title placement, toolbar, New note and body: 30/30 cases.
Mutation observations, restored byte-exact before passing:
missing title intent: 4 failures/15 passes; missing question normalization:
2 failures/17 passes; disabled heardSegments insertion: 19 failures/7 passes.
Logs: /tmp/title-intent-mutation.log, /tmp/title-question-normalization-mutation.log,
/tmp/title-insertion-mutation.log, /tmp/title-final-focused.log.

Optional standalone E2E tsc invocation found 130 unique diagnostics identical
to the published baseline after location normalization, zero introduced.
Required frontend typechecks and the actual E2E gate passed. Comparison logs:
/tmp/title-e2e-candidate-typecheck.log and /tmp/title-e2e-baseline-typecheck.log.
No extra paid calls. Active implementation about 8 minutes; suite waits excluded.

Coordinator inspected production, mounted tests, E2E and terminal logs. Fresh
post-change refactor made expected spoken/saved values literal and removed the
orphan caret step/page-object helper. One-time whole-product search for
placeTitleCaretAfter and its Gherkin wording returned no references; retained
caret tests and persisted partial replacement prove the surviving behavior.
After refactor, invalidated two title files: 19/19 cases and both frontend
typechecks passed; whitespace check passed. Existing E2E boundaries were
unchanged by those edits. Selective formatter passed; reviewed mechanical
formatting introduces no semantic changes. G2 and G4 are proved by this slice.
Previous accepted publication: f7dfae9e25307064497edc7379eb502318dc6479.
