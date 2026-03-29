import { render } from 'ink-testing-library'
import { describe, expect, test } from 'vitest'
import { InteractiveCliApp } from '../src/InteractiveCliApp.js'
import { formatVersionOutput } from '../src/commands/version.js'

function stripAnsi(s: string): string {
  const esc = String.fromCharCode(0x1b)
  return s.replace(new RegExp(`${esc}\\[[0-9;?]*[a-zA-Z]`, 'g'), '')
}

/** True when a frame shows the /exit farewell and then paints another empty REPL line (`> `). */
function farewellFollowedByCommandPrompt(ansiStrippedFrame: string): boolean {
  const farewell = 'Bye.'
  if (!ansiStrippedFrame.includes(farewell)) return false
  const after = ansiStrippedFrame.slice(
    ansiStrippedFrame.lastIndexOf(farewell) + farewell.length
  )
  return /\n\s*>\s/.test(after)
}

/** Advance the event loop until `predicate` holds or `maxTicks` is exhausted (no fixed wall-clock sleep). */
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
    `Output condition not met within ${maxTicks} event-loop turns. Last frames:\n${combined}`
  )
}

/** Render the app and confirm end-to-end input handling is active before returning. */
async function renderApp() {
  const result = render(<InteractiveCliApp />)
  // React 19 registers useInput's listeners via useEffect in deferred phases.
  // Probe: write a character and wait for it to appear (confirms both the raw-mode
  // 'readable' listener and the 'input' event-emitter listener are fully active),
  // then delete it to restore the initial empty-buffer state.
  result.stdin.write('|')
  await waitForFrames(
    () => stripAnsi(result.lastFrame() ?? ''),
    (f) => f.includes('> |')
  )
  result.stdin.write('\x7f') // DEL — removes the probe character
  await waitForFrames(
    () => stripAnsi(result.lastFrame() ?? ''),
    (f) => f.includes('> ') && !f.includes('> |')
  )
  return result
}

describe('InteractiveCliApp (ink-testing-library)', () => {
  test('shows version in the first frame', () => {
    const { lastFrame } = render(<InteractiveCliApp />)
    expect(lastFrame()).toContain(formatVersionOutput())
  })

  test('empty committed line leaves transcript unchanged; later line still commits', async () => {
    const { stdin, frames } = await renderApp()
    const before = frames.join('\n')
    expect(before).toContain(formatVersionOutput())
    expect(before).not.toContain('Not supported')

    stdin.write('\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) => c === before && !c.includes('Not supported')
    )

    stdin.write('x\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) => c.includes('Not supported') && c.includes('x')
    )
  })

  test('/help records user line and assistant help listing', async () => {
    const { stdin, frames } = await renderApp()

    stdin.write('/help\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) =>
        c.includes('/help') &&
        c.includes('/add-access-token') &&
        c.includes('/list-access-token') &&
        c.includes('/add gmail') &&
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
    const { stdin, frames } = await renderApp()

    stdin.write('hello\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) =>
        c.includes('hello') &&
        c.includes('Not supported') &&
        c.includes('\x1b[100m')
    )

    const combined = frames.join('\n')
    expect(combined).toContain('hello')
    expect(combined).toContain('Not supported')
    expect(combined).toContain('\x1b[100m')
  })

  test('submitting /exit as one chunk line+CR records it in output', async () => {
    const { lastFrame, stdin, frames } = await renderApp()
    expect(lastFrame()).toContain(formatVersionOutput())

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
      lines.slice(userIdx + 2).some((l) => l.includes('>')),
      'after Bye., the REPL must not paint another "> " line (see dedicated /exit TTY test)'
    ).toBe(false)
  })

  test('after /exit, TTY must not repaint the empty command line below Bye.', async () => {
    const { stdin, frames } = await renderApp()

    stdin.write('/exit\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) => c.includes('Bye.') && c.includes('/exit')
    )

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
        '(the "> " line with the block cursor). That means the REPL drew one more input line',
        'before shutdown — the farewell should be the last interactive chrome.',
        'ANSI stripped offending frame(s):',
        ...offenders.map((f) => `---\n${stripAnsi(f)}\n---`),
      ].join('\n')
    ).toEqual([])
  })

  test('submitting /exit character by character records it in output', async () => {
    const { lastFrame, stdin, frames } = await renderApp()
    expect(lastFrame()).toContain(formatVersionOutput())

    let expectedBuffer = ''
    for (const ch of '/exit') {
      expectedBuffer += ch
      stdin.write(ch)
      const expected = expectedBuffer
      await waitForFrames(
        () => stripAnsi(lastFrame() ?? ''),
        (f) => f.includes(`> ${expected}`)
      )
    }
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
