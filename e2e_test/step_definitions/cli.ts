import { Given, When, Then } from '@badeball/cypress-cucumber-preprocessor'
import { backendBaseUrl } from '../support/backendUrl'
import { mock_services } from '../start'
import {
  getHistoryOutputContent,
  getHistoryInputContent,
  getLastCommandOutput,
  getRecallDisplaySections,
  getCurrentGuidanceDebug,
  getCurrentGuidanceAndHistoryRaw,
} from './cliSectionParser'

const GOOGLE_MOCK_URL = 'http://localhost:5003'

function cliEnvWithConfigDir(configDir: string): Record<string, string> {
  return {
    DOUGHNUT_CONFIG_DIR: configDir,
    DOUGHNUT_API_BASE_URL: backendBaseUrl(),
  }
}

function runCliWithConfig(args: string[]) {
  return cy.get<string>('@cliConfigDir').then((configDir) =>
    cy
      .task('runCliDirectWithArgs', {
        args,
        env: cliEnvWithConfigDir(configDir),
      })
      .as('doughnutOutput')
  )
}

function assertOutputIncludes(
  content: string,
  expected: string,
  label: string
): void {
  expect(
    content,
    `Expected "${expected}" in ${label}. Content:\n${content.slice(0, 500)}${content.length > 500 ? '...' : ''}`
  ).to.include(expected)
}

function assertOutputNotIncludes(content: string, expected: string): void {
  expect(content, `Did not expect "${expected}"`).not.to.include(expected)
}

function buildCurrentGuidanceFailureMessage(
  output: string,
  expected: string
): string {
  const { currentGuidanceContent, inputBoxLineRange, lineCount, rawTail } =
    getCurrentGuidanceDebug(output)
  const linesAfterBox =
    inputBoxLineRange.end >= 0 ? lineCount - inputBoxLineRange.end - 1 : 0
  return [
    `Expected "${expected}" in Current guidance (prompts, hints, options for the current input).`,
    ``,
    `Parser: input box ┌ at line ${inputBoxLineRange.start}, └ at line ${inputBoxLineRange.end} of ${lineCount} lines. Lines after └: ${linesAfterBox}.`,
    ``,
    `Current guidance: ${currentGuidanceContent ? `"${currentGuidanceContent}"` : '(empty)'}`,
    ``,
    `Raw output tail (\\r→\\r \\n→\\n ):`,
    rawTail,
  ].join('\n')
}

// --- Setup ---

Given('the backend is serving the CLI and install script', () => {
  cy.request('GET', `${backendBaseUrl()}/install`)
    .its('status')
    .should('eq', 200)
})

// --- CLI installation and run ---

When('I install the CLI from localhost without affecting my system', () => {
  cy.task<string>('installCli', backendBaseUrl())
    .should('be.a', 'string')
    .and('not.be.empty')
    .as('doughnutPath')
})

When('I run the installed doughnut command', () => {
  cy.get<string>('@doughnutPath')
    .should('be.a', 'string')
    .and('not.be.empty')
    .then((doughnutPath) => {
      cy.task<string>('runInstalledCli', { doughnutPath, input: 'exit\n' }).as(
        'doughnutOutput'
      )
    })
})

When('I run the installed doughnut version command', () => {
  cy.get<string>('@doughnutPath').then((doughnutPath) => {
    cy.task<string>('runInstalledCli', {
      doughnutPath,
      args: ['version'],
    }).as('doughnutOutput')
  })
})

// --- CLI execution: piped input (non-interactive) ---

When('I run the doughnut command with input {string}', (input: string) => {
  const trimmed = input.trim()
  const exitSuffix = trimmed === 'exit' || trimmed === '/exit' ? '' : `\nexit`
  cy.task('runCliDirectWithInput', { input: `${input}${exitSuffix}` }).as(
    'doughnutOutput'
  )
})

// --- Interactive CLI: input and keypress ---

When('I input {string} in the interactive CLI', (input: string) => {
  cy.task<string>('sendToInteractiveCli', { input }).as('doughnutOutput')
})

When('I press ESC in the interactive CLI', () => {
  cy.task<string>('sendToInteractiveCli', { input: '\x1b' }).as(
    'doughnutOutput'
  )
})

When(
  'I answer {string} in the interactive CLI to prompt {string}',
  (answer: string, expectedPromptText: string) => {
    cy.get<string>('@doughnutOutput').then((output) => {
      const { currentGuidanceAndHistory } = getRecallDisplaySections(output)
      assertOutputIncludes(
        currentGuidanceAndHistory,
        expectedPromptText,
        'Current guidance'
      )
    })
    cy.task<string>('sendToInteractiveCli', { input: answer }).as(
      'doughnutOutput'
    )
  }
)

When(
  'I input down-arrow selection for {string} in the interactive CLI',
  (command: string) => {
    cy.task<string>('sendToInteractiveCli', { input: command })
    cy.task<string>('sendToInteractiveCli', { input: '2' }).as('doughnutOutput')
  }
)

// --- CLI execution: -c, version, update ---

When('I run the doughnut command with -c {string}', (input: string) => {
  runCliWithConfig(['-c', input])
})

When('I run the doughnut version command', () => {
  cy.task('runCliDirectWithArgs', { args: ['version'] }).as('doughnutOutput')
})

