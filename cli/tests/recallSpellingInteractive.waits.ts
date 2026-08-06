import { LEAVE_RECALL_PROMPT } from '../src/commands/recall/leaveRecallSessionCopy.js'
import {
  type RecallInkWaitHelpers,
  reLiteral,
} from './recallInteractiveShared.js'

export async function waitSpellingPromptVisible(ink: RecallInkWaitHelpers) {
  await ink.waitUntilLastFrame(
    (p) =>
      p.includes('Spell the title') &&
      p.includes('Recalling') &&
      !p.includes('Loading spelling question')
  )
}

export async function waitSpellingIncorrect(
  ink: RecallInkWaitHelpers,
  answer: string
) {
  await ink.waitForFramesToInclude(
    new RegExp(`(?=.*Incorrect\\.)(?=.*Your answer: ${reLiteral(answer)})`, 's')
  )
}

export async function waitSpellingCorrect(
  ink: RecallInkWaitHelpers,
  answer: string
) {
  await ink.waitForFramesToInclude(
    new RegExp(`(?=.*Correct!)(?=.*Your answer: ${reLiteral(answer)})`, 's')
  )
}

export async function waitReturnsToSpellingWithBuffer(
  ink: RecallInkWaitHelpers,
  bufferSuffix: string
) {
  await ink.waitUntilLastFrame(
    (p) =>
      p.includes('Spell the title') &&
      p.includes(bufferSuffix) &&
      !p.includes(LEAVE_RECALL_PROMPT)
  )
}
