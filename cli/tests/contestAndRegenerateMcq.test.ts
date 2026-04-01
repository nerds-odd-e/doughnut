import * as fs from 'node:fs'
import { RecallPromptController } from 'doughnut-api'
import makeMe from 'doughnut-test-fixtures/makeMe'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import { contestAndRegenerateMcq } from '../src/commands/recall/RecallMcqStage.js'
import { tempConfigWithToken } from './tempConfigTestHelpers.js'

describe('contestAndRegenerateMcq', () => {
  let configDir: string
  let savedConfigDir: string | undefined

  beforeEach(() => {
    configDir = tempConfigWithToken()
    savedConfigDir = process.env.DOUGHNUT_CONFIG_DIR
    process.env.DOUGHNUT_CONFIG_DIR = configDir
  })

  afterEach(() => {
    vi.restoreAllMocks()
    if (savedConfigDir === undefined) {
      delete process.env.DOUGHNUT_CONFIG_DIR
    } else {
      process.env.DOUGHNUT_CONFIG_DIR = savedConfigDir
    }
    fs.rmSync(configDir, { recursive: true, force: true })
  })

  test('rejected contest returns message and does not call regenerate', async () => {
    const regenerate = vi.spyOn(RecallPromptController, 'regenerate')
    vi.spyOn(RecallPromptController, 'contest').mockResolvedValue({
      data: { rejected: true, advice: 'Legitimate question.' },
    } as Awaited<ReturnType<typeof RecallPromptController.contest>>)

    await expect(contestAndRegenerateMcq(1, 7)).resolves.toEqual({
      outcome: 'rejected',
      message: 'Legitimate question.',
    })
    expect(regenerate).not.toHaveBeenCalled()
  })

  test('throws when regenerate returns a non-pending MCQ', async () => {
    vi.spyOn(RecallPromptController, 'contest').mockResolvedValue({
      data: { advice: 'proceed' },
    } as Awaited<ReturnType<typeof RecallPromptController.contest>>)
    vi.spyOn(RecallPromptController, 'regenerate').mockResolvedValue({
      data: makeMe.aRecallPrompt.withId(7).withSpellingStem('spell').please(),
    } as Awaited<ReturnType<typeof RecallPromptController.regenerate>>)

    await expect(contestAndRegenerateMcq(1, 7)).rejects.toThrow(
      'Regenerated recall prompt is not a pending MCQ.'
    )
  })
})
