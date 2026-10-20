import { navigationActions } from '../actions/navigationActions'
import { waitUntilAppIsNotBusy } from '../pageBase'

function expectRecentUpdateRelation(
  left: string,
  right: string,
  expectation: 'newer' | 'as-recent'
) {
  waitUntilAppIsNotBusy()
  navigationActions.jumpToNotePage(left)
  cy.get('[data-testid="note-updated-at"]')
    .invoke('attr', 'data-freshness')
    .then((leftFreshness) => {
      const leftEmphasis = Number(leftFreshness)
      navigationActions.jumpToNotePage(right)
      cy.get('[data-testid="note-updated-at"]')
        .invoke('attr', 'data-freshness')
        .then((rightFreshness) => {
          const rightEmphasis = Number(rightFreshness)
          if (expectation === 'newer') {
            expect(
              leftEmphasis,
              `Expected "${left}" to appear newer than "${right}" (greater date emphasis), but left=${leftEmphasis}, right=${rightEmphasis}`
            ).to.be.greaterThan(rightEmphasis)
          } else {
            expect(
              leftEmphasis,
              `Expected "${left}" to appear as recent as "${right}" (same date emphasis), but left=${leftEmphasis}, right=${rightEmphasis}`
            ).to.equal(rightEmphasis)
          }
        })
    })
}

export function expectNoteAppearsNewerThan(left: string, right: string) {
  expectRecentUpdateRelation(left, right, 'newer')
}

export function expectNoteAppearsAsRecentAs(left: string, right: string) {
  expectRecentUpdateRelation(left, right, 'as-recent')
}
