import * as fs from 'node:fs'
import * as os from 'node:os'
import * as path from 'node:path'
import { render } from 'ink-testing-library'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import {
  GUIDANCE_LIST_ROW_BUDGET,
  GUIDANCE_MORE_BELOW_LABEL,
} from '../src/guidanceListWindow.js'
import { MainInteractivePrompt } from '../src/mainInteractivePrompt/index.js'
import { USER_INPUT_HISTORY_FILENAME } from '../src/mainInteractivePrompt/userInputHistoryFile.js'
import { stripAnsi, waitForFrames } from './inkTestHelpers.js'

let promptConfigDir: string
let prevDoughnutConfigDir: string | undefined

/** StripAnsi line that contains the main `→` prompt (inside the bordered box). */
function lineWithMainPrompt(plain: string): string {
  return plain.split('\n').find((l) => l.includes('→')) ?? ''
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

async function renderMainInteractivePrompt(
  onCommittedLine: (line: string) => void = () => undefined
) {
  const result = render(
    <MainInteractivePrompt onCommittedLine={onCommittedLine} />
  )
  await waitForFrames(
    () => stripAnsi(result.lastFrame() ?? ''),
    (f) => f.includes('→') && f.includes('/ commands')
  )
  const probe = '@'
  result.stdin.write(probe)
  await waitForFrames(
    () => stripAnsi(result.lastFrame() ?? ''),
    (f) => lineWithMainPrompt(f).includes(probe)
  )
  result.stdin.write('\x7f')
  await waitForFrames(
    () => stripAnsi(result.lastFrame() ?? ''),
    (f) => !lineWithMainPrompt(f).includes(probe)
  )
  return result
}

describe('MainInteractivePrompt slash guidance (phase 1)', () => {
  test('default shows static / commands hint', async () => {
    const { lastFrame } = await renderMainInteractivePrompt()
    const plain = stripAnsi(lastFrame() ?? '')
    expect(plain).toContain('/ commands')
    expect(plain).toContain('`exit` to quit.')
  })

  test('partial / prefix shows matching commands with first row inverse-highlighted', async () => {
    const { stdin, lastFrame } = await renderMainInteractivePrompt()

    stdin.write('/')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => f.includes('→ /')
    )
    stdin.write('he')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => f.includes('/help') && f.includes('List available commands')
    )

    const raw = lastFrame() ?? ''
    expect(raw).toContain('\x1b[7m')
    const helpLine = stripAnsi(raw)
      .split('\n')
      .find((l) => l.includes('/help') && l.includes('List available'))
    expect(helpLine, 'expected a visible /help completion row').toBeTruthy()

    const beforeHelp = raw.slice(0, raw.indexOf('/help'))
    const openInverseBeforeHelp = beforeHelp.lastIndexOf('\x1b[7m')
    const closeAfterOpen = beforeHelp.indexOf('\x1b[27m', openInverseBeforeHelp)
    expect(
      openInverseBeforeHelp,
      'inverse SGR should start before /help on the first (highlighted) row'
    ).toBeGreaterThan(-1)
    expect(
      closeAfterOpen === -1 || closeAfterOpen > openInverseBeforeHelp,
      'highlight should extend through /help row start'
    ).toBe(true)
  })

  test('bare slash shows a fixed-height list with more-below when commands overflow budget', async () => {
    const { stdin, lastFrame } = await renderMainInteractivePrompt()

    stdin.write('/')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) =>
        lineWithMainPrompt(f).includes('→ /') &&
        f.includes(GUIDANCE_MORE_BELOW_LABEL)
    )

    const frame = stripAnsi(lastFrame() ?? '')
    const listRows = frame
      .split('\n')
      .filter((l) => l.includes(GUIDANCE_MORE_BELOW_LABEL) || l.includes('  /'))
    expect(listRows.length).toBe(GUIDANCE_LIST_ROW_BUDGET)
  })

  test('trailing space after slash command shows hint only, not the completion list', async () => {
    const { stdin, lastFrame } = await renderMainInteractivePrompt()

    stdin.write('/help ')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => f.includes('→ /help ')
    )
    const frame = stripAnsi(lastFrame() ?? '')
    expect(frame).toContain('/ commands')
    expect(frame).not.toMatch(/\/help\s+List available commands/)
  })
})

