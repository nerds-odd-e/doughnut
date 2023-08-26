export type MessageToMatch = {
  role?: "user" | "assistant" | "system"
  content: string | RegExp
}
