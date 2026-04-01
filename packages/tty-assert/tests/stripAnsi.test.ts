import { describe, expect, it } from 'vitest'
import { stripAnsiCliPty } from '../src/stripAnsi'

describe('stripAnsiCliPty', () => {
  it('leaves plain text unchanged', () => {
    expect(stripAnsiCliPty('hello world')).toBe('hello world')
  })

  it('strips simple SGR foreground and reset', () => {
    const raw = '\x1b[31mred\x1b[0m'
    expect(stripAnsiCliPty(raw)).toBe('red')
  })

  it('strips bold and compound SGR params', () => {
    const raw = '\x1b[1;32mbold green\x1b[22;39m'
    expect(stripAnsiCliPty(raw)).toBe('bold green')
  })

  it('strips cursor movement CSI that ends the pattern', () => {
    const raw = 'a\x1b[2Kb'
    expect(stripAnsiCliPty(raw)).toBe('ab')
  })

  it('handles empty string', () => {
    expect(stripAnsiCliPty('')).toBe('')
  })

  it('strips 8-bit CSI introducer where matched by pattern', () => {
    const csi = `${String.fromCharCode(0x9b)}31m`
    expect(stripAnsiCliPty(`${csi}x`)).toBe('x')
  })
})
