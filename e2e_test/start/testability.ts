import ServiceMocker from "support/ServiceMocker"

const hourOfDay = (days: number, hours: number) => {
  return new Date(1976, 5, 1 + days, hours)
}

const postToTestabilityApi = (
  cy: Cypress.cy & CyEventEmitter,
  path: string,
  options: { body?: Record<string, unknown>; failOnStatusCode?: boolean },
) => {
  return cy.request({
    method: "POST",
    url: `/api/testability/${path}`,
    ...options,
  })
}

const postToTestabilityApiSuccessfully = (
  cy: Cypress.cy & CyEventEmitter,
  path: string,
  options: { body?: Record<string, unknown>; failOnStatusCode?: boolean },
) => {
  postToTestabilityApi(cy, path, options).its("status").should("equal", 200)
}

const cleanAndReset = (cy: Cypress.cy & CyEventEmitter, countdown: number) => {
  postToTestabilityApi(cy, "clean_db_and_reset_testability_settings", {
    failOnStatusCode: countdown === 1,
  }).then((response) => {
    if (countdown > 0 && response.status !== 200) {
      cleanAndReset(cy, countdown - 1)
    }
  })
}

const seededNoteIdMapAliasName = "seededNoteIdMap"

const testability = () => {
  return {
    cleanDBAndResetTestabilitySettings() {
      cleanAndReset(cy, 5)
    },

    featureToggle(enabled: boolean) {
      postToTestabilityApiSuccessfully(cy, "feature_toggle", { body: { enabled } })
    },

    seedNotes(seedNotes: unknown[], externalIdentifier = "", circleName = null) {
      postToTestabilityApi(cy, "seed_notes", {
        body: {
          externalIdentifier,
          circleName,
          seedNotes,
        },
      }).then((response) => {
        expect(Object.keys(response.body).length).to.equal(seedNotes.length)
        cy.wrap(response.body).as(seededNoteIdMapAliasName)
      })
    },

    seedLink(type: string, fromNoteTopic: string, toNoteTopic: string) {
      cy.get(`@${seededNoteIdMapAliasName}`).then((seededNoteIdMap) => {
        expect(seededNoteIdMap).haveOwnPropertyDescriptor(fromNoteTopic)
        expect(seededNoteIdMap).haveOwnPropertyDescriptor(toNoteTopic)
        const fromNoteId = seededNoteIdMap[fromNoteTopic]
        const toNoteId = seededNoteIdMap[toNoteTopic]
        postToTestabilityApiSuccessfully(cy, "link_notes", {
          body: {
            type,
            source_id: fromNoteId,
            target_id: toNoteId,
          },
        })
      })
    },

    getSeededNoteIdByTitle(noteTopic: string) {
      return cy.get(`@${seededNoteIdMapAliasName}`).then((seededNoteIdMap) => {
        expect(seededNoteIdMap).haveOwnPropertyDescriptor(noteTopic)
        return seededNoteIdMap[noteTopic]
      })
    },

    timeTravelTo(day: number, hour: number) {
      this.backendTimeTravelTo(day, hour)
      cy.window().then((window) => {
        cy.tick(hourOfDay(day, hour).getTime() - new window.Date().getTime())
      })
    },

    backendTimeTravelTo(day: number, hour: number) {
      postToTestabilityApiSuccessfully(cy, "time_travel", {
        body: { travel_to: JSON.stringify(hourOfDay(day, hour)) },
      })
    },

    backendTimeTravelRelativeToNow(hours: number) {
      postToTestabilityApiSuccessfully(cy, "time_travel_relative_to_now", {
        body: { hours: JSON.stringify(hours) },
      })
    },

    randomizerAlwaysChooseLast() {
      postToTestabilityApiSuccessfully(cy, "randomizer", { body: { choose: "last" } })
    },

    triggerException() {
      postToTestabilityApi(cy, "trigger_exception", { failOnStatusCode: false })
    },

    shareToBazaar(noteTopic: string) {
      postToTestabilityApiSuccessfully(cy, "share_to_bazaar", { body: { noteTopic } })
    },

    seedCircle(circleInfo: Record<string, string>) {
      postToTestabilityApiSuccessfully(cy, "seed_circle", { body: circleInfo })
    },

    updateCurrentUserSettingsWith(hash: Record<string, string>) {
      postToTestabilityApiSuccessfully(cy, "update_current_user", { body: hash })
    },

    setServiceUrl(serviceName: string, serviceUrl: string) {
      return postToTestabilityApi(cy, `replace_service_url`, {
        body: { [serviceName]: serviceUrl },
      }).then((response) => {
        expect(response.body).to.haveOwnProperty(serviceName)
        expect(response.body[serviceName]).to.include("http")
        cy.wrap(response.body[serviceName])
      })
    },
    mockBrowserTime() {
      //
      // when using `cy.clock()` to set the time,
      // for Vue component with v-if for a ref/react object that is changed during mount by async call
      // the event, eg. click, will not work.
      //
      cy.clock(hourOfDay(0, 0), [
        "setTimeout",
        "setInterval",
        "clearInterval",
        "clearTimeout",
        "Date",
      ])
    },
    mockService(serviceMocker: ServiceMocker) {
      this.setServiceUrl(serviceMocker.serviceName, serviceMocker.serviceUrl).as(
        serviceMocker.savedServiceUrlName,
      )
      serviceMocker.install()
    },

    restoreMockedService(serviceMocker: ServiceMocker) {
      cy.get(`@${serviceMocker.savedServiceUrlName}`).then((saved) =>
        this.setServiceUrl(serviceMocker.serviceName, saved as unknown as string),
      )
    },
  }
}

export default testability
