/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Before, After } from "@badeball/cypress-cucumber-preprocessor"
import TestabilityHelper from "../support/TestabilityHelper"
import { services } from "page_objects"

Before(() => {
  cy.testability().cleanDBAndResetTestabilitySettings()
  cy.wrap("no").as("firstVisited")
})

After(() => {
  // make sure nothing is loading on the page.
  // So to avoid async request from this test
  // messing up the next test.
  cy.pageIsNotLoading()
})

Before({ tags: "@mockBrowserTime" }, () => {
  //
  // when using `cy.clock()` to set the time,
  // for Vue component with v-if for a ref/react object that is changed during mount by async call
  // the event, eg. click, will not work.
  //
  cy.testability().then((testability: TestabilityHelper) => {
    cy.clock(testability.hourOfDay(0, 0), [
      "setTimeout",
      "setInterval",
      "clearInterval",
      "clearTimeout",
      "Date",
    ])
  })
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

Before({ tags: "@featureToggle" }, () => {
  cy.testability().featureToggle(true)
})

Before({ tags: "@usingMockedWikidataService" }, () => {
  services.wikidataService().mock()
})

After({ tags: "@usingMockedWikidataService" }, () => {
  services.wikidataService().restore()
})

Before({ tags: "@usingMockedOpenAiService" }, () => {
  services.openAiService().mock()
})

After({ tags: "@usingMockedOpenAiService" }, () => {
  services.openAiService().restore()
})
