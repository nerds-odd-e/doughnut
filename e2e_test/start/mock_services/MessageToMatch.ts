export type TextMessageToMatch = {
  role?: 'user' | 'assistant' | 'system' | 'developer'
  content: string
}

export type ToolCallToMatch = {
  role: 'tool' | 'function'
  content?: string
  name: string
}

export type MessageToMatch = TextMessageToMatch | ToolCallToMatch
