import mock_services from "./mock_services"
import { questionGenerationService } from "./questionGenerationService"
import { higherOrderActions } from "./higherOrderActions"
import basicActions from "./basicActions"
import TestabilityHelper from "support/TestabilityHelper"

const start = {
  ...basicActions,
  ...higherOrderActions,
  questionGenerationService,
  testability() {
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
        testability.getSeededNoteIdByTitle(cy, noteTopic)
      },

      timeTravelTo(day: number, hour: number) {
        cy.wrap(testability).backendTimeTravelTo(day, hour)
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
    }
  },
}
export default start
export { mock_services }
