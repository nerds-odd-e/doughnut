/**
 * Shared Gmail E2E fixtures: config dir JSON and OAuth client ids baked into the CLI bundle (phase 1.3+).
 */

export const GMAIL_E2E_GOOGLE_CLIENT_ID = 'e2e-test-client'
export const GMAIL_E2E_GOOGLE_CLIENT_SECRET = 'e2e-test-secret'

export const GMAIL_E2E_OAUTH_ADD_CONFIG = {
  clientId: GMAIL_E2E_GOOGLE_CLIENT_ID,
  clientSecret: GMAIL_E2E_GOOGLE_CLIENT_SECRET,
  accounts: [],
}

export const GMAIL_E2E_MOCK_ACCOUNT_CONFIG = {
  accounts: [
    {
      email: 'e2e@gmail.com',
      accessToken: 'mock_access_token',
      refreshToken: 'mock_refresh_token',
      expiresAt: Date.now() + 3600_000,
    },
  ],
}
