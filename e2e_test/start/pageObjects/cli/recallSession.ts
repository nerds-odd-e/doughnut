/**
 * CLI recall session assertions. Domain: recall session state after /stop.
 * - Stopped from MCQ: question display, user typed /stop
 * - Stopped during review: y/n prompt, user typed /stop
 */
import { getRecallDisplaySections } from '../../../step_definitions/cliSectionParser'
import { OUTPUT_ALIAS } from './outputAssertions'

function recallSession() {
  return {
    expectStopped() {
      cy.get<string>(OUTPUT_ALIAS).then((output) => {
        const { currentGuidanceAndHistory, historyOutput } =
          getRecallDisplaySections(output)
        expect(currentGuidanceAndHistory).to.include(
          'What is the meaning of sedition?'
        )
        expect(historyOutput).to.include('Stopped recall')
      })
    },
    expectStoppedDuringReview() {
      cy.get<string>(OUTPUT_ALIAS).then((output) => {
        const { currentGuidanceAndHistory, historyOutput } =
          getRecallDisplaySections(output)
        expect(currentGuidanceAndHistory).to.include('sedition')
        expect(currentGuidanceAndHistory).to.include('Yes, I remember?')
        expect(historyOutput).to.include('Stopped recall')
      })
    },
  }
}

export { recallSession }
