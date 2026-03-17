/**
 * Parses CLI stdout by ANSI styling into sections for E2E assertions.
 * Sections: history-input (grey bg), history-output (default), status (dim fg).
 */

const GREY_BG = '\x1b[48;5;236m'
const DIM = '\x1b[90m'
// biome-ignore lint/suspicious/noControlCharactersInRegex: stripping ANSI escapes
const ANSI_PATTERN = /\x1b\[[0-9;]*m/g

function stripAnsi(str: string): string {
  return str.replace(ANSI_PATTERN, '')
}

function isHistoryInputLine(line: string): boolean {
  return line.includes(GREY_BG)
}

function isStatusLine(line: string): boolean {
  return line.includes(DIM) && !line.includes(GREY_BG)
}

export type Section = 'history-input' | 'history-output' | 'status'

export function getSectionContent(output: string, section: Section): string {
  const lines = output.split('\n')
  const result: string[] = []

  for (const line of lines) {
    const stripped = stripAnsi(line)
    if (section === 'history-input' && isHistoryInputLine(line)) {
      if (stripped.trim()) {
        result.push(stripped)
      }
    } else if (section === 'status' && isStatusLine(line)) {
      if (stripped.trim()) {
        result.push(stripped)
      }
    } else if (
      section === 'history-output' &&
      !isHistoryInputLine(line) &&
      !isStatusLine(line)
    ) {
      if (stripped.trim()) {
        result.push(stripped)
      }
    }
  }

  return result.join('\n')
}

export function getSectionContentRaw(output: string, section: Section): string {
  const lines = output.split('\n')
  const result: string[] = []

  for (const line of lines) {
    if (section === 'history-input' && isHistoryInputLine(line)) {
      if (line.trim()) {
        result.push(line)
      }
    } else if (section === 'status' && isStatusLine(line)) {
      if (line.trim()) {
        result.push(line)
      }
    } else if (
      section === 'history-output' &&
      !isHistoryInputLine(line) &&
      !isStatusLine(line)
    ) {
      if (line.trim()) {
        result.push(line)
      }
    }
  }

  return result.join('\n')
}

export function getLastCommandOutput(output: string): string {
  const lines = output.split('\n')
  const blocks: string[] = []
  let current: string[] = []

  for (const line of lines) {
    const stripped = stripAnsi(line)
    if (isHistoryInputLine(line)) {
      if (current.length > 0) {
        blocks.push(current.join('\n'))
        current = []
      }
    } else if (!isStatusLine(line) && stripped.trim()) {
      current.push(stripped)
    } else if (isStatusLine(line) && current.length > 0) {
      blocks.push(current.join('\n'))
      current = []
    }
  }
  if (current.length > 0) {
    blocks.push(current.join('\n'))
  }

  return blocks.length > 0 ? blocks[blocks.length - 1]! : ''
}
