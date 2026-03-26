/**
 * Raw CSI kept out of chalk: full reset, cursor show/hide.
 * Styled foreground/background lives in {@link ./terminalChalk.ts}.
 */
export const RESET = '\x1b[0m'
export const HIDE_CURSOR = '\x1b[?25l'
export const SHOW_CURSOR = '\x1b[?25h'
