# Superseded: Restart primary SUT only with proven process ownership

Status: superseded by explicit user direction on 2026-09-12; not executable.

The backlog item has been repurposed as
[Run E2E tests with automatic service startup and cleanup](../../seeds/SEED-015-concurrent-worktree-environments.md#story-8).
That seed section owns the current goal, scope, examples, and open decisions.
No executable plan for the replacement story has been authorized.

The old correction would have authenticated primary SUT listeners before
restart. The user instead selected removal of standalone SUT lifecycle controls
in favor of runner-owned startup and cleanup. Its one-slice implementation and
5–8 minute estimate no longer apply. The original ownership risk must be removed
with the obsolete public restart path; killing listeners by port is not an
acceptable replacement runner behavior.

Historical provenance: retrospective of SEED-017 Story 4 / quick/103,
before-cleanup commit `a32e908380`; execution commits
`7c664f3e713bfaa9897ef0c2be3445a63feb3ef0` and
`1be32355f5e5f5188707a22da6ec19b301c79617`.
The unsafe primary port-signalling path predated those changes (`3beee1da81`).
Recover the original unexecuted plan from Git history when needed.
