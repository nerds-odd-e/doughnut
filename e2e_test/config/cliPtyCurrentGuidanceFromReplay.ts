const DEFAULT_PROMPT_MARKER = '> '

/** Ink command line with empty buffer: only `>` and horizontal whitespace (cursor may add a space cell). */
function isEmptyInteractivePromptLine(line: string): boolean {
  return /^\s*>\s*$/.test(line)
}

/** Bottom border of Ink `borderStyle="single"` main prompt box (light up-and-right). */
const MAIN_PROMPT_BOX_BOTTOM_RE = /^\s*└/

/**
 * Plaintext below the interactive command line.
 * Main REPL: boxed prompt ends with a `└` box-drawing row; guidance is everything below that.
 * Stages still use a bare `> ` line — empty `> ` row or last line containing `> ` (markdown blockquotes use `> ` too; see isEmptyInteractivePromptLine).
 * Fallback when neither matches: last 8 rows of the simulated screen (see cli.mdc Current guidance).
 */
export function extractCurrentGuidanceFromReplayedPlaintext(
  plainScreen: string,
  promptMarker: string = DEFAULT_PROMPT_MARKER
): string {
  const lines = plainScreen.split('\n')
  for (let i = lines.length - 1; i >= 0; i--) {
    const line = lines[i]
    if (line !== undefined && MAIN_PROMPT_BOX_BOTTOM_RE.test(line)) {
      return lines.slice(i + 1).join('\n')
    }
  }
  let promptLineIdx = -1
  for (let i = lines.length - 1; i >= 0; i--) {
    const line = lines[i]
    if (line !== undefined && isEmptyInteractivePromptLine(line)) {
      promptLineIdx = i
      break
    }
  }
  if (promptLineIdx < 0) {
    for (let i = lines.length - 1; i >= 0; i--) {
      const line = lines[i]
      if (line !== undefined && line.includes(promptMarker)) {
        promptLineIdx = i
        break
      }
    }
  }
  if (promptLineIdx < 0) {
    return lines.slice(Math.max(0, lines.length - 8)).join('\n')
  }
  return lines.slice(promptLineIdx + 1).join('\n')
}
