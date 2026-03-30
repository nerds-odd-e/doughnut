import * as fs from 'node:fs'
import { RecallsController } from 'doughnut-api'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import { recallStatus } from '../src/commands/recallStatus.js'
import { tempConfigWithToken } from './tempConfigTestHelpers.js'

describe('recallStatus', () => {
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

  test('0 notes when toRepeat is absent', async () => {
    recallingSpy.mockResolvedValue({
      data: { totalAssimilatedCount: 0 },
    } as Awaited<ReturnType<typeof RecallsController.recalling>>)
    await expect(recallStatus()).resolves.toBe('0 notes to recall today')
  })

  test('0 notes when toRepeat is empty', async () => {
    recallingSpy.mockResolvedValue({
      data: { totalAssimilatedCount: 0, toRepeat: [] },
    } as Awaited<ReturnType<typeof RecallsController.recalling>>)
    await expect(recallStatus()).resolves.toBe('0 notes to recall today')
  })

  test('singular when exactly one due tracker', async () => {
    recallingSpy.mockResolvedValue({
      data: {
        totalAssimilatedCount: 0,
        toRepeat: [{ memoryTrackerId: 1, spelling: false }],
      },
    } as Awaited<ReturnType<typeof RecallsController.recalling>>)
    await expect(recallStatus()).resolves.toBe('1 note to recall today')
  })

  test('plural when two due trackers', async () => {
    recallingSpy.mockResolvedValue({
      data: {
        totalAssimilatedCount: 0,
        toRepeat: [
          { memoryTrackerId: 1, spelling: false },
          { memoryTrackerId: 2, spelling: false },
        ],
      },
    } as Awaited<ReturnType<typeof RecallsController.recalling>>)
    await expect(recallStatus()).resolves.toBe('2 notes to recall today')
  })

  test('plural for larger counts', async () => {
    const toRepeat = Array.from({ length: 10 }, (_, i) => ({
      memoryTrackerId: i + 1,
      spelling: false,
    }))
    recallingSpy.mockResolvedValue({
      data: { totalAssimilatedCount: 0, toRepeat },
    } as Awaited<ReturnType<typeof RecallsController.recalling>>)
    await expect(recallStatus()).resolves.toBe('10 notes to recall today')
  })
})
