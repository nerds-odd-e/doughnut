import { waitUntilAppIsNotBusy } from '../pageBase'
import router from '../router'

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
  router().push('settingsRecallStats')
  waitUntilAppIsNotBusy()
  return recallStatsPage()
}
