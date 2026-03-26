import { runInteractiveTtySession } from './interactiveTtySession.js'
import type { TTYDeps } from './ttyDeps.js'

export type { TTYDeps } from './ttyDeps.js'

type TTYInput = NodeJS.ReadableStream & {
  setRawMode?: (mode: boolean) => void
  resume?: () => void
  setEncoding?: (encoding: BufferEncoding) => void
}

export async function runTTY(stdin: TTYInput, deps: TTYDeps): Promise<void> {
  runInteractiveTtySession(stdin, deps)
}
