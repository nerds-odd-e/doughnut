import * as fs from 'node:fs'
import * as http from 'node:http'
import type * as net from 'node:net'
import { describe, expect, test } from 'vitest'
import { InteractiveCliApp } from '../src/InteractiveCliApp.js'
import { formatVersionOutput } from '../src/commands/version.js'
import {
  renderInkWhenCommandLineReady,
  stripAnsi,
  waitForFrames,
} from './inkTestHelpers.js'
import { tempConfigWithToken } from './tempConfigTestHelpers.js'

type StubOpts = {
  readonly memoryTrackerJson: Record<string, unknown>
  readonly markAsRecalledCount?: { n: number }
}

async function withRecallJustReviewBackend(
  opts: StubOpts,
  run: () => Promise<void>
): Promise<void> {
  const markCount = opts.markAsRecalledCount ?? { n: 0 }
  const server = http.createServer((req, res) => {
    const url = req.url ?? ''
    if (req.method === 'GET' && url.startsWith('/api/recalls/recalling')) {
      res.writeHead(200, { 'Content-Type': 'application/json' })
      res.end(
        JSON.stringify({
          totalAssimilatedCount: 0,
          toRepeat: [{ memoryTrackerId: 1 }],
        })
      )
      return
    }
    if (
      req.method === 'GET' &&
      url.startsWith('/api/memory-trackers/1/recall-prompts')
    ) {
      res.writeHead(200, { 'Content-Type': 'application/json' })
      res.end(JSON.stringify([]))
      return
    }
    if (
      req.method === 'PATCH' &&
      url.startsWith('/api/memory-trackers/1/mark-as-recalled')
    ) {
      markCount.n += 1
      res.writeHead(200, { 'Content-Type': 'application/json' })
      res.end(JSON.stringify({}))
      return
    }
    if (
      req.method === 'GET' &&
      url.match(/^\/api\/memory-trackers\/1(?:\?|$)/)
    ) {
      res.writeHead(200, { 'Content-Type': 'application/json' })
      res.end(JSON.stringify(opts.memoryTrackerJson))
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
  const savedConfigDir = process.env.DOUGHNUT_CONFIG_DIR
  const savedApiBaseUrl = process.env.DOUGHNUT_API_BASE_URL
  process.env.DOUGHNUT_CONFIG_DIR = configDir
  process.env.DOUGHNUT_API_BASE_URL = `http://127.0.0.1:${addr.port}`
  try {
    await run()
  } finally {
    await new Promise<void>((resolve) => server.close(() => resolve()))
    fs.rmSync(configDir, { recursive: true, force: true })
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
  }
}

const baseNoteTimes = {
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

function memoryTrackerWithNote(note: Record<string, unknown>) {
  return {
    id: 1,
    nextRecallAt: '2026-06-01T00:00:00Z',
    note,
  }
}

describe('recall just-review (interactive)', () => {
  test('empty Enter and non-y/n committed line do not recall; y then completes once', async () => {
    const markAsRecalledCount = { n: 0 }
    await withRecallJustReviewBackend(
      {
        markAsRecalledCount,
        memoryTrackerJson: memoryTrackerWithNote({
          id: 99,
          ...baseNoteTimes,
          noteTopology: {
            id: 1,
            title: 'Alpha',
            notebookTitle: 'NB',
          },
          details: 'body',
        }),
      },
      async () => {
        const { stdin, frames } = await renderInkWhenCommandLineReady(
          <InteractiveCliApp />
        )
        expect(stripAnsi(frames.join('\n'))).toContain(formatVersionOutput())

        stdin.write('/recall\r')
        await waitForFrames(
          () => frames.join('\n'),
          (c) =>
            stripAnsi(c).includes('Yes, I remember?') &&
            stripAnsi(c).includes('Alpha')
        )

        stdin.write('\r')
        await waitForFrames(
          () => frames.join('\n'),
          (c) =>
            stripAnsi(c).includes('Yes, I remember?') &&
            !stripAnsi(c).includes('Recalled successfully')
        )

        stdin.write('q\r')
        await waitForFrames(
          () => frames.join('\n'),
          (c) =>
            stripAnsi(c).includes('Yes, I remember?') &&
            !stripAnsi(c).includes('Recalled successfully')
        )

        stdin.write('\x7f')
        await waitForFrames(
          () => stripAnsi(frames.at(-1) ?? ''),
          (f) => f.includes('> ') && !f.includes('> q')
        )

        stdin.write('y\r')
        await waitForFrames(
          () => frames.join('\n'),
          (c) => stripAnsi(c).includes('Recalled successfully')
        )
        expect(markAsRecalledCount.n).toBe(1)
      }
    )
  })

  test('missing note title falls back to Note; empty details; no notebook line', async () => {
    await withRecallJustReviewBackend(
      {
        memoryTrackerJson: memoryTrackerWithNote({
          id: 99,
          ...baseNoteTimes,
          noteTopology: {
            id: 1,
            title: '   ',
          },
        }),
      },
      async () => {
        const { stdin, frames } = await renderInkWhenCommandLineReady(
          <InteractiveCliApp />
        )
        stdin.write('/recall\r')
        await waitForFrames(
          () => frames.join('\n'),
          (c) => {
            const plain = stripAnsi(c)
            return (
              plain.includes('Yes, I remember?') &&
              plain.includes('Note') &&
              !plain.includes('Alpha')
            )
          }
        )

        const combined = stripAnsi(frames.join('\n'))
        expect(combined).toContain('Note')
        expect(combined).not.toContain('Alpha')
        stdin.write('n\r')
        await waitForFrames(
          () => frames.join('\n'),
          (c) => stripAnsi(c).includes('Marked as not recalled.')
        )
      }
    )
  })
})
