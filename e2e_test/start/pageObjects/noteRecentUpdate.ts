import { navigationActions } from '../actions/navigationActions'
import { waitUntilAppIsNotBusy } from '../pageBase'

function firstRgbChannel(cssColor: string): number {
  return parseInt(cssColor.match(/\d+/)?.[0] ?? '', 10)
}

function expectRecentUpdateRelation(
  left: string,
  right: string,
  expectation: 'newer' | 'as-recent'
) {
  waitUntilAppIsNotBusy()
  navigationActions.jumpToNotePage(left)
  cy.get('.note-recent-update-indicator')
    .invoke('css', 'color')
    .then((leftColor) => {
      const leftChannel = firstRgbChannel(String(leftColor))
      navigationActions.jumpToNotePage(right)
      cy.get('.note-recent-update-indicator')
        .invoke('css', 'color')
        .then((rightColor) => {
          const rightChannel = firstRgbChannel(String(rightColor))
          if (expectation === 'newer') {
            expect(
              leftChannel,
              `Expected "${left}" to appear newer than "${right}" (higher freshness channel), but left=${leftChannel}, right=${rightChannel}`
            ).to.be.greaterThan(rightChannel)
          } else {
            expect(
              leftChannel,
              `Expected "${left}" to appear as recent as "${right}" (same freshness channel), but left=${leftChannel}, right=${rightChannel}`
            ).to.equal(rightChannel)
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
