import type { ToolCallResult } from "@generated/backend"
import { DummyForGeneratingTypes } from "@generated/backend"
import type { RestAiControllerService } from "@generated/backend/services/RestAiControllerService"
import { type Suggestion } from "./suggestions"

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
    message?: {
      role?: string
      content?: string | null
      tool_calls?: Array<{
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
  context: AiActionContext,
  aiController: RestAiControllerService
): Record<string, AiReplyState> => {
  const states: Record<string, AiReplyState> = {
    "chat.completion.chunk": {
      status: "Streaming response...",
      handleEvent: async (data) => {
        const chunk = JSON.parse(data) as ChatCompletionChunk
        const choice = chunk.choices?.[0]
        if (!choice) return

        // Handle content delta
        if (choice.message?.content) {
          context.append(choice.message.content)
        }

        // Handle tool calls
        if (
          choice.finish_reason === "tool_calls" &&
          choice.message?.tool_calls
        ) {
          try {
            const results: Record<string, ToolCallResult> = {}

            for (const toolCall of choice.message.tool_calls) {
              const functionArgs = toolCall.function?.arguments || "{}"
              const functionName = toolCall.function?.name

              let result: ToolCallResult
              if (
                functionName ===
                DummyForGeneratingTypes.aiToolName.COMPLETE_NOTE_DETAILS
              ) {
                result = await context.handleSuggestion({
                  suggestionType: "completion",
                  content: JSON.parse(functionArgs),
                  threadId: "synthetic",
                  runId: "synthetic",
                  toolCallId: toolCall.id || "synthetic",
                })
              } else if (
                functionName ===
                DummyForGeneratingTypes.aiToolName.SUGGEST_NOTE_TITLE
              ) {
                const { newTitle } = JSON.parse(functionArgs)
                result = await context.handleSuggestion({
                  suggestionType: "title",
                  content: newTitle,
                  threadId: "synthetic",
                  runId: "synthetic",
                  toolCallId: toolCall.id || "synthetic",
                })
              } else {
                result = await context.handleSuggestion({
                  suggestionType: "unknown",
                  content: {
                    rawJson: functionArgs,
                    functionName: functionName!,
                  },
                  threadId: "synthetic",
                  runId: "synthetic",
                  toolCallId: toolCall.id || "synthetic",
                })
              }

              results[toolCall.id || "synthetic"] = result
            }

            await aiController.submitToolCallsResult(
              "synthetic",
              "synthetic",
              results
            )
          } catch (e) {
            if (e instanceof Error && e.message === "Tool call was rejected") {
              await aiController.cancelRun("synthetic", "synthetic")
            } else {
              throw e
            }
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
