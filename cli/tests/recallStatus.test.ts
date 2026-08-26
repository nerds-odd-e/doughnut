import * as fs from 'node:fs'
import { RecallsController } from 'donut-api'
import makeMe from 'donut-test-fixtures/makeMe'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import { recallStatus } from '../src/commands/recallStatus.js'
import { tempConfigWithToken } from './tempConfigTestHelpers.js'

function dueList(toRepeat: { memoryTrackerId: number; spelling: boolean }[]) {
  return makeMe.aDueMemoryTrackersList
    .totalAssimilatedCount(0)
    .toRepeat(toRepeat)
    .please()
}

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

  test.each([
    ['toRepeat absent', { totalAssimilatedCount: 0 }],
    ['toRepeat empty', dueList([])],
  ])('0 notes when %s', async (_label, data) => {
    recallingSpy.mockResolvedValue({
      data,
    } as Awaited<ReturnType<typeof RecallsController.recalling>>)
    await expect(recallStatus()).resolves.toBe('0 notes to recall today')
  })

  test('singular when exactly one due tracker', async () => {
    recallingSpy.mockResolvedValue({
      data: dueList([{ memoryTrackerId: 1, spelling: false }]),
    } as Awaited<ReturnType<typeof RecallsController.recalling>>)
    await expect(recallStatus()).resolves.toBe('1 note to recall today')
  })

  test('plural when multiple due trackers', async () => {
    recallingSpy.mockResolvedValue({
      data: dueList([
        { memoryTrackerId: 1, spelling: false },
        { memoryTrackerId: 2, spelling: false },
      ]),
    } as Awaited<ReturnType<typeof RecallsController.recalling>>)
    await expect(recallStatus()).resolves.toBe('2 notes to recall today')
  })
})
