import { Given, When, Then } from '@badeball/cypress-cucumber-preprocessor'
import { mock_services } from '../start'
import { cli } from '../start/pageObjects/cli'

Given('the backend is serving the CLI and install script', () =>
  cli.backend().expectInstallScriptServed()
)

When('I install the CLI from localhost without affecting my system', () =>
  cli.installation().installFromLocalhost()
)
When('I run the installed doughnut command in interactive mode', () =>
  cli.installation().runInstalled()
)
When('I run the installed doughnut version command', () =>
  cli.installation().runVersion()
)
When(
  'I run the installed doughnut update command with BASE_URL from localhost',
  () => cli.installation().runUpdate()
)

When('the backend serves the CLI with version {string}', (version: string) =>
  cli.backend().serveVersion(version)
)

When(
  'I enter the slash command {string} in the interactive CLI',
  (command: string) => cli.interactive().enterSlashCommand(command)
)
When('I enter {string} in the interactive CLI', (line: string) =>
  cli.interactive().enterLine(line)
)
When('I cancel the token list with q in the interactive CLI', () =>
  cli.interactive().typeRawKey('q')
)
When('I press Enter in the interactive CLI', () =>
  cli.interactive().pressEnter()
)
When(
  'I answer {string} in the interactive CLI to prompt {string}',
  (answer: string, expectedPromptText: string) =>
    cli.interactive().answerToPrompt(answer, expectedPromptText)
)
When(
  'I add the saved access token in the interactive CLI using add-access-token',
  () => cli.accessToken().addSavedTokenInteractive()
)

Then(
  'I should see the {word} remove success message for {string}',
  (removalType: string, label: string) =>
    cli.removeToken().expectSuccess(removalType, label)
)

Then(
  'I should see {string} in the non-interactive output',
  (expected: string) => cli.nonInteractiveOutput().expectContains(expected)
)
Then(
  'I should see {string} in past CLI assistant messages',
  (expected: string) => cli.pastCliAssistantMessages().expectContains(expected)
)
Then('I should see {string} in past user messages', (expected: string) =>
  cli.pastUserMessages().expectContains(expected)
)
Then('I should see {string} in the Current guidance', (expected: string) =>
  cli.currentGuidance().expectContains(expected)
)
Then(
  'I should see {string} styled in the Current guidance',
  (expected: string) => cli.currentGuidance().expectStyled(expected)
)
Then('the input box UI should be normal', () =>
  cli.inputBoxTopBorder().expectExactlyOne()
)

Given(
  'the Google API mock returns tokens and profile for {string}',
  (email: string) => {
    cy.wrap(
      mock_services
        .google()
        .stubTokenExchange('mock_access_token', 'mock_refresh_token')
        .then(() => mock_services.google().stubGmailProfile(email))
    )
  }
)

Given(
  'the Google API mock returns messages and message {string} with subject {string}',
  (messageId: string, subject: string) => {
    cy.wrap(
      mock_services
        .google()
        .stubGmailMessages([{ id: messageId }])
        .then(() => mock_services.google().stubGmailMessage(messageId, subject))
    )
  }
)
