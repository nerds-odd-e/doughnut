import { createCanvas } from 'canvas'
import type { Terminal } from '@xterm/headless'

const FONT_SIZE_PX = 14
const VIEWPORT_PADDING_PX = 6
const DEFAULT_BG = '#1e1e1e'

/** VGA-style ANSI 0–15 (foreground); index 0 is black, 7 light gray, 15 white. */
const ANSI16: [number, number, number][] = [
  [0, 0, 0],
  [205, 49, 49],
  [13, 188, 121],
  [229, 229, 16],
  [36, 114, 200],
  [188, 63, 188],
  [17, 168, 205],
  [229, 229, 229],
  [102, 102, 102],
  [241, 76, 76],
  [35, 209, 139],
  [245, 245, 67],
  [59, 142, 234],
  [214, 112, 214],
  [41, 184, 219],
  [255, 255, 255],
]

const CUBE_LEVELS = [0, 95, 135, 175, 215, 255]

function rgbCss(r: number, g: number, b: number): string {
  return `rgb(${r},${g},${b})`
}

function ansi256ToRgb(idx: number): [number, number, number] {
  if (idx >= 0 && idx < 16) return ANSI16[idx]!
  if (idx >= 232) {
    const v = 8 + (idx - 232) * 10
    return [v, v, v]
  }
  if (idx >= 16 && idx <= 231) {
    const i = idx - 16
    const r = CUBE_LEVELS[Math.floor(i / 36)]!
    const g = CUBE_LEVELS[Math.floor((i % 36) / 6)]!
    const b = CUBE_LEVELS[i % 6]!
    return [r, g, b]
  }
  return [204, 204, 204]
}

type CellLike = {
  getWidth(): number
  getChars(): string
  isInverse(): boolean
  isInvisible(): boolean
  isFgRGB(): boolean
  isBgRGB(): boolean
  isFgPalette(): boolean
  isBgPalette(): boolean
  isFgDefault(): boolean
  isBgDefault(): boolean
  getFgColor(): number
  getBgColor(): number
  isBold(): boolean
}

function xtermRgbColor(n: number): [number, number, number] {
  const r = (n >>> 16) & 255
  const g = (n >>> 8) & 255
  const b = n & 255
  return [r, g, b]
}

function resolveSideRgb(cell: CellLike, fg: boolean): [number, number, number] {
  if (fg) {
    if (cell.isFgRGB()) return xtermRgbColor(cell.getFgColor())
    if (cell.isFgPalette()) return ansi256ToRgb(cell.getFgColor() & 255)
    if (cell.isFgDefault()) return [204, 204, 204]
    return xtermRgbColor(cell.getFgColor())
  }
  if (cell.isBgRGB()) return xtermRgbColor(cell.getBgColor())
  if (cell.isBgPalette()) return ansi256ToRgb(cell.getBgColor() & 255)
  if (cell.isBgDefault()) return [30, 30, 30]
  return xtermRgbColor(cell.getBgColor())
}

function effectiveRgb(cell: CellLike | undefined): {
  fg: [number, number, number]
  bg: [number, number, number]
} {
  if (!cell) {
    return {
      fg: [204, 204, 204],
      bg: [30, 30, 30],
    }
  }
  let fg = resolveSideRgb(cell, true)
  let bg = resolveSideRgb(cell, false)
  if (cell.isInverse()) {
    ;[fg, bg] = [bg, fg]
  }
  return { fg, bg }
}

/**
 * Rasterize the xterm **viewport** (visible rows) to a PNG buffer.
 * Uses node-canvas; intended for failure artifacts (CLI E2E), not pixel-perfect theme parity.
 */
export function viewportPngFromHeadlessTerminal(term: Terminal): Buffer {
  const cols = term.cols
  const rows = term.rows
  const buffer = term.buffer.active
  const viewportY = buffer.viewportY

  const probe = createCanvas(16, 16)
  const pctx = probe.getContext('2d')
  pctx.font = `${FONT_SIZE_PX}px monospace`
  const cellW = Math.max(8, Math.ceil(pctx.measureText('M').width))
  const cellH = Math.ceil(FONT_SIZE_PX * 1.25)

  const w = cols * cellW + VIEWPORT_PADDING_PX * 2
  const h = rows * cellH + VIEWPORT_PADDING_PX * 2
  const canvas = createCanvas(w, h)
  const ctx = canvas.getContext('2d')
  ctx.fillStyle = DEFAULT_BG
  ctx.fillRect(0, 0, w, h)
  ctx.textBaseline = 'top'

  for (let row = 0; row < rows; row++) {
    const termLine = buffer.getLine(viewportY + row)
    let prev = termLine?.getCell(0) as CellLike | undefined
    for (let col = 0; col < cols; col++) {
      const cell = termLine?.getCell(col, prev) as CellLike | undefined
      prev = cell
      if (cell && cell.getWidth() === 0) continue

      const raw = cell?.getChars() ?? ''
      const text = raw === '' ? ' ' : raw
      if (cell?.isInvisible()) {
        const { bg } = effectiveRgb(cell)
        ctx.fillStyle = rgbCss(bg[0], bg[1], bg[2])
        ctx.fillRect(
          VIEWPORT_PADDING_PX + col * cellW,
          VIEWPORT_PADDING_PX + row * cellH,
          cellW,
          cellH
        )
        continue
      }

      const { fg, bg } = effectiveRgb(cell)
      const x = VIEWPORT_PADDING_PX + col * cellW
      const y = VIEWPORT_PADDING_PX + row * cellH
      ctx.fillStyle = rgbCss(bg[0], bg[1], bg[2])
      ctx.fillRect(x, y, cellW, cellH)
      const weight = cell?.isBold() ? 'bold' : 'normal'
      ctx.font = `${weight} ${FONT_SIZE_PX}px monospace`
      ctx.fillStyle = rgbCss(fg[0], fg[1], fg[2])
      ctx.fillText(text, x, y)
    }
  }

  return canvas.toBuffer('image/png')
}
