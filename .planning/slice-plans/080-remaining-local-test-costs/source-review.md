# Source review: SEED-098#remaining-test-costs

**Launch:** 2026-11-02, fresh general-purpose Claude Code subagent with no
conversation inheritance, launched by the execution coordinator (agent
Rina-chan) at candidate `6c045ccfd5d27af88590b132ce379a9ac8abedab`.

**Input:** only the story's heading, Goal, Scope and Key examples text from
`.planning/seeds/SEED-098-remaining-local-test-costs.md#remaining-test-costs`
(no plan, return, product or proof), with the instruction to read no project
files, change nothing, and return numbered commitments, qualifications,
examples and exclusions.

**Reviewer notes (verbatim summary):** item 1 is one alternative outcome
(measured reduction, or an evidenced no-gain result), not two deliverables;
"readiness and cleanup" split into 4.2 and 4.3; items 5–9 come from the key
examples; item 9 keeps unpicked families on record, it does not drop them.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| 1 | Contributors wait less for trustworthy ordinary local test feedback by removing measured repeated setup, OR receive an evidenced no-gain result | slice 3 | |
| 1.1 | The feedback that gets faster must stay trustworthy | slice 3 | |
| 1.2 | Only measured repeated setup counts as the thing removed | slice 3 | |
| 1.3 | A no-gain result must be backed by evidence | slice 3 | |
| 2 | Attribute the recorded first-batch E2E startup cost | slice 1 | observed: PLAN.md slice 1 Result: per-stage first vs warm attribution of the ~12.4s gap |
| 3 | Assess the compatible spec merges in the replacement plan | slice 3 | |
| 3.1 | Assess the compatible AI-reply protocol spec merge | slice 2 | observed: PLAN.md slice 2 Result: merge assessed with alternating focused and full measurements, undone for no demonstrated gain |
| 3.2 | Assess the compatible mounted-component sibling-spec merges | slice 3 | |
| 3.3 | Only compatible merges are in scope, assessed in the replacement plan | slice 2 | observed: PLAN.md slice 2 Result: only the planned compatible content+toolCalls merge was tried, with no production or runner change |
| 4 | Preserve the listed protections | slice 3 | |
| 4.1 | Existing cases | slice 3 | |
| 4.2 | Owned-stack readiness | slice 1 | observed: slice 1 runs: real health predicate "SUT healthy after N poll(s)" each run, no startup change |
| 4.3 | Owned-stack cleanup | slice 1 | observed: slice 1 runs: no leftover java/Cypress/Gradle/Mountebank process or port after each run |
| 4.4 | Local HMR | slice 3 | |
| 4.5 | Browser-file isolation | slice 3 | |
| 4.6 | Cucumber proof | slice 1 | observed: e2e_test/proof/20261102T0450*/0451*/0452* recordings, one per run |
| 4.7 | Normal test/CI selection | slice 3 | |
| 5 | Cold and warm observations keep their actual cache/build conditions | slice 1 | observed: PLAN.md slice 1 Result table: State before column (build cache warm, checkout-local caches absent) |
| 6 | A missing stage baseline is not accepted as an inherent cost | slice 1 | observed: PLAN.md slice 1 Result: unattributed Cypress window and cache-miss compile listed as missing evidence, candidate pending |
| 7 | A spec merge keeps every observable case | slice 3 | |
| 8 | A spec merge is kept only if the ordinary command measurably improves | slice 3 | |
| 9 | Families not picked stay recorded as candidates | slice 3 | |
