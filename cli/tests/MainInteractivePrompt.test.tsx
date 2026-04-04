import * as fs from 'node:fs'
import * as os from 'node:os'
import * as path from 'node:path'
import { render } from 'ink-testing-library'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import { interactiveSlashCommands } from '../src/commands/interactiveSlashCommands.js'
import {
  InputHistoryProvider,
  USER_INPUT_HISTORY_FILENAME,
} from '../src/inputHistory/index.js'
import { MainInteractivePrompt } from '../src/mainInteractivePrompt/index.js'
import {
  extendInkRenderForInteractiveTests,
  inkCommandLineProbeUndelete,
  stripAnsi,
  waitUntilInkLastFrameStripped,
} from './inkTestHelpers.js'

/** Expected scroll UI copy (private in commonUIComponents/guidanceListWindowInk). */
const EXPECT_GUIDANCE_MORE_BELOW = '↓ more below'
const EXPECT_GUIDANCE_ROW_BUDGET = 5

let promptConfigDir: string
let prevDoughnutConfigDir: string | undefined

/** StripAnsi line that contains the main `→` prompt (inside the bordered box). */
function lineWithMainPrompt(plain: string): string {
  return plain.split('\n').find((l) => l.includes('→')) ?? ''
}

function rawLineIncludesBoldMarker(raw: string, marker: string): boolean {
  return raw
    .split('\n')
    .some((line) => line.includes(marker) && line.includes('\x1b[1m'))
}

/** Typed buffer after `→`, before Ink right-padding / `│` column border. */
function mainPromptDraftAfterArrow(plain: string): string {
  const lm = lineWithMainPrompt(plain)
  const idx = lm.indexOf('→')
  if (idx < 0) return ''
  const after = lm.slice(idx + '→'.length).trimStart()
  return after.replace(/\s*│.*$/, '').trimEnd()
}

beforeEach(() => {
  prevDoughnutConfigDir = process.env.DOUGHNUT_CONFIG_DIR
  promptConfigDir = fs.mkdtempSync(path.join(os.tmpdir(), 'doughnut-mip-'))
  process.env.DOUGHNUT_CONFIG_DIR = promptConfigDir
})

afterEach(() => {
  if (prevDoughnutConfigDir === undefined) {
    delete process.env.DOUGHNUT_CONFIG_DIR
  } else {
    process.env.DOUGHNUT_CONFIG_DIR = prevDoughnutConfigDir
  }
  try {
    fs.rmSync(promptConfigDir, { recursive: true, force: true })
  } catch {
    // temp dir may be missing
  }
})

const MAIN_PROMPT_PLACEHOLDER = '`exit` to quit.'

async function renderMainInteractivePrompt(
  onCommittedLine: (line: string) => void = () => undefined
) {
  const result = render(
    <InputHistoryProvider>
      <MainInteractivePrompt
        onCommittedCommand={() => undefined}
        onCommittedLine={onCommittedLine}
        slashCommands={interactiveSlashCommands}
        placeholder={MAIN_PROMPT_PLACEHOLDER}
      />
    </InputHistoryProvider>
  )
  await waitUntilInkLastFrameStripped(
    result.lastFrame,
    (f) => f.includes('→') && f.includes('/ commands')
  )
  await inkCommandLineProbeUndelete(result, {
    probeChar: '@',
    probeVisible: (f) => lineWithMainPrompt(f).includes('@'),
    probeHidden: (f) => !lineWithMainPrompt(f).includes('@'),
  })
  return { ...result, ...extendInkRenderForInteractiveTests(result) }
}

