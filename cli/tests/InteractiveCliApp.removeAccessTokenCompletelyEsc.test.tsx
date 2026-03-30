import * as fs from 'node:fs'
import * as http from 'node:http'
import type * as net from 'node:net'
import { afterAll, beforeAll, describe, test } from 'vitest'
import { InteractiveCliApp } from '../src/InteractiveCliApp.js'
import {
  pressEscapeAndWaitForCancelledLine,
  renderInkWhenCommandLineReady,
  waitForFrames,
} from './inkTestHelpers.js'
import { tempConfigWithToken } from './tempConfigTestHelpers.js'

describe('InteractiveCliApp /remove-access-token-completely inline + Esc (hung HTTP)', () => {
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

  test('inline label + hung DELETE + Esc shows Cancelled in transcript', async () => {
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
    const configDir = tempConfigWithToken('doughnut-cli-ratc-esc-')
    process.env.DOUGHNUT_CONFIG_DIR = configDir
    process.env.DOUGHNUT_API_BASE_URL = `http://127.0.0.1:${addr.port}`

    try {
      const { stdin, frames } = await renderInkWhenCommandLineReady(
        <InteractiveCliApp />
      )

      stdin.write('/remove-access-token-completely t\r')
      await waitForFrames(
        () => frames.join('\n'),
        (c) => c.includes('Revoking token')
      )

      await pressEscapeAndWaitForCancelledLine(stdin, () => frames.join('\n'))
    } finally {
      await new Promise<void>((resolve) => server.close(() => resolve()))
      fs.rmSync(configDir, { recursive: true, force: true })
    }
  })
})
