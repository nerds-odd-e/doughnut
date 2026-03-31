/** One scrollback row after a recall answer; Phases 2+ add richer `kind` variants. */
export type RecallAnsweredRowPayload = PlainRecallAnsweredRowPayload

type PlainRecallAnsweredRowPayload = {
  readonly kind: 'plain'
  readonly text: string
}

export function plainRecallAnsweredRow(text: string): RecallAnsweredRowPayload {
  return { kind: 'plain', text }
}
