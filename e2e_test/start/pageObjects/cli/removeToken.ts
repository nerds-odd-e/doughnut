/**
 * CLI access-token removal success assertions.
 * Domain: local (config only) vs complete (config + server revocation).
 */
import { historyOutput } from './outputAssertions'

function removeToken() {
  return {
    expectSuccess(removalType: string, label: string) {
      const expected =
        removalType === 'complete'
          ? 'removed locally and from server'
          : `Token "${label}" removed.`
      historyOutput().expectContains(expected)
    },
  }
}

export { removeToken }
