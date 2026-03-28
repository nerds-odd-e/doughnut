import { render } from 'ink-testing-library'
import { describe, expect, test } from 'vitest'
import { InteractiveCliApp } from '../src/InteractiveCliApp.js'
import { formatVersionOutput } from '../src/commands/version.js'

function stripAnsi(s: string): string {
  const esc = String.fromCharCode(0x1b)
  return s.replace(new RegExp(`${esc}\\[[0-9;?]*[a-zA-Z]`, 'g'), '')
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

describe('InteractiveCliApp (ink-testing-library)', () => {
  test('shows version in the first frame', () => {
    const { lastFrame } = render(<InteractiveCliApp />)
    expect(lastFrame()).toContain(formatVersionOutput())
  })

  test('empty committed line leaves transcript unchanged; later line still commits', async () => {
    const { stdin, frames } = render(<InteractiveCliApp />)
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

  test('plain committed line records user message and Not supported', async () => {
    const { stdin, frames } = render(<InteractiveCliApp />)

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
    const { lastFrame, stdin, frames } = render(<InteractiveCliApp />)
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
  })

  test('submitting /exit records it in output', async () => {
    const { lastFrame, stdin, frames } = render(<InteractiveCliApp />)
    expect(lastFrame()).toContain(formatVersionOutput())

    for (const ch of '/exit') {
      stdin.write(ch)
      await new Promise<void>((r) => {
        setImmediate(r)
      })
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
      'command prompt should appear after the bottom padding row'
    ).toBe(true)
  })
})