describe('MainInteractivePrompt slash guidance (phase 1)', () => {
  test('default shows static / commands hint', async () => {
    const { lastStrippedFrame } = await renderMainInteractivePrompt()
    const plain = lastStrippedFrame()
    expect(plain).toContain('/ commands')
    expect(plain).toContain('`exit` to quit.')
  })

  test('partial / prefix shows matching commands with first row bold-highlighted', async () => {
    const { stdin, lastFrame, waitForLastFrameToInclude, waitUntilLastFrame } =
      await renderMainInteractivePrompt()

    stdin.write('/')
    await waitForLastFrameToInclude('→ /')
    stdin.write('he')
    await waitUntilLastFrame(
      (f) => f.includes('/help') && f.includes('List available commands')
    )

    const raw = lastFrame() ?? ''
    const helpLineRaw = raw
      .split('\n')
      .find((l) => l.includes('/help') && l.includes('List available'))
    expect(helpLineRaw, 'expected a visible /help completion row').toBeTruthy()
    expect(helpLineRaw).toContain('\x1b[1m')
    expect(helpLineRaw).not.toContain('\x1b[7m')
  })

  test('bare slash shows a fixed-height list with more-below when commands overflow budget', async () => {
    const { stdin, lastStrippedFrame, waitUntilLastFrame } =
      await renderMainInteractivePrompt()

    stdin.write('/')
    await waitUntilLastFrame(
      (f) =>
        lineWithMainPrompt(f).includes('→ /') &&
        f.includes(EXPECT_GUIDANCE_MORE_BELOW)
    )

    const frame = lastStrippedFrame()
    const listRows = frame
      .split('\n')
      .filter(
        (l) => l.includes(EXPECT_GUIDANCE_MORE_BELOW) || l.includes('  /')
      )
    expect(listRows.length).toBe(EXPECT_GUIDANCE_ROW_BUDGET)
  })

  test('trailing space after slash command shows hint only, not the completion list', async () => {
    const { stdin, lastStrippedFrame, waitForLastFrameToInclude } =
      await renderMainInteractivePrompt()

    stdin.write('/help ')
    await waitForLastFrameToInclude('→ /help ')
    const frame = lastStrippedFrame()
    expect(frame).toContain('/ commands')
    expect(frame).not.toMatch(/\/help\s+List available commands/)
  })
})

describe('MainInteractivePrompt Tab completion (phase 2)', () => {
  test('Tab with several usages sharing a prefix extends draft to longest common prefix', async () => {
    const {
      stdin,
      lastStrippedFrame,
      waitForLastFrameToInclude,
      waitUntilLastFrame,
    } = await renderMainInteractivePrompt()

    stdin.write('/remove')
    await waitForLastFrameToInclude('→ /remove')
    stdin.write('\t')
    await waitUntilLastFrame((f) =>
      lineWithMainPrompt(f).trimEnd().includes('→ /remove-access-token')
    )
    const promptLine = lineWithMainPrompt(lastStrippedFrame()).trimEnd()
    expect(promptLine.includes('→ /remove-access-token')).toBe(true)
  })

  test('Tab with a unique matching usage completes to usage plus trailing space', async () => {
    const { stdin, lastStrippedFrame, waitForLastFrameToInclude } =
      await renderMainInteractivePrompt()

    stdin.write('/hel')
    await waitForLastFrameToInclude('→ /hel')
    stdin.write('\t')
    await waitForLastFrameToInclude('→ /help ')
    expect(lineWithMainPrompt(lastStrippedFrame()).includes('/help ')).toBe(
      true
    )
  })

  test('Tab with a unique match does not append <argument> placeholder to the draft', async () => {
    const { stdin, lastStrippedFrame, waitForLastFrameToInclude } =
      await renderMainInteractivePrompt()

    stdin.write('/add-acc')
    await waitForLastFrameToInclude('→ /add-acc')
    stdin.write('\t')
    await waitForLastFrameToInclude('→ /add-access-token ')
    expect(lineWithMainPrompt(lastStrippedFrame())).not.toMatch(/<token>/)
  })

  test('Tab with no usage prefix match leaves draft unchanged', async () => {
    const { stdin, lastStrippedFrame, waitForLastFrameToInclude } =
      await renderMainInteractivePrompt()

    stdin.write('/zzz')
    await waitForLastFrameToInclude('→ /zzz')
    stdin.write('\t')
    await waitForLastFrameToInclude('→ /zzz')
    expect(lineWithMainPrompt(lastStrippedFrame()).trimEnd()).toContain(
      '→ /zzz'
    )
  })
})

