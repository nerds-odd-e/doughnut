import { describe, expect, test } from 'vitest'
import { InteractiveCliApp } from '../src/InteractiveCliApp.js'
import {
  renderInkWhenCommandLineReady,
  stripAnsi,
  waitForFrames,
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

    const combined = frames.join('\n')
    expect(combined).toContain('recall')
    expect(combined).toContain('Not supported')
    expect(combined).not.toContain('Recalling')
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

    const combined = frames.join('\n')
    expect(combined).toContain('hello')
    expect(combined).toContain('Not supported')
    expect(combined).toContain('\x1b[100m')
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

    const combined = frames.join('\n')
    expect(combined).toContain('/no-such-command')
    expect(combined).toContain('unsupported command')
    expect(combined).not.toContain('Not supported')
  })

  test('submitting /exit as one chunk line+CR records it in output', async () => {
    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )
    stdin.write('/exit\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) =>
        c.includes('/exit') && c.includes('Bye.') && c.includes('\x1b[100m')
    )

    const combined = frames.join('\n')
    expect(combined).toContain('/exit')
    expect(combined).toContain('Bye.')
    expect(combined).toContain('\x1b[100m')

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
      'after Bye., the REPL must not paint another main prompt line (see dedicated /exit TTY test)'
    ).toBe(false)
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

    const combined = frames.join('\n')
    expect(combined).toContain('Bye.')
    expect(
      farewellFollowedByCommandPrompt(stripAnsi(combined)),
      'bare exit must not leave a live command prompt after Bye.'
    ).toBe(false)

    const snapshot =
      [...frames].reverse().find((f) => {
        const lines = stripAnsi(f).split('\n')
        return lines.some((l) => l.trim() === 'exit')
      }) ?? ''
    expect(snapshot).toMatch(/\S/)
    const lines = stripAnsi(snapshot).split('\n')
    const userIdx = lines.findIndex((l) => l.trim() === 'exit')
    expect(userIdx).toBeGreaterThanOrEqual(0)
  })

  test('after /exit, TTY must not repaint the empty command line below Bye.', async () => {
    const { stdin, frames, waitForFramesToInclude } =
      await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    stdin.write('/exit\r')
    await waitForFramesToInclude('Bye.')
    await waitForFramesToInclude('/exit')

    for (let t = 0; t < 300; t++) {
      await new Promise<void>((resolve) => {
        setImmediate(resolve)
      })
    }

    const offenders = frames.filter((f) =>
      farewellFollowedByCommandPrompt(stripAnsi(f))
    )
    expect(
      offenders,
      [
        'After /exit prints "Bye.", the terminal must not show another interactive read prompt',
        '(the boxed main prompt with →). That means the REPL drew one more input line',
        'before shutdown — the farewell should be the last interactive chrome.',
        'ANSI stripped offending frame(s):',
        ...offenders.map((f) => `---\n${stripAnsi(f)}\n---`),
      ].join('\n')
    ).toEqual([])
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

    const combined = frames.join('\n')
    expect(combined).toContain('/exit')
    expect(combined).toContain('Bye.')
    expect(combined).toContain('\x1b[100m')
  })
})
