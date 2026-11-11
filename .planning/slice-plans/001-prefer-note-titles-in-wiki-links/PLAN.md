# Prefer note titles in wiki links

**Identity:** SEED-122#prefer-note-titles-in-wiki-links
**Source:** [Prefer note titles in wiki links](../../seeds/SEED-122-prefer-note-titles-in-wiki-links.md#prefer-note-titles-in-wiki-links)
**Source review:** [Source commitments](source-review.md)

## Execution context

Established story-branch execution, Rina-chan, publisher
`dashboard-territory.local-doughnut`, owned workspace
`/Users/terryyin/git/doughnut/.worktrees/prefer-note-titles-in-wiki-links`, branch
`codex/prefer-note-titles-in-wiki-links`. Starting revision
`88fe9e0df8418a18a26ae15734d1b7652a4dd3ad`; accepted claim and first increment base
`f6d89e3470a6ed3064443c324763a074bd5bc884`, confirmed on origin/main and the
execution branch. No new claim is needed. Replanning authority remains the
planned-work default; scope changes require the developer.

Checkout setup and `./scripts/run.sh pnpm --version` passed on 2026-11-11 with
locked dependencies and pnpm 12.19.0. The shared pre-commit hook is check-only
`./scripts/run.sh pnpm lint:changed`; coordinator selective formatting is
`./scripts/run.sh pnpm format:changed` once after refactoring.

Observer: GitHub `nerds-odd-e/doughnut`, `ci.yml` verified as active `donut CI`,
target `codex/prefer-note-titles-in-wiki-links`, coordinator `rina-title-links`,
directory `/tmp/dough-ci-501/watch-OGMWx4`, PID 12514, stream session 31228,
yielded cell `g157:6`. Runtime resolves inside this exact checkout. The claim on
trunk is unobserved by this story-branch observer.

Proof workload decision: Territory.local, 16 logical CPUs, 48 GiB RAM, load
4.07/4.19/7.45 and 50% memory free on 2026-11-11. One unrelated Node process
uses one core. Run one ordinary full backend suite, owned by the implementer;
reconsider upon contention or failure. Product, tests and generated frontend
content are unchanged from the retained frontend observation at `090302e`;
reuse its full suite and both typechecks after checking actual consumer effects.

The first execution backend run completed with 3,111 tests, two failures and
two skipped. Both failures were new test expectations: duplicate case-insensitive
reference identity and comparison of raw input with canonical saved content.
Inspection also found an unindexed alias in the competing-property fixture.
G1–G3 retain these proof gaps and their correction ownership below. The original
failure XMLs were retained outside the checkout under
`/var/folders/65/16p4k5qj42qg7l46k2j0nhj40000gn/T/title-precedence-first-run-2coy6td7`.
The suite took 4m31s after migration under increased host pressure; a bounded
liveness check confirmed ongoing result writes rather than a stalled worker.

Before the corrected sequential run, Territory.local load was
13.61/15.33/11.81 with 48% memory free and unrelated Python/Spotlight work.
The implementer chose one full ordinary-concurrency rerun, retaining coverage
and timeouts, and owns session 47021 to terminal. Source freshness was unchanged
at `f6d89e3470a6ed3064443c324763a074bd5bc884` before both suite launches.

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
Status: done
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

## Story obligations

### G1. Make the competing alias property participate in proof
Reported: slice 1 — "Acceptance inspection found raw content did not populate the alias index in the missing-title-property fixture; the competing valid alias property was not yet established."
Story clause: "Select the note before checking its property"
Disposition: proved by slice 1: NoteControllerShowWikiLinkTitlePrecedenceTests.selectedTitleWithoutPropertyLeavesLinkUnresolvedDespiteAliasProperty now saves the competing alias/property through TextContentController; the real index participates, and the empty wikiLinks assertion passes in the final full backend run.

### G2. Observe case comparison without duplicate reference identity
Reported: slice 1 — "The new authored-form test expected six references but got five because color and COLOR share the existing case-insensitive reference identity."
Story clause: "Keep the existing title comparison and alias normalization rules"
Disposition: proved by slice 1: selectsTitleBeforeAliasesAcrossAuthoredLinkForms gives COLOR its own Case label, so six distinct references all resolve to the title; existing case-insensitive reference identity remains intact and the final full backend run passes.

### G3. Compare saved content across the live transition
Reported: slice 1 — "The live-reference test compared raw input against canonical saved content; save inserts type: Note frontmatter. Destination and inbound assertions passed before that mismatch."
Story clause: "preserving the authored destination and label"
Disposition: proved by slice 1: newTitleTakesOverLiveResolutionAndInboundReferencesWithoutResavingSource compares the canonical save result before title creation with the reopened source afterward; title destination, inbound source and unchanged saved content all pass in the final full backend run.

## Accepted slice proof

Coordinator acceptance on 2026-11-11 inspected both production diffs, all changed
controller tests, repository title/alias availability queries, frontend route,
label and insertion assertions, and the final XML results. The source review
maps all 61 returned commitments to the observed results or inspected unchanged
contracts. No source/plan mismatch or unowned remaining promise remains.

`./scripts/run.sh pnpm backend:test_only` passed with 626 suites, 3,111 tests,
zero failures/errors and two existing skipped tests. Migration took 11s and the
test build 1m17s. XML timestamps identify Territory.local on 2026-11-11. The new
seven-case controller file and both live alias-cardinality tests executed, as
did authored paths, ambiguity, property-key case and spelling all-match proof.

The retained frontend full suite and both typechecks above are reused on
identical frontend/test/generated content. Inspected click assertions route
backend IDs to noteShow/noteProperty; rendering preserves backend labels; insert,
repair, recall and overlap consumers transport backend-authored Portable paths.

[Execution consumer trace](consumer-trace.md) retains all located consumer
dispositions. It was generated through the installed consumer-trace script
against HEAD plus the actual working diff with supplied terms WikiLinkResolver,
authoredPortablePath, authoredWikiLinkTokenForInsert,
authoredWikiLinkTokenFromOriginalPath, forNotebookAndTitle and
readableNotebookMatches. All backend consumers ran; frontend consumers reuse
matching proof; unchanged generated contracts and incidental planning/guidance
filename matches are excluded for the stated boundary reasons.

One-time removal acceptance reading: searched the removed private helper names
through backend, frontend, CLI, packages, E2E, docs, agent guidance and planning
(excluding this plan and source review); zero matches. Shared note-ID
deduplication remains used by spelling accidental matches. This is a reading,
separate from surviving-behavior proof.

Acceptance source freshness is unchanged at
`f6d89e3470a6ed3064443c324763a074bd5bc884`; no named siblings require settlement.

Independent refactor `/root/refactor` returned `## REFACTOR COMPLETE`: no product
edits; preparation evidence extracted intact to keep every changed file under
250 lines. Whitespace and obligation checks passed. Its record-relocation trace
at `/tmp/note-title-refactor-consumer-trace.md` retains dispositions; accepted
product and frontend proof is unchanged, so no tests were repeated.

## Preparation evidence

[Retained preparation evidence](preparation-evidence.md) records the decisive
premises, disposable probe, consumer assessment and readiness reviews with their
original dates, revisions, commands and limitations. Accepted execution evidence
and outstanding obligations remain in this plan.
