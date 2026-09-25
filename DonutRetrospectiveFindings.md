# Donut Retrospective Findings

Findings specific to Donut’s product, repository tooling, or local conventions.
Findings about shared Open Dough skills, supporting guidelines, and skill scripts
remain in [DearDough.md](DearDough.md), including those observed in this project.
Existing finding identifiers and occurrence evidence are preserved when moved.

## Review — 2026-09-25

No unresolved project-specific findings remain in this file. Reviewed both
finding logs at `42ef5d8ff2396536cdd01d6b0816b60305ee6637`.
Ownership follows the failing responsibility, not the repository in which it
was observed. None of the current DearDough entries warrants moving here.

### Shared findings retained in DearDough

| Responsibility | Findings | Ownership evidence |
| --- | --- | --- |
| Measurement scope | ODF-030 | Disposable profiling versus durable delivery scope is shared execution feedback. |
| CI observation and host/runtime integration | ODF-034, ODF-069, ODF-085, DD-107, DD-112 | Published [CI observation guidance](https://github.com/terryyin/open-dough/blob/v0.3.38/src/skills/dough-execute-plan/references/ci-monitor.md) owns target selection, observer coverage, runtime binding, and managed attachment. Donut workflow details are occurrence evidence. |
| Delegation, proof, and failure attribution | ODF-042, ODF-051, ODF-059, ODF-082, ODF-090, ODF-091, DD-109, DD-111, DD-113 | Published [delivery and proof guidance](https://github.com/terryyin/open-dough/blob/v0.3.38/src/skills/dough-execute-plan/references/wrap-up.md) owns report verification, consumer coverage, independent refactoring, and failure diagnosis. Specific CLI/backend tests do not make these process failures project-owned. |
| Execution workspace, claims, and maintenance receipts | ODF-081, ODF-083, ODF-084, ODF-086, DD-108, DD-110 | Shared startup, backlog claim, and increment-delivery responsibilities; published [delivery guidance](https://github.com/terryyin/open-dough/blob/v0.3.38/src/skills/dough-execute-plan/references/wrap-up.md) delegates checkout preservation and maintenance to the shared runtime. |

The published [finding registry](https://github.com/terryyin/open-dough/blob/v0.3.38/docs/maintainer/finding-names.md)
provides the ODF identities. Newer DD entries retain their existing identifiers;
this review does not allocate or rename upstream findings. Repeated CI discovery
and session-attachment failures remain shared feedback even after an attempted
shared fix; recurrence alone does not transfer ownership to Donut.

### Resolved project finding removed

Removed DD-103 (parallel Gradle processes racing on local batch E2E startup).
Correction `d86864023c2bbf2dcb39e787cd6c70cb486f4ab4` is an ancestor of the
reviewed revision. Current `scripts/e2e-runner.mjs` passes
`backendReload: false` for batches and `true` for interactive sessions;
`scripts/sut-services.mjs` selects `backend:sut:ci` for the batch path.
The launch-path regression cases remain in
`scripts/e2e-runner-backend-reload.cases.mjs`; the recorded correction includes
successful clean-build, successive-source-edit, invalid-source, and interactive
reload checks. No subsequent change to the runner or those regression cases,
or later occurrence in either finding log, contradicts that resolution.
This review inspected the retained evidence and current code; it did not rerun
E2E. The obsolete DD-103 record and its original occurrences are recoverable at
`42ef5d8ff2396536cdd01d6b0816b60305ee6637:DonutRetrospectiveFindings.md`.
Current behavior is documented in [End-to-end Testing](docs/end-to-end-testing.md).

### Backlog decision

There are zero unresolved project findings in these logs, so there are no two
supported project stories to rank by frequency and impact. The
[product backlog](.planning/PRODUCT-BACKLOG.md) is unchanged. The existing
[attachment-representation correction](.planning/quick/031-finish-single-attachment-representation/PLAN.md)
already owns its separate product findings and is in Taken; do not duplicate it.
The E2E-authoring guidance story is already queued, but DD-103 concerns runner
startup and supplies no evidence of an authoring-guidance recurrence.

Reopen a project finding when a new occurrence contradicts its actual correction,
link that evidence to the existing story or correction if still active, and
otherwise queue a bounded recurrence story. A previously recorded resolution
without supporting correction evidence is insufficient to close a finding.
