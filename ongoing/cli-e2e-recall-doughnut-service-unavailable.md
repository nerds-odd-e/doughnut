# CLI E2E: `cli_recall` “Doughnut service is not available” (GitHub Actions)

## Symptom

Interactive scenarios (e.g. **Recall Just Review**, long session scenario) failed: assertions expected note text (e.g. `sedation`) in **Current guidance**, but the transcript showed:

`Doughnut service is not available. Check DOUGHNUT_API_BASE_URL and ensure the service is running.`

That message is the **generic** branch in `userVisibleMessageForSdkThrowable` when the caught error is not classified as transport-only, 401, or 403 — including many API error bodies thrown under `throwOnError: true`.

## Why this is a different failure from `cli_access_token`

- **Access token** failure: wrong **expected string** in Gherkin after copy/401-mapping changed.
- **Recall** failure: **runtime behavior** after SDK `throwOnError: true` + backend returning a **non-OK** response for `askAQuestion` when OpenAI is disabled in E2E (`@disableOpenAiService`). The URL and backend were fine; the failure was **misleading copy** masking an application error from the question endpoint.

## Journey to root cause

1. **Rule out “CLI can’t reach backend” first**  
   Same scenario Background runs `/add-access-token` via a separate CLI process; if the API base URL were wrong, token setup would also fail. The error appeared specifically after `/recall` during “Loading recall questions.”

2. **Pattern in failing vs passing scenarios**  
   Scenarios with **`@usingMockedOpenAiService`** (MCQ paths) passed. Scenarios with **`@disableOpenAiService`** that need a **non-MCQ** recall path (just review, spelling, multi-note session) failed. **Recall status** (`/recall-status`, only `recalling` API) still passed.

   That pointed to the chain inside `recallNext` **after** `RecallsController.recalling` — i.e. **`MemoryTrackerController.askAQuestion`**, then optionally `showMemoryTracker`.

3. **Backend logs**  
   `backend/logs/doughnut-e2e.log` during a failing run showed repeated:

   `OpenAiNotAvailableException: OpenAI is not available (no API key configured).`

   So for notes **without** spelling-only / existing prompt, `askAQuestion` still hits code paths that need OpenAI; when the service is disabled for E2E, Spring resolves that as a **non-2xx** API response, not `200` with `null` body.

4. **Why old CLI “worked”**  
   Before **`throwOnError: true`**, the generated client returned an **error envelope** instead of throwing. `recallNext` treated missing `data` like “no question” and **fell through** to `showMemoryTracker` → **just review** UI. That matched what the E2E scenarios expect when OpenAI is off.

   After **`throwOnError: true`**, the same backend response **throws**. `withBackendClient` wraps it as `Error(userVisibleMessageForSdkThrowable(e))`, which often becomes the generic “Doughnut service is not available…” line — wrong for this case.

5. **Dead end: `127.0.0.1` vs `localhost`**  
   A prior hypothesis was Linux resolving `localhost` → `::1` while Tomcat listens IPv4-only. Trying to force `127.0.0.1` only for CLI env broke or confused local runs and was **not** the real recall bug; backend logs already showed **OpenAI** exceptions, not connection refused.

## Fix

In `cli/src/recall.ts`, wrap **`askAQuestion`** in try/catch:

- On success: keep existing behavior (`recallQuestionForTerminal` → MCQ/spelling or fall through).
- On failure: treat like **no question** (same as old envelope with no data), then continue to **`showMemoryTracker`** for just review — **unless** the error is a **user abort** (`isFetchAbortedByCaller`), which must still propagate for Esc during fetch-wait.

Rebuild bundle for CI (`pnpm cli:bundle` / `bundle:all`).

## Files touched

- `cli/src/recall.ts` — resilient `askAQuestion` handling + import `isFetchAbortedByCaller`.
