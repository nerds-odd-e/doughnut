/// <reference types="Cypress" />
// @ts-check
import type { TimeTravelRelativeToNow } from '@generated/donut-backend-api'
import { TestabilityRestController } from '@generated/donut-backend-api/sdk.gen'

function clockAt(days: number, hours: number) {
  // Backend time-travel parses JSON.stringify clock fields as naive local time.
  // Date.UTC keeps the requested hour regardless of the machine timezone.
  return new Date(Date.UTC(1976, 5, 1 + days, hours))
}

function postTimeTravel(date: Date) {
  return cy.wrap(
    TestabilityRestController.timeTravel({
      body: { travel_to: JSON.stringify(date) },
    }),
    { log: false }
  )
}

type TimeTravelApi = {
  backendTimeTravelTo(day: number, hour: number): Cypress.Chainable<unknown>
}

export const timeTravelTestabilityMethods = {
  timeTravelTo(this: TimeTravelApi, day: number, hour: number) {
    this.backendTimeTravelTo(day, hour)
    cy.window().then((window) => {
      cy.tick(clockAt(day, hour).getTime() - new window.Date().getTime())
    })
  },

  backendTimeTravelTo(day: number, hour: number) {
    return postTimeTravel(clockAt(day, hour))
  },

  backendTimeTravelRelativeToNow(hours: number) {
    const requestBody: TimeTravelRelativeToNow = {
      hours,
    }

    return cy.wrap(
      TestabilityRestController.timeTravelRelativeToNow({
        body: requestBody,
      }),
      { log: false }
    )
  },

  mockBrowserTime() {
    //
    // when using `cy.clock()` to set the time,
    // for Vue component with v-if for a ref/react object that is changed during mount by async call
    // the event, eg. click, will not work.
    //
    cy.clock(clockAt(0, 0), [
      'setTimeout',
      'setInterval',
      'clearInterval',
      'clearTimeout',
      'Date',
    ])
  },
}
