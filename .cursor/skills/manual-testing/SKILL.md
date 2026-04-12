---
name: manual-testing
description: >-
  Browser-based manual testing of the doughnut web app. Use ONLY when the
  developer explicitly asks to test manually or when a plan phase includes
  manual testing. Do NOT use proactively.
---

# Manual Testing

## Prerequisites

Assume `pnpm sut` is already running (backend, frontend, Mountebank — all auto-reload on code changes).

If not running, suggest the user start it in a separate terminal.

To use AI services, set `OPENAI_API_TOKEN` before `pnpm sut`.

## Access Points

- Frontend: http://localhost:5173/
- Backend API: http://localhost:9081

## Test Accounts

- `old_learner` / `password`
- `another_old_learner` / `password`
- `admin` / `password`

## Browser Tool Workflow

1. `browser_navigate` to http://localhost:5173/
2. `browser_snapshot` to understand current state
3. Interact: `browser_click`, `browser_type`, `browser_select_option`
4. `browser_snapshot` to verify result
5. `browser_console_messages` / `browser_network_requests` if issues occur

## Login Flow

1. Navigate to the app URL
2. Click "Login via Github" (redirects to username/password form)
3. Fill `id="username"` and `id="password"`
4. Click `id="login-button"`

## Finding UI Elements

- Prefer `id` or `aria-label` attributes
- Fall back to element text/role
- Use `browser_snapshot` to see available elements and refs

## Linting Errors

The frontend dev server runs linting in the background. Errors appear in terminal output (overlays disabled). Check terminal and fix errors before committing.
