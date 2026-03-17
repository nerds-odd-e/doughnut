import { Given, When, Then } from '@badeball/cypress-cucumber-preprocessor'
import { mock_services } from '../start'
import { getSectionContent, getLastCommandOutput } from './cliSectionParser'

const BASE_URL = 'http://localhost:9081'
const GOOGLE_MOCK_URL = 'http://localhost:5003'

function runCliWithConfig(args: string[]) {
  return cy.get<string>('@cliConfigDir').then((configDir) =>
    cy
      .task('runCliDirectWithArgs', {
        args,
        env: {
          DOUGHNUT_CONFIG_DIR: configDir,
          DOUGHNUT_API_BASE_URL: BASE_URL,
        },
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
  cy.request('GET', `${BASE_URL}/install`).its('status').should('eq', 200)
})

Given('the CLI is built with version {string}', (version: string) => {
  cy.task('bundleAndCopyCliWithVersion', version)
})

When('I install the CLI from localhost without affecting my system', () => {
  cy.task<string>('installCli', BASE_URL)
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
        env: {
          DOUGHNUT_CONFIG_DIR: configDir,
          DOUGHNUT_API_BASE_URL: BASE_URL,
        },
      }).as('doughnutOutput')
    )
  }
)

When(
  'I run the doughnut command in interactive mode with input {string} and {string}',
  (command: string, answer: string) => {
    cy.get<string>('@cliConfigDir').then((configDir) =>
      taskWithCliTiming('runCliDirectWithInput', {
        input: `${command}\n${answer}\nexit\n`,
        env: {
          DOUGHNUT_CONFIG_DIR: configDir,
          DOUGHNUT_API_BASE_URL: BASE_URL,
        },
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
        env: {
          DOUGHNUT_CONFIG_DIR: configDir,
          DOUGHNUT_API_BASE_URL: BASE_URL,
        },
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
        env: {
          DOUGHNUT_CONFIG_DIR: configDir,
          DOUGHNUT_API_BASE_URL: BASE_URL,
        },
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
        env: {
          DOUGHNUT_CONFIG_DIR: configDir,
          DOUGHNUT_API_BASE_URL: BASE_URL,
        },
      }).as('doughnutOutput')
    )
  }
)

When('I run a recall session with load more from future days', () => {
  cy.get<string>('@cliConfigDir').then((configDir) =>
    taskWithCliTiming('runCliDirectWithInput', {
      input: '/recall\ny\ny\ny\ny\nexit\n',
      env: {
        DOUGHNUT_CONFIG_DIR: configDir,
        DOUGHNUT_API_BASE_URL: BASE_URL,
      },
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
          env: {
            DOUGHNUT_CONFIG_DIR: configDir,
            DOUGHNUT_API_BASE_URL: BASE_URL,
          },
        })
        .as('doughnutOutput')
    )
  }
)

When(
  'I run the doughnut command in interactive mode with recall MCQ and cancel with ESC',
  () => {
    const esc = '\x1b'
    cy.get<string>('@cliConfigDir').then((configDir) =>
      cy
        .task('runCliDirectWithInputAndPty', {
          input: `/recall\n${esc}\ny\nexit\n`,
          env: {
            DOUGHNUT_CONFIG_DIR: configDir,
            DOUGHNUT_API_BASE_URL: BASE_URL,
          },
        })
        .as('doughnutOutput')
    )
  }
)

When(
  'I run the doughnut command in interactive mode with down-arrow selection for {string}',
  (command: string) => {
    const downArrow = '\x1b[B'
    const startTime = Date.now()
    cy.get<string>('@cliConfigDir').then((configDir) =>
      cy
        .task('runCliDirectWithInputAndPty', {
          input: `${command}\n${downArrow}\r\nexit\n`,
          env: {
            DOUGHNUT_CONFIG_DIR: configDir,
            DOUGHNUT_API_BASE_URL: BASE_URL,
          },
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
        env: { BASE_URL },
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

Then('I should see {string} in the status', (expected: string) => {
  cy.get<string>('@doughnutOutput').then((output) => {
    const content = getSectionContent(output, 'status')
    expect(
      content,
      `Expected "${expected}" in status. status contains:\n${content.slice(0, 500)}${content.length > 500 ? '...' : ''}`
    ).to.include(expected)
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
    expect(getSectionContent(output, 'status')).to.include(
      'What is the meaning of sedition?'
    )
    // ESC path shows "Stop recall? (y/n)"; fallback /stop path on CI does not
    expect(getSectionContent(output, 'history-output')).to.include(
      'Stopped recall'
    )
  })
})

Then('I stopped the recall during review', () => {
  cy.get<string>('@doughnutOutput').then((output) => {
    expect(getSectionContent(output, 'status')).to.include('sedition')
    expect(getSectionContent(output, 'status')).to.include('Yes, I remember?')
    expect(getSectionContent(output, 'history-output')).to.include(
      'Stopped recall'
    )
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
