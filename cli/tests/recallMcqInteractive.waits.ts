import { LEAVE_RECALL_PROMPT } from '../src/commands/recall/leaveRecallSessionCopy.js'
import type { RecallInkWaitHelpers } from './recallInteractiveShared.js'

export const MCQ_HINT_SUBSTR = '↑↓ Enter or number to select'

export async function waitMcqVisible(ink: RecallInkWaitHelpers): Promise<void> {
  await ink.waitUntilLastFrame(
    (p) =>
      p.includes('Choose') &&
      p.includes('Alpha') &&
      !p.includes('**') &&
      p.includes(MCQ_HINT_SUBSTR)
  )
}

export async function waitMcqLoadMore(
  ink: RecallInkWaitHelpers
): Promise<void> {
  await ink.waitForLastFrameToInclude(/Load more from next 3 days\?/)
}

export async function waitMcqIncorrectOnLastFrame(
  ink: RecallInkWaitHelpers
): Promise<void> {
  await ink.waitForLastFrameToInclude('Incorrect.')
}

export async function waitReturnsToMcq(
  ink: RecallInkWaitHelpers
): Promise<void> {
  await ink.waitUntilLastFrame(
    (p) =>
      p.includes('Choose') &&
      p.includes('Alpha') &&
      !p.includes(LEAVE_RECALL_PROMPT)
  )
}
