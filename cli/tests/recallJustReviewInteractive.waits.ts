import { LEAVE_RECALL_PROMPT } from '../src/commands/recall/leaveRecallSessionCopy.js'
import { pressEscape } from './inkTestHelpers.js'
import {
  type RecallInkWaitHelpers,
  startRecall,
} from './recallInteractiveShared.js'

export async function waitRememberCard(
  ink: RecallInkWaitHelpers,
  title: string,
  opts?: { ynHint: boolean }
) {
  const re = opts?.ynHint
    ? new RegExp(`(?=.*Yes, I remember\\?)(?=.*${title})(?=.*\\(y/n\\))`, 's')
    : new RegExp(`(?=.*Yes, I remember\\?)(?=.*${title})`, 's')
  await ink.waitForLastFrameToInclude(re)
}

export async function waitLoadMore(ink: RecallInkWaitHelpers) {
  await ink.waitForLastFrameToInclude(
    /(?=.*Load more from next 3 days\?)(?=.*\(Y\/n\))/s
  )
}

export async function waitRecalledSummary(
  ink: RecallInkWaitHelpers,
  summary: 'Recalled 1 note' | 'Recalled 2 notes'
) {
  await ink.waitForLastFrameToInclude(summary)
}

export async function waitReturnsToSingleRememberCard(
  ink: RecallInkWaitHelpers,
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
  ink: RecallInkWaitHelpers,
  rejectedInBuffer: string
) {
  stdin.write('\x7f')
  await ink.waitUntilLastFrame(
    (f) => f.includes('→') && !f.includes(rejectedInBuffer)
  )
}

export async function emptyEnterAndInvalidLineStayOnRemember(
  stdin: { write(data: string): void },
  ink: RecallInkWaitHelpers,
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
  ink: RecallInkWaitHelpers
) {
  startRecall(stdin)
  await waitRememberCard(ink, 'Alpha')
  stdin.write('y\r')
  await waitLoadMore(ink)
}

export async function reachLeaveRecallOnRemember(
  stdin: { write(data: string): void },
  ink: RecallInkWaitHelpers,
  noteTitle: string
) {
  await waitRememberCard(ink, noteTitle)
  await pressEscape(stdin)
  await ink.waitForLastFrameToInclude(/Leave recall\?/)
}
