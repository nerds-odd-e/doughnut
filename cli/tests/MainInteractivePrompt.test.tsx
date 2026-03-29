import { render } from 'ink-testing-library'
import { describe, expect, test } from 'vitest'
import { MainInteractivePrompt } from '../src/MainInteractivePrompt.js'

function stripAnsi(s: string): string {
  const esc = String.fromCharCode(0x1b)
  return s.replace(new RegExp(`${esc}\\[[0-9;?]*[a-zA-Z]`, 'g'), '')
}

async function waitForFrames(
  getCombined: () => string,
  predicate: (combined: string) => boolean,
  maxTicks = 5000
): Promise<void> {
  for (let i = 0; i < maxTicks; i++) {
    if (predicate(getCombined())) {
      return
    }
    await new Promise<void>((resolve) => {
      setImmediate(resolve)
    })
  }
  const combined = getCombined()
  throw new Error(
    `Output condition not met within ${maxTicks} event-loop turns. Last:\n${combined}`
  )
}

async function renderMainInteractivePrompt() {
  const result = render(
    <MainInteractivePrompt onCommittedLine={() => undefined} />
  )
  result.stdin.write('|')
  await waitForFrames(
    () => stripAnsi(result.lastFrame() ?? ''),
    (f) => f.includes('> |')
  )
  result.stdin.write('\x7f')
  await waitForFrames(
    () => stripAnsi(result.lastFrame() ?? ''),
    (f) => f.includes('> ') && !f.includes('> |')
  )
  return result
}

describe('MainInteractivePrompt slash guidance (phase 1)', () => {
  test('default shows static / commands hint', async () => {
    const { lastFrame } = await renderMainInteractivePrompt()
    expect(stripAnsi(lastFrame() ?? '')).toContain('/ commands')
  })

  test('partial / prefix shows matching commands with first row inverse-highlighted', async () => {
    const { stdin, lastFrame } = await renderMainInteractivePrompt()

    stdin.write('/')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => f.includes('> /')
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

  test('trailing space after slash command shows hint only, not the completion list', async () => {
    const { stdin, lastFrame } = await renderMainInteractivePrompt()

    stdin.write('/help ')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => f.includes('> /help ')
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
      (f) => f.includes('> /remove')
    )
    stdin.write('\t')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) =>
        (f.split('\n')[0] ?? '').trimEnd().startsWith('> /remove-access-token')
    )
    const firstLine = (
      stripAnsi(lastFrame() ?? '').split('\n')[0] ?? ''
    ).trimEnd()
    expect(firstLine.startsWith('> /remove-access-token')).toBe(true)
  })

  test('Tab with a unique matching usage completes to usage plus trailing space', async () => {
    const { stdin, lastFrame } = await renderMainInteractivePrompt()

    stdin.write('/hel')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => f.includes('> /hel')
    )
    stdin.write('\t')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => f.includes('> /help ')
    )
    expect((stripAnsi(lastFrame() ?? '').split('\n')[0] ?? '').trimEnd()).toBe(
      '> /help '
    )
  })

  test('Tab with no usage prefix match leaves draft unchanged', async () => {
    const { stdin, lastFrame } = await renderMainInteractivePrompt()

    stdin.write('/zzz')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => f.includes('> /zzz')
    )
    stdin.write('\t')
    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (f) => f.includes('> /zzz')
    )
    expect((stripAnsi(lastFrame() ?? '').split('\n')[0] ?? '').trimEnd()).toBe(
      '> /zzz'
    )
  })
})
