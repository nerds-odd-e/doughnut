/**
 * OSC 133 ; A ST — FinalTerm / shell-integration "prompt start".
 * Invisible on screen; shared by renderer (emission) and E2E PTY harness (detection).
 */
export const OSC_133_INPUT_BOX_SETTLED = '\x1b]133;A\x07' as const
