import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import * as fs from 'node:fs'
import * as os from 'node:os'
import * as path from 'node:path'
import {
  MemoryTrackerController,
  RecallsController,
  RecallPromptController,
  UserController,
} from '@generated/doughnut-backend-api/sdk.gen'
import { addAccessToken } from '../src/accessToken.js'
import {
  answerQuiz,
  answerSpelling,
  contestAndRegenerate,
  markAsRecalled,
  recallNext,
  recallStatus,
} from '../src/recall.js'

vi.mock('@generated/doughnut-backend-api/sdk.gen', () => ({
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

describe('recallNext', () => {
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

  test('returns none when no notes due', async () => {
    vi.mocked(RecallsController.recalling).mockResolvedValue({
      data: { toRepeat: [] },
    } as never)
    vi.mocked(UserController.getTokenInfo).mockResolvedValue({
      data: { id: 1, label: 'Test Token' },
    } as never)
    await addAccessToken('test-token')

    const result = await recallNext()

    expect(result).toEqual({ type: 'none', message: '0 notes to recall today' })
    expect(MemoryTrackerController.askAQuestion).not.toHaveBeenCalled()
  })

  test('passes dueindays to recalling API', async () => {
    vi.mocked(RecallsController.recalling).mockResolvedValue({
      data: { toRepeat: [] },
    } as never)
    vi.mocked(UserController.getTokenInfo).mockResolvedValue({
      data: { id: 1, label: 'Test Token' },
    } as never)
    await addAccessToken('test-token')

    await recallNext(3)

    expect(RecallsController.recalling).toHaveBeenCalledWith(
      expect.objectContaining({
        query: expect.objectContaining({
          dueindays: 3,
        }),
      })
    )
  })

  test('returns none when dueindays 3 has no notes', async () => {
    vi.mocked(RecallsController.recalling).mockResolvedValue({
      data: { toRepeat: [] },
    } as never)
    vi.mocked(UserController.getTokenInfo).mockResolvedValue({
      data: { id: 1, label: 'Test Token' },
    } as never)
    await addAccessToken('test-token')

    const result = await recallNext(3)

    expect(result).toEqual({ type: 'none', message: '0 notes to recall today' })
  })

  test('returns just-review when askAQuestion returns null', async () => {
    vi.mocked(RecallsController.recalling).mockResolvedValue({
      data: { toRepeat: [{ memoryTrackerId: 42 }] },
    } as never)
    vi.mocked(MemoryTrackerController.askAQuestion).mockResolvedValue({
      data: null,
    } as never)
    vi.mocked(MemoryTrackerController.showMemoryTracker).mockResolvedValue({
      data: {
        note: { noteTopology: { title: 'My Note Title' } },
      },
    } as never)
    vi.mocked(UserController.getTokenInfo).mockResolvedValue({
      data: { id: 1, label: 'Test Token' },
    } as never)
    await addAccessToken('test-token')

    const result = await recallNext()

    expect(result).toMatchObject({
      type: 'just-review',
      memoryTrackerId: 42,
      title: 'My Note Title',
    })
    expect(MemoryTrackerController.askAQuestion).toHaveBeenCalledWith(
      expect.objectContaining({ path: { memoryTracker: 42 } })
    )
  })

  test('returns mcq when askAQuestion returns MCQ', async () => {
    vi.mocked(RecallsController.recalling).mockResolvedValue({
      data: { toRepeat: [{ memoryTrackerId: 42 }] },
    } as never)
    vi.mocked(MemoryTrackerController.askAQuestion).mockResolvedValue({
      data: {
        id: 100,
        questionType: 'MCQ',
        multipleChoicesQuestion: {
          f0__stem: 'What is 2+2?',
          f1__choices: ['4', '3', '5'],
        },
      },
    } as never)
    vi.mocked(UserController.getTokenInfo).mockResolvedValue({
      data: { id: 1, label: 'Test Token' },
    } as never)
    await addAccessToken('test-token')

    const result = await recallNext()

    expect(result).toEqual({
      type: 'mcq',
      recallPromptId: 100,
      stem: 'What is 2+2?',
      choices: ['4', '3', '5'],
    })
    expect(MemoryTrackerController.showMemoryTracker).not.toHaveBeenCalled()
  })

  test('returns spelling when askAQuestion returns SPELLING', async () => {
    vi.mocked(RecallsController.recalling).mockResolvedValue({
      data: { toRepeat: [{ memoryTrackerId: 42 }] },
    } as never)
    vi.mocked(MemoryTrackerController.askAQuestion).mockResolvedValue({
      data: {
        id: 100,
        questionType: 'SPELLING',
        spellingQuestion: { stem: 'means incite violence' },
      },
    } as never)
    vi.mocked(UserController.getTokenInfo).mockResolvedValue({
      data: { id: 1, label: 'Test Token' },
    } as never)
    await addAccessToken('test-token')

    const result = await recallNext()

    expect(result).toEqual({
      type: 'spelling',
      recallPromptId: 100,
      stem: 'means incite violence',
    })
  })

  test('returns spelling with empty stem when spellingQuestion missing', async () => {
    vi.mocked(RecallsController.recalling).mockResolvedValue({
      data: { toRepeat: [{ memoryTrackerId: 42 }] },
    } as never)
    vi.mocked(MemoryTrackerController.askAQuestion).mockResolvedValue({
      data: { id: 100, questionType: 'SPELLING' },
    } as never)
    vi.mocked(UserController.getTokenInfo).mockResolvedValue({
      data: { id: 1, label: 'Test Token' },
    } as never)
    await addAccessToken('test-token')

    const result = await recallNext()

    expect(result).toEqual({
      type: 'spelling',
      recallPromptId: 100,
      stem: '',
    })
  })

  test('throws when no default token', async () => {
    vi.mocked(RecallsController.recalling).mockResolvedValue({
      data: { toRepeat: [{ memoryTrackerId: 42 }] },
    } as never)

    await expect(recallNext()).rejects.toThrow(
      'No default access token. Add one first with /add-access-token.'
    )
  })

  test('returns details when showMemoryTracker has note details', async () => {
    vi.mocked(RecallsController.recalling).mockResolvedValue({
      data: { toRepeat: [{ memoryTrackerId: 42 }] },
    } as never)
    vi.mocked(MemoryTrackerController.askAQuestion).mockResolvedValue({
      data: null,
    } as never)
    vi.mocked(MemoryTrackerController.showMemoryTracker).mockResolvedValue({
      data: {
        note: {
          noteTopology: { title: 'Bold Word' },
          details: '**Bold** and _italic_',
        },
      },
    } as never)
    vi.mocked(UserController.getTokenInfo).mockResolvedValue({
      data: { id: 1, label: 'Test Token' },
    } as never)
    await addAccessToken('test-token')

    const result = await recallNext()

    expect(result).toMatchObject({
      type: 'just-review',
      memoryTrackerId: 42,
      title: 'Bold Word',
      details: '**Bold** and _italic_',
    })
  })

  test('uses Untitled note when showMemoryTracker has no title', async () => {
    vi.mocked(RecallsController.recalling).mockResolvedValue({
      data: { toRepeat: [{ memoryTrackerId: 42 }] },
    } as never)
    vi.mocked(MemoryTrackerController.askAQuestion).mockResolvedValue({
      data: null,
    } as never)
    vi.mocked(MemoryTrackerController.showMemoryTracker).mockResolvedValue({
      data: { note: {} },
    } as never)
    vi.mocked(UserController.getTokenInfo).mockResolvedValue({
      data: { id: 1, label: 'Test Token' },
    } as never)
    await addAccessToken('test-token')

    const result = await recallNext()

    expect(result).toMatchObject({
      type: 'just-review',
      memoryTrackerId: 42,
      title: 'Untitled note',
    })
  })
})

describe('answerQuiz', () => {
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

  test('calls answerQuiz with choiceIndex and returns correct true', async () => {
    vi.mocked(RecallPromptController.answerQuiz).mockResolvedValue({
      data: { answer: { correct: true } },
    } as never)
    vi.mocked(UserController.getTokenInfo).mockResolvedValue({
      data: { id: 1, label: 'Test Token' },
    } as never)
    await addAccessToken('test-token')

    const result = await answerQuiz(100, 0)

    expect(result).toEqual({ correct: true })
    expect(RecallPromptController.answerQuiz).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { recallPrompt: 100 },
        body: { choiceIndex: 0 },
      })
    )
  })

  test('returns correct false when answer is wrong', async () => {
    vi.mocked(RecallPromptController.answerQuiz).mockResolvedValue({
      data: { answer: { correct: false } },
    } as never)
    vi.mocked(UserController.getTokenInfo).mockResolvedValue({
      data: { id: 1, label: 'Test Token' },
    } as never)
    await addAccessToken('test-token')

    const result = await answerQuiz(100, 1)

    expect(result).toEqual({ correct: false })
    expect(RecallPromptController.answerQuiz).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { recallPrompt: 100 },
        body: { choiceIndex: 1 },
      })
    )
  })

  test('returns correct false when answer is undefined', async () => {
    vi.mocked(RecallPromptController.answerQuiz).mockResolvedValue({
      data: {},
    } as never)
    vi.mocked(UserController.getTokenInfo).mockResolvedValue({
      data: { id: 1, label: 'Test Token' },
    } as never)
    await addAccessToken('test-token')

    const result = await answerQuiz(100, 2)

    expect(result).toEqual({ correct: false })
  })

  test('passes thinkingTimeMs to API when provided', async () => {
    vi.mocked(RecallPromptController.answerQuiz).mockResolvedValue({
      data: { answer: { correct: true } },
    } as never)
    vi.mocked(UserController.getTokenInfo).mockResolvedValue({
      data: { id: 1, label: 'Test Token' },
    } as never)
    await addAccessToken('test-token')

    await answerQuiz(100, 0, 3000)

    expect(RecallPromptController.answerQuiz).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { recallPrompt: 100 },
        body: { choiceIndex: 0, thinkingTimeMs: 3000 },
      })
    )
  })
})

