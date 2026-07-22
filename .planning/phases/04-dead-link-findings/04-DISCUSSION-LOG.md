# Phase 4: Dead-link findings - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-22
**Phase:** 4-Dead-link findings
**Areas discussed:** Resolve authority and scan scope, Token uniqueness and finding identity, Per-note nesting shape, Implementation surface
**Mode:** `--auto` (recommended defaults selected; no interactive prompts)

---

## Resolve authority and scan scope

| Option | Description | Selected |
|--------|-------------|----------|
| Viewer-readable WikiLinkResolver + live notes + FM+body extract | Same path as editor/cache; `wikiLinkInnersInOccurrenceOrder`; one rule for body+frontmatter | ✓ |
| Any-target resolve (ignore readability) | Count unreadable-but-existing targets as live | |
| Cache-absence as dead | Treat missing wiki title cache entries as dead without live resolve | |
| Separate body vs frontmatter rule ids | Two HealthRule beans / groups | |

**User's choice:** [auto] Viewer-readable WikiLinkResolver + live notes + FM+body extract (recommended default)
**Notes:** Aligns with PITFALLS (no second resolver / cache-as-truth) and ARCHITECTURE dead-link data flow.

---

## Token uniqueness and finding identity

| Option | Description | Selected |
|--------|-------------|----------|
| Distinct unresolved inners per note; wikiLinkToken = raw inner | Dedupe preserve order; noteId + wikiLinkToken + label=inner | ✓ |
| One finding per textual occurrence | Report every repeat of the same dead token | |
| wikiLinkToken includes `[[ ]]` wrappers | Match Phase 1 fixture literal style | |
| Split body vs FM in message | Optional message tags source region | |

**User's choice:** [auto] Distinct unresolved inners per note; wikiLinkToken = raw inner (recommended default)
**Notes:** Matches `WikiLinkResolver.resolveWikiLinksForCache` dedupe; keeps resolver input identical to stored token.

---

## Per-note nesting shape

| Option | Description | Selected |
|--------|-------------|----------|
| Top group + children per note with dead tokens as items | Always emit top group; autoFixable=false; warning | ✓ |
| Flat items only (noteId on each item, no children) | Simpler DTO; weaker Phase 5 expand-by-note | |
| Emit group only when findings exist | Skip empty dead_wiki_links group | |

**User's choice:** [auto] Top group + children per note with dead tokens as items (recommended default)
**Notes:** Phase 1 reserved `children` for this; HLTH-03 expects dead links nested by note.

---

## Implementation surface

| Option | Description | Selected |
|--------|-------------|----------|
| New HealthRule bean on existing lint endpoint; backend tests only | No new API/UI; regen only if schema changes | ✓ |
| New dedicated dead-link endpoint | Separate from folder lint | |
| Add @wip E2E / Health UI now | Pull Phase 5 work forward | |

**User's choice:** [auto] New HealthRule bean on existing lint endpoint; backend tests only (recommended default)
**Notes:** Same delivery pattern as Phases 2–3.

---

## Claude's Discretion

- Batching strategy for many-note resolve
- Child group field mirroring details
- Whether label ever shows pre-`|` target only (default: full inner)

## Deferred Ideas

- Health tab UI / Run — Phase 5
- Click-through to editor — v2 HLTH-11
- Dead-link auto-fix — v2 / out of v1
- Body-vs-FM message split — rejected for v1
