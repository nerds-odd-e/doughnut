/**
 * Simulates completing Google OAuth in CLI E2E: when PTY output contains an accounts.google.com URL,
 * fetches redirect_uri with a mock authorization code.
 */

import type { IPty } from '@lydell/node-pty'

const GOOGLE_ACCOUNTS_URL_PATTERN = /https:\/\/accounts\.google\.com\/[^\s]+/
const E2E_MOCK_OAUTH_CODE = 'e2e_mock_auth_code'

type OAuthSimulationState = { done: boolean }

function notifyOAuthSimulationIfNeeded(
  fullStdout: string,
  state: OAuthSimulationState
): void {
  if (state.done) return
  const authMatch = fullStdout.match(GOOGLE_ACCOUNTS_URL_PATTERN)
  if (!authMatch) return
  const redirectUri = new URL(authMatch[0]).searchParams.get('redirect_uri')
  if (!redirectUri) return
  state.done = true
  fetch(`${redirectUri}?code=${E2E_MOCK_OAUTH_CODE}`).catch(() => {
    /* ignore OAuth callback errors */
  })
}

export type CliInteractivePtySessionForOAuth = {
  pty: IPty
  buf: { text: string }
}

/**
 * Registers a second `onData` handler that completes Google OAuth by `fetch`ing the redirect with a mock code.
 *
 * **Call at most once per PTY / per scenario** (after the interactive session starts, before steps that print the OAuth URL).
 * Calling again on the same live session stacks listeners and can fire duplicate callbacks; there is no unsubscribe.
 */
export function attachGoogleOAuthSimulation(
  session: CliInteractivePtySessionForOAuth
): void {
  const oauthState: OAuthSimulationState = { done: false }
  session.pty.onData(() => {
    notifyOAuthSimulationIfNeeded(session.buf.text, oauthState)
  })
}
