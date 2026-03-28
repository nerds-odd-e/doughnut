import { render } from 'ink-testing-library'
import { describe, expect, test } from 'vitest'
import { PastUserMessageBlock } from '../src/pastUserMessageBlock.js'

function stripAnsi(s: string): string {
  const esc = String.fromCharCode(0x1b)
  return s.replace(new RegExp(`${esc}\\[[0-9;?]*[a-zA-Z]`, 'g'), '')
}

describe('PastUserMessageBlock', () => {
  test('gray background (100m), blank padded row above and below the text', () => {
    const { lastFrame } = render(<PastUserMessageBlock text="/exit" />)
    const raw = lastFrame() ?? ''
    expect(raw).toContain('\x1b[100m')
    expect(raw).toContain('/exit')
    expect(raw).not.toContain(`${String.fromCharCode(0x1b)}[90m/exit`)
    const lines = stripAnsi(raw).split('\n')
    const i = lines.findIndex((l) => l.includes('/exit'))
    expect(i).toBeGreaterThanOrEqual(0)
    expect(lines[i - 1]?.trim()).toBe('')
    expect(lines[i + 1]?.trim()).toBe('')
  })
})
