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
    expectDailyProbeTrend() {
      cy.get('[data-testid="daily-probe-trend"]').should('be.visible')
      return this
    },
    expectNoDailyProbeTrend() {
      cy.get('[data-testid="daily-probe-trend"]').should('not.exist')
      return this
    },
    expectEmptyRecallStats() {
      cy.get('[data-testid="recall-stats-empty"]').should('be.visible')
      return this
    },
    expectDailyProbeSpeedTrendDays(days: number) {
      cy.window().then((win) => {
        const todayIso = new win.Date().toISOString()
        cy.get('[data-testid="daily-probe-speed-polyline"]').should(($el) => {
          const points = ($el.attr('points') ?? '')
            .trim()
            .split(/\s+/)
            .filter(Boolean)
          expect(
            points.length,
            `Expected Daily probe speed trend to show ${days} day(s), but found ${points.length} (browser ${todayIso})`
          ).to.equal(days)
        })
      })
      return this
    },
    viewTrendWindow(window: 30 | 90 | 'all') {
      cy.get(`[data-testid="trend-window-${window}"]`).click()
      return this
    },
  }
}

export const visitRecallStatsPage = () => {
  router().visitNamed('settingsRecallStats')
  waitUntilAppIsNotBusy()
  return recallStatsPage()
}
