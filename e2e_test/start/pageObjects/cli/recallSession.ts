/**
 * CLI recall session assertions. Domain: recall session state after /stop.
 * Stopped from MCQ: question text may no longer be on the live PTY grid (Ink cleared it);
 * merged transcript section still finds it. “Stopped recall” is chat history output.
 */
import { getRecallDisplaySections } from '../../../step_definitions/cliSectionParser'
import { OUTPUT_ALIAS } from './outputAssertions'

function recallSession() {
  return {
    expectStopped() {
      cy.get<string>(OUTPUT_ALIAS).then((output) => {
        const { currentGuidanceAndHistory, historyOutput: historyOut } =
          getRecallDisplaySections(output)
        expect(currentGuidanceAndHistory).to.include(
          'What is the meaning of sedition?'
        )
        expect(historyOut).to.include('Stopped recall')
      })
    },
  }
}

export { recallSession }
