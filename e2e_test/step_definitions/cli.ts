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

When(
  'I enter the slash command {string} in the interactive CLI',
  (command: string) =>
    cli.interactiveCli().enterSlashCommandInInteractiveCli(command)
)

Then('I should see {string} in past user messages', (expected: string) =>
  cli.interactiveCli().pastUserMessages().expectContains(expected)
)
