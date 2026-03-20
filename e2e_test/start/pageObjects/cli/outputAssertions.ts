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
  countInputBoxTopBorderLinesInInteractivePtyTranscript,
} from '../../../step_definitions/cliSectionParser'

const SECTION_LABELS = {
  nonInteractiveOutput: 'non-interactive output',
  historyOutput: 'history output',
  historyInput: 'history input',
  currentGuidance: 'Current guidance',
} as const

export const OUTPUT_ALIAS = '@doughnutOutput'
const CONTENT_PREVIEW_LEN = 500

function withOutput(cb: (output: string) => void): void {
  cy.get<string>(OUTPUT_ALIAS).then(cb)
}

function assertInSection(
  content: string,
  expected: string,
  sectionLabel: string,
  expectPresent: boolean
): void {
  const preview =
    content.length > CONTENT_PREVIEW_LEN
      ? `${content.slice(0, CONTENT_PREVIEW_LEN)}...`
      : content
  const message = expectPresent
    ? `Expected "${expected}" in ${sectionLabel}. Content:\n${preview}`
    : `Did not expect "${expected}" in ${sectionLabel}`
  if (expectPresent) {
    expect(content, message).to.include(expected)
  } else {
    expect(content, message).not.to.include(expected)
  }
}

function outputSection(
  getContent: (output: string) => string,
  sectionLabel: string,
  options: { expectNotContains?: boolean } = { expectNotContains: true }
) {
  const { expectNotContains } = options
  return {
    expectContains(expected: string) {
      withOutput((output) =>
        assertInSection(getContent(output), expected, sectionLabel, true)
      )
    },
    ...(expectNotContains && {
      expectNotContains(expected: string) {
        withOutput((output) =>
          assertInSection(getContent(output), expected, sectionLabel, false)
        )
      },
    }),
  }
}

function nonInteractiveOutput() {
  return outputSection((o) => o, SECTION_LABELS.nonInteractiveOutput)
}

function historyOutput() {
  return outputSection(getHistoryOutputContent, SECTION_LABELS.historyOutput)
}

function historyInput() {
  return outputSection(getHistoryInputContent, SECTION_LABELS.historyInput, {
    expectNotContains: false,
  })
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
        assertInSection(
          rawContent,
          expected,
          `raw ${SECTION_LABELS.currentGuidance}`,
          true
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

function inputBoxTopBorder() {
  return {
    expectExactlyOne() {
      withOutput((output) => {
        const count =
          countInputBoxTopBorderLinesInInteractivePtyTranscript(output)
        expect(
          count,
          `Expected exactly one input box top border (┌─┐) in simulated interactive PTY grid, found ${count}`
        ).to.equal(1)
      })
    },
  }
}

export {
  nonInteractiveOutput,
  historyOutput,
  historyInput,
  currentGuidance,
  inputBoxTopBorder,
}
