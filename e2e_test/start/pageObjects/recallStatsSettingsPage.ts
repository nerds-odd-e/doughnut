import { waitUntilAppIsNotBusy } from '../pageBase'
import router from '../router'

export const visitRecallStatsSettingsPage = () => {
  // Remount: the learner returns to a freshly loaded app.
  router().visitNamed('settingsRecallStats')
  waitUntilAppIsNotBusy()
  return recallStatsSettingsPage()
}

export const recallStatsSettingsPage = () => {
  return {
    spreadOutRecallBacklog() {
      cy.findByRole('button', { name: 'Spread out my recall backlog' }).click()
      waitUntilAppIsNotBusy()
      return this
    },
    expectForecast(today: number, nextDays: number[]) {
      cy.get('[data-testid="recall-forecast-today"]')
        .invoke('text')
        .should((text) => {
          expect(text.trim(), "today's remaining count").to.equal(
            `Today: ${today}`
          )
        })
      cy.get('[data-testid="recall-forecast-day"]').then(($days) => {
        const counts = $days
          .toArray()
          .slice(0, nextDays.length)
          .map((day) => Number(day.textContent?.split(': ')[1]))
        expect(counts, 'counts for the next days').to.deep.equal(nextDays)
      })
      return this
    },
  }
}
