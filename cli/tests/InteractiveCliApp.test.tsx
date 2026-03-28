import { render } from 'ink-testing-library'
import { describe, expect, test } from 'vitest'
import { InteractiveCliApp } from '../src/InteractiveCliApp.js'
import { formatVersionOutput } from '../src/commands/version.js'

function stripAnsi(s: string): string {
  const esc = String.fromCharCode(0x1b)
  return s.replace(new RegExp(`${esc}\\[[0-9;?]*[a-zA-Z]`, 'g'), '')
}

describe('InteractiveCliApp (ink-testing-library)', () => {
  test('shows version in the first frame', () => {
    const { lastFrame } = render(<InteractiveCliApp />)
    expect(lastFrame()).toContain(formatVersionOutput())
  })

  test('plain committed line records user message and Not supported', async () => {
    const { stdin, frames } = render(<InteractiveCliApp />)

    stdin.write('hello\r')
    await new Promise<void>((resolve) => {
      setTimeout(resolve, 50)
    })

    const combined = frames.join('\n')
    expect(combined).toContain('hello')
    expect(combined).toContain('Not supported')
    expect(combined).toContain('\x1b[100m')
  })

  test('submitting /exit as one chunk line+CR records it in output', async () => {
    const { lastFrame, stdin, frames } = render(<InteractiveCliApp />)
    expect(lastFrame()).toContain(formatVersionOutput())

    stdin.write('/exit\r')
    await new Promise<void>((resolve) => {
      setTimeout(resolve, 50)
    })

    const combined = frames.join('\n')
    expect(combined).toContain('/exit')
    expect(combined).toContain('Bye.')
    expect(combined).toContain('\x1b[100m')
  })

  test('submitting /exit records it in output', async () => {
    const { lastFrame, stdin, frames } = render(<InteractiveCliApp />)
    expect(lastFrame()).toContain(formatVersionOutput())

    await new Promise<void>((resolve) => {
      setTimeout(resolve, 50)
    })

    for (const ch of '/exit') {
      stdin.write(ch)
      await new Promise<void>((r) => {
        setImmediate(r)
      })
    }
    stdin.write('\r')
    await new Promise<void>((resolve) => {
      setTimeout(resolve, 50)
    })

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
