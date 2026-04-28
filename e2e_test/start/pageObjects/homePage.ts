import { pageIsNotLoading } from '../pageBase'

/** Home route: welcome banner with the signed-in user's display name */
export const assumeHomePage = () => {
  pageIsNotLoading()
  return {
    expectWelcomeHeadingNamesUser(displayName: string) {
      cy.get('h1.welcome-text').should('contain', `Welcome ${displayName}`)
      return this
    },
  }
}
