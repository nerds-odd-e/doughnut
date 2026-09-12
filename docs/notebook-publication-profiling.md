# Notebook publication profiling

## Findings and next experiment

Repeated ORM flushing is the first investigation priority. The two completed
valid large publications (1,000 existing concepts, 10,000 additions, 20 folders)
place **98.25% and 98.213% of publication-thread execution samples** in
Hibernate flush traversal, with roughly 571 GB of weighted request allocation
in each. Property- and alias-index refresh callers recur in these stacks
(54,321/54,728 samples in the first capture; 54,565/54,948 in the repeat).
These overlapping sampled stacks identify a dominant CPU cost; they do **not**
predict 98% wall-time savings or establish that any flush can safely be
removed.

The first focused experiment should trace flush requirements and frequency
along these index-refresh paths on a small fixture, then evaluate one
correctness-safe change to that work. Explicit and query-triggered flushes may
serve visibility or ordering requirements; establish those requirements before
changing them. Keep accepted-head/content and late-rejection/rollback checks,
including note identity and learning state. Parsing, Git work and database
waits have weaker measured CPU evidence and should not displace this priority
without new evidence.

Use the [baseline captures](#baseline-captures), their persistent raw data,
and the versioned [analysis script](../scripts/profiling/AnalyzePublication.java).
The [small late-rejection capture](#small-late-rejection-capture) establishes
late processing and preserved state; **large rejection latency is unmeasured**.
Stop investigating once a dominant cost and one concrete next experiment are
clear — do not repeat 10,000-note captures merely to reconfirm this ranking.
Reserve a large confirmation run for a candidate improvement that leaves a
specific scaling question unanswered.

## Baseline captures

Both large captures used the same synthetic fixture: 1,000 existing concepts,
10,000 additions, 20 folders. Fixture fingerprints (SHA-256 of recursive Git
tree listings) matched on both runs — baseline
`30f8c2a12e2214f1a9b7f34e4c79d09d8473279899e3138b9ca0a029a0afa8f9`, proposal
`dbea65fb646eb7660faf6a509cee84f1b7adda9204e2aaa4fae137edb90da65d`. Both ran
on the same owned JVM (PID 2406, JDK 25.0.3, MySQL 8.4.11,
`-XX:TieredStopAtLevel=1`, 12 GiB max heap, ordinary E2E logging, no explicit
warm-up) — these are local, not optimized-production, JVM conditions.

| Capture (timestamp dir) | Source revision | Elapsed HTTP time | Accepted-content proof |
| --- | --- | --- | --- |
| `2026-09-11T10-50-26.810Z` — **sleep-affected, not a clean baseline** | `b650a294f644dcdb24a622c3d801621fc881aaee` | 4,987,017.426 ms (83 min 7.017 s), including host suspension 11:12:05–11:37:27 UTC | Cypress runner failed to pass; independent read-only check verified accepted head `766dbfdbc7a32df0db60069a5e12c11d8f651538` (from baseline `b7105dbdfcc2a4848f8380f2fd3b6d0ea435afaf`), tree `22a5eeb3dd6dc92e2778929a6392eecb9d5c85fc`, and all 10,000 documents byte-for-byte |
| `2026-09-11T12-37-08.116Z` — **awake, usable elapsed-time reference** | `4543afeed147922187d2b5eb2954e4488e191427` | 3,772,320.997 ms (62 min 52.321 s), no sleep during the request | Cypress's own per-file receiver verification was still running when the user requested a safe stop; independent read-only check passed all 10,000 file-byte comparisons, accepted head/tree and clean status in 2.5 s |

Do not average the sleep-affected latency with the awake repeat as though
conditions were equivalent; use the awake run for elapsed-time comparisons.

## Small late-rejection capture

Run with only the final added Git path's alias changed to
`aliases: {invalid: shape}` (`--expose tags=@publicationProfileRejection`).
Two reset runs passed on 2026-09-11:

- `2026-09-11T10-31-50.438Z`: CLI 683 ms; recording interval 1174 ms.
- `2026-09-11T10-32-37.440Z`: CLI 574 ms; recording interval 1022 ms.

Both allocated 19 (`additions - 1`) preceding note identities, preserved
baseline head `650445c7f56dde3ed3afbf4f1ca8e94794757b0c` plus stored rows and
learning state, and failed with
`Invalid authored property at path "group-19/Added-00019.md"`. This proves
late processing and correct rollback for a **small** fixture only —
**large rejection latency remains unmeasured**. Keep the small-first rule:
only escalate to a large run for a specific unanswered scaling question; do
not repeat 10,000-note captures merely to reconfirm this ranking.

## Running the profiler

Raw JFRs and recovery artifacts persist outside Git under
`~/Library/Application Support/Donut/publication-profiles/` (one directory per
capture, named by timestamp).

Minimum analysis invocation:

```bash
CURSOR_DEV=true nix develop -c java scripts/profiling/AnalyzePublication.java <jfr-file> <startTime> <endTime> <thread-name>
```

For example, against the awake capture above:

```bash
CURSOR_DEV=true nix develop -c java scripts/profiling/AnalyzePublication.java '/Users/terryyin/Library/Application Support/Donut/publication-profiles/2026-09-11T12-37-08.116Z/publication.jfr' 2026-09-11T12:37:08.695249Z 2026-09-11T13:40:01.411885541Z http-nio-50809-exec-10
```

Small fixture (opt-in, excluded from ordinary CI). From an owned linked
worktree with a healthy E2E stack, run:

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_web_created_note.feature --expose tags=@publicationProfile
```

Use `--expose tags=@publicationProfileRejection` for the rejection variant.
Defaults are 20 existing concepts, 20 additions, 20 folders; override with
`PUBLICATION_PROFILE_EXISTING`, `PUBLICATION_PROFILE_ADDITIONS` and
`PUBLICATION_PROFILE_FOLDERS`. The accepted scenario now verifies every
received file in one bulk Node-side check (the `verifyPublicationReceiverFiles`
Cypress task, backed by `scripts/profiling/verify-publication-receiver.mjs`)
instead of one browser assertion per file, so no manual read-only workaround
is needed for ordinary runs.

For an HTTP capture (needed for large fixtures, since the installed-CLI path
keeps a 60-second wait), use the HTTP tags and scale the same env vars:

```bash
PUBLICATION_PROFILE_REQUEST_TIMEOUT_MS=60000 CURSOR_DEV=true nix develop -c pnpm cypress run --browser chrome --spec e2e_test/features/cli/cli_notebook_web_created_note.feature --config taskTimeout=66000 --expose 'tags=@publicationProfileHttp or @publicationProfileHttpRejection'
```

A timeout or a runner failure leaves incomplete evidence — inspect the owned
backend for actual completion before any reset or retry; never relabel a
failed/incomplete run as passing. Repeat a command to reset the owned E2E
database and fixture before the next run, following
[ADR 0007](adrs/0007-environments-and-isolation-accepted.md) isolation.

## Large HTTP confirmation (blocked in fixture setup)

Worktree `doughnut_e2e_wt_55773df2d6ac449796e9d4c0abac2671`, revision
`61624927455b3479a84acb356d5d3b90e0c32e8f`, 2026-09-12T08:31:05Z, host kept
awake. Command used `PUBLICATION_PROFILE_EXISTING=1000`
`PUBLICATION_PROFILE_ADDITIONS=10000` `PUBLICATION_PROFILE_FOLDERS=20`
`PUBLICATION_PROFILE_REQUEST_TIMEOUT_MS=60000` and
`--config taskTimeout=66000`.

Cypress failed before publication: `cy.wrap()` timed out at **6000 ms**
(`defaultCommandTimeout`) while seeding 1,000 existing concepts. No profile
directory, `timing.json`, JFR, fingerprints, or bulk-checker outcome. This is
incomplete evidence, not a passed or failed 60 s publication. The HTTP deadline
must stay at 60,000 ms; fixture and verification waits need longer Cypress
`defaultCommandTimeout` and `taskTimeout` (the awake large captures used
`taskTimeout=43260000,defaultCommandTimeout=600000`).

## Large HTTP confirmation (HTTP deadline exceeded)

Authorized retry of the same 1,000 / 10,000 / 20 fixture on
`doughnut_e2e_wt_55773df2d6ac449796e9d4c0abac2671`, revision
`61624927455b3479a84acb356d5d3b90e0c32e8f`. Host stayed awake
(`caffeinate` 16:34:16–16:36:18 +0800; no Sleep/Wake in that interval).
JDK 25.0.3, `-XX:TieredStopAtLevel=1`, 12 GiB max heap, Hibernate
7.4.5.Final, MySQL 8.4.11. Same fingerprints as the awake baseline:
baseline `30f8c2a12e2214f1a9b7f34e4c79d09d8473279899e3138b9ca0a029a0afa8f9`,
proposal `dbea65fb646eb7660faf6a509cee84f1b7adda9204e2aaa4fae137edb90da65d`.
Material difference vs the 2026-09-11 awake capture: the
COMMIT-through-`ownRowsBySourceLocalKey` persist change, isolated worktree ports,
and a 60,000 ms HTTP deadline instead of a one-hour wait.

Capture `2026-09-12T08-35-11.940Z`:

| Field | Value |
| --- | --- |
| HTTP `elapsedMs` | **60,003.112** (`timeoutMs` 60,000) |
| Outcome | incomplete — `Publication benchmark HTTP deadline exceeded` |
| Accepted head/tree | not received |
| Bulk checker | not reached |
| Request thread | `http-nio-63814-exec-10` |
| Request allocation weight | 10,503,558,320 bytes in the truncated 60 s span |
| Request-thread samples | 3,006 / 3,015; `AbstractFlushingEventListener` 2,671; `NoteAliasIndexService` 1,081; `NotePropertyIndexService` 608 |

Artifacts:
`~/Library/Application Support/Donut/publication-profiles/2026-09-12T08-35-11.940Z/`
(`timing.json`, `result.json`, `capture.json`, `incomplete.jfr`,
`jfr-summary.txt`, `proposal.bundle`, `observed-request-span-analysis.txt`).

Owned backend was still in `NotebookGitProposalPublisher.publish` when the
client disconnected (Hikari leak detection at 16:36:13 on the request
thread). The runner then began graceful shutdown. A disconnected client
does not establish server cancellation. This is incomplete evidence, not a
passed publication. Compared with the awake baseline **3,772,320.997 ms**,
the request was aborted at about 1.6% of that wall time and had not
finished. Return for story scope review; do not weaken the 60 s target or
start a second optimization.

The full prior investigation record (static-candidate inspection, individual
smoke-run accounts, recovery commands, and continuation-probe narration) is
recoverable with `git show 13a2e2edc6:docs/notebook-publication-profiling.md`.
