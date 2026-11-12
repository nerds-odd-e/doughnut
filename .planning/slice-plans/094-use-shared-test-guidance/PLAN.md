# Use shared test guidance

**Identity:** SEED-126#use-shared-test-guidance
**Source:** [refined story](../../seeds/SEED-126-use-shared-test-guidance.md#use-shared-test-guidance)
**Source review:** [source-only commitments](source-review.md)

## Goal and boundaries

A Doughnut test author or refactorer reaches the installed Open Dough MakeMe
and browser page-object conventions for the entries enabling them, together
with the local integration guidance needed to apply them. Execution is authorized by the developer’s Dough Execute Plan instruction.

Audit the five local testing skills and their linked guidance. Replace generic
convention copies with selection-aware routing. Preserve TypeScript imports,
Java construction and persistence, destructive-fixture integrity, CLI helpers,
Cypress named navigation, and app-busy waits in their local homes. Existing
tests and helpers are reading examples, not conversion targets. Product code,
test suites, builders, shared Open Dough conventions, and behavioral coverage
are outside the change.

## Current decisions and existing solutions

- PFE: reuse `dough-makeme`, `dough-page-objects`, their convention references,
  and `dough-execute-plan/scripts/test-entry-rules.mjs`. The selector already
  owns Intent membership and independent opt-ins; add no second selector or
  copied convention. Route ordinary authoring and implicated refactoring from
  the existing local skill entrypoints, not just from Execute Plan.
- Keep the current `intent.locations` configuration. For Cucumber support-only
  work, document tracing the changed step or page-object operation through its
  callers to the owning browser feature(s), selecting those feature paths,
  and carrying edited support paths plus owner evidence into the brief. This
  is explicit owner routing; a support path itself does not match the feature
  entry. For a new support file, identify its intended owning feature first.
  An unresolved owner/selection stops only convention-dependent work, following
  the installed skills' unresolved-rule handling.
- A CLI feature selects `pageObjects`, but the shared convention applies only
  when the work concerns browser tests. Preserve terminal-specific helpers and
  their independent observations. Scripts select neither convention and keep
  their existing fixture practice. A browser-mode frontend unit test does not
  acquire page objects merely by running in Chromium.
- The MakeMe routing replaces the unconditional generic fixture prescription
  in `unit-testing`; stable boundaries, focused assertions, and removal proof
  remain there. Stack skills retain imports, real database/persistence
  requirements, and helper-specific examples rather than another generic rule.
- The existing North Star covers product architecture, attachments and audio;
  none governs this guidance-only change. No domain model or Accepted ADR is
  being changed, so no architecture decision or new North Star topic is needed.
- File-size preparation: local skill sizes at `772ae10251` are unit-testing
  108, frontend-testing 228, backend-testing 117, CLI 119, E2E authoring 271
  lines. Slice 2 reorganizes E2E's environment/origin/run/profiling/debugging
  responsibility into `references/running-e2e.md` with a discoverable entrypoint
  link, preserving commands and relative links. Browser authoring, named
  navigation and busy waits remain together in `SKILL.md`. Its callers are
  authors/refactorers entering the E2E skill; its preserved proof is the linked
  authoring/running walkthrough below. Keep every edited file within the
  existing 250-line check, using cohesive responsibilities rather than cuts.

## Outside-in proof ownership

The user of this change is a test author reading the local skill. Proof is a
documented guidance walkthrough plus the real installed selector, using actual
source paths. No application or fixture behavior changes, so the frontend,
backend, CLI and Cypress suites are not local gates for this documentation-only
change. Do not add tests that mirror prose or check retired prose's absence.

| Final-state promise | Owner | Observable proof |
| --- | --- | --- |
| Frontend, CLI and Java test authors reach shared MakeMe according to entry selection | 1 | From each local skill, follow the shared entrypoint and selector, then its selected convention; replay the three MakeMe paths in the baseline command below |
| Local imports, Java construction/persistence and destructive-fixture completeness remain usable | 1 | Read the retained local guidance against `noteStore.trashNote.spec.ts`, `contestAndRegenerateMcq.test.ts`, `TextContentValidatorTest.java`, and the existing destructive-fixture section; observe where each local requirement is supplied |
| Scripts keep their fixture practice; MakeMe/pageObjects remain independent selections | 1 | Walk the always-on unit-testing routing with the two scripts paths below and the browser feature; use the selector results rather than unconditional local fixture instructions |
| Browser feature authors reach shared page objects with local navigation and loading rules | 2 | Follow the E2E entrypoint to shared guidance and local named-router/app-busy guidance for `conversation_about_a_note.feature` |
| Step-only and page-object-only edits receive the owning browser feature's convention | 2 | Replay the documented route separately starting from `conversation.ts` and `conversationAboutNotePage.ts`, with no feature edit; trace callers to the feature and run its selection, retaining the support/owner mapping |
| Terminal-only CLI work keeps its CLI-specific helpers | 2 | Walk `cli_install_and_run.feature` through E2E and CLI guidance: its pageObjects opt-in is read together with browser-only applicability and terminal helper guidance |
| Retained guidance and relocated running instructions are reachable | Both | Follow each new/changed Markdown link from its consumer, including links within `running-e2e.md`; read their destinations and check affected file sizes |

The one-time acceptance sweep is a separate reading: search the whole product's
guidance and dependent references for the generic MakeMe and browser page-object
paragraphs being retired, classify canonical shared guidance and local
integration details, and remove remaining duplicate copies or stale links.
Keep no permanent absence assertion, checker, or historical removal note.

## Decisive premises and preparation observations

Observed 2026-11-12 in this owned workspace at `772ae102512618543396ec58f0f89f1cde4714b0`;
the relevant config, local skills and shared selector remain unchanged. No
scratch product edit was made. The literal baseline command, reused for final
selection proof, is:

```sh
CURSOR_DEV=true nix develop -c node .agents/skills/dough-execute-plan/scripts/test-entry-rules.mjs \
  --project /Users/terryyin/git/doughnut/.worktrees/use-shared-makeme-and-page-object-guidance \
  --source frontend/tests/store/noteStore.trashNote.spec.ts \
  --source cli/tests/contestAndRegenerateMcq.test.ts \
  --source backend/src/test/java/com/odde/donut/validators/TextContentValidatorTest.java \
  --source e2e_test/features/messages/conversation_about_a_note.feature \
  --source e2e_test/step_definitions/conversation.ts \
  --source e2e_test/start/pageObjects/conversationAboutNotePage.ts \
  --source e2e_test/features/cli/cli_install_and_run.feature \
  --source scripts/test/run.sh.test \
  --source scripts/test/path-routing/path-routing-validation.test.mjs
```

Use the execution checkout as `--project` when execution starts elsewhere;
recheck selection there before depending on it.

| Premise | Consuming operation/form and result | Unverified remainder |
| --- | --- | --- |
| Current entries independently select the intended conventions | The exact command above exited 0, `status: resolved`: all three unit-test sources select MakeMe only; both feature sources select pageObjects only; support sources and both scripts select neither | None for current membership; replay after guidance changes |
| Support-only authoring needs explicit feature ownership | Starting from each conversation support source, the raw selector above returns both rules off; tracing the existing feature/step/page-object chain supplies a pageObjects-selected browser feature | The new prose route is slice 2's deliverable, not an already observed implementation |
| Shared guidance covers generic rules and leaves stack mechanics local | Read the selected shared convention references: MakeMe names fresh builders, defaults, domain operations, isolation and independent evidence, and explicitly leaves language/import/persistence to the project; page objects names focused owners, caller inputs/variants, meaningful waits and independent evidence, and leaves framework mechanics local | Final link-and-content walkthrough belongs to each slice |
| E2E entrypoint can retain its full authoring responsibility under the size check | Reading and line count: 271 lines; environment/origin/run/profiling/debugging form a cohesive running reference separate from authoring/navigation/waits | Slice 2 checks the final seams, links and sizes |

These are guidance and selection observations. They make no claim about running
the application journeys or the existing examples' behavioral coverage.

The inspected owner route is the feature's `I start a conversation about the
note … inviting AI with the message …` step → `conversation.ts` →
`start.jumpToNotePage(note).sendMessageToAI(conversation)` →
`noteConversationAndQuestionMethods.ts` →
`ConversationAboutNotePage.replyToConversationAndInviteAiToReply()`, which owns
input, click and the busy wait. Its messages Then step passes caller-owned
role/message expectations to `ConversationAboutNotePage.expectMessages()`.

## Ordered slices

### 1. Follow shared MakeMe from local testing guidance
Type: Behavior
Status: done
Proof: The slice-1 rows in the proof table, replaying the selector command and
following the author/refactorer guidance chain for frontend, CLI, Java and scripts.

Behavior: A test author or refactorer starts in the always-on unit-testing or
stack testing guidance for a concrete source → follows its fixture guidance →
the source is selected through shared MakeMe's entrypoint, and the selected
convention plus Doughnut's imports and construction/persistence rules give a
complete applicable instruction. Script tests retain their established practice.

Replace generic builder/default/extension copies in unit-testing,
frontend-testing, backend-testing and CLI guidance with shared routing. Audit
their linked guidance as part of the same ownership change. Retain stable
boundary and independent-proof rules, Java persistence and destructive-fixture
integrity, TypeScript fixture imports, SDK mocking and CLI helper details.
Sizing: about five active minutes including focused reading and selector replay;
the preparation already identified the shared owners and affected local homes.

### 2. Follow shared browser guidance for features and support-only edits
Type: Behavior
Status: done
Proof: The slice-2 rows above, separately exercising the feature, step-only,
page-object-only, and terminal-only guidance routes; final one-time sweep.

Behavior: A browser test author or refactorer starts with a feature or only a
Cucumber support source → follows E2E guidance, resolving the owning browser
feature where necessary → the shared convention is selected, with local named
navigation and action-owned app-busy waits available. Terminal-only work follows
its existing CLI helpers with the browser scope explicit.

Replace generic page-object ownership/fluent/naming prose with shared routing;
retain the local Cypress/CLI mechanics and behavioral assertions' independent
evidence. Carry the support-to-feature relationship through ordinary authoring
and implicated refactoring, including cases where `--changed` lists only the
support source. Perform the E2E running-reference reorganization described above
inside this slice; audit its linked guidance and repair moved relative links.
Sizing: five to eight active minutes including relocation and walkthroughs.
Scrutiny above the five-minute target: the running-reference move preserves the
same E2E entrypoint and enables this one browser-guidance outcome; separating it
would fragment that proof loop. No application-suite wait is involved.

## Delivery and concern evidence

Execution follows the repository's normal Jidoka, fresh post-change-refactor,
coordinator `./scripts/run.sh pnpm format:changed` once, plan update, agent
commit/check-only hook, and push gates (`AGENTS.md`, Execution wrap-up). No API
generation is expected. Use `scripts/check_diff_whitespace.sh` for whitespace.
Run the consumer-trace procedure after guidance edits and disposition its
guidance consumers; retained test sources are reading examples, not changed
behavior. A wider suite becomes necessary only if implementation expands beyond
this documentation/routing boundary under appropriate authority.

Slice-plan refinement was not needed: two independently usable guidance
outcomes each have one walkthrough/selector proof loop. The common model is
shared entry selection plus shared conventions and local integration details;
support ownership routes to that model rather than introducing new membership.
Remaining slice-specific concerns: none identified after the baseline and
guidance audit. No human decision or external dependency blocks these slices.

## Learnings

None beyond the preparation observations above.

## Execution context

Established start retained: story-branch mode, workspace
`/Users/terryyin/git/doughnut/.worktrees/use-shared-makeme-and-page-object-guidance`,
branch `codex/use-shared-makeme-and-page-object-guidance`, remote `origin`,
trunk `main`, publisher `dashboard-territory.local-doughnut`, agent Kirara-chan.
Starting revision `b503972834ecdae1026118f6d0a57a5fad5f44de`;
claim and initial published base `a5f5b56692c3749f9508bfe7e30fcefee3b1ab3c`.
Slice 1 accepted on the execution branch: `78a6275f59584502829c57b1f3ca204fac1e88ba`
(suffix base was the claim); observation attached to the retained stream.
The existing owned worktree was reused; no integration checkout was supplied.
Replanning retains the existing planned-execution authority and scope.
No user-visible path or persisted behavior is removed by these guidance edits.

Checkout setup: `./scripts/run.sh bash scripts/worktree_setup.sh` exited 0,
locked dependencies unchanged. The literal baseline selector command above
exited 0 with the recorded selections in this exact checkout on 2026-11-12.
The active pre-commit hook runs check-only `lint:changed`; the coordinator owns
`./scripts/run.sh pnpm format:changed` through guarded fresh delivery.

CI source: GitHub Actions `ci.yml`, verified active as `donut CI`, push-triggered
on all branches. Story-branch target is
`nerds-odd-e/doughnut:codex/use-shared-makeme-and-page-object-guidance`.
Claim CI on trunk is unobserved. Observer coordinator `kirara-seed126`,
directory `/tmp/dough-ci-501/watch-9bfSqM`, PID 87514, stream session 18535,
yielded cell `g100:13`, runtime in this checkout's `.agents/skills/dough-execute-plan`.
No expensive verification is needed: the guidance/selector boundary requires
only bounded reading and local commands; application suites are outside it.

## Slice 1 acceptance

The four fixture sections reach shared MakeMe selection and the convention.
The baseline selector replay passed unchanged; TS examples/imports, Java
EntityBuilder completion, DB/refresh rules, FK closure and script fixtures were
read against the representative sources. All local links/anchors and
`scripts/check_diff_whitespace.sh` passed; skills are 92/236/125/119 lines.
Consumer trace `/tmp/seed126-slice1-consumers.md` has 823 classified rows;
unchanged runtime APIs and Storybook's separate practice are outside its effect.
Fresh `refactor_makeme` returned `REFACTOR COMPLETE`, no edits/tests; proof reused.
Eight active implementation minutes reflect bounded consumer classification,
within the ten-minute hard limit. Freshness remained unchanged before delivery.

## Slice 2 acceptance

Feature, step-only and page-object-only guidance/selector walkthroughs pass;
terminal helpers keep their surface-specific practice. All 20 links/anchors
resolve, moved executable examples are exact, and whitespace passes.
E2E authoring/running are 196/88 lines with the planned cohesive seam.
The whole-guidance one-time reading sweep found canonical shared guidance and
local mechanics only; 202 consumer rows are classified in
`/tmp/seed126-slice2-consumers.md`, literal proof in `/tmp/seed126-slice2-proof.md`.
Fresh `refactor_browser` returned `REFACTOR COMPLETE`, no edits; proof reused.
Nine active implementation minutes include extraction and consumer reading.

## Execution complete

Product advice: no change. Product/proof retrospective complete; process selected
and deferred to wrap-up after Intent Coverage. Both slices accepted on the branch,
latest `54f8c5c906a150fcc84a86115ce3d7313154b5ec`; CI pending. Records-only delivery.
