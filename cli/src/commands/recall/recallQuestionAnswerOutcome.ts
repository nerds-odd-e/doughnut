/** Passed from recall card stages to the recall session after each submitted answer. */
export type RecallQuestionAnswerOutcome = {
  readonly successful: boolean
  /** Appended before loading the next card (e.g. Correct!, Incorrect., spell-correct). */
  readonly scrollbackLines: readonly string[]
  /**
   * Just-review “I remember”: load next first, then append “Reviewed: …” only when moving
   * to another card or finishing with first success on an empty-today start.
   */
  readonly justReviewSuccessfulRecall?: {
    readonly noteTitle: string
  }
}
