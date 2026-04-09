import { createCanvas, loadImage, type CanvasRenderingContext2D } from 'canvas'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)

type GifEncoderInstance = {
  createReadStream(): NodeJS.ReadableStream
  start(): void
  setRepeat(n: number): void
  setDelay(ms: number): void
  setQuality(n: number): void
  addFrame(ctx: CanvasRenderingContext2D): void
  finish(): void
}

const GIFEncoder = require('gifencoder') as new (
  width: number,
  height: number
) => GifEncoderInstance

const DEFAULT_FRAME_DELAY_MS = 250

/**
 * Encodes same-size viewport PNG buffers (from {@link viewportPngFromHeadlessTerminal})
 * into an animated GIF. {@link GIFEncoder#setDelay} uses milliseconds per frame.
 */
export async function viewportPngBuffersToGif(
  pngBuffers: Buffer[],
  frameDelayMs = DEFAULT_FRAME_DELAY_MS
): Promise<Buffer> {
  if (pngBuffers.length === 0) {
    throw new Error('viewportPngBuffersToGif: no frames')
  }
  const first = await loadImage(pngBuffers[0]!)
  const w = first.width
  const h = first.height
  const encoder = new GIFEncoder(w, h)
  const out = new Promise<Buffer>((resolve, reject) => {
    const chunks: Buffer[] = []
    encoder
      .createReadStream()
      .on('data', (c: Buffer) => {
        chunks.push(c)
      })
      .on('end', () => {
        resolve(Buffer.concat(chunks))
      })
      .on('error', reject)
  })

  encoder.start()
  encoder.setRepeat(0)
  encoder.setDelay(frameDelayMs)
  encoder.setQuality(10)

  const canvas = createCanvas(w, h)
  const ctx = canvas.getContext('2d')
  if (!ctx) {
    throw new Error('viewportPngBuffersToGif: canvas 2d context unavailable')
  }

  for (const buf of pngBuffers) {
    const img = await loadImage(buf)
    if (img.width !== w || img.height !== h) {
      throw new Error(
        `viewportPngBuffersToGif: frame size ${img.width}x${img.height} != ${w}x${h}`
      )
    }
    ctx.clearRect(0, 0, w, h)
    ctx.drawImage(img, 0, 0)
    encoder.addFrame(ctx)
  }
  encoder.finish()
  return out
}
