import { LEAVE_RECALL_PROMPT } from '../src/commands/recall/leaveRecallSessionCopy.js'

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
}

function reLiteral(s: string): string {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

export const leaveRecallWithYnRe = /(?=.*Leave recall\?)(?=.*\(y\/n\))/s

export function startRecall(stdin: { write(data: string): void }) {
  stdin.write('/recall\r')
}

export async function waitSpellingPromptVisible(ink: InkWaitHelpers) {
  await ink.waitUntilLastFrame(
    (p) =>
      p.includes('Spell the title') &&
      p.includes('Recalling') &&
      !p.includes('Loading spelling question')
  )
}

export async function waitSpellingIncorrect(
  ink: InkWaitHelpers,
  answer: string
) {
  await ink.waitForFramesToInclude(
    new RegExp(`(?=.*Incorrect\\.)(?=.*Your answer: ${reLiteral(answer)})`, 's')
  )
}

export async function waitSpellingCorrect(ink: InkWaitHelpers, answer: string) {
  await ink.waitForFramesToInclude(
    new RegExp(`(?=.*Correct!)(?=.*Your answer: ${reLiteral(answer)})`, 's')
  )
}

export async function waitReturnsToSpellingWithBuffer(
  ink: InkWaitHelpers,
  bufferSuffix: string
) {
  await ink.waitUntilLastFrame(
    (p) =>
      p.includes('Spell the title') &&
      p.includes(bufferSuffix) &&
      !p.includes(LEAVE_RECALL_PROMPT)
  )
}