describe('MainInteractivePrompt caret and slash arrows (phase 3)', () => {
  test('with list visible and caret at end, down arrow cycles completion highlight', async () => {
    const { stdin, waitForLastFrameRaw, waitForLastFrameToInclude } =
      await renderMainInteractivePrompt()

    stdin.write('/re')
    await waitForLastFrameToInclude('/remove-access-token')

    stdin.write('\x1b[B')
    await waitForLastFrameRaw((raw) =>
      rawLineIncludesBoldMarker(
        raw,
        'Revoke a stored access token on the server'
      )
    )
  })

  test('with list visible and caret at end, each down advances highlight to the next matching usage', async () => {
    const { stdin, waitForLastFrameRaw, waitForLastFrameToInclude } =
      await renderMainInteractivePrompt()

    stdin.write('/re')
    await waitForLastFrameToInclude('/remove-access-token')

    stdin.write('\x1b[B')
    await waitForLastFrameRaw((raw) =>
      rawLineIncludesBoldMarker(
        raw,
        'Revoke a stored access token on the server'
      )
    )

    stdin.write('\x1b[B')
    await waitForLastFrameRaw((raw) =>
      rawLineIncludesBoldMarker(
        raw,
        'Recall the next due note (just review when no quiz is pe'
      )
    )
  })

  test('with list visible and caret in the middle, first down moves to end then down cycles highlight', async () => {
    const { stdin, waitForLastFrameRaw, waitForLastFrameToInclude } =
      await renderMainInteractivePrompt()

    stdin.write('/re')
    await waitForLastFrameToInclude('/remove-access-token')

    stdin.write('\x1b[D')
    await waitForLastFrameToInclude('→ /re')

    stdin.write('\x1b[B')
    await waitForLastFrameRaw((raw) =>
      rawLineIncludesBoldMarker(
        raw,
        'Remove a stored access token from local config only'
      )
    )

    stdin.write('\x1b[B')
    await waitForLastFrameRaw((raw) =>
      rawLineIncludesBoldMarker(
        raw,
        'Revoke a stored access token on the server'
      )
    )
  })

  test('with list visible and caret in the middle, up arrow cycles highlight without moving caret home first', async () => {
    const { stdin, lastFrame, waitForLastFrameRaw, waitForLastFrameToInclude } =
      await renderMainInteractivePrompt()

    stdin.write('/re')
    await waitForLastFrameToInclude('/remove-access-token')

    stdin.write('\x1b[D')
    await waitForLastFrameToInclude('→ /re')

    stdin.write('\x1b[A')
    try {
      await waitForLastFrameRaw(
        (r) =>
          rawLineIncludesBoldMarker(
            r,
            'Show how many notes are due for recall today'
          ) && mainPromptDraftAfterArrow(stripAnsi(r)) === '/re'
      )
    } catch (err) {
      throw new Error(
        'With slash list open and caret between /r and e, the first Up should wrap the completion highlight to the last matching row (/recall-status for draft /re) and keep the caret after /r, not move the caret to the start of the line (history-style home). ' +
          (err instanceof Error ? err.message : String(err))
      )
    }

    const raw = lastFrame() ?? ''
    expect(
      rawLineIncludesBoldMarker(
        raw,
        'Show how many notes are due for recall today'
      ),
      'When the slash completion list is open, Up must cycle the highlighted command immediately (wrap from first row to last), not leave the default first-row highlight.'
    ).toBe(true)
    expect(
      mainPromptDraftAfterArrow(stripAnsi(raw)) === '/re',
      'When the slash completion list is open, Up must not act like history “home” (caret to column 0); the draft should stay /re with the caret before e.'
    ).toBe(true)
  })
})

