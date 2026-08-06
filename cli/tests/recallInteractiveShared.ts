import {
  RECALL_BUSY_RECORD_REVIEW_LABEL,
  RECALL_BUSY_SUBMIT_ANSWER_LABEL,
  RECALL_LOADING_NEXT_QUESTION_LABEL,
} from '../src/commands/recall/recallBusyInputCopy.js'

export type RecallInkWaitHelpers = {
  waitForLastFrameToInclude: (
    pattern: string | RegExp,
    maxTicks?: number
  ) => Promise<void>
  waitForFramesToInclude: (
    pattern: string | RegExp,
    maxTicks?: number
  ) => Promise<void>
  waitUntilLastFrame: (
    predicate: (stripped: string) => boolean,
    maxTicks?: number
  ) => Promise<void>
  lastStrippedFrame: () => string
}

export function reLiteral(s: string): string {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

export const leaveRecallWithYnRe = /(?=.*Leave recall\?)(?=.*\(y\/n\))/s

export function startRecall(stdin: { write(data: string): void }) {
  stdin.write('/recall\r')
}

export function deferred<T>(): {
  promise: Promise<T>
  resolve: (value: T) => void
} {
  let resolve!: (value: T) => void
  const promise = new Promise<T>((r) => {
    resolve = r
  })
  return { promise, resolve }
}

export async function waitBusyRecordReview(
  ink: Pick<RecallInkWaitHelpers, 'waitForLastFrameToInclude'>
): Promise<void> {
  await ink.waitForLastFrameToInclude(RECALL_BUSY_RECORD_REVIEW_LABEL)
}

export async function waitBusySubmitAnswer(
  ink: Pick<RecallInkWaitHelpers, 'waitForLastFrameToInclude'>
): Promise<void> {
  await ink.waitForLastFrameToInclude(RECALL_BUSY_SUBMIT_ANSWER_LABEL)
}

export async function waitLoadingNextQuestion(
  ink: Pick<RecallInkWaitHelpers, 'waitUntilLastFrame'>
): Promise<void> {
  await ink.waitUntilLastFrame(
    (p) =>
      p.includes(RECALL_LOADING_NEXT_QUESTION_LABEL) && !p.includes('(y/n)')
  )
}

export async function waitLoadingSpellingNext(
  ink: Pick<RecallInkWaitHelpers, 'waitUntilLastFrame'>,
  hidePlaceholder: string
): Promise<void> {
  await ink.waitUntilLastFrame(
    (p) =>
      (p.includes('Loading spelling question') ||
        p.includes(RECALL_LOADING_NEXT_QUESTION_LABEL)) &&
      !p.includes(hidePlaceholder)
  )
}
