/**
 * Cucumber step assertions on `@doughnutOutput`.
 *
 * Capture kinds:
 * - **Non-interactive** — installed CLI subcommands (`version`, `update`): full stdout, no PTY input-ready OSC.
 * - **PTY interactive** — node-pty sessions: stdout includes `INTERACTIVE_INPUT_READY_OSC`.
 */
import {
  countInputBoxTopBorderLinesInInteractivePtyTranscript,
  getCurrentGuidanceAndHistoryRaw,
  getCurrentGuidanceDebug,
  getHistoryInputContent,
  getHistoryOutputContent,
  getRecallDisplaySections,
  ptyStdoutHasInputReadyMarker,
} from '../../../step_definitions/cliSectionParser'

export const OUTPUT_ALIAS = '@doughnutOutput'

const SECTION = {
  nonInteractive: 'non-interactive output',
  historyOutput: 'history output',
  historyInput: 'history input',
  currentGuidance: 'Current guidance',
} as const

type SectionLabel = (typeof SECTION)[keyof typeof SECTION]

const CONTENT_PREVIEW_LEN = 500

const WRONG_NON_INTERACTIVE_STEP =
  'This capture includes the PTY-only “input ready” OSC (real TTY session). ' +
  'Use: Then I should see "…" in the history output — or Current guidance / history input — not non-interactive output.'

const wrongPtyInteractiveStep = (
  section: SectionLabel | 'interactive CLI input box'
) =>
  'No PTY input-ready marker in this capture (subcommand / one-shot spawn). ' +
  `Use: Then I should see "…" in the non-interactive output — not in the ${section}.`

type ExpectedInStdout =
  | { kind: 'nonInteractive' }
  | {
      kind: 'ptyInteractive'
      assertionTarget: SectionLabel | 'interactive CLI input box'
    }

function assertStdoutMatchesStepKind(
  stdout: string,
  expected: ExpectedInStdout
): void {
  const hasMarker = ptyStdoutHasInputReadyMarker(stdout)
  if (expected.kind === 'nonInteractive') {
    expect(!hasMarker, WRONG_NON_INTERACTIVE_STEP).to.be.true
  } else {
    expect(hasMarker, wrongPtyInteractiveStep(expected.assertionTarget)).to.be
      .true
  }
}

function withStdout(run: (stdout: string) => void): void {
  cy.get<string>(OUTPUT_ALIAS).then(run)
}

function withStdoutFor(
  expected: ExpectedInStdout,
  run: (stdout: string) => void
): void {
  withStdout((stdout) => {
    assertStdoutMatchesStepKind(stdout, expected)
    run(stdout)
  })
}

function expectSectionContainsSubstring(
  haystack: string,
  needle: string,
  sectionLabel: string
): void {
  const preview =
    haystack.length > CONTENT_PREVIEW_LEN
      ? `${haystack.slice(0, CONTENT_PREVIEW_LEN)}...`
      : haystack
  expect(
    haystack,
    `Expected "${needle}" in ${sectionLabel}. Content:\n${preview}`
  ).to.include(needle)
}

function nonInteractiveOutput() {
  return {
    expectContains(expected: string) {
      withStdoutFor({ kind: 'nonInteractive' }, (stdout) =>
        expectSectionContainsSubstring(stdout, expected, SECTION.nonInteractive)
      )
    },
  }
}

function historyOutput() {
  const target = SECTION.historyOutput
  return {
    expectContains(expected: string) {
      withStdoutFor(
        { kind: 'ptyInteractive', assertionTarget: target },
        (stdout) =>
          expectSectionContainsSubstring(
            getHistoryOutputContent(stdout),
            expected,
            target
          )
      )
    },
  }
}

function historyInput() {
  const target = SECTION.historyInput
  return {
    expectContains(expected: string) {
      withStdoutFor(
        { kind: 'ptyInteractive', assertionTarget: target },
        (stdout) =>
          expectSectionContainsSubstring(
            getHistoryInputContent(stdout),
            expected,
            target
          )
      )
    },
  }
}

function currentGuidanceFailureMessage(
  stdout: string,
  expected: string
): string {
  const { currentGuidanceContent, inputBoxLineRange, lineCount, rawTail } =
    getCurrentGuidanceDebug(stdout)
  const linesAfterBox =
    inputBoxLineRange.end >= 0 ? lineCount - inputBoxLineRange.end - 1 : 0
  return [
    `Expected "${expected}" in ${SECTION.currentGuidance} (prompts, hints, options for the current input).`,
    ``,
    `Parser: input box ┌ at line ${inputBoxLineRange.start}, └ at line ${inputBoxLineRange.end} of ${lineCount} lines. Lines after └: ${linesAfterBox}.`,
    ``,
    `${SECTION.currentGuidance}: ${currentGuidanceContent ? `"${currentGuidanceContent}"` : '(empty)'}`,
    ``,
    `Raw output tail (\\r→\\r \\n→\\n ):`,
    rawTail,
  ].join('\n')
}

function currentGuidance() {
  const target = SECTION.currentGuidance
  return {
    expectContains(expected: string) {
      withStdoutFor(
        { kind: 'ptyInteractive', assertionTarget: target },
        (stdout) => {
          const { currentGuidanceAndHistory } = getRecallDisplaySections(stdout)
          const msg = currentGuidanceAndHistory.includes(expected)
            ? undefined
            : currentGuidanceFailureMessage(stdout, expected)
          expect(currentGuidanceAndHistory, msg).to.include(expected)
        }
      )
    },
    expectStyled(expected: string) {
      withStdoutFor(
        { kind: 'ptyInteractive', assertionTarget: target },
        (stdout) => {
          const raw = getCurrentGuidanceAndHistoryRaw(stdout)
          expectSectionContainsSubstring(
            raw,
            expected,
            `raw ${SECTION.currentGuidance}`
          )
          const hasBold = raw.includes('\x1b[1m')
          const hasItalic = raw.includes('\x1b[3m')
          expect(
            hasBold || hasItalic,
            `Expected ANSI styling (bold or italic) in ${SECTION.currentGuidance}. Raw length: ${raw.length}`
          ).to.be.true
        }
      )
    },
  }
}

function inputBoxTopBorder() {
  return {
    expectExactlyOne() {
      withStdoutFor(
        {
          kind: 'ptyInteractive',
          assertionTarget: 'interactive CLI input box',
        },
        (stdout) => {
          const count =
            countInputBoxTopBorderLinesInInteractivePtyTranscript(stdout)
          expect(
            count,
            `Expected exactly one input box top border (┌─┐) in simulated interactive PTY grid, found ${count}`
          ).to.equal(1)
        }
      )
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
