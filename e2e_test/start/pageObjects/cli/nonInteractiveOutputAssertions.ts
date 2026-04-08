/** Cypress `cy.get('@…')` handle; must match `.as(…)` on the stored stdout chain. */
export const DOUGHNUT_OUTPUT_CY_ALIAS = 'doughnutOutput'
export const OUTPUT_ALIAS = `@${DOUGHNUT_OUTPUT_CY_ALIAS}`

const SECTION = {
  nonInteractive: 'non-interactive output',
} as const

function assertSectionContainsSubstring(
  haystack: string,
  needle: string,
  sectionLabel: string
): void {
  if (haystack.includes(needle)) return
  throw new Error(`Expected "${needle}" in ${sectionLabel}`)
}

function nonInteractiveOutput() {
  return {
    expectContains(expected: string) {
      return cy.get<string>(OUTPUT_ALIAS).then((stdout) => {
        assertSectionContainsSubstring(stdout, expected, SECTION.nonInteractive)
      })
    },
  }
}

export { nonInteractiveOutput }
