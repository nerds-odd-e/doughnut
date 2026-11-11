# Prefer note titles in wiki links

**Identity:** SEED-122#prefer-note-titles-in-wiki-links
**Source:** [Prefer note titles in wiki links](../../seeds/SEED-122-prefer-note-titles-in-wiki-links.md#prefer-note-titles-in-wiki-links)

## Goal and scope

A reader follows a shorthand wiki link to its unique matching title despite
colliding aliases. Among readable, available notes in the link's notebook scope,
use matching titles when present and aliases otherwise. Classify that set, then
validate the selected note's property selector. Apply the rule to body and
supported frontmatter links, qualified notebook names, and display labels.
Resolve against live state without changing stored link text.

Keep existing comparison, normalization, exact path, ambiguity, access, trash,
and property-key semantics. No search or spelling-recall ranking change, new UI,
API/schema/route change, or new path syntax is part of this outcome.

## Current decisions and existing solutions

- Change the existing selection behind `WikiLinkResolver`, shared by token
  classification and `readableNotebookMatchUniquelyIdentifies`. PFE found
  `WikiLinkNoteCandidates` gathering title/alias candidates and
  `WikiLinkCandidateClassifier` applying readability and cardinality; retain one
  coherent rule through both entry points. Choose precedence after readability,
  before cardinality/property validation. Reuse the repository queries' current
  availability and matching rules instead of introducing another title comparator.
- `PortablePathAuthoring` uses that same selection. Its unique-title output becomes
  `color` rather than `folder/color` in the presence of an alias collision.
  Retain path fallback for genuinely ambiguous titles.
- `NoteReferenceService`, inbound-reference discovery, health, assimilation and
  rewrite capture already resolve current authored references through this owner.
  Keep the source-owned index and avoid persisting a preferred destination.
- `AccidentalWikiLinkMatches` serves spelling-recall all-match discovery, not
  destination selection. Preserve its title-and-alias candidates and the shared
  note-ID deduplication it uses.
- [ADR 0004 — OKF-compatible notebook Markdown profile](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md#links-and-attachments)
  specifies title precedence, current viewer/state resolution, frontmatter support
  and property validity. [ADR 0005 — Web routes](../../../docs/adrs/0005-web-routes-accepted.md#notebook-links-as-web-destinations)
  maps the resolved IDs to existing note/property routes. Both are Accepted in the
  index and their records; no conflict or exception was found.
- `.planning/NORTH-STAR.md` was reviewed. Existing owners and Accepted decisions
  suffice; this work needs no new short-term direction.

## Outside-in proof ownership

Every row belongs to the single Behavior slice. Use real controller calls and
`makeMe` fixtures with the test database, and existing domain-stable resolver
coverage where appropriate. New tests must discriminate title precedence from
the existing combined candidate set, not merely prove title lookup alone.

| Promise/example | Observable proof |
| --- | --- |
| Unique title with one or several colliding aliases; body, YAML scalar/list, display label, notebook qualification and existing case matching | `NoteController.showNote` returns the title note ID for each authored token; retained content/label observations prove authored text preservation. |
| No readable, available title; one alias, multiple aliases, or no alias | Controller results resolve the sole alias, classify multiple alias notes as ambiguous, and leave a missing token unresolved. Include unreadable and trashed title fixtures. |
| Multiple title matches even with alias/property differences | Controller result remains ambiguous; existing exact-folder/root-path tests prove the usable disambiguating path. |
| Unique selected note has the exact property; selected title lacks it while an alias has it | Controller result selects the title/property when valid; a missing-property link is omitted from resolved `wikiLinks`, so it remains unresolved to the existing renderer. |
| Live destination after adding a matching title, without resaving the source | Save a source through `TextContentController`, observe alias resolution, add the title, then observe its ID and the source in the title's inbound references. |
| Shared shortest-path selection and current alias-index updates | `NoteController.authoredPortablePath` returns title shorthand despite alias collisions. Preserve live alias-only resolved → ambiguous → resolved behavior through content updates. |
| Existing UI consumption of backend selection and authored paths | Existing frontend rendering/navigation, insert/repair, recall offer and overlap tests consume the unchanged response shapes and preserve labels/property routes. Backend tests own actual selection. |

## Ordered slices

### 1. Resolve shorthand through readable titles before aliases
Type: Behavior
Status: planned
Proof: The controller observations above, full backend unit suite, and applicable
frontend consumer proof below. Require a green final suite; the prototype's three
expected legacy failures are evidence for planning, not an accepted delivery.

Behavior: Given a shorthand token and current readable, available notes in its
notebook scope, opening a note selects matching titles when any exist and aliases
otherwise; uniqueness determines resolution and any selected property must exist.
Live inbound references and authored shortest paths use this same selection.

Implementation brief:

- Change the existing candidate/classifier seam so unreadable titles cannot
  suppress a readable alias and a missing property cannot cause alias fallback.
  Both resolution and authoring must consume the same preferred candidate set.
- Replace the title/alias collision expectation in
  `NoteControllerShowWikiLinkAmbiguityTests` with positive title-destination proof,
  locating added examples in a cohesive controller test file below 250 lines.
  Update `NoteControllerAuthoredPortablePathTests` to expect the title shorthand.
- In `TextContentControllerUpdateNoteContentAliasCardinalityTests`, use an
  alias-only starting match for its live alias ambiguity transitions. Retain that
  behavior rather than weakening the test to an assertion about the displaced rule.
  Add the necessary title/property/access/live-reference examples from the table.
- Delete helpers made unused by the change while retaining shared deduplication.
  Perform any removal-name sweep once at acceptance as a reading, separately from
  the surviving-behavior proof. Add no permanent absence test.

Sizing: about five minutes of edits and local cleanup plus verification. The
backend observation took about two minutes, so scrutinize a total above five
minutes. Keep one cohesive proof loop; required suite waiting is the stated
focused-verification exception if elapsed time crosses ten minutes. Stop and
replan for substantive implementation overrun, unexpected consumers or structural
work, rather than splitting by layer or by link location.

Expected production edits are the 80-line candidate owner and 99-line classifier.
Existing implicated controller files are 168, 129 and 38 lines; do not grow the
229-line general wiki-link test file into an oversized catch-all. No expected
edit currently requires reorganization of a file above the 250-line limit.

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
