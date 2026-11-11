# Preparation evidence for note-title precedence

Retained preparation observations and review for [the execution plan](PLAN.md).
Dates, revisions, inputs, results and limitations below are preserved from the
preparation record. They do not substitute for accepted execution proof.

## Decisive premises and observations

Observations were local and unpaid in this preparation's owned worktree
`/Users/terryyin/git/doughnut/.worktrees/prefer-note-titles-in-wiki-links`, at
`090302e0995ca47adfab14f4b733db75b25a832c`, on 2026-11-11. Backend runs used the
ordinary `test` profile and automatically isolated worktree MySQL database.

| Premise and consuming operation | Observed form, result and limitations |
| --- | --- |
| Current rendering and authoring classify a title/alias collision together | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed. Inspected XML results confirm the collision-ambiguity and folder-path expectations both executed and passed. Backend test build: 2m 8s, excluding migration setup. |
| The shared seam can implement precedence after readability, before property validation, across the promised journey | A disposable edit passed a readability predicate into candidate selection, preferred the readable title query results and used aliases only when empty. `./scripts/run.sh pnpm backend:test_only` ran 3,110 tests: seven new controller probe cases passed; three existing expectations failed and two tests were skipped. Failures were collision rendering (`AMBIGUOUS` → `RESOLVED`), authored path (`WikiDup Pantry/color` → `color`), and alias-edit title collision (`AMBIGUOUS` → `RESOLVED`). Test build: 1m 52s. An earlier probe compile error used `Note::getId` for `NoteTopology` references; it was corrected before this result. |
| Eligibility and property ordering work at the consuming controller boundary | The seven probe methods observed body/scalar/list/qualified/label selection, unreadable-title fallback, trashed-title fallback, valid title property, missing title property despite a valid alias property, duplicate-title property ambiguity, and alias-to-title live destination plus inbound-reference transition. The missing-property observation was an empty `wikiLinks` result, not a health-report observation. |
| Current callers outside the selector can consume the replacement result | Full backend probe run exposed exactly the three legacy expectations named above. Existing other backend cases passed, including exact paths, alias-only matches, property handling, rewriting and spelling recall. This is not proof of every possible collision in every caller; retain mapped new examples and whole-suite proof at execution. Frontend observation is recorded below. |
| The existing frontend transports authored paths and renders/routes the selected note/property without another title/alias policy | After reverting the backend probe, `./scripts/run.sh pnpm frontend:test` passed 413 files / 2,542 tests in 95.31s. `./scripts/run.sh pnpm -C frontend exec vue-tsc --noEmit` and `./scripts/run.sh pnpm -C frontend exec tsc --noEmit` both passed. Inspected insert/recall/overlap tests use HTTP stand-ins; they prove frontend response consumption, while the backend probe proves actual selection. Product, frontend tests and generated API content remained at the cited revision. |

The probe was fully reverted before writing this seed/plan: both production files
restored, temporary test deleted, and `git status --short` was empty. Its source
is not retained or delivered. The rows above retain inputs/actions/results and
their limitations, not implementation completion.

## Consumer trace and verification

During the disposable edit, ran:

```sh
./scripts/run.sh node .agents/skills/dough-execute-plan/scripts/consumer-trace.mjs --checkout /Users/terryyin/git/doughnut/.worktrees/prefer-note-titles-in-wiki-links --base HEAD --term 'WikiLinkResolver' --term 'authoredPortablePath' --term 'authoredWikiLinkTokenForInsert' --term 'authoredWikiLinkTokenFromOriginalPath'
```

The trace searched 4,226 checkout files. The following dispositions cover every
located consumer; rerun the trace against the actual implementation diff and
extend terms if its effect reaches further journeys or test support.

| Located consumers | Disposition and boundary |
| --- | --- |
| Backend `WikiLinkMarkdownRewrite`, `AuthoredPortablePath`, `NoteController`, `AuthoredNoteReferenceInboundFacade`, `AuthoredNoteReferenceRowRepository`, `DeadWikiLinkHealthRule`, `MemoryTrackerService`, `NoteConstructionService`, `NoteReferenceHandling`, `NoteReferenceService`, `NoteService`, `PortablePathAuthoring`, `SpellingRecallGrading`, `UnassimilatedPropertyService`, `WikiLinkReferenceCapture`, `WikiLinkRelocationRewrite`, `WikiLinkResolver`, `WikiLinkRewriteService`, `WikiLinkRewriteSupport`, `AccidentalWikiLinkMatches` | run: full backend suite and the new mapped controller cases. Preserve the accidental-match contract; it uses shared deduplication but does not use shorthand precedence. |
| Backend `NoteControllerAuthoredPortablePathTests`, `WikiLinkResolverFrontmatterAndBodyResolutionTest`, `WikiLinkResolverReferenceResolutionTest` | run: included in full backend proof. The experiment also found the rendering and alias-edit tests by executing the whole suite. |
| Frontend `MatchedNoteWikiLinkOrRelationshipOffer.vue`, `SearchForm.vue`, `appendOverlapWikiLinkToNoteContent.ts`, `wikiLinkAuthoring.ts` | run: frontend suite, with backend controller proof owning actual destination/path selection. These consumers wrap and insert the returned path. |
| Frontend `AnsweredSpellingQuestionAccidentalMatch.spec.ts`, `AnsweredSpellingQuestionAddAsOverlapped.spec.ts`, `MatchedNoteWikiLinkOrRelationshipOffer.spec.ts`, `appendOverlapWikiLinkToNoteContent.spec.ts`, `InsertWikiLink.spec.ts`, `SearchDialog.spec.ts` | run: frontend suite. Inspected setup/assertions use the backend-returned string, not a local reconstruction of precedence. |
| `open_api_docs.yaml`, generated client `index.ts`, `sdk.gen.ts`, `types.gen.ts`, and `api-summary.md` | excluded: the effect changes response values, not the API schema or generated signatures these artifacts describe. No API regeneration is needed. |

