# Source-only review

Launch: fresh `/root/deliberate_proof_source_review`, `fork_turns=none`; supplied only Terry's original delegated migration source and explicit cleanup preference. Reviewer was instructed to read no project files, change nothing and derive promises without slicing. The actual launch and complete numbered return are retained in the chat. No product, plan, implementation return or proof was supplied.

The reviewer returned P01–P33 as product commitments, Q01–Q08 as qualifications/journeys, X01–X05 as exclusions. The table preserves each returned meaning and allocates it before delegation. Trigger → expected result → actual observation is recorded in the last column at acceptance.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| P01 | Normal passing runs leave proof byte-for-byte unchanged. | slice 1 | |
| P02 | Normal failing runs leave proof byte-for-byte unchanged. | slice 1 | |
| P03 | Normal runs create no proof files. | slice 1 | |
| P04 | Normal runs create no proof directories. | slice 1 | |
| P05 | Normal runs create no proof locks. | slice 1 | |
| P06 | An explicit, project-owned option generates proof. | slice 1 | |
| P07 | That option refreshes proof. | slice 2 | |
| P08 | Explicit recording uses the same tests. | slice 1 | |
| P09 | Explicit recording uses the same selection. | slice 1 | |
| P10 | Explicit recording preserves normal test output. | slice 1 | |
| P11 | Explicit recording preserves normal exit meaning. | slice 1 | |
| P12 | Every existing Cucumber producer is gated by the deliberate recording choice. | slice 1 | |
| P13 | The Cypress Cucumber Messages writer is gated. | slice 1 | |
| P14 | Any existing terminal Cucumber path is gated. | slice 1 | |
| P15 | Explicit focused refresh updates only actually executed scenarios. | slice 2 | |
| P16 | When an executed scenario’s final result is unsuccessful, refresh clears that scenario’s earlier success. | slice 2 | |
| P17 | Refresh preserves unexecuted identities. | slice 2 | |
| P18 | Retained snapshots are compact. | slice 2 | |
| P19 | Retained snapshots are stable. | slice 2 | |
| P20 | Legacy latest-first success/failure semantics are preserved. | slice 2 | |
| P21 | Retained proof and CI verdicts remain independent observations. | slice 1 | |
| P22 | Every Cucumber Intent entry in .planning/open-dough.json has proofGeneration. | slice 1 | |
| P23 | Each such entry supplies exactly one of the stated alternatives: a nonempty command string or a project-owned skill invocation string. | slice 1 | |
| P24 | For the command alternative, the command runs from the repository root. | slice 1 | |
| P25 | For the command alternative, the command includes the explicit recording option. | slice 1 | |
| P26 | For the command alternative, the command includes any required wrapper. | slice 1 | |
| P27 | For the command alternative, the command includes any required selection. | slice 1 | |
| P28 | Old proof remains readable. | slice 2 | |
| P29 | Ordinary tests do not opt into recording. | slice 1 | |
| P30 | Test optimization does not opt into recording. | slice 1 | |
| P31 | Recording does not commit automatically. | slice 1 | |
| P32 | Project-owned commands are updated for the migration. | slice 1 | |
| P33 | Project-owned documentation is updated for the migration. | slice 1 | |
| Q01 | Ordinary-run scope covers passing and failing runs; no proof artifacts does not prohibit ordinary test artifacts. | slice 1 | |
| Q02 | Explicit option is the deliberate trigger for generation with unchanged tests, selection, output and exits. | slice 1 | |
| Q03 | Named Cypress Messages and terminal paths are examples within every existing producer. | slice 1 | |
| Q04 | Focused refresh updates executed identities, clears executed final failures and preserves unexecuted identities. | slice 2 | |
| Q05 | Unsuccessful cleanup concerns the final executed result. | slice 2 | |
| Q06 | Command and project-owned skill forms remain alternatives; command form carries root, option, wrapper and selection qualifications. | slice 1 | |
| Q07 | Compatibility includes old-proof readability and latest-first legacy semantics. | slice 2 | |
| Q08 | Named producers and focused refresh are examples and do not establish additional rejection rules. | slice 2 | |
| X01 | Do not add a parallel recorder. | slice 1 | |
| X02 | Do not accumulate run files as the retained proof model. | slice 2 | |
| X03 | No local-proof/CI comparison is requested. | slice 1 | |
| X04 | No other framework adapters are requested. | slice 1 | |
| X05 | No deployment is requested. | slice 1 | |

## Delivery and lifecycle ownership

Reviewer L01–L13 distinguish migration using installed guidance, meaningful passing/failing/refresh verification, ordinary refactor/validation/landing/CI, ownership of installation d43e39016a and migration failures through applicable final CI, complete worktree cleanup and useful material on trunk. Product observations belong above. Publication, final CI and retirement remain owned by ordinary execution/closure at the boundary they can observe; precommit product checks do not assert those later operations already happened. Retain their actual receipts in the execution conversation and completion result.

