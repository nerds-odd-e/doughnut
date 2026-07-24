---
phase: 2
slug: accidental-match-grading-penalty
status: verified
threats_open: 0
asvs_level: 1
created: 2026-07-24
---

# Phase 2 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|----------------|
| User → POST /api/recall-prompts/{id}/answer-spelling | Spelling answer used as title/alias lookup key | User input string |
| Wider lookup → matchedNoteId in response | Accidental-match note id returned without navigation | Note id existence (IDOR surface) |

---

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-2-01 | Information Disclosure (IDOR) | WikiLinkResolver.findAccidentalMatch → Answer.matchedNoteId | medium | mitigate | `userMayReadNotebook` filter + reviewed-note exclusion; `shouldNotLeakMatchedNoteIdFromUnreadableNotebook` | closed |
| T-2-02 | Spoofing / Elevation | RecallPromptController.answerSpelling | low | accept | No new endpoint; existing `assertCanMutateRecallPrompt` | closed |
| T-2-03 | Tampering (SQL injection) | NoteRepository / NoteAliasIndexRepository | low | accept | Parameterized JPQL bind params | closed |
| T-2-04 | Information Disclosure (non-accidental paths) | MemoryTrackerService.answerSpelling | low | accept | Populate matchedNoteId/outcome only on accidental-match branch | closed |
| T-2-05 | Information Disclosure (IDOR via alias leg) | WikiLinkResolver alias leg | medium | mitigate | Same `firstReadableAccidentalCandidate` / `userMayReadNotebook` filter | closed |
| T-2-06 | Tampering (SQL injection alias key) | NoteAliasIndexRepository | low | accept | Parameterized JPQL + normalizedLookupKey | closed |
| T-2-07 | Information Disclosure (stale alias index) | NoteAliasIndexRepository | low | accept | Pre-existing assumption A1 | closed |
| T-2-SC | Tampering | package installs | low | accept | No new packages in Phase 2 | closed |

*Status: open · closed · open — below high threshold (non-blocking)*
*Severity: critical > high > medium > low — only open threats at or above workflow.security_block_on count toward threats_open*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-2-02 | T-2-02 | No new endpoint; existing auth gate reused | plan threat_model | 2026-07-24 |
| AR-2-03 | T-2-03 | Parameterized JPQL only | plan threat_model | 2026-07-24 |
| AR-2-04 | T-2-04 | Fields null outside accidental-match branch | plan threat_model | 2026-07-24 |
| AR-2-06 | T-2-06 | Parameterized alias lookup | plan threat_model | 2026-07-24 |
| AR-2-07 | T-2-07 | Stale alias index is pre-existing (A1) | plan threat_model | 2026-07-24 |
| AR-2-SC | T-2-SC | No new packages | plan threat_model | 2026-07-24 |

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-07-24 | 8 | 8 | 0 | gsd-security-auditor (verify-work post) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-07-24
