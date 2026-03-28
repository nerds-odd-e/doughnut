/**
 * Cucumber assertions on `@doughnutOutput`.
 *
 * - **Non-interactive**: one-shot CLI output.
 * - **Past messages**: parsed transcript — domain steps “past CLI assistant messages” / “past user messages”.
 * - **Simulated PTY screen**: cursor/erase replay — “user-visible” plain text.
 */
import {
  countInputBoxTopBorderLinesInInteractivePtyTranscript,
  getPastUserMessagesContent,
  getPastCliAssistantMessagesContent,
  ptyTranscriptSimulatedPlainScreen,
} from '../../../step_definitions/cliSectionParser'

export const OUTPUT_ALIAS = '@doughnutOutput'

const SECTION = {
  nonInteractive: 'non-interactive output',
  pastCliAssistantMessages: 'past CLI assistant messages',
  pastUserMessages: 'past user messages',
  currentGuidance: 'Current guidance',
  simulatedVisiblePtyScreen: 'simulated visible PTY screen',
} as const

type SectionLabel = (typeof SECTION)[keyof typeof SECTION]

const CONTENT_PREVIEW_LEN = 500
const SIMULATED_SCREEN_TAIL_LEN = 1200

const WRONG_NON_INTERACTIVE_STEP =
  'This capture looks like PTY interactive output. ' +
  'Use: Then I should see "…" in past CLI assistant messages — or Current guidance / past user messages — not non-interactive output.'

const wrongPtyInteractiveStep = (
  section: SectionLabel | 'interactive CLI input box'
) =>
  'No interactive command-line prompt row detected in this capture (likely subcommand / one-shot spawn). ' +
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
  const looksInteractive =
    countInputBoxTopBorderLinesInInteractivePtyTranscript(stdout) > 0 ||
    stdout.includes('\x1b[2K') ||
    stdout.includes('y or n; /stop to exit recall') ||
    stdout.includes('↑↓ Enter or number to select; Esc to cancel') ||
    stdout.includes('↑↓ Enter to select; other keys cancel')
  if (expected.kind === 'nonInteractive') {
    expect(!looksInteractive, WRONG_NON_INTERACTIVE_STEP).to.be.true
  } else {
    expect(looksInteractive, wrongPtyInteractiveStep(expected.assertionTarget))
      .to.be.true
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

/** Substring must appear on the simulated plain screen (Ink live region, not line-split merge). */
function expectInteractivePtySimulatedScreenContains(args: {
  assertionTarget: SectionLabel | 'interactive CLI input box'
  needle: string
  whenMissing: string
}): void {
  withStdoutFor(
    { kind: 'ptyInteractive', assertionTarget: args.assertionTarget },
    (stdout) => {
      const plain = ptyTranscriptSimulatedPlainScreen(stdout)
      const tail = plain.slice(-SIMULATED_SCREEN_TAIL_LEN)
      expect(
        plain,
        `Expected "${args.needle}". ${args.whenMissing}\nTail:\n${tail}`
      ).to.include(args.needle)
    }
  )
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

function pastCliAssistantMessages() {
  const target = SECTION.pastCliAssistantMessages
  return {
    expectContains(expected: string) {
      withStdoutFor(
        { kind: 'ptyInteractive', assertionTarget: target },
        (stdout) =>
          expectSectionContainsSubstring(
            getPastCliAssistantMessagesContent(stdout),
            expected,
            target
          )
      )
    },
  }
}

function pastUserMessages() {
  const target = SECTION.pastUserMessages
  return {
    expectContains(expected: string) {
      withStdoutFor(
        { kind: 'ptyInteractive', assertionTarget: target },
        (stdout) =>
          expectSectionContainsSubstring(
            getPastUserMessagesContent(stdout),
            expected,
            target
          )
      )
    },
  }
}

function currentGuidance() {
  const target = SECTION.currentGuidance
  return {
    expectContains(expected: string) {
      expectInteractivePtySimulatedScreenContains({
        assertionTarget: target,
        needle: expected,
        whenMissing: `Gherkin step “… in the Current guidance” uses ${SECTION.simulatedVisiblePtyScreen} (cursor/erase replay).`,
      })
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
            `Expected exactly one live command-line prompt row (→) in simulated interactive PTY grid, found ${count}`
          ).to.equal(1)
        }
      )
    },
  }
}

export {
  nonInteractiveOutput,
  pastCliAssistantMessages,
  pastUserMessages,
  currentGuidance,
  inputBoxTopBorder,
}
