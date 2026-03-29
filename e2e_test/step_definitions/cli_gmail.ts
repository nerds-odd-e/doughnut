import { Given } from '@badeball/cypress-cucumber-preprocessor'
import { mock_services } from '../start'

Given(
  'the interactive CLI has Google OAuth callback simulation enabled',
  () => {
    cy.task('cliInteractivePtyEnableGoogleOAuthSimulation')
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
