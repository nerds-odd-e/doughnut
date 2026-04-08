import { Terminal } from '@xterm/headless'
import { describe, expect, it } from 'vitest'
import { viewportPngFromHeadlessTerminal } from '../src/viewportPngFromHeadlessTerminal'

describe('viewportPngFromHeadlessTerminal', () => {
  it('returns PNG magic bytes after replaying output', async () => {
    const term = new Terminal({ cols: 40, rows: 6, allowProposedApi: true })
    await new Promise<void>((resolve, reject) => {
      try {
        term.write('hello \x1b[32mworld\x1b[0m\r\n', () => resolve())
      } catch (e) {
        reject(e)
      }
    })
    const buf = viewportPngFromHeadlessTerminal(term)
    term.dispose()
    expect(
      buf.subarray(0, 4).equals(Buffer.from([0x89, 0x50, 0x4e, 0x47]))
    ).toBe(true)
    expect(buf.length).toBeGreaterThan(200)
  })
})
