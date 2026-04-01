import { Given } from '@badeball/cypress-cucumber-preprocessor'
import { mock_services } from '../start'
import { cli } from '../start/pageObjects/cli'

Given(
  'the interactive CLI has Google OAuth callback simulation enabled',
  () => {
    cli.ttyAssertTerminal().enableGoogleOAuthSimulation()
  }
)

const GMAIL_E2E_ACCESS_TOKEN = 'e2e-gmail-access-token'
const GMAIL_E2E_REFRESH_TOKEN = 'e2e-gmail-refresh-token'

Given(
  'the Google API mock returns tokens and profile for {string}',
  (email: string) => {
    const google = mock_services.google()
    cy.wrap(null).then(async () => {
      await google.stubTokenExchange(
        GMAIL_E2E_ACCESS_TOKEN,
        GMAIL_E2E_REFRESH_TOKEN
      )
      await google.stubGmailProfile(email)
    })
  }
)

Given(
  'the Google API mock returns messages and message {string} with subject {string}',
  (messageId: string, subject: string) => {
    const google = mock_services.google()
    cy.wrap(null).then(async () => {
      await google.stubGmailMessages([{ id: messageId }])
      await google.stubGmailMessage(messageId, subject)
    })
  }
)
