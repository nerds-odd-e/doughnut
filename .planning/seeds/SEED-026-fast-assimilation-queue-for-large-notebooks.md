---
id: SEED-026
status: dormant
planted: 2026-09-18
planted_during: product backlog capture from owner observation of slow next-to-assimilate retrieval
trigger_when: learners with large notebooks wait noticeably for the next item to assimilate
scope: medium
---

# SEED-026: Get the next item to assimilate quickly for large notebooks

## Why This Matters

Learners with large notebooks wait a noticeable time whenever Donut finds the
next note or property to assimilate. The wait recurs on every assimilation step,
so it slows the whole assimilation session. Owner observation, 2026-09-18; no
timed profile has been captured yet.

## Alternatives and Decision

Profile the retrieval first, then optimize the measured dominant cost. Static
inspection alone cannot establish runtime importance. Do not add caching or
background precomputation before a measurement shows the synchronous query
cannot be made fast enough.

## Story Decomposition

<a id="story-1"></a>

### 1. Profile and optimize getting the assimilation queue for large notebooks

- **For / why:** Learners with large notebooks need the next item to assimilate
  to appear without a noticeable wait.
- **Evaluation:** A reproducible measurement of next-item retrieval on a large
  notebook before and after the change, showing a stated reduction, with
  existing assimilation ordering and skipping behavior preserved.
- **Value / learning:** Removes a recurring wait from every assimilation step
  and reveals where the retrieval cost lives.
- **Effort hypothesis:** M — low confidence until profiled.
- **Depends on:** none.
- **Safe stopping point:** A retained profile with the dominant cost identified
  is useful on its own even if optimization is deferred.

Captured only; goal, scale, and acceptable waiting time are unrefined. Route to
[dough-story-refinement](../../.claude/skills/dough-story-refinement/SKILL.md)
before slice planning.

## Ordering and Scope Reduction

One story. Drop optimization before dropping the profile.

## Open Decisions

None that change story selection.

## When to Surface

When a learner reports or the owner observes slow next-item retrieval during
assimilation, or when assimilation retrieval code changes.

## Breadcrumbs

- `backend/src/main/java/com/odde/donut/controllers/AssimilationController.java` — `GET /api/assimilation/next`
- `backend/src/main/java/com/odde/donut/services/AssimilationService.java`,
  `UnassimilatedNoteUnitSource.java`, `UnassimilatedPropertyUnitSource.java`
- [SEED-018](SEED-018-publish-large-authored-notebooks.md) — prior
  profile-then-optimize approach and profiling infrastructure
