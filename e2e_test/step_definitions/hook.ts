/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Before, After } from "@badeball/cypress-cucumber-preprocessor"
import start, { mock_services } from "start"

Before(() => {
  start.testability().cleanDBAndResetTestabilitySettings()
  cy.wrap("no").as("firstVisited")
})

After(() => {
  // make sure nothing is loading on the page.
  // So to avoid async request from this test
  // messing up the next test.
  cy.pageIsNotLoading()
})

Before({ tags: "@mockBrowserTime" }, () => {
  start.testability().mockBrowserTime()
})

// If a test needs to stop the timer, perhaps the tested
// page refreshes automatically. The mocked timer is restored
// between tests. It may cause a hard-to-trace problem when
// the next test resets the DB while the current page refreshes
// itself. So, here it visits the blank page at the end of each test.
After({ tags: "@mockBrowserTime" }, () => {
  cy.window().then((win) => {
    win.location.href = "about:blank"
  })
})

Before({ tags: "@startWithEmptyDownloadsFolder" }, () => {
  const downloadsFolder = Cypress.config("downloadsFolder")
  cy.task("deleteFolder", downloadsFolder)
})

Before({ tags: "@featureToggle" }, () => {
  start.testability().featureToggle(true)
})

Before({ tags: "@usingMockedWikidataService" }, () => {
  mock_services.wikidata().mock()
})

After({ tags: "@usingMockedWikidataService" }, () => {
  mock_services.wikidata().restore()
})

Before({ tags: "@usingMockedOpenAiService" }, () => {
  mock_services.openAi().mock()
})

After({ tags: "@usingMockedOpenAiService" }, () => {
  mock_services.openAi().restore()
})
