let enabled = false

const setTouchEmulation = (on: boolean) => {
  enabled = on
  return cy.wrap(null, { log: false }).then(() =>
    Cypress.automation('remote:debugger:protocol', {
      command: 'Emulation.setTouchEmulationEnabled',
      params: on ? { enabled: true, maxTouchPoints: 5 } : { enabled: false },
    })
  )
}

export const touchDevice = {
  use() {
    setTouchEmulation(true)
  },
  reset() {
    if (enabled) setTouchEmulation(false)
  },
  expectCoarsePointer() {
    cy.window().should((win) => {
      expect(win.matchMedia('(pointer: coarse)').matches).to.equal(true)
    })
  },
}
