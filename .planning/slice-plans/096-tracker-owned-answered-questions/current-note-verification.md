# Current-note verification

**Accepted implementation proof (2026-11-13).** `RecallPrompt.getNote()`
derives the tracker note; conversation subject/title, prompt read authorization,
contest and regeneration consume it. The explicit generation-note evaluation
path retains its input, and stored question-note writes are unchanged.
Actual property reification through `ControllerTestBase.reifyTrackedProperty`
supplies the move in the controller observations; it never sets the resulting
tracker note. History observes the retained answered MCQ beside the current
tracker note; conversation start/listing and answer payload observe current
note identity; contest, regeneration and conversation AI requests observe the
exact `<focus_note>` title, and AI correction capture observes current note ID.
The source-review rows name each observing test; current-note authorization
also has positive access and active other-owner refusal observations.

Literal final proof command:
`CURSOR_DEV=true nix develop -c pnpm backend:test_only > /tmp/donut-slice2-backend-test-final.log 2>&1`.
Terminal exit 0, `BUILD SUCCESSFUL in 1m 54s`: 3,202 tests, zero failures/errors,
two skipped across 637 current reports (01:55:51–01:57:33 UTC). The earlier full
pass took 5m30s after a 1m03s cold migration; final proof renewed three initially
broad title assertions to identify the focus section specifically. Editing and
inspection took about seven minutes; verification waits were separate.
Consumer trace: `/tmp/donut-slice2-consumer-trace-final.md`; inspected consumers
include answer DTO, conversation AI/correction capture/review/acceptance,
generation/evaluation, spelling and test builders. Their relevant controller
and generation cases ran in the final full suite; menu unread counts and
planning/path-only matches do not consume the changed attribution.

Expensive-proof decision: this Mac has 16 logical CPUs/48 GiB RAM. Initial
load 38.88/34.84/31.82 with memory pressure reporting 43% free; final launch
load 24.50/33.03/32.90 and 47% free. One full suite at a time, no competing
owned heavy proof; reconsider after failure or changed contention. Both runs
reached terminal pass. No wire/schema shape changed, so generation is not
triggered. Source freshness was unchanged at `9baf479ddac80f7e0b8bf27390f876109b0ff4d9`;
no named sibling coordination dependency applies. C84's actual compatibility
release observation belongs to slice 8, rather than being claimed from local
code.

Independent refactoring completed: `RecallPrompt` delegates notebook, spelling
and edited-note helpers to `getNote()`; conversation listing titles delegate to
`Conversation.getSubjectNote()`. Existing MCQ/spelling/unknown-subject conditions
and all setup/assertions remain unchanged. Refactor trace:
`/tmp/donut-slice2-refactor-trace.md` (51 consumers, all dispositioned).
Renewed literal proof command:
`CURSOR_DEV=true nix develop -c pnpm backend:test_only > /tmp/donut-slice2-refactor-backend.log 2>&1`.
Exit 0: migration/compile 20s, tests 1m59s; 3,202 tests, zero failures/errors,
two skipped across 637 current reports (02:02:53–02:04:41 UTC), profile `test`,
isolated database `doughnut_wt_6c52c877a9c84ab186ec2c44fc117692_test`.
Launch load was 6.74/13.69/23.12 with memory 44% free and no competing owned
heavy proof; one suite at a time remained the selected choice. Refactor marker
and whitespace check passed. The pre-existing oversized plan was reorganized
by cohesive execution context/evidence/instruction seams; the independent pass
verified unchanged live leaf fields, source instructions, dated evidence,
release gates and incoming anchors, with every record under 250 lines.
This records-only delivery repair changes no story outcome or slice sequence.
