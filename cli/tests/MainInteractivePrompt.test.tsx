import { render } from 'ink-testing-library'
import { describe, expect, test } from 'vitest'
import { MainInteractivePrompt } from '../src/MainInteractivePrompt.js'
import { stripAnsi, waitForFrames } from './inkTestHelpers.js'

async function renderMainInteractivePrompt() {
  const result = render(
    <MainInteractivePrompt onCommittedLine={() => undefined} />
  )
  await waitForFrames(
    () => stripAnsi(result.lastFrame() ?? ''),
    (f) => f.includes('>') && f.includes('/ commands')
  )
  const firstLine = (f: string) => (f.split('\n')[0] ?? '').trimEnd()
  const probe = '@'
  result.stdin.write(probe)
  await waitForFrames(
    () => stripAnsi(result.lastFrame() ?? ''),
    (f) => firstLine(f).includes(probe)
  )
  result.stdin.write('\x7f')
  await waitForFrames(
    () => stripAnsi(result.lastFrame() ?? ''),
    (f) => !firstLine(f).includes(probe)
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
    expect(
      (stripAnsi(lastFrame() ?? '').split('\n')[0] ?? '').includes('/help ')
    ).toBe(true)
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
      (f) => f.includes('> /re')
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
