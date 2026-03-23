/** ANSI escape codes for terminal display (no imports to avoid cycles). */
export const GREY = '\x1b[90m'
export const RED = '\x1b[31m'
export const ITALIC = '\x1b[3m'
/** Foreground for the **Current Stage Indicator** label during interactive fetch-wait on the TTY (bright blue). */
export const INTERACTIVE_FETCH_WAIT_PROMPT_FG = '\x1b[94m'
export const RESET = '\x1b[0m'
export const REVERSE = '\x1b[7m'
export const HIDE_CURSOR = '\x1b[?25l'
export const SHOW_CURSOR = '\x1b[?25h'
