---
phase: 1
slug: extend-answer-outcome-api
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-07-23
---

# Phase 1 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.
>
> **Phase 1 is a pure STRUCTURE phase:** it extends the `Answer` / `AnsweredQuestion`
> contract types and regenerates the OpenAPI client so the frontend type-checks.
> No backend behavior is wired (new states are representable but not yet returned),
> no new endpoint, no new service, no UI. Verification proves "structure only, no
> behavior" — the new fields are `@Transient`/optional and **absent** on the
> existing spelling-answer path (Jackson `NON_NULL` omits unset fields).

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework (backend)** | JUnit 5 (Spring Boot starter-test) |
| **Framework (frontend)** | Vitest 4.1.10 |
| **Config file (backend)** | `backend/build.gradle`; `@SpringBootTest @ActiveProfiles("test") @Transactional` |
| **Config file (frontend)** | `frontend/vitest.config.ts` |
| **Quick run (backend)** | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` |
| **Quick run (frontend, targeted)** | `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/QuestionDisplay.spec.ts` |
| **Full suite (frontend)** | `CURSOR_DEV=true nix develop -c pnpm frontend:test` |
| **Contract gate (regen + lint)** | `CURSOR_DEV=true nix develop -c pnpm generateTypeScript && pnpm openapi:lint` |
| **Estimated runtime** | ~45 seconds (targeted backend + frontend + regen/lint) |

---

## Sampling Rate

- **After every task commit:** Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only` + `pnpm generateTypeScript` + targeted `pnpm frontend:test` for recall components.
- **After every plan wave:** Run `CURSOR_DEV=true nix develop -c pnpm frontend:test` + `pnpm openapi:lint`.
- **Before `/gsd-verify-work`:** Full backend + frontend green, OpenAPI lint green, `types.gen.ts` contains the new fields.
- **Max feedback latency:** ~60 seconds

---

## Per-Task Verification Map

> Task IDs are seeded at the requirement level; the planner/executor refines
> `{N}-WW-TT` rows as PLAN.md tasks are finalized. "Existing (extend)" = an
> existing test file that must keep type-checking / be extended with a
> no-behavior assertion; "Wave 0" = a new check this phase must add.

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 1-01-TBD | 01 | 1 | API-01 | Information Disclosure (T-1-01) | New `Answer` fields (`matchedNoteId`, `outcome`) are `@Transient`/optional and **absent** on the existing spelling path — no matched-note data returned until Phase 2 | unit (backend) | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` (RecallPromptControllerTests) | ✅ existing (extend) | ⬜ pending |
| 1-01-TBD | 01 | 1 | API-01 | — | Generated `Answer` TS type carries the new optional fields | build/regen + grep | `CURSOR_DEV=true nix develop -c pnpm generateTypeScript` then grep `packages/generated/doughnut-backend-api/types.gen.ts` for `matchedNoteId` / `outcome` | ❌ Wave 0 (add grep check) | ⬜ pending |
| 1-01-TBD | 01 | 1 | API-02 | Information Disclosure (T-1-01) | `AnsweredQuestion` carries optional `overlap` + `matchedNotes`; **absent** on the existing path | unit (backend) | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` (RecallsControllerTests) | ✅ existing (extend) | ⬜ pending |
| 1-01-TBD | 01 | 1 | API-02 | — | Regenerated client compiles; frontend type-checks against new contract (no UI change) | build (frontend) | `CURSOR_DEV=true nix develop -c pnpm frontend:test` | ✅ existing (green) | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] Add a backend controller test asserting the new `Answer` / `AnsweredQuestion` fields are **null/absent** on the existing spelling-answer path (proves "representable but not returned" — a no-behavior assertion, not a behavior assertion).
- [ ] Add a regen-verification step: grep `packages/generated/doughnut-backend-api/types.gen.ts` for `matchedNoteId` / `outcome` / `matchedNotes` / `overlap` after `pnpm generateTypeScript`.
- [ ] No framework install needed — existing JUnit / Vitest infrastructure covers all requirements.

*If none: "Existing infrastructure covers all phase requirements." — here, existing infra covers it; Wave 0 adds two no-behavior assertions/greps.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| _none_ | — | — | — |

*All phase behaviors have automated verification. Phase 1 has no user-visible behavior (pure structure), so there is nothing to verify manually.*

---

## Security Domain

`security_enforcement: true`, ASVS level 1, block on `high`. This phase changes only response DTO/schema types — no new inputs, endpoints, auth, crypto, or persistence.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|------------------|
| V2 Authentication | no | Unchanged (existing OAuth2/Bearer). |
| V3 Session Management | no | Unchanged. |
| V4 Access Control | no | Existing endpoints keep `assertCanMutateRecallPrompt` / `assertReadAuthorization`; no new endpoint. |
| V5 Input Validation | no | No new `@RequestBody` types; `AnswerDTO`/`AnswerSpellingDTO` unchanged. New fields are response-only. |
| V6 Cryptography | no | No crypto touched. |

### Known Threat Patterns for this change

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Information exposure via new response fields | Information Disclosure | New fields are `@Transient`/optional and unset in Phase 1 (omitted via `NON_NULL`) — no matched-note data is returned until Phase 2 wires it behind existing `assertReadAuthorization`. |
| Mass-assignment of new fields | Tampering | `Answer` is response-only (no `@RequestBody` binds it); no inbound deserialization of the new fields. |

**Verdict:** No new attack surface. Phase 2 must re-check V4 (matched-note search across all readable notebooks) when it wires behavior.

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 60s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