When('the backend serves the CLI with version {string}', (version: string) => {
  cy.task('bundleAndCopyCliWithVersion', version)
})

When(
  'I run the installed doughnut update command with BASE_URL from localhost',
  () => {
    cy.get<string>('@doughnutPath').then((doughnutPath) => {
      cy.task<string>('runInstalledCli', {
        doughnutPath,
        args: ['update'],
        env: { BASE_URL: backendBaseUrl() },
      }).as('doughnutOutput')
    })
  }
)

// --- Access token commands ---

When('I run the doughnut CLI add-access-token with the saved token', () => {
  cy.get<string>('@savedAccessToken').then((token) => {
    runCliWithConfig(['-c', `/add-access-token ${token}`])
  })
})

When(
  'I run the doughnut CLI add-access-token with token {string}',
  (token: string) => {
    runCliWithConfig(['-c', `/add-access-token ${token}`])
  }
)

When(
  'I run the doughnut CLI remove-access-token with label {string}',
  (label: string) => {
    runCliWithConfig(['-c', `/remove-access-token ${label}`])
  }
)

When(
  'I run the doughnut CLI create-access-token with label {string}',
  (label: string) => {
    runCliWithConfig(['-c', `/create-access-token ${label}`])
  }
)

When(
  'I run the doughnut CLI remove-access-token-completely with label {string}',
  (label: string) => {
    runCliWithConfig(['-c', `/remove-access-token-completely ${label}`])
  }
)

// --- Section assertions: History output, History input, Current guidance ---

Then('I should see {string} in the history output', (expected: string) => {
  cy.get<string>('@doughnutOutput').then((output) =>
    assertOutputIncludes(
      getHistoryOutputContent(output),
      expected,
      'history output'
    )
  )
})

Then('I should see {string} in the Current guidance', (expected: string) => {
  cy.get<string>('@doughnutOutput').then((output) => {
    const { currentGuidanceAndHistory } = getRecallDisplaySections(output)
    const msg = currentGuidanceAndHistory.includes(expected)
      ? undefined
      : buildCurrentGuidanceFailureMessage(output, expected)
    expect(currentGuidanceAndHistory, msg).to.include(expected)
  })
})

Then('I should see {string} in the history input', (expected: string) => {
  cy.get<string>('@doughnutOutput').then((output) =>
    assertOutputIncludes(
      getHistoryInputContent(output),
      expected,
      'history input'
    )
  )
})

Then(
  'I should see {string} styled in the Current guidance',
  (expected: string) => {
    cy.get<string>('@doughnutOutput').then((output) => {
      const rawContent = getCurrentGuidanceAndHistoryRaw(output)
      expect(
        rawContent,
        `Expected "${expected}" in raw Current guidance`
      ).to.include(expected)
      // Markdown is rendered via markdansi: bold=\x1b[1m, italic=\x1b[3m
      const hasBold = rawContent.includes('\x1b[1m')
      const hasItalic = rawContent.includes('\x1b[3m')
      expect(
        hasBold || hasItalic,
        `Expected ANSI styling (bold or italic) in Current guidance. Raw length: ${rawContent.length}`
      ).to.be.true
    })
  }
)

Then('I should see {string} in the last command output', (expected: string) => {
  cy.get<string>('@doughnutOutput').then((output) =>
    assertOutputIncludes(
      getLastCommandOutput(output),
      expected,
      'last command output'
    )
  )
})

Then('I should not see {string} in the history output', (expected: string) => {
  cy.get<string>('@doughnutOutput').then((output) =>
    assertOutputNotIncludes(getHistoryOutputContent(output), expected)
  )
})

// --- Recall session assertions ---

Then('the recall session was stopped', () => {
  cy.get<string>('@doughnutOutput').then((output) => {
    const { currentGuidanceAndHistory, historyOutput } =
      getRecallDisplaySections(output)
    expect(currentGuidanceAndHistory).to.include(
      'What is the meaning of sedition?'
    )
    expect(historyOutput).to.include('Stopped recall')
  })
})

Then('I stopped the recall during review', () => {
  cy.get<string>('@doughnutOutput').then((output) => {
    const { currentGuidanceAndHistory, historyOutput } =
      getRecallDisplaySections(output)
    expect(currentGuidanceAndHistory).to.include('sedition')
    expect(currentGuidanceAndHistory).to.include('Yes, I remember?')
    expect(historyOutput).to.include('Stopped recall')
  })
})

// --- Access token assertions ---

Then(
  'I should see the {word} remove success message for {string}',
  (removalType: string, label: string) => {
    const expected =
      removalType === 'local'
        ? `Token "${label}" removed.`
        : 'removed locally and from server'
    cy.get<string>('@doughnutOutput').then((output) =>
      assertOutputIncludes(
        getHistoryOutputContent(output),
        expected,
        'history output'
      )
    )
  }
)

// --- Gmail / Google API setup ---

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

// --- Gmail commands ---

When('I run the CLI add gmail command with simulated OAuth callback', () => {
  cy.task('runCliDirectWithGmailAdd', {
    googleBaseUrl: GOOGLE_MOCK_URL,
  })
    .its('stdout')
    .as('doughnutOutput')
})

When('I run the CLI last email command with pre-configured account', () => {
  cy.task('runCliDirectWithLastEmail', {
    googleBaseUrl: GOOGLE_MOCK_URL,
  }).as('doughnutOutput')
})
