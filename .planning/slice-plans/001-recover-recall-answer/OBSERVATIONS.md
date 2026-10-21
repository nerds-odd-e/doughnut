# Recall recovery observations

## Preparation observations and decisive premises

Observed on 2026-10-21 in
`/Users/terryyin/git/doughnut/.worktrees/recover-a-recall-answer-after-a-failed-or-lost-s`
at `c242d19afddb88b0f2a0246cc1c69290de7f23eb` plus temporary observation tests.
These are bounded reproductions, not production incidents or proof of a remedy.
All scratch tests were deleted before recording preparation.

| Premise / consuming operation | Setup, literal observation and result | Limit / disposition |
| --- | --- | --- |
| A failed spelling request leaves no usable submission action | Temporary `frontend/tests/recall/AnswerRecoveryPreparation.spec.ts` mounted the real RecallPromptCard with its spelling child; existing test support supplied the prompt, SDK returned a wrapped network error, and the form submitted `cat`. `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/recall/AnswerRecoveryPreparation.spec.ts` failed at the expected enabled-submit assertion: `disabled` was `""`; retained input assertion passed. One test, one expected failure; browser run 4.13s. | Reproduces frontend failure through DOM input and form submission. SDK mock does not prove server acceptance; slices 2 and 4 own that separately. |
| Repeating an accepted MCQ request cannot recover it | Temporary `RecallAnswerRecoveryPreparationTest.acceptedMcqCanBeRecoveredByRepeatingItsSubmission` accepted a controller answer, flushed/cleared persistence, reloaded the prompt and submitted again. `CURSOR_DEV=true nix develop -c pnpm backend:test_only` threw `QuestionAnswerException: The question is already answered` at the second controller call. | Real isolated MySQL persistence, but the test transaction did not simulate a lost wire response or concurrent committed requests. Slice 1 probes the latter; slice 2 establishes the replacement contract. |
| Reloaded spelling projection omits accidental-match continuation data | In that same run, `acceptedAccidentalMatchCanBeProjectedAfterReload` submitted a real accidental match through the controller, confirmed one matched note, flushed/cleared and projected the reloaded prompt with `AnsweredQuestion.from`. The required matchedNotes assertion failed with null. | Confirms the projection gap, not that a new result path works. Slice 4 must construct and observe the complete recovered reply through the controller, including several matching notes. |
| Existing backend regression baseline runs here | The same all-backend run completed 2,857 tests, with exactly the two expected scratch failures and two skipped; no other failures. Gradle test phase reported 1m13s, after initial isolated DB provisioning/migration. | Existing passing tests are baseline evidence, not claimed discriminating proof of recovery. No implementation was retained. |
| Overlapping requests can share one acceptance boundary with fresh committed state | The existing lock is located, but its behavior with preloaded controller entities has not been observed. | Bounded by the early state-changing MySQL probe in slice 1; do not infer serialization from annotation presence. |

## Probe reassessment

- Probe reassessment: preloading inside a repeatable-read acceptance transaction
  established a snapshot that still omitted the committed Answer after locked
  refresh (2026-10-21 scratch `RecallPromptSerializationProbeTest`, committed
  case); its rollback case accepted successfully. All-backend command
  `CURSOR_DEV=true nix develop -c pnpm backend:test_only` ran 2,857 tests,
  one scratch failure and two skips. This stronger setup differs from HTTP
  path-variable loading before controller transaction entry. The next bounded
  probe must reproduce that before-transaction managed preload under OSIV;
  default isolation remains the first candidate. No dependent implementation
  may infer fresh visibility from the failed snapshot setup. If the faithful
  boundary also fails, reassess isolation or read construction before slice 2.
