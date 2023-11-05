import TestabilityHelper from "support/TestabilityHelper"
import ServiceMocker from "support/ServiceMocker"

const testability = () => {
  const testability = new TestabilityHelper()

  return {
    cleanDBAndResetTestabilitySettings() {
      testability.cleanAndReset(cy, 5)
    },

    featureToggle(enabled: boolean) {
      testability.postToTestabilityApiSuccessfully(cy, "feature_toggle", { body: { enabled } })
    },

    seedNotes(seedNotes: unknown[], externalIdentifier = "", circleName = null) {
      testability.seedNotes(cy, seedNotes, externalIdentifier, circleName)
    },

    seedLink(type: string, fromNoteTopic: string, toNoteTopic: string) {
      testability.seedLink(cy, type, fromNoteTopic, toNoteTopic)
    },

    getSeededNoteIdByTitle(noteTopic: string) {
      return testability.getSeededNoteIdByTitle(cy, noteTopic)
    },

    timeTravelTo(day: number, hour: number) {
      this.backendTimeTravelTo(day, hour)
      cy.window().then((window) => {
        cy.tick(testability.hourOfDay(day, hour).getTime() - new window.Date().getTime())
      })
    },

    backendTimeTravelTo(day: number, hour: number) {
      testability.postToTestabilityApiSuccessfully(cy, "time_travel", {
        body: { travel_to: JSON.stringify(testability.hourOfDay(day, hour)) },
      })
    },

    backendTimeTravelRelativeToNow(hours: number) {
      testability.postToTestabilityApiSuccessfully(cy, "time_travel_relative_to_now", {
        body: { hours: JSON.stringify(hours) },
      })
    },

    randomizerAlwaysChooseLast() {
      testability.postToTestabilityApiSuccessfully(cy, "randomizer", { body: { choose: "last" } })
    },

    triggerException() {
      testability.postToTestabilityApi(cy, "trigger_exception", { failOnStatusCode: false })
    },

    shareToBazaar(noteTopic: string) {
      testability.postToTestabilityApiSuccessfully(cy, "share_to_bazaar", { body: { noteTopic } })
    },

    seedCircle(circleInfo: Record<string, string>) {
      testability.postToTestabilityApiSuccessfully(cy, "seed_circle", { body: circleInfo })
    },

    updateCurrentUserSettingsWith(hash: Record<string, string>) {
      testability.postToTestabilityApiSuccessfully(cy, "update_current_user", { body: hash })
    },

    setServiceUrl(serviceName: string, serviceUrl: string) {
      return testability
        .postToTestabilityApi(cy, `replace_service_url`, {
          body: { [serviceName]: serviceUrl },
        })
        .then((response) => {
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
      cy.clock(testability.hourOfDay(0, 0), [
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
