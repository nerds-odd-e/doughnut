import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import * as fs from 'node:fs'
import * as os from 'node:os'
import * as path from 'node:path'
import { RecallsController, UserController } from 'doughnut-api'
import { addAccessToken } from '../src/accessToken.js'
import { processInput, resetRecallStateForTesting } from '../src/interactive.js'
import { cancelInFlightRecallNextFetchFor } from '../src/interactiveFetchWait.js'

vi.mock('doughnut-api', () => ({
  getApiConfig: () => ({ apiBaseUrl: 'http://localhost:9081' }),
  configureClient: vi.fn(),
  MemoryTrackerController: {
    askAQuestion: vi.fn(),
    markAsRecalled: vi.fn(),
    showMemoryTracker: vi.fn(),
  },
  RecallsController: {
    recalling: vi.fn(),
  },
  RecallPromptController: {
    answerQuiz: vi.fn(),
    answerSpelling: vi.fn(),
    contest: vi.fn(),
    regenerate: vi.fn(),
  },
  UserController: {
    getTokenInfo: vi.fn(),
  },
}))

function createTempDir(): string {
  return fs.mkdtempSync(path.join(os.tmpdir(), 'doughnut-slow-recall-pi-'))
}

function outputAdapter() {
  return {
    log: vi.fn(),
    logError: vi.fn(),
    writeCurrentPrompt: vi.fn(),
    beginCurrentPrompt: vi.fn(),
    onInteractiveFetchWaitChanged: vi.fn(),
  }
}

describe('slow recall load with real recallNext (phase 3.2)', () => {
  let originalConfigDir: string | undefined
  let originalSlow: string | undefined

  beforeEach(() => {
    originalConfigDir = process.env.DOUGHNUT_CONFIG_DIR
    process.env.DOUGHNUT_CONFIG_DIR = createTempDir()
    originalSlow = process.env.DOUGHNUT_CLI_SLOW_RECALL_LOAD_MS
    process.env.DOUGHNUT_CLI_SLOW_RECALL_LOAD_MS = '60000'
    vi.clearAllMocks()
    resetRecallStateForTesting()
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
    if (originalConfigDir === undefined) {
      delete process.env.DOUGHNUT_CONFIG_DIR
    } else {
      process.env.DOUGHNUT_CONFIG_DIR = originalConfigDir
    }
    if (originalSlow === undefined) {
      delete process.env.DOUGHNUT_CLI_SLOW_RECALL_LOAD_MS
    } else {
      process.env.DOUGHNUT_CLI_SLOW_RECALL_LOAD_MS = originalSlow
    }
  })

  test('/recall: cancel during slow delay logs Cancelled by user.', async () => {
    vi.mocked(RecallsController.recalling).mockResolvedValue({
      data: { toRepeat: [] },
    } as never)
    vi.mocked(UserController.getTokenInfo).mockResolvedValue({
      data: { id: 1, label: 'Test Token' },
    } as never)
    await addAccessToken('test-token')

    const out = outputAdapter()
    const done = processInput('/recall', out)
    await vi.waitFor(() =>
      expect(out.onInteractiveFetchWaitChanged).toHaveBeenCalled()
    )
    expect(cancelInFlightRecallNextFetchFor(out)).toBe(true)
    await done
    expect(out.log).toHaveBeenCalledWith('Cancelled by user.')
    expect(RecallsController.recalling).not.toHaveBeenCalled()
    expect(out.onInteractiveFetchWaitChanged).toHaveBeenCalledTimes(2)
  })
})
