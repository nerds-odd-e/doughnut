# Tracker-owned creation fixtures

Structure 10 retires `McqBuilder.forNote`, `RecallPromptBuilder.withMcqForNote`
and the exclusive Note arguments in tracker/prompt test helpers. All 63 selected
test sources now construct their question through the tracker/prompt fixture.
Production mappings, writers, FK, schema, scalar-write assertions, G3 counters
and committed cleanup remain unchanged for their later owners.

## Preserved fixture and proof semantics

The prompt builder's missing-scalar fallback and `entityPersister.save(mcq)`
remain byte-identical. The save merges a raw question with an existing ID after
assigning its input from the owning tracker. Both opposite-ownership cases in
`AuthorizationServiceTest` retain explicit local original-note scalars and their
current-tracker access/refusal assertions. Those temporary physical fixtures
belong to Behavior 12's retirement.

The continuation helper's Note input and genuine tracker Note inputs remain.
Prompt creation still precedes real reification in moved export, conversation
and history cases. Raw question ordering/timestamps, same-tracker sharing,
learner privacy and committed isolation are preserved. Seven Note fields used
only during setup became local variables with allocation order intact; the
standalone contest fixture retains its actual Note input. No new fixture
framework, alias for a retired API or behavioral test was introduced.

A one-time balanced-expression reading against accepted base
`337b8f323ba9a17148a99a8684bdbdd1a6022904` found 734 assertion/verify/andExpect
expressions and 506 payload/owner/timing expressions token-identical (ignoring
whitespace/comments). Scenario annotations and assertion lines are unchanged.
Reports: `/tmp/donut-slice10-preservation-summary.json` and
`/tmp/donut-slice10-preservation-reading.json`. Exact paths/sizes/hashes:
`/tmp/donut-slice10-owned-sources.json`; all 63 homes are under 250 lines,
maximum 247. Root inspected the bridge, helper and authorization diffs.

## Full backend proof

`CURSOR_DEV=true nix develop -c pnpm backend:test_only > /tmp/donut-slice10-creation-fixtures-backend.log 2>&1`

Native session 46310 reached terminal 0: 640 suites, 3,195 tests, zero
failures/errors, two skips, unchanged counts. Log birth:
`2026-11-13T08:23:45.969970Z`; all XML mtimes are
`08:29:54.229731Z`–`08:29:54.268090Z`, with zero stale reports. Gradle reported
5m18s; the observed command/proof-collection window was 390.243s. All 63 source
hashes stayed unchanged through proof and independent review. Root separately
counted XML. Machine evidence: `/tmp/donut-slice10-backend-proof.json`; archived
reports: `/tmp/donut-slice10-backend-xml.zip`; complete commands/cases/paths:
`/tmp/donut-slice10-handoff.md`.

Semantic edit/cleanup window: `08:23:02.602034Z`–`08:23:42.368106Z`, about
40 seconds. The full suite is the separate indivisible proof exception. Before
its one serial run, the 16-CPU/48-GiB host had load 16.67/20.37/22.82 and 43%
free memory; no competing owned heavy work. Unowned Playwright work was left
untouched. All owned commands reached terminal results.

## Consumer reading and independent refactor

The installed trace used base `337b8f323ba9a17148a99a8684bdbdd1a6022904`, both
builder paths, both controller-helper base paths, their class names and the
retired/helper method names. `/tmp/donut-slice10-consumer-trace.md` and
`/tmp/donut-slice10-consumer-dispositions.json` contain 110 individually
dispositioned readers: 24 covered by full backend proof, 86 justified exclusions,
zero pending. Generic suffix matches are classified by their actual domain.

Separate one-time removal reading:
`git grep -n -E 'withMcqForNote|McqBuilder\.forNote|public McqBuilder forNote' -- . ':!.planning'`
returned zero, including tracked docs and agent guidance. Backend test
`.forNote(` reading returned zero. Live planning instructions remain owned by
execution cleanup. This reading is separate from surviving proof; no maintained
absence checker/assertion was added.

Selector: 63 resolved sources, 50 MakeMe enabled, 13 disabled, no page objects;
`/tmp/donut-slice10-test-entry-selection.json`. Fresh independent refactor
returned `none — already clean` and `REFACTOR COMPLETE`; it confirmed bridge,
ownership, order, no orphan fixtures, all sizes and zero consumer gaps. No
edits or redundant tests. Project whitespace check passed. Source freshness
settled at unchanged authorized main
`4059ac2353c590f33c971f5d531cf3853dacd4a8`. No named sibling was supplied.
No generator input changed; compatibility retirement remains Behavior 12.

The first guarded formatting/commit attempt stopped at the hook's Spotless
zero-lint-error result; no commit or push occurred. Its one cached clean output
matched source. Recomputing `spotlessJavaCheck --rerun-tasks --info` then exposed
real line-wrapping differences in 17 owned test sources, so that preparation was
invalidated. The retry applies the selective formatter again for this repair;
the hook stays enabled. Diagnostic: `/tmp/donut-slice10-spotless-renewed.log`.
This formatting repair changes no fixture or assertion meaning.
