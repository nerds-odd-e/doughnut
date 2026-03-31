import type { RecallAnsweredRowPayload } from './recallAnsweredRowPayload.js'

/** Passed from recall card stages to the recall session after each submitted answer. */
export type RecallQuestionAnswerOutcome = {
  readonly successful: boolean
  /** Appended before loading the next card (e.g. Correct!, Incorrect., spell-correct). */
  readonly answeredRows: readonly RecallAnsweredRowPayload[]
}
