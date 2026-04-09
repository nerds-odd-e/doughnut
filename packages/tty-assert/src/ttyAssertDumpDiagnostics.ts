import {
  TERMINAL_ERROR_RAW_TAIL_BYTES,
  headPreview,
  sanitizeVisibleTextForError,
  tailPreview,
} from './errorSnapshotFormatting'
import { stripAnsiCliPty } from './stripAnsi'

export type TtyAssertDumpDiagnostics = {
  rawByteLength: number
  ansiStrippedLength: number
  replayedScreenPlaintextHeadPreview: string
  replayedScreenPlaintextTailPreview: string
  strippedTranscriptTailPreview: string
  rawTailSanitizedPreview: string
}

export function buildTtyAssertDumpDiagnostics(input: {
  raw: string
  replayedPlain: string
}): TtyAssertDumpDiagnostics {
  const stripped = stripAnsiCliPty(input.raw)
  return {
    rawByteLength: input.raw.length,
    ansiStrippedLength: stripped.length,
    replayedScreenPlaintextHeadPreview: headPreview(input.replayedPlain),
    replayedScreenPlaintextTailPreview: tailPreview(input.replayedPlain),
    strippedTranscriptTailPreview: tailPreview(stripped),
    rawTailSanitizedPreview: sanitizeVisibleTextForError(
      tailPreview(input.raw.slice(-TERMINAL_ERROR_RAW_TAIL_BYTES))
    ),
  }
}
