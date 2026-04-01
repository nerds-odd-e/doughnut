import { render } from 'ink-testing-library'
import { describe, expect, test } from 'vitest'
import { PastUserMessageBlock } from '../src/commonUIComponents/pastUserMessageBlock.js'
import { stripAnsi } from './inkTestHelpers.js'

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
    expect(lines[i]?.startsWith(' ')).toBe(true)
    expect(lines[i]?.trim()).toBe('/exit')
    expect(lines[i - 1]?.trim()).toBe('')
    expect(lines[i + 1]?.trim()).toBe('')
    expect(lines[i - 1]?.length).toBe(lines[i]?.length)
    expect(lines[i + 1]?.length).toBe(lines[i]?.length)
  })
})
