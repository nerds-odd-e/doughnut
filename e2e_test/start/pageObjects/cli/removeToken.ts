import { pastCliAssistantMessages } from './outputAssertions'

export function expectedLocalRemoveSuccessMessage(label: string): string {
  return `Token "${label}" removed.`
}

export function expectedCompleteRemoveSuccessMessage(label: string): string {
  return `Token "${label}" removed locally and from server.`
}

/**
 * Assertions for `/remove-access-token` and `/remove-access-token-completely` outcomes.
 * Failures use {@link pastCliAssistantMessages} (transcript head/tail preview).
 */
function removeToken() {
  return {
    expectRemoveSuccess(removalType: 'local' | 'complete', label: string) {
      const expected =
        removalType === 'local'
          ? expectedLocalRemoveSuccessMessage(label)
          : expectedCompleteRemoveSuccessMessage(label)
      cy.log(
        `expect remove success: type=${removalType}, label=${JSON.stringify(label)}`
      )
      pastCliAssistantMessages().expectContains(expected)
    },
  }
}

export { removeToken }
