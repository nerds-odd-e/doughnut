import { waitUntilAppIsNotBusy } from '../pageBase'

const DAILY_PROBE_INSTRUCTION =
  'Each trial shows ← or →. Press F for left, J for right (arrow keys also work). Go as fast as you can without mistakes.'
const DAILY_PROBE_TRIAL_COUNT = 24
const DAILY_PROBE_RESPONSE_MS = 250
const DAILY_PROBE_ISI_MS = 2000

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
    const runRemaining = (left: number) => {
      if (left === 0) return
      cy.get('[data-testid="daily-probe-stimulus"]')
        .should('be.visible')
        .invoke('text')
        .then((text) => {
          cy.tick(DAILY_PROBE_RESPONSE_MS)
          const key = text.includes('←') ? 'f' : 'j'
          cy.window().then((win) => {
            win.dispatchEvent(
              new win.KeyboardEvent('keydown', { key, bubbles: true })
            )
          })
          cy.tick(DAILY_PROBE_ISI_MS)
          runRemaining(left - 1)
        })
    }
    runRemaining(DAILY_PROBE_TRIAL_COUNT)
    return this
  },
  expectDailyProbeSpeed(speed: string) {
    cy.get('[data-testid="daily-probe-speed"]').should('contain', speed)
    return this
  },
  continueFromDailyProbe() {
    cy.contains('button', 'Continue').click()
    waitUntilAppIsNotBusy()
    return this
  },
})
