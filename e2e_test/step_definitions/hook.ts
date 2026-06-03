/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { After, Before } from '@badeball/cypress-cucumber-preprocessor'
import {
  GMAIL_E2E_GOOGLE_MOCK_BASE_URL,
  GMAIL_E2E_MOCK_ACCOUNT_CONFIG,
  GMAIL_E2E_OAUTH_ADD_CONFIG,
} from '../config/cliGmailE2eConfig'
import start, { mock_services } from '../start'
import { cli } from '../start/pageObjects/cli'

Before(() => {
  cy.task('clearTestState')
  start.testability().cleanDBAndResetTestabilitySettings()
  cy.wrap('no').as('firstVisited')
})

After(() => {
  // make sure nothing is loading on the page.
  // So to avoid async request from this test
  // messing up the next test.
  start.pageIsNotLoading()
})

Before({ tags: '@mockBrowserTime' }, () => {
  start.testability().mockBrowserTime()
})

// If a test needs to stop the timer, perhaps the tested
// page refreshes automatically. The mocked timer is restored
// between tests. It may cause a hard-to-trace problem when
// the next test resets the DB while the current page refreshes
// itself. So, here it visits the blank page at the end of each test.
After({ tags: '@mockBrowserTime' }, () => {
  cy.window().then((win) => {
    win.location.href = 'about:blank'
  })
})

Before({ tags: '@startWithEmptyDownloadsFolder' }, () => {
  const downloadsFolder = Cypress.config('downloadsFolder')
  cy.task('deleteFolder', downloadsFolder)
})

Before({ tags: '@featureToggle' }, () => {
  start.testability().featureToggle(true)
})

Before({ tags: '@randomizerAlwaysInAscendOrder' }, () => {
  start.testability().randomizerSettings('first', 0)
})

Before({ tags: '@randomizerWithFixedSeed' }, () => {
  start.testability().randomizerSettings('seed', 1)
})

Before({ tags: '@usingMockedWikidataService' }, () => {
  mock_services.wikidata().mock()
})

After({ tags: '@usingMockedWikidataService' }, () => {
  mock_services.wikidata().restore()
})

Before({ tags: '@usingMockedOpenAiService' }, () => {
  mock_services.openAi().mock()
})

After({ tags: '@usingMockedOpenAiService' }, () => {
  mock_services.openAi().restore()
})

Before({ tags: '@usingMockedGoogleService' }, () => {
  mock_services.google().mock()
})

After({ tags: '@usingMockedGoogleService' }, () => {
  mock_services.google().restore()
})

Before({ tags: '@disableOpenAiService' }, () => {
  mock_services.openAi().disable()
})

After({ tags: '@disableOpenAiService' }, () => {
  start.testability().setOpenAiTokenOverride(null)
})

// the Afer hook from cypress-cucumber-preprocessor is not working
// as expected.
// When a test fail, the After hook is not called.
// So we need to call the task in the Before hook as well.
Before({ tags: '@BundleFirstAndTerminateMCPServerWhenTeardown' }, () => {
  cy.task('bundleMcpServer')
  cy.task('disconnectMcpServer')
})

After({ tags: '@BundleFirstAndTerminateMCPServerWhenTeardown' }, () => {
  cy.task('disconnectMcpServer')
})

Before({ tags: '@bundleCliE2eInstall' }, () =>
  cli.backend().bundleCliForE2eInstall()
)

After({ tags: '@bundleCliE2eInstall' }, () =>
  cli.backend().removeE2eInstallCliBundle()
)

Before({ tags: '@mockMineruLib', order: 0 }, () => {
  cy.task<string>('getMineruE2eMockSitePath').then((p) => {
    Cypress.env('DOUGHNUT_E2E_MINERU_MOCK_SITE', p)
  })
})

After({ tags: '@mockMineruLib' }, () => {
  Cypress.env('DOUGHNUT_E2E_MINERU_MOCK_SITE', undefined)
})

Before({ tags: '@withCliConfig', order: 1 }, () => {
  cy.task('createCliConfigDir').as('cliConfigDir')
})

Before({ tags: '@withCliGmailOAuthAddConfig', order: 1 }, () => {
  cy.task('createCliConfigDirWithGmail', GMAIL_E2E_OAUTH_ADD_CONFIG).as(
    'cliConfigDir'
  )
})

Before({ tags: '@withCliGmailMockAccountConfig', order: 1 }, () => {
  cy.task('createCliConfigDirWithGmail', GMAIL_E2E_MOCK_ACCOUNT_CONFIG).as(
    'cliConfigDir'
  )
})

Before({ tags: '@interactiveCLI', order: 2 }, () => {
  cli.ttyAssertTerminal().kill()
  cy.get<string>('@cliConfigDir').then((configDir) => {
    const mockSite = Cypress.env('DOUGHNUT_E2E_MINERU_MOCK_SITE') as
      | string
      | undefined
    if (typeof mockSite === 'string' && mockSite.length > 0) {
      return cy
        .task<string>('prependMineruMockToPythonPath', mockSite)
        .then((pythonPath) =>
          cli.ttyAssertTerminal().startRepoInteractive({
            env: {
              DOUGHNUT_CONFIG_DIR: configDir,
              PYTHONPATH: pythonPath,
            },
          })
        )
    }
    return cli.ttyAssertTerminal().startRepoInteractive({
      env: { DOUGHNUT_CONFIG_DIR: configDir },
    })
  })
})

Before({ tags: '@interactiveCLIGmail', order: 2 }, () => {
  cli.ttyAssertTerminal().kill()
  cy.get<string>('@cliConfigDir').then((configDir) => {
    const mockSite = Cypress.env('DOUGHNUT_E2E_MINERU_MOCK_SITE') as
      | string
      | undefined
    const base: NodeJS.ProcessEnv = {
      DOUGHNUT_CONFIG_DIR: configDir,
      GOOGLE_BASE_URL: GMAIL_E2E_GOOGLE_MOCK_BASE_URL,
      DOUGHNUT_NO_BROWSER: '1',
    }
    if (typeof mockSite === 'string' && mockSite.length > 0) {
      return cy
        .task<string>('prependMineruMockToPythonPath', mockSite)
        .then((pythonPath) =>
          cli.ttyAssertTerminal().startRepoInteractive({
            env: { ...base, PYTHONPATH: pythonPath },
          })
        )
    }
    return cli.ttyAssertTerminal().startRepoInteractive({ env: base })
  })
})

After({ tags: '@interactiveCLI' }, () => {
  cli.ttyAssertTerminal().kill()
})

After({ tags: '@interactiveCLIGmail' }, () => {
  cli.ttyAssertTerminal().kill()
})