describe('MainInteractivePrompt Enter picks completion (phase 4)', () => {
  test('Enter with list open replaces draft with highlighted usage plus space and does not commit', async () => {
    const onCommittedLine = vi.fn()
    const {
      stdin,
      waitForLastFrameRaw,
      waitForLastFrameToInclude,
      waitUntilLastFrame,
    } = await renderMainInteractivePrompt(onCommittedLine)
    stdin.write('/re')
    await waitForLastFrameToInclude('/remove-access-token')
    stdin.write('\x1b[B')
    await waitForLastFrameRaw((raw) =>
      rawLineIncludesBoldMarker(
        raw,
        'Revoke a stored access token on the server'
      )
    )
    stdin.write('\r')
    await waitUntilLastFrame((f) =>
      lineWithMainPrompt(f).includes('/remove-access-token-completely ')
    )
    expect(onCommittedLine).not.toHaveBeenCalled()
  })

  test('Enter with list open and default highlight picks first match usage plus space', async () => {
    const onCommittedLine = vi.fn()
    const { stdin, waitForLastFrameToInclude, waitUntilLastFrame } =
      await renderMainInteractivePrompt(onCommittedLine)
    stdin.write('/re')
    await waitForLastFrameToInclude('/remove-access-token')
    stdin.write('\r')
    await waitUntilLastFrame((f) =>
      lineWithMainPrompt(f).includes('/remove-access-token ')
    )
    expect(onCommittedLine).not.toHaveBeenCalled()
  })
})

describe('MainInteractivePrompt user input history (↑↓ recall)', () => {
  test('after Enter, up recalls committed line; down restores pre-walk draft', async () => {
    const onCommittedLine = vi.fn()
    const { stdin, waitUntilLastFrame } =
      await renderMainInteractivePrompt(onCommittedLine)

    stdin.write('alpha')
    await waitUntilLastFrame((f) => lineWithMainPrompt(f).includes('alpha'))
    stdin.write('\r')
    await waitUntilLastFrame((f) => !lineWithMainPrompt(f).includes('alpha'))
    expect(onCommittedLine).toHaveBeenCalledWith('alpha')

    stdin.write('\x1b[A')
    await waitUntilLastFrame((f) => lineWithMainPrompt(f).includes('alpha'))

    stdin.write('\x1b[B')
    await waitUntilLastFrame((f) => !lineWithMainPrompt(f).includes('alpha'))
  })

  test('with slash list dismissed, up at caret 0 recalls history instead of cycling list', async () => {
    const onCommittedLine = vi.fn()
    const { stdin, waitUntilLastFrame } =
      await renderMainInteractivePrompt(onCommittedLine)

    stdin.write('memo\r')
    await waitUntilLastFrame((f) => !lineWithMainPrompt(f).includes('memo'))
    expect(onCommittedLine).toHaveBeenCalledWith('memo')

    stdin.write('/he')
    await waitUntilLastFrame(
      (f) => f.includes('/help') && f.includes('List available commands')
    )
    stdin.write('\x1b')
    await waitUntilLastFrame(
      (f) => f.includes('/ commands') && mainPromptDraftAfterArrow(f) === '/he'
    )

    stdin.write('\x1b[D\x1b[D\x1b[D')
    await waitUntilLastFrame((f) => lineWithMainPrompt(f).includes('/he'))

    stdin.write('\x1b[A')
    await waitUntilLastFrame((f) => lineWithMainPrompt(f).includes('memo'))
  })

  test('after Esc hides slash list, up recalls history; down restores pre-walk /re draft', async () => {
    const { stdin, waitUntilLastFrame } = await renderMainInteractivePrompt()

    stdin.write('z\r')
    await waitUntilLastFrame((f) => !lineWithMainPrompt(f).includes('z'))

    stdin.write('/re')
    await waitUntilLastFrame((f) => f.includes('/remove-access-token'))

    stdin.write('\x1b')
    await waitUntilLastFrame(
      (f) => f.includes('/ commands') && mainPromptDraftAfterArrow(f) === '/re'
    )

    stdin.write('\x1b[D\x1b[D\x1b[D')
    await waitUntilLastFrame((f) => lineWithMainPrompt(f).includes('/re'))

    stdin.write('\x1b[A')
    await waitUntilLastFrame((f) => lineWithMainPrompt(f).includes('z'))

    stdin.write('\x1b[B')
    await waitUntilLastFrame(
      (f) =>
        lineWithMainPrompt(f).includes('/re') &&
        !lineWithMainPrompt(f).includes('z') &&
        f.includes('/remove-access-token')
    )
  })
})

