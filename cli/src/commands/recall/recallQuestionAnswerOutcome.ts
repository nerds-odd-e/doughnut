import type { ReactNode } from 'react'

/** Passed from recall card stages to the recall session after each submitted answer. */
export type RecallQuestionAnswerOutcome = {
  readonly successful: boolean
  /** Appended before loading the next card; each node is an Ink subtree for session scrollback. */
  readonly answeredRows: readonly ReactNode[]
}
