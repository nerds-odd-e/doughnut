# Phase 9: Build a link from resolve dialog - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-08-05
**Phase:** 9-Build a link from resolve dialog
**Mode:** `--auto`
**Areas discussed:** Link offer hosting, Stay-on-result / offer exit, Readonly unload gates, Test coverage restore

---

## Link offer hosting

| Option | Description | Selected |
|--------|-------------|----------|
| Single Modal step swap to MatchedNoteLinkOffer (recommended) | Same Resolve Modal; never nest PopButton | ✓ |
| Nested PopButton per row inside resolve dialog | Rehosts v1.1 tree as-is | |
| New dedicated link wizard in dialog | Duplicate property/relationship UI | |

**User's choice:** [auto] Single Modal step swap to MatchedNoteLinkOffer (recommended default)
**Notes:** Pitfall 3 / ARCHITECTURE lock; Phase 7–8 already forbade nested PopButton.

[auto] Link offer hosting — Q: "How should Build a link open from the resolve dialog?" → Selected: "Single Modal step swap to MatchedNoteLinkOffer" (recommended default)

[auto] Link offer hosting — Q: "Where should list↔offer step state live?" → Selected: "AccidentalMatchResolveDialog (or thin recall host); AnsweredSpellingQuestion stays CTA+PopButton" (recommended default)

[auto] Link offer hosting — Q: "Where is the Build a link CTA?" → Selected: "Per-row on AccidentalMatchResolveRow; no Add-as-overlapped yet" (recommended default)

---

## Stay-on-result / offer exit

| Option | Description | Selected |
|--------|-------------|----------|
| Return to match list in same Modal after offer close/success (recommended) | Multi-match can continue; outer dismiss only via Modal closer | ✓ |
| Close entire resolve Modal on every offer closeDialog | Simpler; forces reopen for next match | |
| Navigate away after relationship success | Breaks AMR-06 stay-on-result | |

**User's choice:** [auto] Return to match list; keep MatchedNoteLinkOffer navigate-on-success=false
**Notes:** Offer `closeDialog` means exit offer step, not necessarily dismiss resolve.

[auto] Stay-on-result / offer exit — Q: "After Build a link succeeds or go-back?" → Selected: "Return to match list in same Modal" (recommended default)

[auto] Stay-on-result / offer exit — Q: "Reuse MatchedNoteLinkOffer?" → Selected: "Reuse as-is; adapt host only" (recommended default)

---

## Readonly unload gates

| Option | Description | Selected |
|--------|-------------|----------|
| Hide Build a link when reviewed readonly or realms unloaded (recommended) | Port v1.1 canOfferLinkToMatched-style gates | ✓ |
| Show always; fail on write | Confusing errors (Pitfall 7) | |
| Disable (greyed) instead of hide | Different from prior link CTA pattern | |

**User's choice:** [auto] Hide when readonly or unloaded; titles/path may remain
**Notes:** AMR-07; Add-as-overlapped CTA deferred to Phase 11 but same gate rules apply later.

[auto] Readonly unload gates — Q: "When is Build a link unavailable?" → Selected: "Hide when reviewed notebook readonly or required realms not loaded" (recommended default)

---

## Test coverage restore

| Option | Description | Selected |
|--------|-------------|----------|
| Vitest gates + untag @wip link E2E via page-object (recommended) | Prefer page-object over Gherkin rewrites | ✓ |
| E2E only; skip Vitest gate cases | Weaker AMR-07 evidence | |
| Rewrite Gherkin steps from scratch | Phase 7 preferred page-object changes | |

**User's choice:** [auto] Vitest + untag @wip with page-object path
**Notes:** Keep overlap E2E uncoupled.

[auto] Test coverage restore — Q: "How to restore link E2E?" → Selected: "Page-object: open Resolve then existing link helpers; untag @wip when green" (recommended default)

---

## Claude's Discretion

- Step-state enum / go-back chrome ownership
- Reviewed-realm hydrate at dialog host vs per row
- Build a link button visual density

## Deferred Ideas

- Phase 10 overlap alias util
- Phase 11 Add as overlapped note
- Phase 12 AMR-05 reopen polish
- AMR-10..13 / SEED-001
