# Source-only review

Reviewer: fresh `/root/source_review`, fork_turns=none, launched 2026-11-10.
Input: only the seed’s Goal, Scope, Key examples and explicit 2026-11-10 Decision,
transcribed in the launch message; no plan, product, proof or implementation.
Instruction: read no project files, change nothing, number individual commitments,
qualifications, examples and exclusions. The actual launch and full return remain
in this native session; the returned source text is retained below verbatim.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| C01 | Contributors comparing CI builds can distinguish changes in test-case counts from changes in test-file counts in Intent. | slice 3 | |
| C02 | Attribute outcomes to the unit the runner reports. | slice 3 | |
| C03 | Attribute recorded execution times to the unit the runner reports. | slice 3 | |
| C04 | Adopt the new CI proof format in Doughnut’s existing publisher. | slice 3 | |
| C05 | Publish individual JUnit results as test cases. | slice 3 | |
| C06 | Publish individual Vitest results as test cases. | slice 3 | |
| C07 | Publish individual Cucumber results as test cases. | slice 3 | |
| C08 | Publish individual Node results as test cases. | slice 2 | |
| C09 | Publish Bash runner whole-file results as test files. | slice 2 | |
| C10 | Preserve the Scripts and Infrastructure proof location. | slice 2 | |
| C11 | Preserve that location’s file breakdown. | slice 2 | |
| C12 | Preserve its outcomes. | slice 2 | |
| C13 | Preserve its recorded execution times. | slice 2 | |
| C14 | Preserve existing proved locations. | slice 3 | |
| C15 | Preserve existing producer commands. | slice 3 | |
| C16 | Retain source-file attribution for individual case results. | slice 3 | |
| C17 | Retain recorded execution times for individual case results. | slice 3 | |
| C18 | Retain untimed individual case results. | slice 3 | |
| C19 | Publish a file that fails to load before producing cases as one failed test-file result. | slice 3 | |
| C20 | Keep that failed file visible at its path. | slice 3 | |
| C21 | Retain available timing for that failed file. | slice 3 | |
| C22 | Have Vitest producers publish separate case and file streams. | slice 3 | |
| C23 | Have Node producers publish separate case and file streams. | slice 2 | |
| C24 | Publish a present, zero-result fragment when an existing report contains no results in a declared unit. | slice 3 | |
| C25 | Use Open Dough guidance at least as recent as the referenced installed release. | slice 1 | observed: Installed 0.3.94 CI-proof copy recipe and byte comparison of all seven project modules against .agents/skills/dough-test-optimization/scripts; matching evidence/options included. |
| C26 | Copy all seven `ci-proof-*.mjs` collector modules into `scripts/ci/` through the CI proof copy recipe. | slice 1 | observed: Installed 0.3.94 CI-proof copy recipe and byte comparison of all seven project modules against .agents/skills/dough-test-optimization/scripts; matching evidence/options included. |
| C27 | Add a unit input to `.github/ci_proof_fragments`. | slice 2 | |
| C28 | Pass that input as `--unit case\|file`, preserving the two supported alternatives. | slice 2 | |
| C29 | Declare every `.github/ci_proof_summary` expectation as `<entry>=<parts>:<unit>`. | slice 3 | |
| C30 | Number each unit’s parts separately. | slice 3 | |
| C31 | Leave a part missing in its declared unit when it writes no fragment. | slice 3 | |
| C32 | Preserve proof from other parts when one part is missing. | slice 3 | |
| C33 | Preserve proof from other units when one part is missing. | slice 3 | |
| C34 | Make file streams describe the aggregate outcomes reported by runners. | slice 3 | |
| Q01 | Open Dough 0.3.93 publishes the required units. | slice 1 | observed: Installed 0.3.94 CI-proof copy recipe and byte comparison of all seven project modules against .agents/skills/dough-test-optimization/scripts; matching evidence/options included. |
| Q02 | The project already has Open Dough 0.3.94 installed. | slice 1 | observed: Installed 0.3.94 CI-proof copy recipe and byte comparison of all seven project modules against .agents/skills/dough-test-optimization/scripts; matching evidence/options included. |
| Q03 | `ci-proof-evidence.mjs` is a new collector module. | slice 1 | observed: Installed 0.3.94 CI-proof copy recipe and byte comparison of all seven project modules against .agents/skills/dough-test-optimization/scripts; matching evidence/options included. |
| Q04 | `ci-proof-options.mjs` is a new collector module. | slice 1 | observed: Installed 0.3.94 CI-proof copy recipe and byte comparison of all seven project modules against .agents/skills/dough-test-optimization/scripts; matching evidence/options included. |
| Q05 | The developer delegated the load-failure unit decision on 2026-11-10. | slice 3 | |
| Q06 | The resulting decision assigns load failures to test files so case counts represent actual cases. | slice 3 | |
| Q07 | Until both compared builds declare matching units, the released Intent reader treats unit-less results as legacy test results. | slice 3 | |
| Q08 | Under that comparison condition, Intent shows no count difference. | slice 3 | |
| Q09 | Under that comparison condition, Intent shows no time difference. | slice 3 | |
| Q10 | These comparison behaviors come from the released reader contract. | slice 3 | |
| J01 | When one attempt contains individual Node case outcomes and Bash whole-file outcomes, Scripts and Infrastructure receives separate test-case and test-file totals. | slice 2 | |
| J02 | Each total retains the outcomes belonging to its unit. | slice 2 | |
| J03 | Each total retains its file breakdown. | slice 2 | |
| J04 | Each total retains its recorded time. | slice 2 | |
| J05 | A Bash file containing several internal assertions contributes one file result. | slice 2 | |
| J06 | The first expectation, in location order, is `e2e_test/features=6:case`. | slice 3 | |
| J07 | The second expectation is `frontend/tests=2:case`. | slice 3 | |
| J08 | The third expectation is `frontend/tests=2:file`. | slice 3 | |
| J09 | The fourth expectation is `backend/src/test/java=1:case`. | slice 3 | |
| J10 | The fifth expectation is `scripts=12:case`. | slice 2 | |
| J11 | The sixth expectation is `scripts=13:file`. | slice 2 | |
| J12 | The twelve `lint:all` Node reports correspond to script case parts 1–12. | slice 2 | |
| J13 | Those same reports correspond to script file parts 1–12. | slice 2 | |
| J14 | `run_all_script_tests.sh` corresponds to script file part 13. | slice 2 | |
| J15 | Each frontend shard uses the same part number in its case and file streams. | slice 3 | |
| J16 | A producer that writes no report leaves its declared part missing. | slice 3 | |
| J17 | A report with two actual case outcomes and one pre-case load failure produces two case results. | slice 3 | |
| J18 | That same report produces one failed file result. | slice 3 | |
| J19 | A report containing only case outcomes produces a present, zero-result file fragment. | slice 3 | |
| J20 | A missing report leaves both declared case and file parts missing. | slice 3 | |
| J21 | Comparing against an older unit-less build invokes the supplied legacy-reader behavior until both builds declare matching units. | slice 3 | |
| X01 | Adding proof for further suites is outside this story. | slice 3 | |
| X02 | Changing Intent itself is outside this story. | slice 3 | |
| X03 | Do not extract test cases from the Bash runner’s whole-file report. | slice 2 | |
| X04 | Case reports do not create additional successful-file results. | slice 3 | |
