/**
 * CLI access-token removal success assertions.
 * Domain: local (config only) vs complete (config + server revocation).
 */
import { nonInteractiveOutput } from './outputAssertions'

function removeToken() {
  return {
    expectSuccess(removalType: string, label: string) {
      const expected =
        removalType === 'complete'
          ? 'removed locally and from server'
          : `Token "${label}" removed.`
      nonInteractiveOutput().expectContains(expected)
    },
  }
}

export { removeToken }
