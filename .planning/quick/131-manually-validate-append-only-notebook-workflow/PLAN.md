# Manually validate the complete append-only notebook workflow

Status: planned
Source: [SEED-009 story 42](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-42),
refined 2026-09-17. The owner authorized slice planning, but not execution.

## Goal and scope

Give the notebook owner and product owner one current, bounded manual evaluation
of the complete second paragraph of the backlog's near-future direction: refine
a Git-backed notebook in Donut and ordinary local tools, preserve work, note
identity and learning history, and retain one append-only commit sequence when
either side is several commits behind.

The observation uses a browser, the installed Donut CLI, and ordinary Git in
one isolated E2E environment owned by the future Story Branch Mode execution
worktree. It covers representative edits, rename or movement with content
change, multiple local commits published once, multiple accepted web commits
pulled once, owner-facing guidance, and refusal of independently advanced
histories.

The testing session has a hard 60-minute elapsed budget beginning with manual
environment preparation and ending with the observation report. Queue claim,
Story Branch worktree creation, and execute-plan bookkeeping happen outside
that testing clock. Within the hour allocate:

- 10 minutes to preparation and starting-state confirmation;
- 25 minutes to breadth across local-ahead, accepted-ahead, and divergent cases;
- 15 minutes to selective depth on identity, learning state, commit ancestry,
  guidance, and any surprise; and
- 10 minutes to confirmation, cleanup, and the evidence-backed report.

Stop when the budget expires. Unfinished promised observations are material
coverage gaps, not implicit passes or permission to extend the session.

## Execution and manual-testing contract

Execute later through `dough-execute-plan` in default **Story Branch Mode**.
Do not pass `--trunk` and do not select current-branch execution. The execution
workflow owns the backlog claim commit, execution branch/worktree, proof
acceptance, delivery, and later retrospective. Record the resolved originating,
execution, and integration identities in this plan before observation begins.
Story Branch Mode is the development-delivery workspace; it does not introduce
branches into the notebook product workflow being evaluated.

Resolved execution identity:

- Originating checkout/branch: `/Users/terryyin/git/doughnut` on `main` (claim
  commit `979a667474`).
- Execution checkout/branch:
  `/Users/terryyin/git/doughnut-worktrees/131-manually-validate-append-only-notebook-workflow`
  on `131-manually-validate-append-only-notebook-workflow`.
- Integration checkout/branch and authorized remote target: originating
  checkout, `main`, remote `origin`.

During the slice:

- use `dough-manual-testing` as the authority for coverage, time allocation,
  evidence, and the final manual finding report;
- use the repository `manual-testing` skill for browser login, navigation,
  snapshots, and issue-only console/network inspection;
- keep browser-operation evidence available to the execute-plan coordinator;
  the coordinator's story result follows `dough-manual-testing`: exactly
  `Good.` only after complete planned coverage with no actionable finding or
  material uncertainty, otherwise only discrepancies, unresolved expectations,
  improvements, and material coverage gaps;
- do not diagnose, repair, or create permanent automated tests, product code,
  runner support, or documentation from a finding; and
- treat a no-product-change return as an explained empty change. Temporary
  setup files and credentials are owned artifacts and must be removed.

## Oracle and boundaries

