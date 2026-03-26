/**
 * Full clear + cursor home (CSI). Used only by TTY stdout replay helpers in tests.
 * The interactive shell does not emit this sequence (no product full-screen clear).
 */
export const TTY_FULL_CLEAR_SEQUENCE = '\x1b[H\x1b[2J' as const
