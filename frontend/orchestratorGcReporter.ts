import type { Reporter, Vitest } from "vitest/node"

// Long browser-mode runs leak detached iframes/frame-graph state in the Vitest
// orchestrator until Chromium kills the renderer, surfacing as
// "Browser connection was closed while running tests" /
// "[birpc] rpc is closed, cannot call createTesters".
// Forcing GC in the orchestrator page every few seconds keeps those counters at
// baseline. See https://github.com/vitest-dev/vitest/pull/10300.

interface CdpSession {
  send: (method: string, params?: unknown) => Promise<unknown>
}

interface BrowserProvider {
  pages: Map<string, unknown>
  getCDPSession: (sessionId: string) => Promise<CdpSession>
}

const GC_INTERVAL_MS = 5000

export function orchestratorGcReporter(): Reporter {
  let interval: NodeJS.Timeout | null = null
  let cdp: CdpSession | null = null

  const resolveProvider = (vitest: Vitest): BrowserProvider | undefined => {
    for (const project of vitest.projects) {
      const browser = project.browser as unknown as {
        provider?: BrowserProvider
      } | null
      if (browser?.provider?.pages?.size) {
        return browser.provider
      }
    }
  }

  const tick = async (vitest: Vitest) => {
    if (!cdp) {
      const provider = resolveProvider(vitest)
      const sessionId = provider?.pages.keys().next().value
      if (!provider || !sessionId) {
        return
      }
      cdp = await provider.getCDPSession(sessionId)
    }
    await cdp.send("HeapProfiler.collectGarbage")
  }

  return {
    onInit(vitest: Vitest) {
      interval = setInterval(() => {
        // Reset the session on failure so a closed page reconnects next tick.
        tick(vitest).catch(() => {
          cdp = null
        })
      }, GC_INTERVAL_MS)
      interval.unref?.()
    },
    onTestRunEnd() {
      if (interval) {
        clearInterval(interval)
        interval = null
      }
      cdp = null
    },
  }
}