The oracle is the current human-owned direction in
[PRODUCT-BACKLOG.md](../../PRODUCT-BACKLOG.md#near-future-direction) and the
refined story, not existing implementation, CLI narration, or tests.

Required history model:

- accepted and local history is one contiguous, single-parent, append-only
  sequence;
- a clean local ancestor may fast-forward across any number of accepted commits;
- accepted history may advance to a local descendant containing several
  original commits;
- independently advanced histories are refused without changing the checkout,
  accepted head, or either body of work; and
- no rebase, merge-based reconciliation, synthetic replay/replacement commit,
  conflict-resolution flow, or branch workflow is accepted behavior.

The implementation's current rebase/replay source and its CLI guidance are
known audit evidence, not a substitute for observation and not a product-scope
question. If the product exhibits them, report expected versus actual with the
captured heads, status, files, and guidance. The fast-forward implementation may
internally use `git merge --ff-only`; because it creates no merge commit and
preserves the accepted sequence, that command alone is not divergent-history
reconciliation.

Exclude performance measurement, cross-notebook synchronization, projection-
drift recovery, native Git transport, attachment/history UI, and all repairs.
Do not broaden the manual session to enumerate every automatically covered path
variation.

## Existing solution and preparation route

Existing installed-CLI E2E journeys already provide representative fixtures,
steps, and observations:

- `cli_notebook_publish_to_clean_clone.feature` covers accumulated local
  rename/edit publication, original history, a clean receiver, and the original
  note route;
- `cli_notebook_web_note_renames.feature`,
  `cli_notebook_web_note_moves.feature`, and
  `cli_notebook_web_folder_moves.feature` cover accepted web changes, learned
  notes, stable identity, final paths, and successive accepted heads; and
- `cli_notebook_web_local_reconciliation.feature` contains older rebase
  expectations that conflict with the current oracle. Reuse its fixture ideas,
  not its expected result.

Do not rerun these scenarios as the manual result. Reuse their fixture vocabulary,
installed-CLI setup, Git observations, and testability endpoint to establish one
small realistic notebook. The cheapest supported live route in a linked
worktree is:

1. unset conflicting datasource and port overrides documented in
   `docs/worktree-browser-tests.md`;
2. reuse existing steps or a temporary setup-only Cypress feature to create one
   user, Git-backed notebook, learned note, access token, and two checkout paths;
3. remove the setup-only artifact after it produces the starting state;
4. start `CURSOR_DEV=true nix develop -c pnpm cy:open` from the execution
   worktree so its isolated stack remains live; and
5. use the printed **Browser origin** for browser automation rather than assuming
   `localhost:5173`. Sign in with the E2E seeded account selected by the setup.

If setup automation already proved a behavior, count it only as automated setup,
not manual acceptance. Confirm the live state through the browser and terminal
before starting breadth. If no supported setup route leaves the browser and CLI
state usable within the 10-minute preparation allocation, stop that path and
report the environment gap; do not add permanent runner plumbing.

## Outside-in observations

### Local is several commits ahead

Given two clean clones of one Git-backed notebook containing a learned note,
make two ordinary local commits in the publisher: rename or move and edit the
note, then edit it again. Publish once.

Observe:

- the installed CLI accepts the publisher's tip without replacing either local
  commit;
- the browser opened at the note's original identity route shows final content
  and retained learning state;
- the receiving clone pulls to the same accepted head with exact final paths and
  bytes; and
- ordinary Git shows the receiver's original head followed by the publisher's
  original two commits in one single-parent chain.

### Accepted history is several commits ahead

From a clean checkout, make at least two accepted web changes to the same
notebook, including an ordinary content edit and a rename or move. Pull once.

Observe:

- the checkout fast-forwards through all recorded accepted heads;
- the final path exists once, the old path is absent, and authored bytes match
  the browser result;
- the checkout is clean at the accepted head with its original head and each
  recorded accepted head in the ancestor chain; and
- the browser still reaches the same note identity and retained learning state.

### Both histories advanced independently

From a clean clone, commit an unpublished local content edit, then append an
independent web change before pulling.

Expected result: pull refuses clearly. Observe the local head, status, index,
working-tree bytes, accepted head, and absence of rebase/merge operation state
before and after. Both commits remain available, and guidance does not instruct
the owner to continue or resolve a rebase/merge.

If pull rebases, auto-merges, synthesizes a replacement commit, pauses a Git
operation, or rewrites guidance around that workflow, record one discrepancy
with the exact expected/actual history and owner-visible message. Do not continue
publishing that rewritten result as though it were accepted behavior.

## Ordered slices

### 1. Observe the complete append-only owner journey within one hour

Type: Behavior
Status: done
Proof: Browser snapshots plus terminal observations establish the three
outside-in cases within the time budget, or the final report names each
discrepancy, unresolved expectation, improvement, and material coverage gap
without claiming unobserved behavior.

Behavior: Given one isolated Git-backed notebook and ordinary browser/CLI/Git
access, when the owner alternates representative multi-commit local and web
work and then attempts a divergent pull, the manual report establishes whether
the product preserves one append-only sequence, identity, learning history, and
both bodies of work under the refined direction.

Run breadth first in the order local-ahead, accepted-ahead, divergence. Preserve
the final 10-minute reporting reserve even if setup or an early journey overruns;
reduce selective depth before dropping a breadth area. Capture commit IDs,
parentage, status and exact relevant files at each history boundary. Use browser
snapshots for note identity/content/learning and owner-facing messages; inspect
console or network only when a visible issue needs evidence. Remove owned
temporary setup artifacts and end the live E2E session without deleting or
adopting another checkout's resources.

Sizing: one 60-minute externally timed observation session. The slice is not a
five-minute implementation leaf because the human explicitly supplied the
one-hour testing budget; it contains one evaluable manual outcome and no product
change or separable delivery boundary.

## Proof ownership

| Promise | Observation owner |
| --- | --- |
| Several local commits publish unchanged as one accepted chain | Local-ahead terminal ancestry and receiver observations |
| Several accepted web commits pull by fast-forward | Accepted-ahead recorded heads, clean checkout, paths and bytes |
| Stable note identity and retained learning | Browser observations at the original note route in both linear journeys |
| Divergence preserves both bodies of work without rebase/merge | Before/after heads, status, bytes, Git operation state, and CLI guidance |
| One-hour breadth-first evaluation | Timestamped allocation and explicit coverage-gap reporting |

## Current decisions

- This story validates only the second near-future-direction paragraph.
- Story Branch Mode is mandatory for later execution; planning leaves the item
  in **Backlog list**.
- The desired divergence outcome is refusal, not reconciliation. It is settled
  product scope, not an open refinement question.
- Manual findings produce a report only. Diagnosis, repair, permanent tests,
  runner changes, new backlog items, and execution beyond 60 minutes require
  separate authority.
- Existing automated evidence informs risk and setup but is not counted as the
  requested manual observation.

## Learnings

- Current source implements eligible rebase and exact-subtree replay despite
  the later no-rebase direction; the divergence journey is therefore the
  highest-risk observation.
- Linked Story Branch worktrees receive isolated E2E databases and ports. The
  actual runner-reported browser origin must be used for manual browser control.
- Existing CLI E2E fixtures already express the required notebook, learning,
  Git-binding, access-token, clone, ancestry, path, and original-route setup, so
  a permanent manual-test harness is not justified.

## Manual finding report

Session: 2026-09-17, 12:10:46–12:21:19 (about 11 minutes elapsed of the
60-minute budget). Isolated worktree stack (`pnpm cy:open`, worktree id
`wt_c65eddea68f84ad5931d04e3f2260fc9`, browser origin `http://127.0.0.1:58269`),
one fresh user, notebook "Manual Notebook 131" (id 66871), note "Overview"
(id 85359, route `/n85359`) assimilated under Understanding, a generated access
token, and the real installed CLI bundle (`cli/dist/donut-cli.bundle.mjs`) run
directly with `DONUT_API_BASE_URL`/`DONUT_CONFIG_DIR` against that stack —
publisher and receiver clones for breadth, a third clean clone for divergence.

- **Local is several commits ahead — matches the oracle.** Two local commits
  (move `Overview.md` into `Notes/` with an edit, then a further edit) published
  as the publisher's unchanged tip (accepted head `1bb9119`). The receiver
  pulled to the same head with the receiver's original head followed by the
  publisher's two original commits in one single-parent chain, exact final path
  and bytes, clean status. The browser at `/n85359` showed the final content and
  the retained Understanding tracker (Recall badge unchanged) at the same route.
- **Accepted history is several commits ahead — matches the oracle.** Two
  accepted web changes (a content edit, then a note rename/move) applied to a
  clean checkout: the checkout fast-forwarded through both recorded accepted
  heads (`bf4bea0`, `e1f6086`), the final path existed once with the old path
  absent, bytes matched the web result, and the checkout was clean at the
  accepted head with the original head as an ancestor. The browser still
  resolved the same note identity and retained learning state.
- **Discrepancy — divergent histories are not refused.** From a clean clone at
  accepted head `e1f6086`, an unpublished local content edit (commit `075e121`)
  followed by an independent accepted web edit (new accepted head, `cf97ffe`),
  then `donut notebook pull`: expected a clear refusal leaving the checkout,
  local head, and accepted head unchanged with both bodies of work intact and
  no rebase/merge guidance. Actual: the CLI attempted a rebase, left the
  checkout in a detached-HEAD, mid-rebase state (`.git/rebase-merge` present,
  `git status` reporting "interactive rebase in progress") with an unresolved
  native conflict marker in the working-tree file, and printed guidance
  instructing `git add ... && git rebase --continue` or `git rebase --abort`.
  The `main` branch ref itself stayed at the unpublished local commit and the
  accepted commit's content remained recoverable from the object store, so
  both bodies of work were technically not lost, but the checkout was left in
  a broken, non-clean, conflict-paused state rather than refused cleanly — the
  exact known-risk behavior flagged in Learnings above. Evidence: local head
  `075e121df398e49376c3ce94a5e41f4f2fd1484f`, accepted head `cf97ffe...`,
  `git status -sb` → `## HEAD (no branch)` / `UU "Notes/Overview renamed.md"`,
  CLI output beginning `donut: Git paused a rebase with a conflict in
  "Notes/Overview renamed.md"...`.
- No coverage gaps: all three outside-in cases and their identity/learning
  depth checks completed inside the time budget with no automated-repair,
  diagnosis, or permanent test/runner changes made.
- No product code was changed by this session (temporary clone destinations,
  CLI config, and CLI bundle build artifact were removed; the isolated stack
  was shut down); this plan update is the only tracked change.
