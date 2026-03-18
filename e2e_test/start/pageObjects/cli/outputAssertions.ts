/**
 * CLI output section assertions. Domain concepts (from cli.mdc):
 * - Non-interactive output: entire stdout when running with -c, piped, or installed binary
 * - History output: interactive only, past command results
 * - History input: interactive only, past user input lines
 * - Current guidance: interactive only, prompts/hints/options for current input
 */
import {
  getHistoryOutputContent,
  getHistoryInputContent,
  getRecallDisplaySections,
  getCurrentGuidanceDebug,
  getCurrentGuidanceAndHistoryRaw,
} from '../../../step_definitions/cliSectionParser'

const SECTION_LABELS = {
  nonInteractiveOutput: 'non-interactive output',
  historyOutput: 'history output',
  historyInput: 'history input',
  currentGuidance: 'Current guidance',
} as const

const OUTPUT_ALIAS = '@doughnutOutput'
const CONTENT_PREVIEW_LEN = 500

function withOutput(cb: (output: string) => void): void {
  cy.get<string>(OUTPUT_ALIAS).then(cb)
}

function assertIncludes(
  content: string,
  expected: string,
  sectionLabel: string
): void {
  const preview =
    content.length > CONTENT_PREVIEW_LEN
      ? `${content.slice(0, CONTENT_PREVIEW_LEN)}...`
      : content
  expect(
    content,
    `Expected "${expected}" in ${sectionLabel}. Content:\n${preview}`
  ).to.include(expected)
}

function assertNotIncludes(
  content: string,
  expected: string,
  sectionLabel: string
): void {
  expect(
    content,
    `Did not expect "${expected}" in ${sectionLabel}`
  ).not.to.include(expected)
}

function buildCurrentGuidanceFailureMessage(
  output: string,
  expected: string
): string {
  const { currentGuidanceContent, inputBoxLineRange, lineCount, rawTail } =
    getCurrentGuidanceDebug(output)
  const linesAfterBox =
    inputBoxLineRange.end >= 0 ? lineCount - inputBoxLineRange.end - 1 : 0
  return [
    `Expected "${expected}" in ${SECTION_LABELS.currentGuidance} (prompts, hints, options for the current input).`,
    ``,
    `Parser: input box ┌ at line ${inputBoxLineRange.start}, └ at line ${inputBoxLineRange.end} of ${lineCount} lines. Lines after └: ${linesAfterBox}.`,
    ``,
    `${SECTION_LABELS.currentGuidance}: ${currentGuidanceContent ? `"${currentGuidanceContent}"` : '(empty)'}`,
    ``,
    `Raw output tail (\\r→\\r \\n→\\n ):`,
    rawTail,
  ].join('\n')
}

function nonInteractiveOutput() {
  return {
    expectContains(expected: string) {
      withOutput((output) =>
        assertIncludes(output, expected, SECTION_LABELS.nonInteractiveOutput)
      )
    },
    expectNotContains(expected: string) {
      withOutput((output) =>
        assertNotIncludes(output, expected, SECTION_LABELS.nonInteractiveOutput)
      )
    },
  }
}

function historyOutput() {
  return {
    expectContains(expected: string) {
      withOutput((output) => {
        const content = getHistoryOutputContent(output)
        assertIncludes(content, expected, SECTION_LABELS.historyOutput)
      })
    },
    expectNotContains(expected: string) {
      withOutput((output) => {
        const content = getHistoryOutputContent(output)
        assertNotIncludes(content, expected, SECTION_LABELS.historyOutput)
      })
    },
  }
}

function historyInput() {
  return {
    expectContains(expected: string) {
      withOutput((output) => {
        const content = getHistoryInputContent(output)
        assertIncludes(content, expected, SECTION_LABELS.historyInput)
      })
    },
  }
}

function currentGuidance() {
  return {
    expectContains(expected: string) {
      withOutput((output) => {
        const { currentGuidanceAndHistory } = getRecallDisplaySections(output)
        const msg = currentGuidanceAndHistory.includes(expected)
          ? undefined
          : buildCurrentGuidanceFailureMessage(output, expected)
        expect(currentGuidanceAndHistory, msg).to.include(expected)
      })
    },
    expectStyled(expected: string) {
      withOutput((output) => {
        const rawContent = getCurrentGuidanceAndHistoryRaw(output)
        assertIncludes(
          rawContent,
          expected,
          `raw ${SECTION_LABELS.currentGuidance}`
        )
        const hasBold = rawContent.includes('\x1b[1m')
        const hasItalic = rawContent.includes('\x1b[3m')
        expect(
          hasBold || hasItalic,
          `Expected ANSI styling (bold or italic) in ${SECTION_LABELS.currentGuidance}. Raw length: ${rawContent.length}`
        ).to.be.true
      })
    },
  }
}

export { nonInteractiveOutput, historyOutput, historyInput, currentGuidance }
