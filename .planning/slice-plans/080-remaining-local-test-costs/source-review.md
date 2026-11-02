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
| 1 | Contributors wait less for trustworthy ordinary local test feedback by removing measured repeated setup, OR receive an evidenced no-gain result | slice 3 | observed: PLAN.md slice 3 Result: mounted merge kept on measured focused gain; slices 1-2 evidenced no-gain results |
| 1.1 | The feedback that gets faster must stay trustworthy | slice 3 | observed: PLAN.md slice 3 Result: all 23 focused and 2454 full tests green, both typechecks pass |
| 1.2 | Only measured repeated setup counts as the thing removed | slice 3 | observed: PLAN.md slice 3 Result: removed only the measured duplicate mount/mock setup file (6/6 focused pairs faster) |
| 1.3 | A no-gain result must be backed by evidence | slice 3 | observed: PLAN.md slices 1-2 Results and TEST-OPTIMIZATION-CANDIDATES.md Latest pass: no-gain results with measurements |
| 2 | Attribute the recorded first-batch E2E startup cost | slice 1 | observed: PLAN.md slice 1 Result: per-stage first vs warm attribution of the ~12.4s gap |
| 3 | Assess the compatible spec merges in the replacement plan | slice 3 | observed: PLAN.md slices 2-3 Results: both compatible merges assessed with alternating measurements |
| 3.1 | Assess the compatible AI-reply protocol spec merge | slice 2 | observed: PLAN.md slice 2 Result: merge assessed with alternating focused and full measurements, undone for no demonstrated gain |
| 3.2 | Assess the compatible mounted-component sibling-spec merges | slice 3 | observed: PLAN.md slice 3 Result: mounted streaming/unknown-tool merge measured and kept |
| 3.3 | Only compatible merges are in scope, assessed in the replacement plan | slice 2 | observed: PLAN.md slice 2 Result: only the planned compatible content+toolCalls merge was tried, with no production or runner change |
| 4 | Preserve the listed protections | slice 3 | observed: slices 1-3: no runner, config, startup, selection or profile change; see 4.1-4.7 |
| 4.1 | Existing cases | slice 3 | observed: frontend/tests/components/conversation/AiResponse.reply.spec.ts: 7 cases verbatim; aiReplyState specs unchanged; 2454 tests before and after |
| 4.2 | Owned-stack readiness | slice 1 | observed: slice 1 runs: real health predicate "SUT healthy after N poll(s)" each run, no startup change |
| 4.3 | Owned-stack cleanup | slice 1 | observed: slice 1 runs: no leftover java/Cypress/Gradle/Mountebank process or port after each run |
| 4.4 | Local HMR | slice 3 | observed: git diff 6c045ccfd5: no Vite/frontend config change |
| 4.5 | Browser-file isolation | slice 3 | observed: final full run: 396 files under default per-file browser isolation; no isolation setting changed |
| 4.6 | Cucumber proof | slice 1 | observed: e2e_test/proof/20261102T0450*/0451*/0452* recordings, one per run |
| 4.7 | Normal test/CI selection | slice 3 | observed: git diff 6c045ccfd5: no test selection, tag, worker or CI config change |
| 5 | Cold and warm observations keep their actual cache/build conditions | slice 1 | observed: PLAN.md slice 1 Result table: State before column (build cache warm, checkout-local caches absent) |
| 6 | A missing stage baseline is not accepted as an inherent cost | slice 1 | observed: PLAN.md slice 1 Result: unattributed Cypress window and cache-miss compile listed as missing evidence, candidate pending |
| 7 | A spec merge keeps every observable case | slice 3 | observed: AiResponse.reply.spec.ts: streaming 3 + unknown tool 4 cases with their own setup; exclusionProposal spec unchanged |
| 8 | A spec merge is kept only if the ordinary command measurably improves | slice 3 | observed: PLAN.md slice 2 undone for no gain; slice 3 kept on 6/6 faster focused pairs |
| 9 | Families not picked stay recorded as candidates | slice 3 | observed: .planning/TEST-OPTIMIZATION-CANDIDATES.md Pending candidates: both slugs returned with unpicked families and missing evidence |
