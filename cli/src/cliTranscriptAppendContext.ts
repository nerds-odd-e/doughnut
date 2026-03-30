import { createContext } from 'react'

/** Append assistant text to the session transcript without closing the active stage. */
export const CliTranscriptAppendContext = createContext<
  ((assistantText: string) => void) | null
>(null)
