export function stripAnsi(s: string): string {
  const esc = String.fromCharCode(0x1b)
  return s.replace(new RegExp(`${esc}\\[[0-9;?]*[a-zA-Z]`, 'g'), '')
}

/** Advance the event loop until `predicate` holds or `maxTicks` is exhausted (no fixed wall-clock sleep). */
export async function waitForFrames(
  getCombined: () => string,
  predicate: (combined: string) => boolean,
  maxTicks = 5000
): Promise<void> {
  for (let i = 0; i < maxTicks; i++) {
    if (predicate(getCombined())) {
      return
    }
    await new Promise<void>((resolve) => {
      setImmediate(resolve)
    })
  }
  const combined = getCombined()
  throw new Error(
    `Output condition not met within ${maxTicks} event-loop turns. Last output:\n${combined}`
  )
}

export function waitForLastFrame(
  lastFrame: () => string | undefined,
  predicate: (frame: string) => boolean,
  maxTicks = 5000
): Promise<void> {
  return waitForFrames(() => stripAnsi(lastFrame() ?? ''), predicate, maxTicks)
}
