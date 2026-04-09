import { createCanvas } from 'canvas'
import { describe, expect, it } from 'vitest'
import { viewportPngBuffersToGif } from '../src/xterm/viewportPngSequenceToGif'

function solidPng(w: number, h: number, fill: string): Buffer {
  const c = createCanvas(w, h)
  const ctx = c.getContext('2d')
  ctx.fillStyle = fill
  ctx.fillRect(0, 0, w, h)
  return c.toBuffer('image/png')
}

describe('viewportPngBuffersToGif', () => {
  it('returns a non-empty GIF for two same-size PNGs', async () => {
    const a = solidPng(32, 24, '#ff0000')
    const b = solidPng(32, 24, '#00ff00')
    const gif = await viewportPngBuffersToGif([a, b], 100)
    expect(gif.length).toBeGreaterThan(100)
    expect(gif.subarray(0, 6).toString('ascii')).toBe('GIF89a')
  })

  it('rejects empty buffer list', async () => {
    await expect(viewportPngBuffersToGif([])).rejects.toThrow(/no frames/)
  })
})
