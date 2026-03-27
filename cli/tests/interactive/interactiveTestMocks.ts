import { vi } from 'vitest'

/** When true, help mock returns 12 fake `/cmdN` entries for suggestion scroll tests. */
export const interactiveHelpMockState = {
  useManyCommandsForScrollTests: false,
}

const {
  mockRecallNext,
  mockRecallStatus,
  mockAnswerQuiz,
  mockAnswerSpelling,
  mockMarkAsRecalled,
  mockContestAndRegenerate,
} = vi.hoisted(() => ({
  mockRecallNext: vi.fn(),
  mockRecallStatus: vi.fn(),
  mockAnswerQuiz: vi.fn(),
  mockAnswerSpelling: vi.fn(),
  mockMarkAsRecalled: vi.fn(),
  mockContestAndRegenerate: vi.fn(),
}))

vi.mock('../../src/commands/recall.js', async (importOriginal) => {
  const actual =
    await importOriginal<typeof import('../../src/commands/recall.js')>()
  mockRecallStatus.mockImplementation((signal?: AbortSignal) =>
    actual.recallStatus(signal)
  )
  return {
    ...actual,
    recallNext: mockRecallNext,
    recallStatus: mockRecallStatus,
    answerQuiz: mockAnswerQuiz,
    answerSpelling: mockAnswerSpelling,
    markAsRecalled: mockMarkAsRecalled,
    contestAndRegenerate: mockContestAndRegenerate,
  }
})

vi.mock('../../src/commands/help.js', async (importOriginal) => {
  const actual =
    await importOriginal<typeof import('../../src/commands/help.js')>()
  const manyCommands = Array.from({ length: 12 }, (_, i) => ({
    name: `cmd${i}`,
    usage: `/cmd${i}`,
    description: `Command ${i}`,
    category: 'interactive' as const,
  }))
  return {
    ...actual,
    filterCommandsByPrefix: (_: unknown, prefix: string) => {
      if (
        interactiveHelpMockState.useManyCommandsForScrollTests &&
        prefix.startsWith('/')
      )
        return manyCommands
      return actual.filterCommandsByPrefix(actual.interactiveDocs, prefix)
    },
  }
})
