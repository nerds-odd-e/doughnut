import { vi } from 'vitest'

/** When true, help mock returns 12 fake `/cmdN` entries for suggestion scroll tests. */
export const interactiveHelpMockState = {
  useManyCommandsForScrollTests: false,
}

const {
  mockRecallNext,
  mockAnswerQuiz,
  mockMarkAsRecalled,
  mockContestAndRegenerate,
} = vi.hoisted(() => ({
  mockRecallNext: vi.fn(),
  mockAnswerQuiz: vi.fn(),
  mockMarkAsRecalled: vi.fn(),
  mockContestAndRegenerate: vi.fn(),
}))

vi.mock('../../src/commands/recall.js', async (importOriginal) => {
  const actual =
    await importOriginal<typeof import('../../src/commands/recall.js')>()
  return {
    ...actual,
    recallNext: mockRecallNext,
    answerQuiz: mockAnswerQuiz,
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
