/** Passed from MCQ / spelling stages to the recall session after each submitted answer. */
export type RecallQuestionAnswerOutcome = {
  readonly successful: boolean
  /** Appended before loading the next card (e.g. incorrect, spell-correct when advancing). */
  readonly scrollbackLines: readonly string[]
  /** Appended only when no more cards (e.g. MCQ terminal “Correct!”). */
  readonly scrollbackLinesWhenQueueEmpty?: readonly string[]
}
