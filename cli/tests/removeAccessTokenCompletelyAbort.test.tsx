import * as fs from 'node:fs'
import * as http from 'node:http'
import type * as net from 'node:net'
import { useCallback, useRef, type ReactNode } from 'react'
import type { Key } from 'ink'
import { Box, useInput } from 'ink'
import { render } from 'ink-testing-library'
import { afterAll, beforeAll, describe, expect, test } from 'vitest'
import { AsyncAssistantFetchStage } from '../src/commands/gmail/AsyncAssistantFetchStage.js'
import { removeAccessTokenCompletely } from '../src/commands/accessToken/accessToken.js'
import type { StageKeyHandler } from '../src/commands/accessToken/stageKeyForwardContext.js'
import { SetStageKeyHandlerContext } from '../src/commands/accessToken/stageKeyForwardContext.js'
import { tempConfigWithToken } from './tempConfigTestHelpers.js'
import { waitForFrames } from './inkTestHelpers.js'

function StageKeyRoot({ children }: { readonly children: ReactNode }) {
  const stageKeyHandlerRef = useRef<StageKeyHandler | null>(null)
  const setStageKeyHandler = useCallback((handler: StageKeyHandler | null) => {
    stageKeyHandlerRef.current = handler
  }, [])
  useInput(
    useCallback((input: string, key: Key) => {
      stageKeyHandlerRef.current?.(input, key)
    }, [])
  )
  return (
    <SetStageKeyHandlerContext.Provider value={setStageKeyHandler}>
      <Box>{children}</Box>
    </SetStageKeyHandlerContext.Provider>
  )
}

describe('removeAccessTokenCompletely + AbortSignal (Doughnut HTTP)', () => {
  let savedConfigDir: string | undefined
  let savedApiBaseUrl: string | undefined

  beforeAll(() => {
    savedConfigDir = process.env.DOUGHNUT_CONFIG_DIR
    savedApiBaseUrl = process.env.DOUGHNUT_API_BASE_URL
  })

  afterAll(() => {
    if (savedConfigDir === undefined) {
      delete process.env.DOUGHNUT_CONFIG_DIR
    } else {
      process.env.DOUGHNUT_CONFIG_DIR = savedConfigDir
    }
    if (savedApiBaseUrl === undefined) {
      delete process.env.DOUGHNUT_API_BASE_URL
    } else {
      process.env.DOUGHNUT_API_BASE_URL = savedApiBaseUrl
    }
  })

  test('Escape during revoke wait settles Cancelled when signal is passed to SDK', async () => {
    const server = http.createServer((req, res) => {
      if (
        req.method === 'DELETE' &&
        req.url?.startsWith('/api/user/token-info')
      ) {
        return
      }
      res.writeHead(404)
      res.end()
    })
    await new Promise<void>((resolve, reject) => {
      server.listen(0, '127.0.0.1', () => resolve())
      server.on('error', reject)
    })
    const addr = server.address() as net.AddressInfo
    const configDir = tempConfigWithToken()
    process.env.DOUGHNUT_CONFIG_DIR = configDir
    process.env.DOUGHNUT_API_BASE_URL = `http://127.0.0.1:${addr.port}`

    let settled: string | null = null
    try {
      const { stdin, frames } = render(
        <StageKeyRoot>
          <AsyncAssistantFetchStage
            spinnerLabel="Revoking token…"
            runAssistantMessage={async (signal) => {
              await removeAccessTokenCompletely('t', signal)
              return 'done'
            }}
            onSettled={(t) => {
              settled = t
            }}
          />
        </StageKeyRoot>
      )

      await waitForFrames(
        () => frames.join('\n'),
        (c) => c.includes('Revoking token')
      )

      stdin.write('\u001b')
      await waitForFrames(
        () => frames.join('\n'),
        () => settled !== null
      )

      expect(settled).toBe('Cancelled.')
    } finally {
      await new Promise<void>((resolve) => server.close(() => resolve()))
      fs.rmSync(configDir, { recursive: true, force: true })
    }
  })
})
