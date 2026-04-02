import * as fs from 'node:fs'
import { RecallsController } from 'doughnut-api'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import { InteractiveCliApp } from '../src/InteractiveCliApp.js'
import {
  pressEscapeAndWait,
  renderInkWhenCommandLineReady,
  stripAnsi,
} from './inkTestHelpers.js'
import { tempConfigWithToken } from './tempConfigTestHelpers.js'

describe('InteractiveCliApp /recall-status', () => {
  let configDir: string
  let savedConfigDir: string | undefined
  let recallingSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    configDir = tempConfigWithToken()
    savedConfigDir = process.env.DOUGHNUT_CONFIG_DIR
    process.env.DOUGHNUT_CONFIG_DIR = configDir
    recallingSpy = vi.spyOn(RecallsController, 'recalling')
  })

  afterEach(() => {
    recallingSpy.mockRestore()
    if (savedConfigDir === undefined) {
      delete process.env.DOUGHNUT_CONFIG_DIR
    } else {
      process.env.DOUGHNUT_CONFIG_DIR = savedConfigDir
    }
    fs.rmSync(configDir, { recursive: true, force: true })
  })

  test('Escape during fetch settles Cancelled when recalling honors signal', async () => {
    recallingSpy.mockImplementation(
      async (options: { signal?: AbortSignal }) => {
        const { signal } = options
        if (signal === undefined) {
          throw new Error('expected AbortSignal from recallStatus')
        }
        await new Promise<never>((_, reject) => {
          signal.addEventListener(
            'abort',
            () => {
              reject(new DOMException('Aborted', 'AbortError'))
            },
            { once: true }
          )
        })
      }
    )

    const { stdin, frames, waitForFramesToInclude } =
      await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    stdin.write('/recall-status\r')
    await waitForFramesToInclude('Loading recall status')

    await pressEscapeAndWait(
      stdin,
      () => frames.join('\n'),
      (c) =>
        stripAnsi(c).includes('/recall-status') &&
        stripAnsi(c).includes('Cancelled.')
    )

    const combined = stripAnsi(frames.join('\n'))
    expect(combined).toContain('/recall-status')
    expect(combined).toContain('Cancelled.')
  })

  test('fast recalling mock shows due count line', async () => {
    recallingSpy.mockResolvedValue({
      data: {
        totalAssimilatedCount: 0,
        toRepeat: [
          { memoryTrackerId: 1, spelling: false },
          { memoryTrackerId: 2, spelling: false },
        ],
      },
    } as Awaited<ReturnType<typeof RecallsController.recalling>>)

    const { stdin, lastStrippedFrame, waitForFramesToInclude } =
      await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    stdin.write('/recall-status\r')
    await waitForFramesToInclude('2 notes to recall today')

    const final = lastStrippedFrame()
    expect(final.split('2 notes to recall today').length - 1).toBe(1)
    expect(final).not.toContain('Cancelled.')
  })
})
