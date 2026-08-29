import { waitUntilAppIsNotBusy } from '../pageBase'
import {
  DAILY_PROBE_INSTRUCTION,
  DAILY_PROBE_ISI_MS,
  dailyProbePracticeSequence,
  dailyProbeScoredSequence,
} from '../../../frontend/src/models/dailyProbe'

const DAILY_PROBE_RESPONSE_MS = 250
const DAILY_PROBE_SIDES = [
  ...dailyProbePracticeSequence,
  ...dailyProbeScoredSequence,
]

export const recallDailyProbeMethods = () => ({
  expectDailyProbeInstruction() {
    cy.get('[data-testid="daily-probe"]').should(
      'contain',
      DAILY_PROBE_INSTRUCTION
    )
    return this
  },
  expectNoDailyProbeInstruction() {
    cy.get('[data-testid="daily-probe"]').should('not.exist')
    return this
  },
  completeDailyProbe() {
    const runAt = (index: number) => {
      if (index >= DAILY_PROBE_SIDES.length) return
      cy.get('[data-testid="daily-probe-stimulus"]').should('be.visible')
      cy.tick(DAILY_PROBE_RESPONSE_MS)
      const key = DAILY_PROBE_SIDES[index] === 'left' ? 'f' : 'j'
      cy.window().then((win) => {
        win.dispatchEvent(
          new win.KeyboardEvent('keydown', { key, bubbles: true })
        )
      })
      cy.tick(DAILY_PROBE_ISI_MS)
      runAt(index + 1)
    }
    runAt(0)
    waitUntilAppIsNotBusy()
    return this
  },
  expectDailyProbeSpeed(speed: string) {
    cy.get('[data-testid="daily-probe-speed"]').should('contain', speed)
    return this
  },
  expectDailyProbeAccuracy(accuracy: string) {
    cy.get('[data-testid="daily-probe-accuracy"]').should('contain', accuracy)
    return this
  },
  expectDailyProbeLapses(lapses: string) {
    cy.get('[data-testid="daily-probe-lapses"]').should('contain', lapses)
    return this
  },
  expectDailyProbeVariability(variability: string) {
    cy.get('[data-testid="daily-probe-variability"]').should(
      'contain',
      variability
    )
    return this
  },
  expectDailyProbeSaved() {
    cy.get('[data-testid="daily-probe-saved"]').should('contain', 'Saved')
    return this
  },
  continueFromDailyProbe() {
    cy.contains('button', 'Continue').click()
    waitUntilAppIsNotBusy()
    return this
  },
})
