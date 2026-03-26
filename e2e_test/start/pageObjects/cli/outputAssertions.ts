/**
 * Cucumber assertions on `@doughnutOutput`.
 *
 * - **Non-interactive**: one-shot CLI (no input-ready OSC).
 * - **Chat history**: parsed scrollback — domain steps “history output / input”.
 * - **Simulated PTY screen**: cursor/erase replay — “user-visible” plain text.
 * - **Recall /stop (MCQ)**: line-split merge can still hold the stem after Ink cleared the live grid.
 */
import {
  countInputBoxTopBorderLinesInInteractivePtyTranscript,
  getHistoryInputContent,
  getHistoryOutputContent,
  getRecallDisplaySections,
  getRecallMergedTranscriptRaw,
  ptyTranscriptSimulatedPlainScreen,
  ptyStdoutHasInputReadyMarker,
} from '../../../step_definitions/cliSectionParser'

export const OUTPUT_ALIAS = '@doughnutOutput'

const SECTION = {
  nonInteractive: 'non-interactive output',
  historyOutput: 'history output',
  historyInput: 'history input',
  currentGuidance: 'Current guidance',
  simulatedVisiblePtyScreen: 'simulated visible PTY screen',
  recallStopAssertion: 'interactive CLI transcript (recall /stop)',
} as const

type SectionLabel = (typeof SECTION)[keyof typeof SECTION]

const CONTENT_PREVIEW_LEN = 500
const SIMULATED_SCREEN_TAIL_LEN = 1200

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
    /**
     * Bold/italic: PTY replay skips SGR (`m`), so use merged transcript bytes (Ink still emits ANSI there).
     */
    expectStyled(expected: string) {
      withStdoutFor(
        { kind: 'ptyInteractive', assertionTarget: target },
        (stdout) => {
          const raw = getRecallMergedTranscriptRaw(stdout)
          expectSectionContainsSubstring(
            raw,
            expected,
            `raw merged transcript (${SECTION.currentGuidance} styled step)`
          )
          const hasBold = raw.includes('\x1b[1m')
          const hasItalic = raw.includes('\x1b[3m')
          expect(
            hasBold || hasItalic,
            `Expected ANSI bold or italic in merged transcript for styled Current guidance. Raw length: ${raw.length}`
          ).to.be.true
        }
      )
    },
  }
}

/** Recall session y/n: prompt must appear on simulated PTY screen (not only in line-split merge / scrollback ghosts). */
function assertRecallSessionPromptOnSimulatedPtyScreen(
  expectedPromptSubstring: string
): void {
  expectInteractivePtySimulatedScreenContains({
    assertionTarget: SECTION.simulatedVisiblePtyScreen,
    needle: expectedPromptSubstring,
    whenMissing: `If this fails but other substring checks passed, the text may exist only in scrollback while Ink repainted the live region — use RecallInkConfirmPanel guidanceLines (ShellSessionRoot in-session), not only grey writeCurrentPrompt.`,
  })
}

function recallSession() {
  return {
    /** MCQ stem may be gone from simulated screen after /stop; line-split merge + chat history still hold it. */
    expectStopped() {
      withStdoutFor(
        {
          kind: 'ptyInteractive',
          assertionTarget: SECTION.recallStopAssertion,
        },
        (stdout) => {
          const { mergedTranscriptPlain, chatHistoryOutputPlain } =
            getRecallDisplaySections(stdout)
          expect(mergedTranscriptPlain).to.include(
            'What is the meaning of sedition?'
          )
          expect(chatHistoryOutputPlain).to.include('Stopped recall')
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
            `Expected exactly one live command-line prompt row (→) in simulated interactive PTY grid, found ${count}`
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
  recallSession,
  assertRecallSessionPromptOnSimulatedPtyScreen,
  inputBoxTopBorder,
}
