/**
 * CLI recall session assertions. Domain: recall session state after /stop.
 */
import { getRecallDisplaySections } from '../../../step_definitions/cliSectionParser'

const OUTPUT_ALIAS = '@doughnutOutput'

function withOutput(cb: (output: string) => void): void {
  cy.get<string>(OUTPUT_ALIAS).then(cb)
}

function recallSession() {
  return {
    expectStopped() {
      withOutput((output) => {
        const { currentGuidanceAndHistory, historyOutput } =
          getRecallDisplaySections(output)
        expect(currentGuidanceAndHistory).to.include(
          'What is the meaning of sedition?'
        )
        expect(historyOutput).to.include('Stopped recall')
      })
    },
    expectStoppedDuringReview() {
      withOutput((output) => {
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
