import { describe, expect, it } from 'vitest'
import { ptyTranscriptToVisiblePlaintextViaXterm } from '../src/ptyTranscriptToVisiblePlaintextViaXterm'

describe('ptyTranscriptToVisiblePlaintextViaXterm', () => {
  it('replays a minimal transcript to stable non-empty plain text', async () => {
    const raw = 'hello\nworld'
    const a = await ptyTranscriptToVisiblePlaintextViaXterm(raw, 20, 4)
    const b = await ptyTranscriptToVisiblePlaintextViaXterm(raw, 20, 4)
    expect(a).toBe(b)
    expect(a.length).toBeGreaterThan(0)
    expect(a).toContain('hello')
    expect(a).toContain('world')
  })

  it('clears screen and shows text written after ED', async () => {
    const raw = `noise\x1b[2J\x1b[HOK`
    const visible = await ptyTranscriptToVisiblePlaintextViaXterm(raw, 10, 3)
    expect(visible).toMatch(/^OK/)
    expect(visible).not.toContain('noise')
  })
})