The script found no test-support or feature matches for these expanded terms.
Further reading followed `WikiLinkToken.vue`, `wikiLinkResolvedLocation.ts`,
`NoteTextContent.wikiLinks.spec.ts`, and `wikiLinkMarkup.spec.ts`: the frontend
renders and routes the returned note ID/property selector. Include those existing
tests in the frontend run. CLI and MCP have no separate shorthand resolver.

Execution verification:

```sh
CURSOR_DEV=true nix develop -c pnpm backend:test_only
CURSOR_DEV=true nix develop -c pnpm frontend:test
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit
```

Full backend verification is required by the backend skills; frontend consumers
and their typechecks follow the frontend proof rule. Reuse matching retained
frontend evidence when its product/test/generated content is unchanged. No manual
or E2E run is required for this backend selection change with controller proof.
At delivery follow AGENTS.md and Execute Plan: Jidoka, fresh post-change refactor
agent, API generation only if contracts changed, coordinator format once, plan
update, agent commit with check-only hook, push and owned asynchronous CI repair.

Host choice for the preparation observation: `Territory.local` had load about
6.1 and active unrelated VM/Nix work. Used ordinary suite concurrency and
serialized backend and frontend runs. Reconsider on new contention or failures;
do not weaken proof or interfere with unrelated processes.

## Plan review

One common preferred-candidate rule serves all examples. No preparatory Structure
or delivery of a knowingly failing intermediate state is needed. Separate plan
refinement was not needed: one bounded outcome, one proof loop, and no unresolved
slice-specific concern remains after reviewing the baseline, discarded backend
probe, frontend suite and both typechecks. This preparation records planned work;
it grants no execution authority.

### Follow-up refinement on 2026-11-11

Reviewed the existing seed and plan at
`ffca6fbe29ace57dbceb6557ecb70118d268f2b8` under Challenge, Clarify,
Investigate, UX/UI and Architecture. The strongest consequential risk is the
ordering of selection. A private title suppressing a readable alias, or a
property-bearing alias displacing the selected title, would defeat the reader's
direct title reference. The seed already specifies readable candidates first,
title precedence second, and property validation after note selection. The
retained controller probe above observed both boundaries. The existing goal,
scope and examples remain sufficient and require no new product decision.

An alias link can change destination when a matching title appears. The seed's
reopen example and ADR 0004 explicitly establish that behavior. Reading
`NoteRealmLoader.vue`, `noteStore.ts` and `noteRequests.ts` confirmed that reopening
loads the current realm. `WikiLinkToken.vue` and `wikiLinkResolvedLocation.ts`
consume the backend-selected note/property IDs, and the unresolved-link modal
already offers longer-path repair. These interactions share the existing
resolver and authoring responsibility under ADRs 0004 and 0005. The ADR index and
both relevant records agree on Accepted status. This review found no conflict
or additional architectural choice.

`git diff --name-status 090302e0995ca47adfab14f4b733db75b25a832c HEAD` showed
identical backend, frontend, generated-client and OpenAPI content. The retained
discarded-probe and frontend observations therefore apply to the current product
and test content. They remain preparation evidence with the original provenance
and limitations above. The intervening Gradle wrapper update from 9.9.0 to 9.9.1
prompted a fresh backend baseline through the ordinary command.

For that baseline, `Territory.local` had 16 logical CPUs, load averages
27.74/24.08/17.70, 48 GiB physical memory and concurrent system installation,
Node, Python and Nix work.
Chose one serialized backend run at ordinary concurrency, using the retained
frontend evidence. Unrelated processes remained with their owners. Reconsider
this workload choice on a failed run or changed contention.

`CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed at the reviewed
revision with Gradle 9.9.1 and the ordinary test profile in the automatically
isolated worktree database. Migration build took 3m 7s and test build took
2m 58s. XML results in `backend/build/test-results/test/` report 625 suites,
3,103 tests, two skipped, zero failures and zero errors. Inspected XML also
confirms that the three current collision/cardinality expectations named in the
implementation brief executed and passed. This verifies the current baseline
and command route. The retained disposable probe owns the proposed precedence
observations. The review found no remaining slice-specific concern and requires
no additional plan-refinement pass.
