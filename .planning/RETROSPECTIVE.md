# Project Retrospective

*A living document updated after each milestone. Lessons feed forward into future planning.*

## Milestone: v1.2 — Clean up LIA training participant code

**Shipped:** 2026-08-03
**Phases:** 8 | **Plans:** 12

### What Was Built
- TRIAGE.md with keep/strengthen/remove for portable-workspace stories 1–6
- Strengthened `/export` (`doughnut_id`, wiki→MD, absolute attachments)
- Strengthened `/sync --dry-run` + mutating pull with gated baseline
- Strengthened OKF `/lint` portable contract
- Strengthened `/push --dry-run`; removed Story 6 mutate-push WIP
- Class-ready hygiene verify (spent docs, Terry/YS untouched, CLI green)

### What Worked
- Publish triage first (Phase 7 docs-only), then one Behavior phase per story
- Strengthen-over-rebuild for near-miss valuable CLI work (stories 1–5)
- HYG-02 as standing constraint + final Phase 14 audit instead of per-phase author archaeology
- Coarse single-tracer plans (10–14) kept commit count and wall time low

### What Was Inefficient
- Formal GSD VERIFICATION.md skipped for Phases 8–13; closeout needed override
- Formal `/gsd-audit-milestone` skipped again (same as v1.1)
- init.manager still saw "incomplete" because plan_count vs summary_count drifted on research/validation stubs

### Patterns Established
- TRIAGE.md as sole action source for keep/strengthen/remove phases
- Identity merge via textual inject into splitVerbatim fences (never Frontmatter.fenced)
- Preview taxonomy (`PreviewPullAction`) parallel to apply path; dry-run never writes baseline
- Push dry-run load-only; mutate push deferred until designed

### Key Lessons
1. For training cleanup, triage-then-strengthen beats re-implementing from scratch.
2. Remove WIP cleanly (Story 6) is a valid requirement outcome — not every story must ship product.
3. Phase 14 hygiene (spent docs + author audit + retained green matrix) is the real class-ready gate.

### Cost Observations
- Model mix: not recorded
- Sessions: single calendar day (2026-08-03)
- Notable: 12 plans / 8 phases in ~111min execution (~6h wall)

---

## Milestone: v1.1 — Spelling Answer Match & Link

**Shipped:** 2026-07-25
**Phases:** 6 | **Plans:** 16

### What Was Built
- Answer outcomes for accidental match and overlap, with matched-note topology on the OpenAPI client
- Accidental-match grading (title + alias) with a lighter SRS penalty
- Reveal + offer-link UI after accidental match (property and relationship, stay-on-page)
- Alias-as-wiki-link overlap declaration without breaking resolve/search/cloze
- Overlap try-again with no credit, durable outcome persistence, and live E2E

### What Worked
- Behavior/Structure sequencing: contract structure first, then grading, reveal, link, declaration, try-again
- Reusing `WikiLinkResolver`, `LinkInsertionChoice`, and `updateForgettingCurve` instead of new concepts
- Capability-named E2E (`accidental_match_reveal`, `overlap_try_again`) as the ship gate

### What Was Inefficient
- Alias blast radius required dedicated OVL-03 regression plans (05-02/05-03) with little production change — correct but heavy relative to the declaration seam
- Phase SUMMARY one-liners occasionally captured review noise instead of delivery (05-01)

### Patterns Established
- Plain-only soft parse (`from*`) vs authored wiki-link overlap tokens (`overlapWikiLinkTokensFrom*`)
- Third SRS outcome via `partialFail` / `markAsAccidentalMatch` without 12h override
- Recall stay-on-page via `navigateOnSuccess=false` on relationship finalize

### Key Lessons
1. Declared overlap (alias wiki-link) is safer than auto-detecting shared titles/aliases.
2. Persist grading outcomes when try-again can repeat; exclude non-wrong outcomes from wrong-count.
3. At milestone close, prune spent phase diaries — keep slim ROADMAP/REQUIREMENTS archives only.

### Cost Observations
- Model mix: not recorded
- Sessions: multi-day (2026-07-23 → 2026-07-24 execution; close 2026-07-25)
- Notable: 16 plans / 6 phases in ~2 calendar days of execution

---

## Cross-Milestone Trends

### Process Evolution

| Milestone | Sessions | Phases | Key Change |
|-----------|----------|--------|------------|
| v1.0 | multi-day | 7 | Health lint → tab → gated purge; override closeout |
| v1.1 | ~2 days exec | 6 | Spelling match/link/overlap; slim archive + spent-history cleanup |
| v1.2 | 1 day | 8 | Triage→strengthen/remove LIA CLI; coarse tracers; HYG gate |

### Cumulative Quality

| Milestone | Formal verify | Closeout |
|-----------|---------------|----------|
| v1.0 | partial (override) | override_closeout |
| v1.1 | all 6 phases passed | override_closeout (quick-task forensics + skipped audit) |
| v1.2 | Phases 7+14 only | override_closeout (skipped audit; Phases 8–13 SUMMARY+HYG-03) |

### Top Lessons (Verified Across Milestones)

1. Ship report-only / contract structure before mutating behavior.
2. Prefer capability-named E2E over phase-numbered tests.
3. Clean spent `.planning/` diaries at milestone close; keep product-useful archives only.
4. Triage-then-act beats speculative rewrite for inherited WIP.
