import { waitUntilAppIsNotBusy } from '../pageBase'

export const recallStatsPage = () => {
  return {
    expectPaceTileContains(text: string) {
      cy.get('[data-testid="pace-tile"]', { timeout: 15000 }).should(
        'contain.text',
        text
      )
      return this
    },
  }
}

export const visitRecallStatsPage = () => {
  cy.visit('/settings/recall-stats')
  waitUntilAppIsNotBusy()
  return recallStatsPage()
}
