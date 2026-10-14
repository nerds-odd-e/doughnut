export const EXCLUDED_ATTEMPT_NOTICE = '[data-test="excluded-attempt"]'

export const expectExcludedAsAmbiguous = (
  notice: Cypress.Chainable<JQuery<HTMLElement>>,
  reason: string,
  where: string
) => {
  notice.should(($notice) => {
    const actual = $notice.text().replace(/\s+/g, ' ').trim()
    const expectContains = (expected: string) =>
      expect(
        actual,
        `Expected the ${where} to read "${expected}", but found "${actual}"`
      ).to.contain(expected)
    expectContains('Excluded — ambiguous question')
    expectContains(reason)
    expectContains('originally marked incorrect')
    expectContains(
      'This attempt is not counted as failed or successful recall.'
    )
  })
}
