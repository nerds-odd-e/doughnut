import { describe, expect, test } from 'vitest'
import { InteractiveCliApp } from '../src/InteractiveCliApp.js'
import {
  renderInkWhenCommandLineReady,
  stripAnsi,
  waitForFrames,
  waitTurnsWithoutRepaint,
} from './inkTestHelpers.js'

/** True when a frame shows the /exit farewell and then paints another main REPL prompt (`→`). */
function farewellFollowedByCommandPrompt(ansiStrippedFrame: string): boolean {
  const farewell = 'Bye.'
  if (!ansiStrippedFrame.includes(farewell)) return false
  const after = ansiStrippedFrame.slice(
    ansiStrippedFrame.lastIndexOf(farewell) + farewell.length
  )
  return /\n[^\n]*→/.test(after)
}

function lastFrameWithFarewell(frames: string[]): string {
  for (let i = frames.length - 1; i >= 0; i--) {
    if (stripAnsi(frames[i] ?? '').includes('Bye.')) {
      return frames[i] ?? ''
    }
  }
  return frames.at(-1) ?? ''
}

describe('InteractiveCliApp (ink-testing-library)', () => {
  test('empty committed line leaves transcript unchanged; later line still commits', async () => {
    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )
    const before = frames.join('\n')
    expect(before).not.toContain('Not supported')

    stdin.write('\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) => c === before && !c.includes('Not supported')
    )

    stdin.write('x\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) =>
        c.includes('Not supported') && c.includes('x') && c.includes('\x1b[41m')
    )
  })

  test('/help records user line and assistant help listing', async () => {
    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('/help\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) =>
        c.includes('/help') &&
        c.includes('/set-access-token') &&
        c.includes('/add gmail') &&
        c.includes('/recall-status') &&
        c.includes('/exit') &&
        c.includes('update') &&
        c.includes('version') &&
        c.includes('\x1b[100m')
    )

    const combined = frames.join('\n')
    expect(combined).toMatch(/Subcommands:/)
    expect(combined).toMatch(/Interactive commands/)
  })

  test('plain committed line records user message and Not supported', async () => {
    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('hello\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) =>
        c.includes('hello') &&
        c.includes('Not supported') &&
        c.includes('\x1b[100m') &&
        c.includes('\x1b[41m')
    )
  })

  test('bare recall without slash is Not supported, not a command', async () => {
    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('recall\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) =>
        c.includes('recall') &&
        c.includes('Not supported') &&
        c.includes('\x1b[100m') &&
        c.includes('\x1b[41m')
    )

    expect(frames.join('\n')).not.toContain('Recalling')
  })

  test('unknown slash command records user line and unsupported command', async () => {
    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('/no-such-command\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) =>
        c.includes('/no-such-command') &&
        c.includes('unsupported command') &&
        c.includes('\x1b[100m') &&
        c.includes('\x1b[41m')
    )

    expect(frames.join('\n')).not.toContain('Not supported')
  })

  test('submitting /exit as one chunk line+CR records it in output', async () => {
    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('/exit\r')
    await waitForFrames(
      () => stripAnsi(lastFrameWithFarewell(frames)),
      (f) => f.includes('Bye.')
    )

    const farewellFrame = lastFrameWithFarewell(frames)
    const plain = stripAnsi(farewellFrame)
    expect(plain).toContain('/exit')
    expect(
      frames.some(
        (f) => stripAnsi(f).includes('/exit') && f.includes('\x1b[100m')
      )
    ).toBe(true)

    const snapshot =
      [...frames].reverse().find((f) => {
        const lines = stripAnsi(f).split('\n')
        return lines.some((l) => l.trim() === '/exit')
      }) ?? ''
    expect(
      snapshot,
      'expected a frame with /exit committed to the transcript (own line)'
    ).toMatch(/\S/)
    const lines = stripAnsi(snapshot).split('\n')
    const userIdx = lines.findIndex((l) => l.trim() === '/exit')
    expect(lines[userIdx - 1]?.trim()).toBe('')
    expect(lines[userIdx + 1]?.trim()).toBe('')
    expect(
      lines.slice(userIdx + 2).some((l) => l.includes('→')),
      'after Bye., the REPL must not paint another main prompt line'
    ).toBe(false)

    await waitTurnsWithoutRepaint(
      () => stripAnsi(lastFrameWithFarewell(frames)),
      farewellFollowedByCommandPrompt
    )
  })

  test('submitting exit without slash quits like /exit', async () => {
    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('exit\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) => c.includes('exit') && c.includes('Bye.') && c.includes('\x1b[100m')
    )

    expect(
      farewellFollowedByCommandPrompt(stripAnsi(frames.join('\n'))),
      'bare exit must not leave a live command prompt after Bye.'
    ).toBe(false)
  })

  test('submitting /exit character by character records it in output', async () => {
    const { lastStrippedFrame, stdin, frames, waitForLastFrameToInclude } =
      await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    let expectedBuffer = ''
    for (const ch of '/exit') {
      expectedBuffer += ch
      stdin.write(ch)
      const expected = expectedBuffer
      await waitForFrames(
        () => lastStrippedFrame(),
        (f) => f.includes(`→ ${expected}`)
      )
    }
    stdin.write('\r')
    await waitForLastFrameToInclude('→ /exit ')
    stdin.write('\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) =>
        c.includes('/exit') && c.includes('Bye.') && c.includes('\x1b[100m')
    )
  })
})
