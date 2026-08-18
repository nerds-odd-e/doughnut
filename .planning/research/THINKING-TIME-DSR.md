# Thinking time as a DSR memory-state input

**Researched:** 2026-08-18
**Domain:** Open FSRS-6 / DSR vs response latency
**Confidence:** HIGH (FSRS/Anki/SuperMemo docs); MEDIUM (ACT-R as nearby contrast)

## User Constraints

Product question (discuss/ADR 0003): keep thinking time as a Stability input on ordinary Good, or drop it from memory-state (keep on answers/stats only). No CONTEXT.md. Deferred: E4 fitting. No implementation advice in this note.

## Summary

Open FSRS and SuperMemo DSR treat memory-state updates as functions of grade G, elapsed time t, and D/S/R. Response/thinking time is recorded for stats and (in Anki) optional retention-workload math — not as a DSR variable. Using RT to change next Stability is a persisted state mutation using a non-model signal; it is closer to rejected interval fuzz (due spreading) than to an FSRS memory update, and it is stronger than fuzz because later SInc inherits S.

**Primary recommendation:** RT is not a valid FSRS/DSR memory-state input; keep it on answers/stats only.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| DSR update (D, S, R from G, t) | API / Backend | Database / Storage | Memory state is server-persisted; R is computed |
| Thinking time capture | Browser / Client | API / Backend | Measured at the prompt; stored on Answer |
| Due (`lastRecalledAt + I(0.9,S)`) | API / Backend | — | Follows S; no fuzz |

## Key findings

1. **Open FSRS / Anki FSRS memory-state inputs are G, t, D, S, R only.** Memory state is Difficulty D, Stability S, and Retrievability R. R is computed from elapsed t and S (`R(t,S)`), not stored. Long-term recall uses `S'_r(D,S,R,G)`; post-lapse uses `S'_f(D,S,R)`; same-day success uses `S'(S,G)`. Grade G is Again=1, Hard=2, Good=3, Easy=4. ts-fsrs `next_state(memory_state, t, g, r?)` has no duration argument. Anki FSRS FAQ Q5: FSRS does not use time spent reviewing a card — only interval lengths and grades. Duration appears in optional “Compute minimum recommended retention,” not in D/S. [VERIFIED: awesome-fsrs wiki The Algorithm, ABC of FSRS; ts-fsrs FSRS.next_state / FSRSState; Anki FSRS FAQ Q5]

2. **Published FSRS and SuperMemo do not feed latency into Stability.** SuperMemo FAQ (Siwczyk, 1997): response time does not influence the next interval; the timer is for average RT and Workload. SM-17/19 SInc is a function of D, S, R. Hesitation is a discrete grade (Pass vs Good vs Great) — already G, not a continuous seconds overlay. Nearby ACT-R / Pavlik models treat latency as an *output* of activation (`RT ≈ F·e^(−A)+C`), not an input that updates FSRS-style Stability. [CITED: super-memory.com algorithm FAQ; supermemo.guru Algorithm SM-19; Pavlik & Anderson 2005/2008]

3. **RT→S is a scheduler shuffle written into memory state, not an FSRS memory update.** Open FSRS fuzz (`apply_fuzz` / `enable_fuzz`) jitters scheduled interval I after S is computed; it does not change D or S. Doughnut already rejected fuzz because hour-precision due already spreads clumps. Scaling next S by `sqrt(|thinkingTimeSeconds - 25|) / 10` (clamped; must not invert Good) writes into persisted Stability. That uses a signal FSRS does not treat as a DSR input. It is stronger than fuzz: later SInc depends on S, so the RT jitter compounds. Functionally it still spreads dues of otherwise-identical Goods — the same job fuzz had. [VERIFIED: ts-fsrs apply_fuzz; ADR 0003 Fuzz; ForgettingCurve.adjustForThinkingTime]

4. **Same-minute Goods would share next S without RT.** Two items with the same D and S, graded Good within the same minute, share whole-hour t, G=3, and R. Open FSRS (and Doughnut without the overlay) yield the same next S and due (`lastRecalledAt + I(0.9,S)`). An RT overlay is the only reason those dues would differ. [VERIFIED: ADR 0003 whole-hour elapsed + no fuzz; FSRS-6 S'_r]

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Encode hesitation in S | Continuous RT overlay on Good | Discrete G (Hard/Good/Easy) | FSRS and SuperMemo already put effort in G |
| Spread same-hour dues | Mutate S from RT | Due from `lastRecalledAt + I` (already hour-anchored) | Fuzz-class shuffle; Doughnut rejected fuzz |

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | No later SuperMemo (SM-18/19) quietly added RT as an SInc input | Finding 2 | Would reopen RT as DSR-adjacent; SM-19 page still describes SInc(D,S,R) only |

## Open Questions

None that block the product question. Humans still own accept of ADR 0003; this note only addresses whether RT belongs in DSR.

## Sources

### Primary (HIGH)
- https://github.com/open-spaced-repetition/awesome-fsrs/wiki/The-Algorithm — FSRS-6 formulas; S'_r(D,S,R,G); no RT
- https://github.com/open-spaced-repetition/awesome-fsrs/wiki/ABC-of-FSRS — three-component DSR memory state
- https://open-spaced-repetition.github.io/ts-fsrs/classes/FSRS.html — `next_state(state, t, g, r?)`; `apply_fuzz` on I
- https://anki.mintlify.app/faqs/frequently-asked-questions-about-fsrs — Q5: no review-duration in FSRS
- http://super-memory.com/archive/help16/faq/algorithm-sm15.htm — “response time does not matter”
- https://supermemo.guru/wiki/Algorithm_SM-19 — SInc from D, S, R
- ADR 0003 Decision (Fuzz, Thinking time, whole-hour t); `ForgettingCurve.java` overlay

### Secondary (MEDIUM)
- Pavlik & Anderson (2005/2008) — ACT-R latency as output of activation, not FSRS S input

## Metadata

**Confidence:** Standard stack N/A (no new libraries). Architecture HIGH. Pitfalls HIGH.
**Research date:** 2026-08-18
**Valid until:** 2026-09-17
