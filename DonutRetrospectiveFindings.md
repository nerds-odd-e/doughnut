# Donut Retrospective Findings

Findings specific to Donut’s product, repository tooling, or local conventions.
Findings about shared OpenDough skills, supporting guidelines, and skill scripts
remain in [DearDough.md](DearDough.md), including those observed in this project.
Existing finding identifiers and occurrence evidence are preserved.

Classification: DD-065 and DD-074 concern Donut’s Nix-created, gitignored
`.claude/skills` symlink layout, owned by
[scripts/shell_setup.sh](scripts/shell_setup.sh). Open Dough’s published
[v0.3.26 installer](https://github.com/terryyin/open-dough/blob/v0.3.26/src/install/open-dough-release-apply.sh)
maintains both physical skill installations. These are local integration
findings; the historical descriptions and occurrence evidence below are
preserved unchanged.

## Reliable CI-observer startup in Donut execution worktrees

Reviewed 2026-09-21 against `8766adefee`. Both findings remain unresolved;
none were removed. They share Donut’s skill-path integration as their parent
problem, but have distinct observable failures before and after shell setup.

| Priority | Failure group | Finding | Retained occurrences | Observed impact | Queued story |
| --- | --- | --- | --- | --- | --- |
| 1 | Runtime unavailable in a fresh worktree | [DD-074](#dd-074) | 4 | Ten of eleven pushes unobserved in one execution; repeated copying and discovery work | [Start CI observation from a fresh Donut worktree](.planning/seeds/SEED-038-reliable-ci-observer-startup.md#story-1) |
| 2 | Silent dispatch failure after shell setup | [DD-065](#dd-065) | 4 | Two executions initially reported observation unavailable and pushed before arming it; repeated diagnosis | [Start CI observation reliably after Donut shell setup](.planning/seeds/SEED-038-reliable-ci-observer-startup.md#story-2) |

Frequency is tied; the recorded extent of missed observation puts DD-074 first.
These are retained occurrence counts, not all-time totals. The check below is
validation of the existing findings, not another execution occurrence.

### Current verification and story matching

A temporary export of the committed runtime and `scripts/shell_setup.sh`
reproduced both failures using Node from `CURSOR_DEV=true nix develop`:

- Before shell setup, `node .claude/skills/dough-execute-plan/scripts/ci-mailbox.mjs probe`
  exited 1 with `MODULE_NOT_FOUND`.
- After `source scripts/shell_setup.sh; setup_claude_skills`, the same command
  exited 0 with empty stdout through the generated symlink.
- The control invocation through `.agents/skills/dough-execute-plan/scripts/ci-mailbox.mjs`
  exited 0 and printed `CI_OBSERVER`.

The check used temporary mailbox storage and launched no CI observer or GitHub
run. The tracked files, `.gitignore`, and Nix/cloud setup still retain the
reported mechanisms. No current queued/taken story covers either outcome.
Searches of planning history found no confirmed fix for these mechanisms;
earlier completed observer-shutdown work (SEED-012 story 6, `72701489a7`)
concerned recovering a running observer after compaction, not startup paths.
Thus these are persistent findings, with repeated workarounds rather than an
evidenced fix followed by regression. Both stories belong to
[SEED-038](.planning/seeds/SEED-038-reliable-ci-observer-startup.md).

<a id="dd-065"></a>

## DD-065 — ci-mailbox.mjs's CLI dispatch silently no-ops when invoked through the `.claude/skills` symlink instead of its `.agents/skills` realpath

`ci-mailbox.mjs`'s main-module guard
(`process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href`)
compares the invoked path against the module's symlink-resolved URL. This
project's own Nix shell setup (`scripts/shell_setup.sh`) deliberately
symlinks `.claude/skills/<name>` to `.agents/skills/<name>` in every
checkout/worktree so Claude Code can discover skills, and
`references/runtime-setup.md` documents `.claude/skills/dough-execute-plan`
as the normal Claude Code script path. Invoking
`node .claude/skills/dough-execute-plan/scripts/ci-mailbox.mjs probe` (or
`start`/`register-push`/`stop`) makes Node resolve `import.meta.url` to the
real, post-symlink `.agents/skills/...` path while `process.argv[1]` keeps
the invoked `.claude/skills/...` path, so the strict equality check fails,
the CLI dispatch branch never runs, and the process exits 0 with zero
stdout — no error, no `CI_OBSERVER` receipt, no diagnostic. The identical
command run via the `.agents/skills/...` realpath works correctly and prints
the expected receipt. The failure mode (silent success-looking no-op, not a
stop or error) is worse than `runtime-setup.md`'s existing checkout-identity
guidance anticipates: a coordinator following the documented path literally
gets nothing, which is easy to mistake for a successful no-op rather than an
untriggered CLI guard.

### Occurrences

- Execution: SEED-024 story 1 / quick/135-irreversible-relationship-reduction
  - Timestamp: 2026-09-17, evening +08:00 (during CI-observer arming before
    slice 1 delegation)
  - Tool: Claude Code
  - Model: claude-sonnet-5
  - Open Dough release: unknown
  - Evidence: `node .claude/skills/dough-execute-plan/scripts/ci-mailbox.mjs probe`
    run from the execution worktree root produced no stdout and exit code 0;
    an inline `node -e` script importing the same file's `probeMailbox` export
    directly worked and returned a directory; `node .agents/skills/dough-execute-plan/scripts/ci-mailbox.mjs probe`
    then printed `CI_OBSERVER {"directory":"/tmp/dough-ci-501/watch-oZWLUN"}`
    and the PostToolUse hook added `CI_MONITOR_READY` context, confirming the
    realpath invocation was the fix.
  - Observed effect: no lost coverage — the silent no-op was caught by testing
    an inline import before trusting the CLI, and the observer was armed
    successfully via the realpath before any slice was delegated — but it
    cost extra diagnostic steps, and a less cautious run could have proceeded
    believing CI observation was set up when it was not.
  - Inference: the CLI entry guard should compare canonicalized/realpath forms
    of `process.argv[1]` and the module path (or otherwise detect direct
    invocation more robustly than exact string equality against a path that
    may traverse a project-standard symlink), since this project's own setup
    deliberately creates that exact symlink in every checkout.

- Execution: SEED-028 story 1 / quick/140-admin-job-status-local-time / 1694c122d9
  - Timestamp: 2026-09-18 (session date)
  - Tool: Claude Code
  - Model: claude-sonnet-5
  - Open Dough release: unknown
  - Evidence: `node '.claude/skills/dough-execute-plan/scripts/ci-mailbox.mjs' probe`
    run from the execution worktree root produced no stdout and exit code 0,
    with no `CI_MONITOR_READY` PostToolUse context added. `node '.agents/skills/dough-execute-plan/scripts/ci-mailbox.mjs' probe`
    then printed `CI_OBSERVER {"directory":"/tmp/dough-ci-501/watch-mL5Kzr"}`
    and the hook added `CI_MONITOR_READY`.
  - Observed effect: worse than the prior occurrence — this coordinator
    concluded CI observation was genuinely unavailable from the silent no-op
    alone, reported that limitation to the user, and had already pushed
    slice 1's commit unobserved before checking this log's existing DD-065
    entry, recognizing the exact match, retrying via the realpath, and
    starting the observer late (after the push it should have covered).
    No coverage was permanently lost (the observer's startup snapshot still
    discovered the already-pushed commit's run), but the sequence shows the
    silent-no-op failure mode reliably reproduces a false "unavailable"
    conclusion for a coordinator that does not already know to check this
    log before trusting the probe's silence.
  - Inference: same root cause and same fix as the original finding; the
    false-unavailable conclusion this occurrence reached is itself further
    evidence for fixing the CLI guard rather than relying on operators to
    recall this log entry.

- Execution: SEED-033 story 1 / quick/144-compact-spelling-results / 55fc4ba255
  - Timestamp: 2026-09-18 (session date)
  - Tool: Claude Code
  - Model: claude-sonnet-5
  - Open Dough release: unknown
  - Evidence: `node .claude/skills/dough-execute-plan/scripts/ci-mailbox.mjs probe`
    run from the execution worktree root (with both a bare relative path and
    an absolute path) produced no stdout and exit code 0, with no
    `CI_MONITOR_READY` PostToolUse context added; a standalone inline script
    confirmed `pathToFileURL` resolution matched in isolation, so the silence
    was not an obvious invocation mistake. `node '.agents/skills/dough-execute-plan/scripts/ci-mailbox.mjs' probe`
    then printed `CI_OBSERVER {"directory":"/tmp/dough-ci-501/watch-mAT38C"}`
    and the hook added `CI_MONITOR_READY`.
  - Observed effect: same false-unavailable pattern as the second occurrence —
    this coordinator concluded the bridge was unavailable for this
    non-interactive/background-job session, reported that limitation, and had
    already pushed slice 1's commit unobserved before reaching the
    retrospective's process review, which is what surfaced this log's
    existing DD-065 entry and prompted retrying via the realpath. The
    observer's startup snapshot still discovered the already-pushed commit's
    run once armed and the SHA was registered after the fact, so no coverage
    was permanently lost, but two independent coordinators have now reached
    the same wrong "unavailable" conclusion from the same silent no-op before
    reading this log.
  - Inference: same root cause and fix as the prior occurrences. The repeat
    across three separate executions (two different coordinators reaching the
    false-unavailable conclusion) strengthens the case that this needs the
    CLI guard fixed rather than continuing to rely on retrospective review to
    catch it after the fact.

- Execution: quick/260920-frontend-proof-type-checking / 7b1d80b4e8
  - Timestamp: 2026-09-19 (session date)
  - Tool: Claude Code
  - Model: claude-sonnet-5
  - Open Dough release: unknown
  - Evidence: `node ./.claude/skills/dough-execute-plan/scripts/ci-mailbox.mjs
    start --execution nerds-odd-e/doughnut worktree-260920-frontend-proof-type-checking`
    run from the execution worktree root (after this session had already
    entered a Nix shell there, which creates the `.claude/skills/<name>`
    symlinks) produced no stdout and exit code 0, with no
    `CI_OBSERVER`/`CI_MONITOR_READY` context added. Individually reading that
    same path with `sed`/`grep`/`cp` returned real file content matching
    `.agents/skills/dough-execute-plan/scripts/ci-mailbox.mjs`, while a bare
    `ls -la .claude/skills` from the worktree root (no subpath) showed zero
    entries beside `.` and `..` — the per-file reads and the directory
    listing disagreed about whether the path existed.
    `node ./.agents/skills/dough-execute-plan/scripts/ci-mailbox.mjs start ...`
    then printed the expected `CI_OBSERVER {"directory":...}` receipt and the
    PostToolUse hook added its context.
  - Observed effect: no lost coverage — diagnosed via an inline import script
    that printed the module's actual exports before trusting the CLI's
    silence, then armed and registered the already-pushed commit via the
    realpath before shutting the observer down — but cost several extra
    diagnostic tool calls after the branch had already been pushed, during a
    background/non-interactive session with no live user to consult.
  - Inference: same root cause and fix as the prior three occurrences. The
    disagreement between successful individual-file reads and an empty bare
    directory listing for the same `.claude/skills` path is a new wrinkle
    worth naming: it means confirming the target file is readable is not
    sufficient confirmation that the CLI invocation through that path will
    behave correctly, which makes runtime-setup.md's own "do not reuse the
    installed directory that supplied the initially loaded skill" caution
    easy to satisfy on a shallow check while still hitting this failure mode.

<a id="dd-074"></a>

## DD-074 — The documented `.claude/skills` runtime path did not exist at all in a freshly created execution worktree

[runtime-setup.md](.agents/skills/dough-execute-plan/references/runtime-setup.md) documents
`.claude/skills/dough-execute-plan` as the normal Claude Code location of the
CI observer runtime. In a worktree created by `git worktree add` and used
immediately, that directory does not exist: `.claude/skills/<name>` is a
symlink this project's `scripts/shell_setup.sh` creates when a Nix shell is
entered, and a coordinator that creates the worktree and arms the observer
without first entering a shell in it has only the tracked `.agents/skills`
tree. Invoking the documented path therefore fails loudly with
`MODULE_NOT_FOUND` rather than silently. This is the same documented-path
problem as DD-065 but not the same cause: DD-065's silent no-op comes from the
main-module guard comparing an invoked symlink path against a resolved
realpath, and fixing that guard would not help here, because the file is
absent. The shared fix is for the guidance to name `.agents/skills` (the
tracked realpath) as this project's runtime location, or for worktree setup to
create the symlink before the observer is armed.

### Occurrences

- Execution: SEED-034 story 1 / quick/146-permanently-delete-trashed-content / 6f2a2ff1d8
  - Timestamp: 2026-09-18T17:06+08:00
  - Tool: Claude Code
  - Model: claude-opus-5
  - Open Dough release: unknown
  - Evidence: `node '.claude/skills/dough-execute-plan/scripts/ci-mailbox.mjs' probe`
    run from the execution worktree root exited non-zero with
    `Error: Cannot find module` / `code: 'MODULE_NOT_FOUND'`;
    `ls .claude/skills/dough-execute-plan/scripts/` returned
    `No such file or directory` while `ls .agents/skills/dough-execute-plan/scripts/`
    listed the runtime. `node '.agents/skills/dough-execute-plan/scripts/ci-mailbox.mjs' probe`
    then printed `CI_OBSERVER {"directory":"/tmp/dough-ci-501/watch-4CslQR"}`
    and the PostToolUse hook added `CI_MONITOR_READY`.
  - Observed effect: no coverage was lost and nothing was misdiagnosed. The
    hard error was unambiguous, so the realpath was used on the next call and
    the observer was armed before the first slice was delegated and before any
    push. Cost was one wasted tool call plus one directory listing.
  - Inference: the loud failure mode is strictly better than DD-065's silent
    one — it cannot be mistaken for "bridge unavailable" — but both arise from
    the same documented path being wrong for this project. Which of the two
    failure modes a run hits appears to depend only on whether a Nix shell has
    been entered in that worktree yet, which is not something the guidance
    mentions.
- Execution: SEED-035 story 1 / quick/148-cohesive-accepted-web-folder-changes / ea903668bb
  - Timestamp: unknown (during initial CI-observer setup, before the first
    slice was delegated)
  - Tool: Claude Code
  - Model: claude-sonnet-5
  - Open Dough release: unknown
  - Evidence: `ls .claude/skills/dough-execute-plan/scripts/ci-mailbox.mjs` in
    the freshly created worktree returned "No such file or directory", and
    `ls .claude/` there listed only `settings.json`, no `skills/`. The
    coordinator did not check `.agents/skills` first; `git ls-files
    .agents/skills | grep dough-execute-plan` (run later, during this
    retrospective) confirms `.agents/skills/dough-execute-plan/scripts/ci-mailbox.mjs`
    was already present and git-tracked in that same worktree the whole time.
  - Observed effect: instead of hitting this entry's documented
    `MODULE_NOT_FOUND` or discovering the tracked `.agents/skills` realpath,
    the coordinator ran `cp -R /Users/terryyin/git/doughnut/.claude/skills
    <worktree>/.claude/skills` to populate the missing path, then armed the
    observer from that copy. The copy worked and no coverage was lost, but
    the copy was unnecessary work and leaves the worktree's skill copy able
    to drift from the main checkout's local, gitignored `.claude/skills`
    install if either is updated later.
  - Inference: a third failure mode for the same root cause, beyond the loud
    error and DD-065's silent no-op: working around the missing path by
    duplicating a local install rather than using the already-tracked
    `.agents/skills` realpath this entry already recommends naming in the
    guidance. Reinforces that the fix, once applied, would avoid this cost
    too.
- Execution: SEED-036 story 1 / quick/149-permanent-deletion-loose-ends / a3856444de
  - Timestamp: 2026-09-19T10:33+08:00 (during this execution's retrospective;
    the original miss happened at CI-observer setup, before slice 1)
  - Tool: Claude Code
  - Model: claude-sonnet-5
  - Open Dough release: unknown
  - Evidence: at setup, `find .claude/skills/dough-execute-plan -type f`
    from the freshly created worktree errored "No such file or directory",
    and `ls .claude/` there listed only `settings.json`. The coordinator
    concluded the CI-observer runtime was unavailable and delivered all 11
    slices (11 pushes to `worktree-149-permanent-deletion-loose-ends`)
    unobserved. During the retrospective, `ls
    .agents/skills/dough-execute-plan/scripts/` in that same worktree listed
    the runtime, and `node .agents/skills/dough-execute-plan/scripts/ci-mailbox.mjs probe`
    then `start --execution nerds-odd-e/doughnut worktree-149-permanent-deletion-loose-ends`
    printed `CI_OBSERVER` and the PostToolUse hook added
    `CI_MONITOR_READY`/"CI observer attached to this coordinator" on the
    first call.
  - Observed effect: unlike both prior occurrences, the coordinator did not
    hit the documented `MODULE_NOT_FOUND` error, did not discover
    `.agents/skills`, and did not work around it with a copy — it treated
    the missing `.claude/skills` path alone as proof the bridge was
    unavailable and reported that once, per the shared skill's own fallback
    for a genuinely unavailable bridge. Ten of eleven delivered pushes went
    completely unobserved; the observer was armed only retroactively, for
    the final revision, after all slices were already delivered.
  - Inference: a fourth failure mode for the same root cause: the documented
    path's absence was treated as a terminal "no CI coverage" conclusion
    instead of a prompt to check the tracked `.agents/skills` realpath this
    entry already names as the fix. This is the costliest occurrence of the
    four — real coverage loss across most of an execution, not merely
    wasted calls or a workaround — and is further evidence the guidance
    should name `.agents/skills` directly rather than relying on each
    execution to rediscover it.
- Execution: SEED-035 story 1 / quick/260922-paste-without-formatting / 0c5e5725a0
  - Timestamp: unknown (2026-09-20, during this execution's initial
    CI-observer setup, before the first slice was delegated)
  - Tool: Claude Code
  - Model: claude-sonnet-5
  - Open Dough release: unknown
  - Evidence: `node '.claude/skills/dough-execute-plan/scripts/ci-mailbox.mjs' probe`
    run from the freshly created execution worktree
    (`.claude/worktrees/260922-paste-without-formatting`) exited non-zero with
    `Error: Cannot find module
    '/Users/terryyin/git/doughnut/.claude/worktrees/260922-paste-without-formatting/.claude/skills/dough-execute-plan/scripts/ci-mailbox.mjs'`,
    `code: 'MODULE_NOT_FOUND'`. The coordinator did not check `.agents/skills`;
    it ran `mkdir -p .claude/worktrees/260922-paste-without-formatting/.claude
    && cp -R .claude/skills .claude/worktrees/260922-paste-without-formatting/.claude/skills`
    to populate the missing path, then reprobed successfully from that copy
    (`CI_OBSERVER {"directory":"/tmp/dough-ci-501/watch-n4xced"}`). During this
    retrospective, `git ls-files .agents/skills | grep dough-execute-plan` from
    the main checkout confirms `.agents/skills/dough-execute-plan/` was already
    git-tracked, and `ls .claude/worktrees/260922-paste-without-formatting/.agents/skills/dough-execute-plan/scripts/`
    lists the runtime already present there via the ordinary worktree checkout,
    the entire time.
  - Observed effect: the same third failure mode as the SEED-035/148
    occurrence above: an unnecessary local-install copy instead of using the
    already-tracked `.agents/skills` realpath. No coverage was lost — the copy
    worked and the observer was armed before the first slice was delegated —
    but it cost one wasted probe call, one directory copy, and left the
    worktree's `.claude/skills` able to drift from the main checkout's local,
    gitignored install.
  - Inference: a second confirmed instance of this entry's third failure
    mode, on a different quick-plan (260922 vs 148) but the same coordinator
    identity pattern (SEED-035 story 1) — reinforcing that the documented
    `.claude/skills` path remains the first thing checked, with
    `.agents/skills` never consulted even though this entry already names it
    as the fix.
