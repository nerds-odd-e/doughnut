# Publish the preparation assignment

While a queued story is being prepared, others see it as **Preparing** and
which agent holds it, from an assignment published on the remote target. This
reference starts, keeps, and ends that assignment. It applies inside
[prepare records in an owned workspace](preparation-workspace.md), with the
paths and target recorded in its
[Select or reuse the workspace](preparation-workspace.md#select-or-reuse-the-workspace),
and inside [decide, publish, or discard the written
result](preparation-disposition.md). Run every command below from this
project's installed `dough-story-refinement` skill directory (normally under
`.agents/skills/` or `.claude/skills/`).

## Announce the preparation assignment

Story refinement, slice planning, and plan refinement of an existing queued
story with a stable identity announce that story as **Preparing** before
substantive work. Reading, discussing, answering questions, decomposing a
candidate without a queued identity, bug triage, and a standalone
retrospective record announce nothing.

After selecting the workspace and before its first record write, run:

```text
node <installed>/scripts/preparation-assignment.mjs start \
  --integration <integration checkout> --workspace <owned workspace> \
  --identity <queued story identity> --remote <remote> --target <trunk branch> \
  --push-authorized [--host claude|codex|cursor] [--model <model>] \
  [--declared-owner <id> --requester <id>]
```

Use the recorded paths and target. Supply `--push-authorized` only when
publishing to that target is authorized; without it the command stops. Supply
your own host and model, omitting either you cannot state rather than guessing.
Supply `--declared-owner` and a matching `--requester` only when access to the
default checkout has actually been established.

Keep the receipt with this session and act on its `status`:

- `announced`: remote trunk accepted a commit that adds only your assignment
  profile, under the next free name of the rotation execution also uses. The
  queue and your draft are unchanged. The command then attempts the same safe
  refresh of the integration checkout as Dough Land's
  [Refresh the default checkout](../../dough-land/SKILL.md#refresh-the-default-checkout)
  and reports it in `refresh`. A deferred or stopped refresh preserves that
  checkout without undoing the announcement. Begin preparing in the workspace.
- `continued`: this workspace already holds the story's published assignment;
  nothing new is published. Run `start` at each preparation skill's first write
  in the session, and again on resuming after a pause, so slice planning after
  refinement or a resumed session keeps the same assignment instead of taking
  another name.
- Any stop (`ok: false`): do not begin substantive preparation. Report the
  receipt and preserve the workspace. `unpublished`: remote trunk did not
  accept an announcement, so nothing is assigned; when the receipt carries a
  `candidateSha`, acceptance could not be checked, so rerun `start` in the same
  workspace, which settles it from the remote instead of announcing twice.
  `workspace-not-isolated`: the workspace already holds edits or unpublished
  commits; the announcement comes before the first write and never carries a
  draft. `workspace-assigned-elsewhere`: this workspace still holds a published
  assignment the request does not name, such as another story's; keep or
  abandon that preparation first.
  `agent-unavailable`: every name is held; report its `occupied` list (each
  profile's agent, story, activity, and allocation, or why a file is
  unrecognized) so developers can see who holds what. Never remove, reclaim,
  or wait out another developer's profile: its age, or no process running
  for it on this machine, does not free it. `not-queued`: the story is not
  queued on the fetched target.

The workspace remembers which announcement is its own. Later `start`,
`release`, and `abandon` commands identify the assignment from it, never from
the agent name, story, or tool and model alone, so keep every later command
for this assignment in the same workspace.

An explicit developer instruction not to publish or commit means: do not run
`start`; report that no Preparing assignment was published, so others cannot
see this preparation; continue only as that instruction allows.

A pause keeps the published assignment, however long it lasts. It ends only
when the kept result lands, under
[Release it with the kept result](#release-it-with-the-kept-result), or on an
explicit abandonment, under [Abandon the preparation](#abandon-the-preparation).

## Release it with the kept result

After a validated explicit keep instruction, per
[Keep and publish the retained result](preparation-disposition.md#keep-and-publish-the-retained-result),
stage the release in the owned workspace before landing:

```text
node <installed>/scripts/preparation-assignment.mjs release \
  --workspace <owned workspace> --identity <queued story identity> \
  --remote <remote> --target <trunk branch>
```

`release-staged` means the workspace now stages removal of exactly the profile
its own announcement added, verified against the fetched remote target, beside
the retained result. The landing then publishes result and release in one
snapshot, so no reader sees the result landed while the assignment stays
active. Any stop leaves both unchanged: report it and do not land.
`already-released` means trunk already ended this assignment (its `endedBy`
names the commit); there is nothing to stage, and the landing proceeds without
it. The story stays queued, and its refinement, approach, and readiness are
what the
[preparation recorder](../../dough-product-backlog/references/record-preparation.md)
wrote, never something the release implies.

When the landing stopped before the remote target accepted it, or its push
ended without a clear answer, rerun `release`, which reads trunk again, before
rerunning that landing:

- `release-staged` with `staged: "already-committed"`: the release waits in
  the unpublished landing commit; rerun the landing.
- `already-released`: trunk already contains the release, normally because
  the interrupted landing was accepted. Rerun the landing, which pushes
  nothing already accepted and continues with refresh and retirement.
- `release-conflict` with a `successor`: this assignment already ended and
  trunk now holds a later allocation of the same name, possibly for the same
  story. Landing the removal would end that other assignment. Do not land;
  report the conflict. The removal must come out of the workspace's
  unpublished changes and commits first, which is the developer's decision.

## Abandon the preparation

Apply this section only after an explicit instruction to abandon preparing
the story, per
[Decide what happens to the written result](preparation-disposition.md#decide-what-happens-to-the-written-result).
Pausing, going quiet, a finished or failed session, a discarded draft, a
leave-unpublished instruction, or an assignment's age is never an
abandonment. When publishing to the recorded target is not authorized, or the
developer said not to publish, do not abandon: report that the assignment
stays published and visible as Preparing.

From the owned workspace that announced it, run:

```text
node <installed>/scripts/preparation-assignment.mjs abandon \
  --integration <integration checkout> --workspace <owned workspace> \
  --identity <queued story identity> --remote <remote> --target <trunk branch> \
  --push-authorized [--declared-owner <id> --requester <id>]
```

It publishes a commit on the fetched target that only removes this
workspace's own assignment profile, and leaves the workspace's files, index,
and commits exactly as they were. The story stays queued. Act on `status`:

- `abandoned`: remote trunk accepted the end at `publishedSha`. `refresh`
  reports the integration checkout separately, as for the announcement.
- `already-released`: trunk had already ended this assignment (`endedBy`),
  for example on a repeated request; nothing was published. A `successor`
  is a later allocation of the same name, which is someone else's and stays.
- `unpublished`: the remote did not accept the end; the assignment is still
  published. `unconfirmed`: whether it was accepted is unknown. Report either
  as such, and rerun the same command when the remote is reachable; it rereads
  trunk and never ends anything twice.
- `no-assignment`: this workspace holds no published assignment for the story.

Afterwards, the draft stays in the workspace for a later keep or discard
decision. A keep then lands without an assignment: `release` reports
`already-released`. Preparing the story again announces anew from a clean
workspace. The workspace may be retired under
[Close or retain the workspace](preparation-workspace.md#close-or-retain-the-workspace)
once a disposition for its draft is confirmed.
