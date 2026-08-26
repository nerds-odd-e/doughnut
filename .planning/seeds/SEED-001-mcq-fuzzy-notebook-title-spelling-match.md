---
id: SEED-001
status: dormant
planted: 2026-08-03
planted_during: between milestones (after v1.1 Spelling Answer Match & Link)
trigger_when: when relevant
scope: unknown
---

# SEED-001: MCQ / fuzzy / Notebook:Title spelling match — resume after current detour

## Why This Matters

_To be filled in. Run `/gsd-capture --seed --enrich SEED-001` to add context._

Deferred follow-ons from shipped v1.1 spelling accidental-match + overlap loop (listed in `.planning/PROJECT.md` Next Milestone Goals / Out of Scope).

## When to Surface

**Trigger:** when relevant

This seed will surface during `/gsd-new-milestone` when the milestone scope matches.

Intended pause: resume after the current detour milestone completes (or when consciously choosing spelling follow-ons as the next milestone).

## Scope Estimate

**Unknown** — run `/gsd-capture --seed --enrich SEED-001` to estimate effort.

Likely a full milestone covering some or all of: MCQ accidental-match, fuzzy/partial/substring matching, cross-notebook `Notebook:Title` typing.

## Breadcrumbs

- `.planning/PROJECT.md` — Next Milestone Goals / Out of Scope (MCQ, fuzzy, `Notebook:Title`)
- `.planning/milestones/v1.1-ROADMAP.md` — deferred v2 line: MCQ accidental-match, fuzzy matching, Notebook:Title typing
- `.planning/MILESTONES.md` — v1.1 shipped record
- `backend/src/main/java/com/odde/donut/entities/AnswerOutcome.java`
- `backend/src/main/java/com/odde/donut/entities/Answer.java`
- `backend/src/main/java/com/odde/donut/controllers/dto/AnsweredQuestion.java`
- `backend/src/main/java/com/odde/donut/services/MemoryTrackerService.java`
- `e2e_test/step_definitions/recall.ts`

## Notes

_Captured via one-shot seed capture while pausing spelling follow-ons to pursue a different direction. Enrich with trigger, why, and scope at your convenience._
