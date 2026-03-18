import {
  getHistoryOutputContent,
  getHistoryInputContent,
  getRecallDisplaySections,
  getCurrentGuidanceDebug,
  getCurrentGuidanceAndHistoryRaw,
} from '../../../step_definitions/cliSectionParser'

const NON_INTERACTIVE_OUTPUT = 'non-interactive output'
const CURRENT_GUIDANCE = 'Current guidance'

function buildCurrentGuidanceFailureMessage(
  output: string,
  expected: string
): string {
  const { currentGuidanceContent, inputBoxLineRange, lineCount, rawTail } =
    getCurrentGuidanceDebug(output)
  const linesAfterBox =
    inputBoxLineRange.end >= 0 ? lineCount - inputBoxLineRange.end - 1 : 0
  return [
    `Expected "${expected}" in ${CURRENT_GUIDANCE} (prompts, hints, options for the current input).`,
    ``,
    `Parser: input box ┌ at line ${inputBoxLineRange.start}, └ at line ${inputBoxLineRange.end} of ${lineCount} lines. Lines after └: ${linesAfterBox}.`,
    ``,
    `${CURRENT_GUIDANCE}: ${currentGuidanceContent ? `"${currentGuidanceContent}"` : '(empty)'}`,
    ``,
    `Raw output tail (\\r→\\r \\n→\\n ):`,
    rawTail,
  ].join('\n')
}

function nonInteractiveOutput() {
  return {
    expectContains(expected: string) {
      cy.get<string>('@doughnutOutput').then((output) => {
        expect(
          output,
          `Expected "${expected}" in ${NON_INTERACTIVE_OUTPUT}. Content:\n${output.slice(0, 500)}${output.length > 500 ? '...' : ''}`
        ).to.include(expected)
      })
    },
    expectNotContains(expected: string) {
      cy.get<string>('@doughnutOutput').then((output) => {
        expect(output, `Did not expect "${expected}"`).not.to.include(expected)
      })
    },
  }
}

function historyOutput() {
  return {
    expectContains(expected: string) {
      cy.get<string>('@doughnutOutput').then((output) => {
        const content = getHistoryOutputContent(output)
        expect(
          content,
          `Expected "${expected}" in history output. Content:\n${content.slice(0, 500)}${content.length > 500 ? '...' : ''}`
        ).to.include(expected)
      })
    },
    expectNotContains(expected: string) {
      cy.get<string>('@doughnutOutput').then((output) => {
        const content = getHistoryOutputContent(output)
        expect(
          content,
          `Did not expect "${expected}" in history output`
        ).not.to.include(expected)
      })
    },
  }
}

function historyInput() {
  return {
    expectContains(expected: string) {
      cy.get<string>('@doughnutOutput').then((output) => {
        const content = getHistoryInputContent(output)
        expect(
          content,
          `Expected "${expected}" in history input. Content:\n${content.slice(0, 500)}${content.length > 500 ? '...' : ''}`
        ).to.include(expected)
      })
    },
  }
}

function currentGuidance() {
  return {
    expectContains(expected: string) {
      cy.get<string>('@doughnutOutput').then((output) => {
        const { currentGuidanceAndHistory } = getRecallDisplaySections(output)
        const msg = currentGuidanceAndHistory.includes(expected)
          ? undefined
          : buildCurrentGuidanceFailureMessage(output, expected)
        expect(currentGuidanceAndHistory, msg).to.include(expected)
      })
    },
    expectStyled(expected: string) {
      cy.get<string>('@doughnutOutput').then((output) => {
        const rawContent = getCurrentGuidanceAndHistoryRaw(output)
        expect(
          rawContent,
          `Expected "${expected}" in raw ${CURRENT_GUIDANCE}`
        ).to.include(expected)
        const hasBold = rawContent.includes('\x1b[1m')
        const hasItalic = rawContent.includes('\x1b[3m')
        expect(
          hasBold || hasItalic,
          `Expected ANSI styling (bold or italic) in ${CURRENT_GUIDANCE}. Raw length: ${rawContent.length}`
        ).to.be.true
      })
    },
  }
}

export { nonInteractiveOutput, historyOutput, historyInput, currentGuidance }
