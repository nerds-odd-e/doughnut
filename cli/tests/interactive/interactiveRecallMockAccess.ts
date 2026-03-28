import './interactiveTestMocks.js'
import * as recall from '../../src/commands/recall.js'
import { vi } from 'vitest'

export const mockRecallNext = vi.mocked(recall.recallNext)
export const mockAnswerQuiz = vi.mocked(recall.answerQuiz)
export const mockMarkAsRecalled = vi.mocked(recall.markAsRecalled)
export const mockContestAndRegenerate = vi.mocked(recall.contestAndRegenerate)
