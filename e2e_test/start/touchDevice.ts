let enabled = false

const setTouchEmulation = (on: boolean) => {
  enabled = on
  return Cypress.automation('remote:debugger:protocol', {
    command: 'Emulation.setTouchEmulationEnabled',
    params: { enabled: on, maxTouchPoints: on ? 5 : 0 },
  })
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
