import './interactiveTestMocks.js'
import * as recall from '../../src/recall.js'
import { vi } from 'vitest'

export const mockRecallNext = vi.mocked(recall.recallNext)
export const mockRecallStatus = vi.mocked(recall.recallStatus)
export const mockAnswerQuiz = vi.mocked(recall.answerQuiz)
export const mockAnswerSpelling = vi.mocked(recall.answerSpelling)
export const mockMarkAsRecalled = vi.mocked(recall.markAsRecalled)
export const mockContestAndRegenerate = vi.mocked(recall.contestAndRegenerate)
