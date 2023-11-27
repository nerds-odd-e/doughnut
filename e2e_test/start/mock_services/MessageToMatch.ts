export type TextMessageToMatch = {
  role?: "user" | "assistant" | "system"
  content: string | RegExp
}

export type ToolCallToMatch = {
  role: "tool" | "function"
  content?: string | RegExp
  name: string
}

export type MessageToMatch = TextMessageToMatch | ToolCallToMatch
