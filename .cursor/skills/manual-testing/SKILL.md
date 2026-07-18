---
name: manual-testing
description: >-
  Browser-based manual testing of the doughnut web app. Use ONLY when the
  developer explicitly asks to test manually or when a plan phase includes
  manual testing. Do NOT use proactively.
---

<objective>
Manually verify doughnut web-app behavior in a browser using MCP browser tools.

Purpose: Human-directed or plan-mandated exploratory testing — not a default
agent workflow.

Output: Verified behavior report + summary ending with
`## MANUAL TEST COMPLETE`.
</objective>

<context>
**Use only when:** the developer explicitly asks for manual testing, or a plan
phase includes it. **Do not use proactively.**

**Prerequisites:** Assume `pnpm sut` is already running (backend, frontend,
Mountebank — all auto-reload on code changes). If not running, suggest the user
start it in a separate terminal. To use AI services, set `OPENAI_API_TOKEN`
before `pnpm sut`.

**Access points:**
- Frontend: http://localhost:5173/
- Backend API: http://localhost:9081

**Test accounts:**
- `old_learner` / `password`
- `another_old_learner` / `password`
- `admin` / `password`

**Linting:** The frontend dev server runs linting in the background. Errors
appear in terminal output (overlays disabled). Check terminal and fix errors
before committing.
</context>

<process>

<step name="navigate_and_snapshot">
1. `browser_navigate` to http://localhost:5173/
2. `browser_snapshot` to understand current state
</step>

<step name="interact_and_verify">
3. Interact: `browser_click`, `browser_type`, `browser_select_option`
4. `browser_snapshot` to verify result
5. `browser_console_messages` / `browser_network_requests` if issues occur
</step>

<step name="login_flow">
1. Navigate to the app URL
2. Click "Login via Github" (redirects to username/password form)
3. Fill `id="username"` and `id="password"`
4. Click `id="login-button"`
</step>

<step name="find_elements">
- Prefer `id` or `aria-label` attributes
- Fall back to element text/role
- Use `browser_snapshot` to see available elements and refs
</step>

</process>

<success_criteria>
- `pnpm sut` running (or user notified to start it)
- Target flows exercised via browser MCP tools
- Results verified with snapshots (and console/network if issues)
- Final output includes `## MANUAL TEST COMPLETE`
</success_criteria>

<output>
Report what was tested, observed results, and any issues found, then:

```
## MANUAL TEST COMPLETE
```
</output>

<out_of_scope>
- Do not run proactively without explicit developer or plan request.
- Do not substitute manual testing for automated E2E when the plan calls for
  Cypress.
</out_of_scope>
