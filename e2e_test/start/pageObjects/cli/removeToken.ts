/**
 * CLI remove-access-token success assertions. Domain: local vs complete removal.
 */
import { nonInteractiveOutput } from './outputAssertions'

function removeToken() {
  return {
    expectSuccess(removalType: string, label: string) {
      const expected =
        removalType === 'local'
          ? `Token "${label}" removed.`
          : 'removed locally and from server'
      nonInteractiveOutput().expectContains(expected)
    },
  }
}

export { removeToken }
