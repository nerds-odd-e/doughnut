import { Chalk } from 'chalk'

/**
 * Forced color level so Vitest and non-TTY runs emit the same SGR as a real TTY (phase 11.6).
 * Use for TTY paint strings; keep raw CSI in {@link ./ansi.js} for cursor + full {@link RESET} only.
 */
export const terminalChalk = new Chalk({ level: 3 })

function sgrOpen(style: (text: string) => string): string {
  // biome-ignore lint/suspicious/noControlCharactersInRegex: match SGR open after ESC
  const m = style(' ').match(/^\x1b\[[0-9;]*m/)
  if (m === null) {
    throw new Error('chalk style produced no SGR open sequence')
  }
  return m[0]!
}

/** Opening SGR for grey foreground; matches {@link terminalChalk.gray}. */
export const GREY = sgrOpen((t) => terminalChalk.gray(t))

/** Opening SGR for bright blue (fetch-wait stage label); matches {@link terminalChalk.blueBright}. */
export const INTERACTIVE_FETCH_WAIT_PROMPT_FG = sgrOpen((t) =>
  terminalChalk.blueBright(t)
)

/** Opening SGR for stage band / past-input background (256-color index 236). */
export const GREY_BG = sgrOpen((t) => terminalChalk.bgAnsi256(236)(t))
