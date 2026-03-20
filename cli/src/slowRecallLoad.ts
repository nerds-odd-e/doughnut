/**
 * Optional pause before the first `recalling` request during interactive recall load.
 * Set `DOUGHNUT_CLI_SLOW_RECALL_LOAD_MS` (positive integer ms, max 60000) to exercise
 * wait UI / Esc cancel without depending on network latency. Off by default; only
 * applies when `recallNext` is called with an `AbortSignal` (TTY recall load path).
 */
const ENV_KEY = 'DOUGHNUT_CLI_SLOW_RECALL_LOAD_MS'
const MAX_MS = 60_000

function parseDelayMs(): number {
  const raw = process.env[ENV_KEY]
  if (raw == null || raw === '') return 0
  const n = Number.parseInt(raw, 10)
  if (!Number.isFinite(n) || n <= 0) return 0
  return Math.min(n, MAX_MS)
}

export async function awaitSlowRecallLoadBeforeFirstFetch(
  signal: AbortSignal | undefined
): Promise<void> {
  const ms = parseDelayMs()
  if (ms === 0 || signal == null) return
  await abortableDelay(ms, signal)
}

function abortableDelay(ms: number, signal: AbortSignal): Promise<void> {
  return new Promise((resolve, reject) => {
    if (signal.aborted) {
      reject(new DOMException('The operation was aborted', 'AbortError'))
      return
    }
    const t = setTimeout(() => {
      signal.removeEventListener('abort', onAbort)
      resolve()
    }, ms)
    const onAbort = () => {
      clearTimeout(t)
      signal.removeEventListener('abort', onAbort)
      reject(new DOMException('The operation was aborted', 'AbortError'))
    }
    signal.addEventListener('abort', onAbort, { once: true })
  })
}
