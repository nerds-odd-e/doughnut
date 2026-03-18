import { Given, When, Then } from '@badeball/cypress-cucumber-preprocessor'
import { backendBaseUrl } from '../support/backendUrl'
import { mock_services } from '../start'
import {
  getSectionContent,
  getSectionContentRaw,
  getLastCommandOutput,
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

function taskWithCliTiming(
  taskName: string,
  taskArg: { input: string; env: Record<string, string> }
) {
  const startTime = Date.now()
  return cy.task(taskName, taskArg).then((output: unknown) => {
    if (Cypress.env('RECORD_E2E_TIMING')) {
      return cy
        .task('recordTiming', {
          label: 'cli-run',
          duration: Date.now() - startTime,
        })
        .then(() => cy.wrap(output))
    }
    return cy.wrap(output)
  })
}

Given('the backend is serving the CLI and install script', () => {
  cy.request('GET', `${backendBaseUrl()}/install`)
    .its('status')
    .should('eq', 200)
})

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

When('I run the doughnut command with input {string}', (input: string) => {
  const trimmed = input.trim()
  const exitSuffix = trimmed === 'exit' || trimmed === '/exit' ? '' : `\nexit`
  cy.task('runCliDirectWithInput', { input: `${input}${exitSuffix}` }).as(
    'doughnutOutput'
  )
})

When(
  'I run the doughnut command in interactive mode with input {string}',
  (input: string) => {
    cy.get<string>('@cliConfigDir').then((configDir) =>
      taskWithCliTiming('runCliDirectWithInput', {
        input: `${input}\nexit\n`,
        env: cliEnvWithConfigDir(configDir),
      }).as('doughnutOutput')
    )
  }
)

When('I input {string} in the interactive CLI', (input: string) => {
  cy.task<string>('sendToInteractiveCli', { input }).as('doughnutOutput')
})

When(
  'I answer {string} in the interactive CLI to {string}',
  (input: string, to: string) => {
    cy.get<string>('@doughnutOutput').then((output) =>
      assertExpectedInStatus(output, to, true)
    )
    cy.task<string>('sendToInteractiveCli', { input }).as('doughnutOutput')
  }
)

When(
  'I input down-arrow selection for {string} in the interactive CLI',
  (command: string) => {
    cy.task<string>('sendToInteractiveCli', { input: command })
    cy.task<string>('sendToInteractiveCli', { input: '2' }).as('doughnutOutput')
  }
)

When(
  'I run the doughnut command in interactive mode with input {string} and {string}',
  (command: string, answer: string) => {
    cy.get<string>('@cliConfigDir').then((configDir) =>
      taskWithCliTiming('runCliDirectWithInput', {
        input: `${command}\n${answer}\nexit\n`,
        env: cliEnvWithConfigDir(configDir),
      }).as('doughnutOutput')
    )
  }
)

When(
  'I run the doughnut command in interactive mode with input {string} and {string} and {string}',
  (command: string, answer1: string, answer2: string) => {
    cy.get<string>('@cliConfigDir').then((configDir) =>
      taskWithCliTiming('runCliDirectWithInput', {
        input: `${command}\n${answer1}\n${answer2}\nexit\n`,
        env: cliEnvWithConfigDir(configDir),
      }).as('doughnutOutput')
    )
  }
)

When(
  'I run the doughnut command in interactive mode with input {string} and {string} and {string} and {string}',
  (command: string, answer1: string, answer2: string, answer3: string) => {
    cy.get<string>('@cliConfigDir').then((configDir) =>
      taskWithCliTiming('runCliDirectWithInput', {
        input: `${command}\n${answer1}\n${answer2}\n${answer3}\nexit\n`,
        env: cliEnvWithConfigDir(configDir),
      }).as('doughnutOutput')
    )
  }
)

When(
  'I run a recall session and recall all due notes, declining load more',
  () => {
    cy.get<string>('@cliConfigDir').then((configDir) =>
      taskWithCliTiming('runCliDirectWithInput', {
        input: '/recall\ny\ny\nn\nexit\n',
        env: cliEnvWithConfigDir(configDir),
      }).as('doughnutOutput')
    )
  }
)

When('I run a recall session with load more from future days', () => {
  cy.get<string>('@cliConfigDir').then((configDir) =>
    taskWithCliTiming('runCliDirectWithInput', {
      input: '/recall\ny\ny\ny\ny\nexit\n',
      env: cliEnvWithConfigDir(configDir),
    }).as('doughnutOutput')
  )
})

When(
  'I run the remove-access-token command and cancel with ESC, then list tokens',
  () => {
    const esc = '\x1b'
    cy.get<string>('@cliConfigDir').then((configDir) =>
      cy
        .task('runCliDirectWithInputAndPty', {
          input: `/remove-access-token\n${esc}\n/list-access-token\nexit\n`,
          env: cliEnvWithConfigDir(configDir),
        })
        .as('doughnutOutput')
    )
  }
)

When(
  'I run the doughnut command in interactive mode with recall MCQ and cancel with ESC',
  () => {
    cy.get<string>('@cliConfigDir').then((configDir) =>
      cy
        .task('runCliDirectWithInput', {
          input: '/recall\n/stop\nexit\n',
          env: cliEnvWithConfigDir(configDir),
        })
        .as('doughnutOutput')
    )
  }
)

When(
  'I run the doughnut command in interactive mode with down-arrow selection for {string}',
  (_command: string) => {
    const startTime = Date.now()
    cy.get<string>('@cliConfigDir').then((configDir) =>
      cy
        .task('runCliDirectWithInput', {
          input: '/recall\n2\nexit\n',
          env: cliEnvWithConfigDir(configDir),
        })
        .then((output: unknown) => {
          if (Cypress.env('RECORD_E2E_TIMING')) {
            return cy
              .task('recordTiming', {
                label: 'cli-run',
                duration: Date.now() - startTime,
              })
              .then(() => cy.wrap(output))
          }
          return cy.wrap(output)
        })
        .as('doughnutOutput')
    )
  }
)

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

Then('I should see {string} in the history output', (expected: string) => {
  cy.get<string>('@doughnutOutput').then((output) => {
    const content = getSectionContent(output, 'history-output')
    expect(
      content,
      `Expected "${expected}" in history-output. history-output contains:\n${content.slice(0, 500)}${content.length > 500 ? '...' : ''}`
    ).to.include(expected)
  })
})

Then('I should see {string} in the history input', (expected: string) => {
  cy.get<string>('@doughnutOutput').then((output) => {
    const content = getSectionContent(output, 'history-input')
    expect(
      content,
      `Expected "${expected}" in history-input. history-input contains:\n${content.slice(0, 500)}${content.length > 500 ? '...' : ''}`
    ).to.include(expected)
  })
})

function assertExpectedInStatus(
  output: string,
  expected: string,
  statusOnly = false
): void {
  const statusContent = getSectionContent(output, 'status')
  const content = statusOnly
    ? statusContent
    : `${statusContent}\n${getSectionContent(output, 'history-output')}`.trim()
  expect(
    content,
    `Expected "${expected}" in ${statusOnly ? 'status' : 'status or history'}. content:\n${content.slice(0, 500)}${content.length > 500 ? '...' : ''}`
  ).to.include(expected)
}

Then('I should see {string} in the status', (expected: string) => {
  cy.get<string>('@doughnutOutput').then((output) =>
    assertExpectedInStatus(output, expected)
  )
})

Then('I should see {string} styled in the status', (expected: string) => {
  cy.get<string>('@doughnutOutput').then((output) => {
    const rawStatus = getSectionContentRaw(output, 'status')
    const fallbackRaw = getSectionContentRaw(output, 'history-output')
    const rawContent = `${rawStatus}\n${fallbackRaw}`.trim()
    expect(rawContent, `Expected "${expected}" in raw status`).to.include(
      expected
    )
    // Markdown is rendered via markdansi: bold=\x1b[1m, italic=\x1b[3m
    const hasBold = rawContent.includes('\x1b[1m')
    const hasItalic = rawContent.includes('\x1b[3m')
    expect(
      hasBold || hasItalic,
      `Expected ANSI styling (bold or italic) in status. Raw status length: ${rawContent.length}`
    ).to.be.true
  })
})

Then('I should see {string} in the last command output', (expected: string) => {
  cy.get<string>('@doughnutOutput').then((output) => {
    const content = getLastCommandOutput(output)
    expect(
      content,
      `Expected "${expected}" in last command output. last command output contains:\n${content.slice(0, 500)}${content.length > 500 ? '...' : ''}`
    ).to.include(expected)
  })
})

Then('I should not see {string} in the history output', (expected: string) => {
  cy.get<string>('@doughnutOutput').then((output) => {
    const content = getSectionContent(output, 'history-output')
    expect(
      content,
      `Did not expect "${expected}" in history-output`
    ).not.to.include(expected)
  })
})

Then('the recall session was stopped', () => {
  cy.get<string>('@doughnutOutput').then((output) => {
    const status = getSectionContent(output, 'status')
    const historyOutput = getSectionContent(output, 'history-output')
    expect(`${status}\n${historyOutput}`).to.include(
      'What is the meaning of sedition?'
    )
    // ESC path shows "Stop recall? (y/n)"; fallback /stop path on CI does not
    expect(historyOutput).to.include('Stopped recall')
  })
})

Then('I stopped the recall during review', () => {
  cy.get<string>('@doughnutOutput').then((output) => {
    const status = getSectionContent(output, 'status')
    const historyOutput = getSectionContent(output, 'history-output')
    const combined = `${status}\n${historyOutput}`
    expect(combined).to.include('sedition')
    expect(combined).to.include('Yes, I remember?')
    expect(historyOutput).to.include('Stopped recall')
  })
})

Then(
  'I should see the {word} remove success message for {string}',
  (removalType: string, label: string) => {
    const expected =
      removalType === 'local'
        ? `Token "${label}" removed.`
        : 'removed locally and from server'
    cy.get<string>('@doughnutOutput').then((output) => {
      expect(getSectionContent(output, 'history-output')).to.include(expected)
    })
  }
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
