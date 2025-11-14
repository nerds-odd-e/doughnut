import { DummyForGeneratingTypes } from "@generated/backend"
import { type Suggestion } from "./suggestions"

export type ToolCallResult = {
  status: string
}

export type AiReplyState = {
  handleEvent: (data: string) => Promise<void>
  status: string | undefined
}

export interface AiActionContext {
  set: (text: string) => void
  append: (text: string) => void
  reset: () => Promise<void>
  handleSuggestion: (suggestion: Suggestion) => Promise<ToolCallResult>
}

// Native ChatCompletionChunk types (simplified from OpenAI SDK)
interface ChatCompletionChunk {
  choices?: Array<{
    index: number
    delta?: {
      role?: string
      content?: string | null
      tool_calls?: Array<{
        index?: number
        id?: string
        type?: string
        function?: {
          name?: string
          arguments?: string
        }
      }>
    }
    message?: {
      role?: string
      content?: string | null
      tool_calls?: Array<{
        index?: number
        id?: string
        type?: string
        function?: {
          name?: string
          arguments?: string
        }
      }>
    }
    finish_reason?: string | null
  }>
}

export const createAiReplyStates = (
  context: AiActionContext
): Record<string, AiReplyState> => {
  // Accumulate tool calls across streaming chunks
  // Tool calls come incrementally in delta.tool_calls with fragmented arguments
  const accumulatedToolCalls: Record<
    number,
    {
      id?: string
      type?: string
      index: number
      function: {
        name?: string
        arguments: string
      }
    }
  > = {}

  const states: Record<string, AiReplyState> = {
    "chat.completion.chunk": {
      status: "Streaming response...",
      handleEvent: async (data) => {
        const chunk = JSON.parse(data) as ChatCompletionChunk
        const choice = chunk.choices?.[0]
        if (!choice) return

        // Handle content delta (streaming chunks use delta, final chunks use message)
        const content = choice.delta?.content || choice.message?.content
        if (content) {
          context.append(content)
        }

        // Accumulate tool calls from delta.tool_calls (streaming mode)
        // Tool calls come incrementally with fragmented arguments that need to be concatenated
        const deltaToolCalls = choice.delta?.tool_calls
        if (deltaToolCalls) {
          for (const toolCall of deltaToolCalls) {
            const index = toolCall.index ?? 0

            // Initialize tool call if this is the first chunk for this index
            if (!accumulatedToolCalls[index]) {
              accumulatedToolCalls[index] = {
                id: toolCall.id,
                type: toolCall.type,
                index,
                function: {
                  name: toolCall.function?.name,
                  arguments: toolCall.function?.arguments || "",
                },
              }
            } else {
              // Accumulate: concatenate arguments (they come as fragmented JSON strings)
              if (toolCall.function?.arguments) {
                accumulatedToolCalls[index].function.arguments +=
                  toolCall.function.arguments
              }
              // Update id/type/name if provided in this chunk
              if (toolCall.id) {
                accumulatedToolCalls[index].id = toolCall.id
              }
              if (toolCall.type) {
                accumulatedToolCalls[index].type = toolCall.type
              }
              if (toolCall.function?.name) {
                accumulatedToolCalls[index].function.name =
                  toolCall.function.name
              }
            }
          }
        }

        // Process accumulated tool calls when finish_reason is "tool_calls"
        if (choice.finish_reason === "tool_calls") {
          try {
            const toolCallsArray = Object.values(accumulatedToolCalls)

            for (const toolCall of toolCallsArray) {
              const functionArgs = toolCall.function.arguments || "{}"
              const functionName = toolCall.function.name

              if (
                functionName ===
                DummyForGeneratingTypes.aiToolName.COMPLETE_NOTE_DETAILS
              ) {
                await context.handleSuggestion({
                  suggestionType: "completion",
                  content: JSON.parse(functionArgs),
                  toolCallId: toolCall.id || "synthetic",
                })
              } else if (
                functionName ===
                DummyForGeneratingTypes.aiToolName.SUGGEST_NOTE_TITLE
              ) {
                const { newTitle } = JSON.parse(functionArgs)
                await context.handleSuggestion({
                  suggestionType: "title",
                  content: newTitle,
                  toolCallId: toolCall.id || "synthetic",
                })
              } else {
                await context.handleSuggestion({
                  suggestionType: "unknown",
                  content: {
                    rawJson: functionArgs,
                    functionName: functionName!,
                  },
                  toolCallId: toolCall.id || "synthetic",
                })
              }
            }

            // Clear accumulated tool calls after processing
            Object.keys(accumulatedToolCalls).forEach(
              (key) => delete accumulatedToolCalls[Number(key)]
            )

            // Tool calls are executed inline with Chat Completion API
            // No need to submit results or cancel runs
          } catch (_e) {
            // When user rejects a tool call, do nothing
            // Tool execution is already handled inline
            // Clear accumulated tool calls on error
            Object.keys(accumulatedToolCalls).forEach(
              (key) => delete accumulatedToolCalls[Number(key)]
            )
          }
        }
      },
    },
    done: {
      status: undefined,
      handleEvent: async () => {
        await context.reset()
      },
    },
  }

  return states
}