describe('MainInteractivePrompt Tab completion (phase 2)', () => {
  test('Tab with several usages sharing a prefix extends draft to longest common prefix', async () => {
    const { stdin, lastFrame } = await renderMainInteractivePrompt()

    stdin.write('/remove')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => f.includes('→ /remove')
    )
    stdin.write('\t')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => lineWithMainPrompt(f).trimEnd().includes('→ /remove-access-token')
    )
    const promptLine = lineWithMainPrompt(
      stripAnsi(lastFrame() ?? '')
    ).trimEnd()
    expect(promptLine.includes('→ /remove-access-token')).toBe(true)
  })

  test('Tab with a unique matching usage completes to usage plus trailing space', async () => {
    const { stdin, lastFrame } = await renderMainInteractivePrompt()

    stdin.write('/hel')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => f.includes('→ /hel')
    )
    stdin.write('\t')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => f.includes('→ /help ')
    )
    expect(
      lineWithMainPrompt(stripAnsi(lastFrame() ?? '')).includes('/help ')
    ).toBe(true)
  })

  test('Tab with a unique match does not append <argument> placeholder to the draft', async () => {
    const { stdin, lastFrame } = await renderMainInteractivePrompt()

    stdin.write('/add-acc')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => f.includes('→ /add-acc')
    )
    stdin.write('\t')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => f.includes('→ /add-access-token ')
    )
    expect(lineWithMainPrompt(stripAnsi(lastFrame() ?? ''))).not.toMatch(
      /<token>/
    )
  })

  test('Tab with no usage prefix match leaves draft unchanged', async () => {
    const { stdin, lastFrame } = await renderMainInteractivePrompt()

    stdin.write('/zzz')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => f.includes('→ /zzz')
    )
    stdin.write('\t')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => f.includes('→ /zzz')
    )
    expect(
      lineWithMainPrompt(stripAnsi(lastFrame() ?? '')).trimEnd()
    ).toContain('→ /zzz')
  })
})

describe('MainInteractivePrompt caret and slash arrows (phase 3)', () => {
  test('with list visible and caret at end, down arrow cycles completion highlight', async () => {
    const { stdin, lastFrame } = await renderMainInteractivePrompt()

    stdin.write('/re')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => f.includes('/remove-access-token')
    )

    stdin.write('\x1b[B')
    await waitForFrames(
      () => lastFrame() ?? '',
      (raw) =>
        raw.includes('\x1b[7m') &&
        raw.includes('Revoke a stored access token on the server')
    )
  })

  test('with list visible and caret at end, second down wraps highlight to first row', async () => {
    const { stdin, lastFrame } = await renderMainInteractivePrompt()

    stdin.write('/re')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => f.includes('/remove-access-token')
    )

    stdin.write('\x1b[B')
    await waitForFrames(
      () => lastFrame() ?? '',
      (raw) =>
        raw.includes('\x1b[7m') &&
        raw.includes('Revoke a stored access token on the server')
    )

    stdin.write('\x1b[B')
    await waitForFrames(
      () => lastFrame() ?? '',
      (raw) =>
        raw.includes('\x1b[7m') &&
        raw.includes('Remove a stored access token from local config only')
    )
  })

  test('with list visible and caret in the middle, first down moves to end then down cycles highlight', async () => {
    const { stdin, lastFrame } = await renderMainInteractivePrompt()

    stdin.write('/re')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => f.includes('/remove-access-token')
    )

    stdin.write('\x1b[D')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => f.includes('→ /re')
    )

    stdin.write('\x1b[B')
    await waitForFrames(
      () => lastFrame() ?? '',
      (raw) =>
        raw.includes('\x1b[7m') &&
        raw.includes('Remove a stored access token from local config only')
    )

    stdin.write('\x1b[B')
    await waitForFrames(
      () => lastFrame() ?? '',
      (raw) =>
        raw.includes('\x1b[7m') &&
        raw.includes('Revoke a stored access token on the server')
    )
  })
})

describe('MainInteractivePrompt Enter picks completion (phase 4)', () => {
  test('Enter with list open replaces draft with highlighted usage plus space and does not commit', async () => {
    const onCommittedLine = vi.fn()
    const { stdin, lastFrame } =
      await renderMainInteractivePrompt(onCommittedLine)
    stdin.write('/re')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => f.includes('/remove-access-token')
    )
    stdin.write('\x1b[B')
    await waitForFrames(
      () => lastFrame() ?? '',
      (raw) =>
        raw.includes('\x1b[7m') &&
        raw.includes('Revoke a stored access token on the server')
    )
    stdin.write('\r')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => lineWithMainPrompt(f).includes('/remove-access-token-completely ')
    )
    expect(onCommittedLine).not.toHaveBeenCalled()
  })

  test('Enter with list open and default highlight picks first match usage plus space', async () => {
    const onCommittedLine = vi.fn()
    const { stdin, lastFrame } =
      await renderMainInteractivePrompt(onCommittedLine)
    stdin.write('/re')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => f.includes('/remove-access-token')
    )
    stdin.write('\r')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => lineWithMainPrompt(f).includes('/remove-access-token ')
    )
    expect(onCommittedLine).not.toHaveBeenCalled()
  })
})

