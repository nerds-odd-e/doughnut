import { CLI_INTERACTIVE_PTY_COLS, CLI_INTERACTIVE_PTY_ROWS } from './geometry'

function defaultParam(nums: number[], i: number, d: number): number {
  const v = nums[i]
  if (v === undefined || v === 0) return d
  return v
}

function clampRow(r: number, rows: number): number {
  return Math.max(0, Math.min(rows - 1, r))
}

function clampCol(c: number, cols: number): number {
  return Math.max(0, Math.min(cols - 1, c))
}

function parseCsiParams(paramStr: string): number[] {
  if (paramStr.startsWith('?')) {
    const rest = paramStr.slice(1)
    if (rest === '') return []
    return rest.split(';').map((s) => {
      const n = parseInt(s, 10)
      return Number.isNaN(n) ? 0 : n
    })
  }
  if (paramStr === '') return []
  return paramStr.split(';').map((s) => {
    if (s === '') return 0
    const n = parseInt(s, 10)
    return Number.isNaN(n) ? 0 : n
  })
}

function eraseLine(grid: string[][], row: number, cols: number): void {
  const r = grid[row]
  if (!r) return
  for (let c = 0; c < cols; c++) r[c] = ' '
}

function eraseLineFrom(
  grid: string[][],
  row: number,
  col: number,
  cols: number
): void {
  const r = grid[row]
  if (!r) return
  for (let c = col; c < cols; c++) r[c] = ' '
}

function eraseLineTo(grid: string[][], row: number, col: number): void {
  const r = grid[row]
  if (!r) return
  for (let c = 0; c <= col; c++) r[c] = ' '
}

function scrollUpOneRow(grid: string[][], rows: number, cols: number): void {
  for (let r = 0; r < rows - 1; r++) {
    const dest = grid[r]
    const src = grid[r + 1]
    if (!(dest && src)) continue
    for (let c = 0; c < cols; c++) {
      dest[c] = src[c] ?? ' '
    }
  }
  const bottom = grid[rows - 1]
  if (bottom) {
    for (let c = 0; c < cols; c++) bottom[c] = ' '
  }
}

function eraseDisplay(
  grid: string[][],
  mode: number,
  row: number,
  col: number,
  rows: number,
  cols: number
): void {
  if (mode === 2 || mode === 3) {
    for (let r = 0; r < rows; r++) eraseLine(grid, r, cols)
    return
  }
  if (mode === 1) {
    for (let r = 0; r < row; r++) eraseLine(grid, r, cols)
    eraseLineTo(grid, row, col)
    return
  }
  eraseLineFrom(grid, row, col, cols)
  for (let r = row + 1; r < rows; r++) eraseLine(grid, r, cols)
}

/**
 * Replays a PTY transcript into a fixed-size screen and returns visible plaintext
 * (trailing spaces stripped per row, rows joined with `\n`).
 */
export function ptyTranscriptToVisiblePlaintext(
  raw: string,
  cols: number = CLI_INTERACTIVE_PTY_COLS,
  rows: number = CLI_INTERACTIVE_PTY_ROWS
): string {
  const grid: string[][] = Array.from({ length: rows }, () =>
    Array.from({ length: cols }, () => ' ')
  )
  let row = 0
  let col = 0
  const saved: { row: number; col: number }[] = []

  const advanceRowOrScroll = (): void => {
    if (row < rows - 1) {
      row++
    } else {
      scrollUpOneRow(grid, rows, cols)
    }
    col = 0
  }

  const writeChar = (ch: string): void => {
    if (ch === '\u0007') return
    const code = ch.charCodeAt(0)
    if (code < 0x20 && ch !== '\t') return
    if (ch === '\t') {
      const next = Math.min(cols, (Math.floor(col / 8) + 1) * 8)
      col = next === col ? Math.min(cols, col + 8) : next
      return
    }
    row = clampRow(row, rows)
    col = clampCol(col, cols)
    const line = grid[row]
    if (line) line[col] = ch
    col++
    if (col >= cols) {
      col = 0
      if (row < rows - 1) {
        row++
      } else {
        scrollUpOneRow(grid, rows, cols)
      }
    }
  }

  let i = 0
  while (i < raw.length) {
    const ch = raw[i] as string
    if (ch === '\x1b') {
      const next = raw[i + 1]
      if (next === '[') {
        let j = i + 2
        let paramStr = ''
        while (j < raw.length) {
          const c = raw[j] as string
          const code = c.charCodeAt(0)
          if (code >= 0x40 && code <= 0x7e) {
            const finalCh = c
            const nums = parseCsiParams(paramStr)
            row = clampRow(row, rows)
            col = clampCol(col, cols)

            if (paramStr.startsWith('?')) {
              i = j + 1
              break
            }

            switch (finalCh) {
              case 'A': {
                const n = defaultParam(nums, 0, 1)
                row = clampRow(row - n, rows)
                break
              }
              case 'B': {
                const n = defaultParam(nums, 0, 1)
                row = clampRow(row + n, rows)
                break
              }
              case 'C': {
                const n = defaultParam(nums, 0, 1)
                col = clampCol(col + n, cols)
                break
              }
              case 'D': {
                const n = defaultParam(nums, 0, 1)
                col = clampCol(col - n, cols)
                break
              }
              case 'G': {
                const n = defaultParam(nums, 0, 1)
                col = clampCol(n - 1, cols)
                break
              }
              case 'H':
              case 'f': {
                const r = defaultParam(nums, 0, 1)
                const c = defaultParam(nums, 1, 1)
                row = clampRow(r - 1, rows)
                col = clampCol(c - 1, cols)
                break
              }
              case 'J': {
                const mode = nums[0] ?? 0
                eraseDisplay(grid, mode, row, col, rows, cols)
                break
              }
              case 'K': {
                const mode = nums[0] ?? 0
                if (mode === 2) eraseLine(grid, row, cols)
                else if (mode === 1) eraseLineTo(grid, row, col)
                else eraseLineFrom(grid, row, col, cols)
                break
              }
              case 'm':
                break
              default:
                break
            }
            i = j + 1
            break
          }
          paramStr += c
          j++
        }
        if (j >= raw.length) i = raw.length
        continue
      }
      if (next === ']') {
        let j = i + 2
        while (j < raw.length) {
          if (raw[j] === '\x07') {
            i = j + 1
            break
          }
          if (raw[j] === '\x1b' && raw[j + 1] === '\\') {
            i = j + 2
            break
          }
          j++
        }
        if (j >= raw.length) i = raw.length
        continue
      }
      if (next === '(' || next === ')') {
        i += 3
        continue
      }
      if (next === '7') {
        saved.push({ row, col })
        i += 2
        continue
      }
      if (next === '8') {
        const p = saved.pop()
        if (p) {
          row = clampRow(p.row, rows)
          col = clampCol(p.col, cols)
        }
        i += 2
        continue
      }
      i += 2
      continue
    }

    if (ch === '\r') {
      col = 0
      i++
      continue
    }
    if (ch === '\n') {
      advanceRowOrScroll()
      i++
      continue
    }

    writeChar(ch)
    i++
  }

  return grid
    .map((line) => line.join('').replace(/\s+$/, ''))
    .join('\n')
    .replace(/\n+$/, '')
}
