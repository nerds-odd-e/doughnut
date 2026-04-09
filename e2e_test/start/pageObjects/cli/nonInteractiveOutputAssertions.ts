import { nonInteractiveCliOutputAssertRequest } from './outputAssertions'

function nonInteractiveOutput() {
  return {
    expectContains(expected: string) {
      return cy.task<null>(
        'cliNonInteractiveAssert',
        nonInteractiveCliOutputAssertRequest(expected)
      )
    },
  }
}

export { nonInteractiveOutput }
