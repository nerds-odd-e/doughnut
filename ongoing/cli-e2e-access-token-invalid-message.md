# CLI E2E: “Add invalid access token” assertion failure (GitHub Actions)

## Symptom

`cli_access_token.feature` scenario **Add invalid access token** failed on CI with:

- **Expected:** substring `Token is invalid or expired.` in non-interactive stdout.
- **Actual:** `Access token is invalid or expired. Run doughnut login or add a new token with /add-access-token.`

## Why this is a different failure from `cli_recall`

This is purely a **user-visible string + E2E expectation drift**. The CLI still rejects invalid tokens; only the wording and classification path changed. No backend OpenAI or recall path involved.

## Journey to root cause

1. **Trace the message in code**  
   Grep showed the old literal `Token is invalid or expired.` no longer exists in `cli/`. Invalid-token handling now goes through `withBackendClient` → `userVisibleMessageForSdkThrowable` in `accessToken.ts`, which maps HTTP **401** to `http401StoredTokenRejected`: *“Access token is invalid or expired. Run doughnut login …”*.

2. **Why CI showed the new string but local E2E briefly looked “old”**  
   E2E on CI sets `CI=1`, so Cypress runs the **bundled** CLI (`cli/dist/doughnut-cli.bundle.mjs`). Locally, if `CI=1` is also set in the shell, the same bundle is used. A **stale bundle** still contained `throw new Error("Token is invalid or expired.")` from before the refactor, so a local run could print the old text until `pnpm cli:bundle` was run again.

3. **Direct CLI check**  
   Running `pnpm -C cli exec tsx src/index.ts -- -c "/add-access-token …"` against a real backend showed the **current** source message (the long “Access token is invalid…” line), matching CI.

4. **Conclusion**  
   The feature file was asserting the **pre-refactor** copy. The implementation is intentional (401 → clearer guidance including login / `/add-access-token`).

## Fix

- Update `e2e_test/features/cli/cli_access_token.feature` to assert a stable substring of the new message, e.g. `Access token is invalid or expired` (without requiring the full sentence).
- Ensure CI (and anyone with `CI=1` locally) runs **`pnpm bundle:all` / `pnpm cli:bundle`** before Cypress so the bundle matches `cli/src` (already part of the workflow’s `build` step).

## Files touched

- `e2e_test/features/cli/cli_access_token.feature` — assertion text only.