describe('answerSpelling', () => {
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

  test('calls answerSpelling with spellingAnswer and returns correct true', async () => {
    vi.mocked(RecallPromptController.answerSpelling).mockResolvedValue({
      data: { answer: { correct: true } },
    } as never)
    vi.mocked(UserController.getTokenInfo).mockResolvedValue({
      data: { id: 1, label: 'Test Token' },
    } as never)
    await addAccessToken('test-token')

    const result = await answerSpelling(100, 'sedition')

    expect(result).toEqual({ correct: true })
    expect(RecallPromptController.answerSpelling).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { recallPrompt: 100 },
        body: { spellingAnswer: 'sedition' },
      })
    )
  })

  test('returns correct false when spelling is wrong', async () => {
    vi.mocked(RecallPromptController.answerSpelling).mockResolvedValue({
      data: { answer: { correct: false } },
    } as never)
    vi.mocked(UserController.getTokenInfo).mockResolvedValue({
      data: { id: 1, label: 'Test Token' },
    } as never)
    await addAccessToken('test-token')

    const result = await answerSpelling(100, 'sedicion')

    expect(result).toEqual({ correct: false })
  })

  test('passes thinkingTimeMs to API when provided', async () => {
    vi.mocked(RecallPromptController.answerSpelling).mockResolvedValue({
      data: { answer: { correct: true } },
    } as never)
    vi.mocked(UserController.getTokenInfo).mockResolvedValue({
      data: { id: 1, label: 'Test Token' },
    } as never)
    await addAccessToken('test-token')

    await answerSpelling(100, 'sedition', 5000)

    expect(RecallPromptController.answerSpelling).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { recallPrompt: 100 },
        body: { spellingAnswer: 'sedition', thinkingTimeMs: 5000 },
      })
    )
  })
})

