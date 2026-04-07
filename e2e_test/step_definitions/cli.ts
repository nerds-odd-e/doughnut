/**
 * CLI scenarios: keep steps as one-line glue to `e2e_test/start/pageObjects/cli`.
 * Behavior and assertions belong in page objects under `e2e_test/start/pageObjects/cli`, not here.
 */
import { Given, When, Then } from '@badeball/cypress-cucumber-preprocessor'
import { cli } from '../start/pageObjects/cli'

Given('the backend is serving the CLI and install script', () =>
  cli.backend().expectInstallScriptServed()
)

When('I install the CLI from localhost without affecting my system', () =>
  cli.installation().installFromLocalhost()
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

Then(
  'I should see {string} in the non-interactive output',
  (expected: string) => cli.nonInteractiveOutput().expectContains(expected)
)

When('I run the installed doughnut command in interactive mode', () =>
  cli.installation().runInteractiveMode()
)

Then(
  'I should see {string} in past CLI assistant messages',
  (expected: string) =>
    cli.interactiveCli().pastCliAssistantMessages().expectContains(expected)
)

Then('I should see {string} in answered questions', (expected: string) =>
  cli.interactiveCli().answeredQuestions().expectContains(expected)
)

When('I enter {string} in the interactive CLI', (line: string) => {
  cli.interactiveCli().writeInteractiveLine(line)
})

When(
  'I answer {string} in the interactive CLI to prompt {string}',
  (answer: string, prompt: string) =>
    new Cypress.Promise<void>((resolve) => {
      cli
        .interactiveCli()
        .answerWhenPromptVisible(answer, prompt)
        .then(() => resolve())
    })
)

When(
  'I enter the slash command {string} in the interactive CLI',
  (command: string) => {
    cli.interactiveCli().enterSlashCommandInInteractiveCli(command)
  }
)

When(
  'I enter the slash sub-command {string} in the interactive CLI',
  (line: string) => {
    cli.interactiveCli().writeInteractiveLine(line)
  }
)

When(
  'I input down-arrow selection for {string} in the interactive CLI',
  (command: string) =>
    new Cypress.Promise<void>((resolve) => {
      cli
        .interactiveCli()
        .inputDownArrowSelectionForSlashCommand(command)
        .then(() => resolve())
    })
)

When(
  'I set the saved access token in the interactive CLI using set-access-token',
  () => {
    cy.get<string>('@savedAccessToken').then((token) => {
      expect(token, 'saved access token').to.be.a('string')
      const interactive = cli.interactiveCli()
      return interactive
        .enterSlashCommandInInteractiveCli(`/set-access-token ${token}`)
        .then(() => {
          interactive
            .pastCliAssistantMessages()
            .expectContains('Access token saved')
        })
    })
  }
)

When(
  'I set the access token for {string} in the interactive CLI',
  (userIdentifier: string) =>
    new Cypress.Promise<void>((resolve) => {
      const token = `access-token-of-${userIdentifier}`
      const interactive = cli.interactiveCli()
      interactive
        .enterSlashCommandInInteractiveCli(`/set-access-token ${token}`)
        .then(() => {
          interactive
            .pastCliAssistantMessages()
            .expectContains('Access token saved')
            .then(() => resolve())
        })
    })
)

Then('I should see {string} in the Current guidance', (expected: string) =>
  cli.interactiveCli().currentGuidance().expectContains(expected)
)

Then('I should see {string} styled in the Current guidance', (text: string) =>
  cli.interactiveCli().currentGuidance().expectContainsBold(text)
)

Then('I should see {string} in past user messages', (expected: string) =>
  cli.interactiveCli().pastUserMessages().expectDisplayed(expected)
)