describe('MainInteractivePrompt user input history (↑↓ recall)', () => {
  test('after Enter, up recalls committed line; down restores pre-walk draft', async () => {
    const onCommittedLine = vi.fn()
    const { stdin, lastFrame } =
      await renderMainInteractivePrompt(onCommittedLine)

    stdin.write('alpha')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => lineWithMainPrompt(f).includes('alpha')
    )
    stdin.write('\r')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => !lineWithMainPrompt(f).includes('alpha')
    )
    expect(onCommittedLine).toHaveBeenCalledWith('alpha')

    stdin.write('\x1b[A')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => lineWithMainPrompt(f).includes('alpha')
    )

    stdin.write('\x1b[B')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => !lineWithMainPrompt(f).includes('alpha')
    )
  })

  test('with slash list dismissed, up at caret 0 recalls history instead of cycling list', async () => {
    const onCommittedLine = vi.fn()
    const { stdin, lastFrame } =
      await renderMainInteractivePrompt(onCommittedLine)

    stdin.write('memo\r')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => !lineWithMainPrompt(f).includes('memo')
    )
    expect(onCommittedLine).toHaveBeenCalledWith('memo')

    stdin.write('/he')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => f.includes('/help') && f.includes('List available commands')
    )
    stdin.write('\x1b')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => f.includes('/ commands') && mainPromptDraftAfterArrow(f) === '/he'
    )

    stdin.write('\x1b[D\x1b[D\x1b[D')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => lineWithMainPrompt(f).includes('/he')
    )

    stdin.write('\x1b[A')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => lineWithMainPrompt(f).includes('memo')
    )
  })

  test('after Esc hides slash list, up recalls history; down restores pre-walk /re draft', async () => {
    const { stdin, lastFrame } = await renderMainInteractivePrompt()

    stdin.write('z\r')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => !lineWithMainPrompt(f).includes('z')
    )

    stdin.write('/re')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => f.includes('/remove-access-token')
    )

    stdin.write('\x1b')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => f.includes('/ commands') && mainPromptDraftAfterArrow(f) === '/re'
    )

    stdin.write('\x1b[D\x1b[D\x1b[D')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => lineWithMainPrompt(f).includes('/re')
    )

    stdin.write('\x1b[A')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => lineWithMainPrompt(f).includes('z')
    )

    stdin.write('\x1b[B')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
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
    const { stdin, lastFrame } =
      await renderMainInteractivePrompt(onCommittedLine)

    stdin.write('first\r')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => !lineWithMainPrompt(f).includes('first')
    )

    stdin.write('second\r')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => !lineWithMainPrompt(f).includes('second')
    )

    const p = path.join(promptConfigDir, USER_INPUT_HISTORY_FILENAME)
    expect(JSON.parse(fs.readFileSync(p, 'utf-8'))).toEqual(['second', 'first'])
  })

  test('fresh mount loads history from disk; up recalls stored line', async () => {
    fs.writeFileSync(
      path.join(promptConfigDir, USER_INPUT_HISTORY_FILENAME),
      `${JSON.stringify(['from-disk'])}\n`,
      'utf-8'
    )

    const { stdin, lastFrame } = await renderMainInteractivePrompt()

    stdin.write('\x1b[A')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => lineWithMainPrompt(f).includes('from-disk')
    )
  })

  test('DOUGHNUT_CLI_DISABLE_INPUT_HISTORY=1 skips writing history file', async () => {
    process.env.DOUGHNUT_CLI_DISABLE_INPUT_HISTORY = '1'
    try {
      const onCommittedLine = vi.fn()
      const { stdin, lastFrame } =
        await renderMainInteractivePrompt(onCommittedLine)

      stdin.write('no-file\r')
      await waitForFrames(
        () => stripAnsi(lastFrame() ?? ''),
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
    const { stdin, lastFrame } = await renderMainInteractivePrompt()

    stdin.write('/')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => f.includes('→ /')
    )
    stdin.write('\x1b')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => !lineWithMainPrompt(f).includes('/')
    )
    const promptLine = lineWithMainPrompt(
      stripAnsi(lastFrame() ?? '')
    ).trimEnd()
    expect(promptLine).toContain('→')
    expect(promptLine).not.toContain('/')
  })

  test('Esc on /he with list hides list and keeps draft; typing restores list', async () => {
    const { stdin, lastFrame } = await renderMainInteractivePrompt()

    stdin.write('/he')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => f.includes('/help') && f.includes('List available commands')
    )
    stdin.write('\x1b')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) =>
        f.includes('/ commands') &&
        mainPromptDraftAfterArrow(f) === '/he' &&
        !f.includes('List available commands')
    )

    stdin.write('l')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => f.includes('/help') && f.includes('List available commands')
    )
  })
})