describe('contestAndRegenerate', () => {
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

  test('returns new MCQ when contest and regenerate succeed', async () => {
    vi.mocked(RecallPromptController.contest).mockResolvedValue({
      data: { advice: 'improve', rejected: false },
    } as never)
    vi.mocked(RecallPromptController.regenerate).mockResolvedValue({
      data: {
        id: 200,
        questionType: 'MCQ',
        multipleChoicesQuestion: {
          f0__stem: 'New question?',
          f1__choices: ['A', 'B', 'C'],
        },
      },
    } as never)
    vi.mocked(UserController.getTokenInfo).mockResolvedValue({
      data: { id: 1, label: 'Test Token' },
    } as never)
    await addAccessToken('test-token')

    const result = await contestAndRegenerate(100)

    expect(result).toEqual({
      ok: true,
      result: {
        type: 'mcq',
        recallPromptId: 200,
        stem: 'New question?',
        choices: ['A', 'B', 'C'],
      },
    })
  })

  test('returns error when contest is rejected', async () => {
    vi.mocked(RecallPromptController.contest).mockResolvedValue({
      data: { advice: 'Question is fine', rejected: true },
    } as never)
    vi.mocked(UserController.getTokenInfo).mockResolvedValue({
      data: { id: 1, label: 'Test Token' },
    } as never)
    await addAccessToken('test-token')

    const result = await contestAndRegenerate(100)

    expect(result).toEqual({
      ok: false,
      message: 'Question is fine',
    })
    expect(RecallPromptController.regenerate).not.toHaveBeenCalled()
  })

  test('returns error when regenerate fails', async () => {
    vi.mocked(RecallPromptController.contest).mockResolvedValue({
      data: { advice: 'try again', rejected: false },
    } as never)
    vi.mocked(RecallPromptController.regenerate).mockResolvedValue({
      error: new Error('API error'),
    } as never)
    vi.mocked(UserController.getTokenInfo).mockResolvedValue({
      data: { id: 1, label: 'Test Token' },
    } as never)
    await addAccessToken('test-token')

    const result = await contestAndRegenerate(100)

    expect(result).toEqual({ ok: false, message: 'API error' })
  })
})

describe('markAsRecalled', () => {
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

  test('calls markAsRecalled with successful true', async () => {
    vi.mocked(MemoryTrackerController.markAsRecalled).mockResolvedValue(
      {} as never
    )
    vi.mocked(UserController.getTokenInfo).mockResolvedValue({
      data: { id: 1, label: 'Test Token' },
    } as never)
    await addAccessToken('test-token')

    await markAsRecalled(42, true)

    expect(MemoryTrackerController.markAsRecalled).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { memoryTracker: 42 },
        query: { successful: true },
      })
    )
  })

  test('calls markAsRecalled with successful false', async () => {
    vi.mocked(MemoryTrackerController.markAsRecalled).mockResolvedValue(
      {} as never
    )
    vi.mocked(UserController.getTokenInfo).mockResolvedValue({
      data: { id: 1, label: 'Test Token' },
    } as never)
    await addAccessToken('test-token')

    await markAsRecalled(99, false)

    expect(MemoryTrackerController.markAsRecalled).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { memoryTracker: 99 },
        query: { successful: false },
      })
    )
  })
})
