import { formField } from '../formField'
import { waitUntilAppIsNotBusy } from '../pageBase'

const UNFINISHED_FEATURE_INDICATOR = 'Feature Toggle is On'

/** Home route: welcome banner with the signed-in user's display name */
export const assumeHomePage = () => {
  waitUntilAppIsNotBusy()
  return {
    expectWelcomeHeadingNamesUser(displayName: string) {
      cy.get('h1.welcome-text').should('contain', `Welcome ${displayName}`)
      return this
    },
    turnOnFeatureToggle() {
      cy.get('button[title="Testability"]').click()
      formField('Feature Toggle').click()
      return this
    },
    expectUnfinishedFeatureIndicator() {
      cy.contains(UNFINISHED_FEATURE_INDICATOR).should('exist')
      return this
    },
    expectNoUnfinishedFeatureIndicator() {
      cy.contains(UNFINISHED_FEATURE_INDICATOR).should('not.exist')
      return this
    },
  }
}

export const visitHomePage = () => {
  cy.visit('/')
  cy.contains('Welcome')
  return assumeHomePage()
}