describe('MainInteractivePrompt user input history persistence (phase 4)', () => {
  test('writes user-input-history.json after Enter commits (newest first)', async () => {
    const onCommittedLine = vi.fn()
    const { stdin, waitUntilLastFrame } =
      await renderMainInteractivePrompt(onCommittedLine)

    stdin.write('first\r')
    await waitUntilLastFrame((f) => !lineWithMainPrompt(f).includes('first'))

    stdin.write('second\r')
    await waitUntilLastFrame((f) => !lineWithMainPrompt(f).includes('second'))

    const p = path.join(promptConfigDir, USER_INPUT_HISTORY_FILENAME)
    expect(JSON.parse(fs.readFileSync(p, 'utf-8'))).toEqual(['second', 'first'])
  })

  test('fresh mount loads history from disk; up recalls stored line', async () => {
    fs.writeFileSync(
      path.join(promptConfigDir, USER_INPUT_HISTORY_FILENAME),
      `${JSON.stringify(['from-disk'])}\n`,
      'utf-8'
    )

    const { stdin, waitUntilLastFrame } = await renderMainInteractivePrompt()

    stdin.write('\x1b[A')
    await waitUntilLastFrame((f) => lineWithMainPrompt(f).includes('from-disk'))
  })

  test('DOUGHNUT_CLI_DISABLE_INPUT_HISTORY=1 skips writing history file', async () => {
    process.env.DOUGHNUT_CLI_DISABLE_INPUT_HISTORY = '1'
    try {
      const onCommittedLine = vi.fn()
      const { stdin, waitUntilLastFrame } =
        await renderMainInteractivePrompt(onCommittedLine)

      stdin.write('no-file\r')
      await waitUntilLastFrame(
        (f) => !lineWithMainPrompt(f).includes('no-file')
      )

      const p = path.join(promptConfigDir, USER_INPUT_HISTORY_FILENAME)
      expect(fs.existsSync(p)).toBe(false)
    } finally {
      delete process.env.DOUGHNUT_CLI_DISABLE_INPUT_HISTORY
    }
  })
})

describe('MainInteractivePrompt Esc dismiss (phase 5)', () => {
  test('Esc on bare / clears the draft', async () => {
    const {
      stdin,
      lastStrippedFrame,
      waitForLastFrameToInclude,
      waitUntilLastFrame,
    } = await renderMainInteractivePrompt()

    stdin.write('/')
    await waitForLastFrameToInclude('→ /')
    stdin.write('\x1b')
    await waitUntilLastFrame((f) => !lineWithMainPrompt(f).includes('/'))
    const promptLine = lineWithMainPrompt(lastStrippedFrame()).trimEnd()
    expect(promptLine).toContain('→')
    expect(promptLine).not.toContain('/')
  })

  test('Esc on /he with list hides list and keeps draft; typing restores list', async () => {
    const { stdin, waitUntilLastFrame } = await renderMainInteractivePrompt()

    stdin.write('/he')
    await waitUntilLastFrame(
      (f) => f.includes('/help') && f.includes('List available commands')
    )
    stdin.write('\x1b')
    await waitUntilLastFrame(
      (f) =>
        f.includes('/ commands') &&
        mainPromptDraftAfterArrow(f) === '/he' &&
        !f.includes('List available commands')
    )

    stdin.write('l')
    await waitUntilLastFrame(
      (f) => f.includes('/help') && f.includes('List available commands')
    )
  })
})
