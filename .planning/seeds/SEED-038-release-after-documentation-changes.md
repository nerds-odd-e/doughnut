---
id: SEED-038
status: dormant
planted: 2026-09-23
planted_during: owner request to skip documentation CI without preventing tagged releases
trigger_when: documentation and planning pushes waste CI builds
scope: small
---

# SEED-038: Release after documentation changes without another build

## Why This Matters

Maintainers want pushes confined to `.planning/` and `docs/` to create no CI
workflow while retaining application releases triggered by stable version tags.
Release admission currently requires CI artifacts from the exact tagged commit.

## Alternatives and Decision

The owner selected reuse of a successful ancestor build when only ignored paths
differ. Tagging the earlier tested commit is simpler but prevents selecting the
documentation-updated commit. Rebuilding for every release loses the requested
saving. The workflow YAML remains the authoritative ignored-path policy.

## Story Decomposition

<a id="story-1"></a>

### Release a documentation-only revision using its tested application artifacts

**Identity:** SEED-038#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../quick/014-release-after-documentation-changes/PLAN.md"}
```

**Goal:** A maintainer can push documentation or planning changes without CI and
release that revision using an applicable successful ancestor's artifacts.

**Scope:** Skip branch CI only for the two ignored folders. Reuse CI only after
proving ancestry and identical files outside the tagged revision's ignore policy.
Keep the release tag/SHA distinct from the artifact source SHA/run/attempt.
Preserve failed/pending CI gates, immutable release identities, payload validation,
and explicit recovery when CI or retained artifacts are unavailable. Do not
automatically retry failed CI or publish an untested application. OpenDO owns the
shared watcher compatibility update; no local fork of those skills.

**Key examples:**
- Successful main CI followed by changes only in either/both ignored folders:
  no new branch CI; tagging the new revision selects the ancestor's artifacts.
- Application/workflow changes, non-ancestor CI, or a failed latest applicable
  attempt: do not borrow an unrelated older success.
- Successful exact-SHA CI remains preferred; missing artifacts stop publication
  and retain the source run identity for explicit recovery.

**Effort hypothesis:** S (30–60 minutes), medium confidence; existing isolated
Git/HTTP release-command tests cover most infrastructure.
**Depends on:** none for application release; watcher compatibility is upstream.
**Safe stopping point:** artifact reuse can ship before enabling CI filtering.

## Open Decisions

None. The owner authorized implementation and publication to main.
