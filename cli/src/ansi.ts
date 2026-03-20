/** ANSI escape codes for terminal display (no imports to avoid cycles). */
export const GREY = '\x1b[90m'
/** Current prompt line while waiting on a slow interactive command (bright blue; distinct from separator, hints, commands). */
export const INTERACTIVE_FETCH_WAIT_PROMPT_FG = '\x1b[94m'
export const RESET = '\x1b[0m'
export const REVERSE = '\x1b[7m'
export const HIDE_CURSOR = '\x1b[?25l'
export const SHOW_CURSOR = '\x1b[?25h'
