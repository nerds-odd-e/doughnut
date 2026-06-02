import { LEAVE_RECALL_PROMPT } from '../src/commands/recall/leaveRecallSessionCopy.js'

export type McqInkWaitHelpers = {
  waitForLastFrameToInclude: (
    pattern: string | RegExp,
    maxTicks?: number
  ) => Promise<void>
  waitUntilLastFrame: (
    predicate: (stripped: string) => boolean,
    maxTicks?: number
  ) => Promise<void>
  lastStrippedFrame: () => string
}

export const MCQ_HINT_SUBSTR = '↑↓ Enter or number to select'

export const leaveRecallWithYnRe = /(?=.*Leave recall\?)(?=.*\(y\/n\))/s

export function startRecall(stdin: { write(data: string): void }) {
  stdin.write('/recall\r')
}

export async function waitMcqVisible(ink: McqInkWaitHelpers): Promise<void> {
  await ink.waitUntilLastFrame(
    (p) =>
      p.includes('Choose') &&
      p.includes('Alpha') &&
      !p.includes('**') &&
      p.includes(MCQ_HINT_SUBSTR)
  )
}

export async function waitMcqLoadMore(ink: McqInkWaitHelpers): Promise<void> {
  await ink.waitForLastFrameToInclude(/Load more from next 3 days\?/)
}

export async function waitMcqIncorrectOnLastFrame(
  ink: McqInkWaitHelpers
): Promise<void> {
  await ink.waitForLastFrameToInclude('Incorrect.')
}

export async function waitReturnsToMcq(ink: McqInkWaitHelpers): Promise<void> {
  await ink.waitUntilLastFrame(
    (p) =>
      p.includes('Choose') &&
      p.includes('Alpha') &&
      !p.includes(LEAVE_RECALL_PROMPT)
  )
}
