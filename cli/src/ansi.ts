/** ANSI escape codes for terminal display (no imports to avoid cycles). */
export const GREY = '\x1b[90m'
/** In-progress / loading current-prompt text (distinct from green separator, grey hints, cyan commands). */
export const LOADING_FOREGROUND = '\x1b[94m'
export const RESET = '\x1b[0m'
export const REVERSE = '\x1b[7m'
export const HIDE_CURSOR = '\x1b[?25l'
export const SHOW_CURSOR = '\x1b[?25h'
