export type TranscriptItem =
  | { readonly kind: 'user_line'; readonly id: string; readonly text: string }
  | {
      readonly kind: 'assistant_text'
      readonly id: string
      readonly text: string
    }

export function transcriptUserLine(text: string): TranscriptItem {
  return { kind: 'user_line', id: crypto.randomUUID(), text }
}

export function transcriptAssistantText(text: string): TranscriptItem {
  return { kind: 'assistant_text', id: crypto.randomUUID(), text }
}
