export type ScrollbackEntry =
  | { readonly kind: 'user_line'; readonly id: string; readonly text: string }
  | {
      readonly kind: 'assistant_text'
      readonly id: string
      readonly text: string
    }

export function scrollbackUserLine(text: string): ScrollbackEntry {
  return { kind: 'user_line', id: crypto.randomUUID(), text }
}

export function scrollbackAssistantText(text: string): ScrollbackEntry {
  return { kind: 'assistant_text', id: crypto.randomUUID(), text }
}
