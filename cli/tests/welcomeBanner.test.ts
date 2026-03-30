import { describe, expect, test } from 'vitest'
import { formatInteractiveWelcomeBanner } from '../src/welcomeBanner.js'
import { stripAnsi } from './inkTestHelpers.js'

describe('formatInteractiveWelcomeBanner', () => {
  test('two block-art rows DONUT then PKM, orange SGR, no blocking prompt', () => {
    const raw = formatInteractiveWelcomeBanner()
    const esc = `${String.fromCharCode(0x1b)}`
    expect(raw.includes(`${esc}[38;5;208m`)).toBe(true)
    expect(raw.includes(`${esc}[0m`)).toBe(true)
    const visible = stripAnsi(raw).trimEnd()
    const lines = visible.split('\n').filter((l) => l.length > 0)
    expect(lines.length).toBe(12)
    expect(visible).toContain('██')
    expect(visible).toContain('║')
    expect(visible).not.toMatch(/press|continue|enter/i)
  })
})
