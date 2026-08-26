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
When('I run the installed donut version command', () =>
  cli.installation().runVersion()
)
When(
  'I run the installed donut update command with BASE_URL from localhost',
  () => cli.installation().runUpdate()
)

When('the backend serves a newer CLI than the installed version', () =>
  cli.backend().serveNewerThanInstalled()
)

Then(
  'I should see {string} in the non-interactive output',
  (expected: string) => cli.nonInteractiveOutput().expectContains(expected)
)

Then(
  'I should see the installed CLI version in the non-interactive output',
  () => cli.nonInteractiveOutput().expectInstalledCliVersionBanner()
)

Then(
  'I should see that the CLI was updated to the newer version in the non-interactive output',
  () => cli.nonInteractiveOutput().expectUpdatedFromInstalledToNewer()
)

When('I run the installed donut command in interactive mode', () =>
  cli.installation().runInteractiveMode()
)

Then(
  'I should see {string} in past CLI assistant messages',
  (expected: string) =>
    cli.interactiveCli().pastCliAssistantMessages().expectContains(expected)
)

Then(
  'I should see the installed CLI version in past CLI assistant messages',
  () =>
    cli
      .interactiveCli()
      .pastCliAssistantMessages()
      .expectInstalledCliVersionBanner()
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
    cli.interactiveCli().answerWhenPromptVisible(answer, prompt)
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

When('I choose the next MCQ choice in the interactive CLI', () =>
  cli.interactiveCli().chooseNextMcqChoice()
)

When('I save the Doughnut Access Token in the interactive CLI', () => {
  cy.get<string>('@savedAccessToken').then((token) => {
    expect(token, 'saved access token').to.be.a('string')
    return cli.interactiveCli().saveAccessToken(token)
  })
})

When(
  'I set the access token for {string} in the interactive CLI',
  (userIdentifier: string) => {
    cli.interactiveCli().saveAccessToken(`access-token-of-${userIdentifier}`)
  }
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
