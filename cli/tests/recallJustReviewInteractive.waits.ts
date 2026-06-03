import { LEAVE_RECALL_PROMPT } from '../src/commands/recall/leaveRecallSessionCopy.js'
import { pressEscape } from './inkTestHelpers.js'

export type InkWaitHelpers = {
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

export async function waitRememberCard(
  ink: InkWaitHelpers,
  title: string,
  opts?: { ynHint: boolean }
) {
  const re = opts?.ynHint
    ? new RegExp(`(?=.*Yes, I remember\\?)(?=.*${title})(?=.*\\(y/n\\))`, 's')
    : new RegExp(`(?=.*Yes, I remember\\?)(?=.*${title})`, 's')
  await ink.waitForLastFrameToInclude(re)
}

export async function waitLoadMore(ink: InkWaitHelpers) {
  await ink.waitForLastFrameToInclude(
    /(?=.*Load more from next 3 days\?)(?=.*\(Y\/n\))/s
  )
}

export async function waitRecalledSummary(
  ink: InkWaitHelpers,
  summary: 'Recalled 1 note' | 'Recalled 2 notes'
) {
  await ink.waitForLastFrameToInclude(summary)
}

export async function waitReturnsToSingleRememberCard(
  ink: InkWaitHelpers,
  noteTitle: string
) {
  await ink.waitUntilLastFrame((plain) => {
    return (
      plain.includes('Yes, I remember?') &&
      plain.includes(noteTitle) &&
      !plain.includes(LEAVE_RECALL_PROMPT) &&
      (plain.match(/Yes, I remember\?/g) ?? []).length === 1
    )
  })
}

async function backspaceClearsTyped(
  stdin: { write(data: string): void },
  ink: InkWaitHelpers,
  rejectedInBuffer: string
) {
  stdin.write('\x7f')
  await ink.waitUntilLastFrame(
    (f) => f.includes('→') && !f.includes(rejectedInBuffer)
  )
}

export async function emptyEnterAndInvalidLineStayOnRemember(
  stdin: { write(data: string): void },
  ink: InkWaitHelpers,
  noteTitle: string,
  summaryNotYet: string,
  opts?: { readonly skipInitialWait?: boolean }
) {
  const onRemember = (f: string) =>
    f.includes('Yes, I remember?') &&
    f.includes(noteTitle) &&
    !f.includes(summaryNotYet)

  if (!opts?.skipInitialWait) {
    await ink.waitUntilLastFrame(onRemember)
  }
  stdin.write('\r')
  await ink.waitUntilLastFrame(onRemember)
  stdin.write('q\r')
  await ink.waitUntilLastFrame(onRemember)
  await backspaceClearsTyped(stdin, ink, '→ q')
}

export async function recallSingleAlphaToLoadMore(
  stdin: { write(data: string): void },
  ink: InkWaitHelpers
) {
  startRecall(stdin)
  await waitRememberCard(ink, 'Alpha')
  stdin.write('y\r')
  await waitLoadMore(ink)
}

export function startRecall(stdin: { write(data: string): void }) {
  stdin.write('/recall\r')
}

export async function reachLeaveRecallOnRemember(
  stdin: { write(data: string): void },
  ink: InkWaitHelpers,
  noteTitle: string
) {
  await waitRememberCard(ink, noteTitle)
  await pressEscape(stdin)
  await ink.waitForLastFrameToInclude(/Leave recall\?/)
}
