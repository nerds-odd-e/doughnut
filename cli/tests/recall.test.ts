import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import * as fs from 'node:fs'
import * as os from 'node:os'
import * as path from 'node:path'
import {
  RecallsController,
  UserController,
} from '@generated/doughnut-backend-api/sdk.gen'
import { addAccessToken } from '../src/accessToken.js'
import { recallStatus } from '../src/recall.js'

vi.mock('@generated/doughnut-backend-api/sdk.gen', () => ({
  RecallsController: {
    recalling: vi.fn(),
  },
  UserController: {
    getTokenInfo: vi.fn(),
  },
}))

vi.mock('doughnut-api', () => ({
  getApiConfig: () => ({ apiBaseUrl: 'http://localhost:9081' }),
  configureClient: vi.fn(),
}))

function createTempDir(): string {
  return fs.mkdtempSync(path.join(os.tmpdir(), 'doughnut-recall-test-'))
}

function mockRecalling(toRepeat: unknown[] = []) {
  return vi.mocked(RecallsController.recalling).mockResolvedValue({
    data: { toRepeat },
  } as never)
}

describe('recallStatus', () => {
  let originalConfigDir: string | undefined

  beforeEach(() => {
    originalConfigDir = process.env.DOUGHNUT_CONFIG_DIR
    process.env.DOUGHNUT_CONFIG_DIR = createTempDir()
    vi.clearAllMocks()
  })

  afterEach(() => {
    if (originalConfigDir === undefined) {
      delete process.env.DOUGHNUT_CONFIG_DIR
    } else {
      process.env.DOUGHNUT_CONFIG_DIR = originalConfigDir
    }
  })

  test('returns "0 notes to recall today" when toRepeat is empty', async () => {
    mockRecalling([])
    vi.mocked(UserController.getTokenInfo).mockResolvedValue({
      data: { id: 1, label: 'Test Token' },
    } as never)
    await addAccessToken('test-token')

    const result = await recallStatus()

    expect(result).toBe('0 notes to recall today')
    expect(RecallsController.recalling).toHaveBeenCalledWith(
      expect.objectContaining({
        query: expect.objectContaining({
          timezone: expect.any(String),
          dueindays: 0,
        }),
      })
    )
  })

  test('returns "1 note to recall today" when one note is due', async () => {
    mockRecalling([{ id: 1 }])
    vi.mocked(UserController.getTokenInfo).mockResolvedValue({
      data: { id: 1, label: 'Test Token' },
    } as never)
    await addAccessToken('test-token')

    const result = await recallStatus()

    expect(result).toBe('1 note to recall today')
  })

  test('returns "3 notes to recall today" when three notes are due', async () => {
    mockRecalling([{ id: 1 }, { id: 2 }, { id: 3 }])
    vi.mocked(UserController.getTokenInfo).mockResolvedValue({
      data: { id: 1, label: 'Test Token' },
    } as never)
    await addAccessToken('test-token')

    const result = await recallStatus()

    expect(result).toBe('3 notes to recall today')
  })

  test('uses Intl timezone for recalling API', async () => {
    mockRecalling([])
    const expectedTimezone = Intl.DateTimeFormat().resolvedOptions().timeZone
    vi.mocked(UserController.getTokenInfo).mockResolvedValue({
      data: { id: 1, label: 'Test Token' },
    } as never)
    await addAccessToken('test-token')

    await recallStatus()

    expect(RecallsController.recalling).toHaveBeenCalledWith(
      expect.objectContaining({
        query: expect.objectContaining({
          timezone: expectedTimezone,
          dueindays: 0,
        }),
      })
    )
  })

  test('throws "No default access token" when no token exists', async () => {
    mockRecalling([])

    await expect(recallStatus()).rejects.toThrow(
      'No default access token. Add one first with /add-access-token.'
    )
    expect(RecallsController.recalling).not.toHaveBeenCalled()
  })

  test('handles response with undefined toRepeat', async () => {
    vi.mocked(RecallsController.recalling).mockResolvedValue({
      data: {},
    } as never)
    vi.mocked(UserController.getTokenInfo).mockResolvedValue({
      data: { id: 1, label: 'Test Token' },
    } as never)
    await addAccessToken('test-token')

    const result = await recallStatus()

    expect(result).toBe('0 notes to recall today')
  })
})
