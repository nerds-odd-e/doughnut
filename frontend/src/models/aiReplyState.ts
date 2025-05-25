import type {
  Message,
  MessageDelta,
  Run,
  RunStep,
  DeltaOfRunStep,
  ToolCallResult,
} from "generated/backend"
import { DummyForGeneratingTypes } from "generated/backend"
import type { RestAiControllerService } from "generated/backend/services/RestAiControllerService"
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

export const createAiReplyStates = (
  context: AiActionContext,
  aiController: RestAiControllerService
): Record<string, AiReplyState> => {
  const states: Record<string, AiReplyState> = {
    "thread.message.created": {
      status: "Generating response...",
      handleEvent: async (data) => {
        const response = JSON.parse(data) as Message
        context.set(response.content?.[0]?.text?.value || "")
      },
    },
    "thread.message.delta": {
      status: "Writing response...",
      handleEvent: async (data) => {
        const response = JSON.parse(data) as MessageDelta
        const delta = response.delta?.content?.[0]?.text?.value || ""
        context.append(delta)
      },
    },
    "thread.run.requires_action": {
      status: "Processing actions...",
      handleEvent: async (data) => {
        const response = JSON.parse(data) as Run
        try {
          const toolCalls =
            response.required_action!.submit_tool_outputs!.tool_calls!
          const results: Record<string, ToolCallResult> = {}

          for (const toolCall of toolCalls) {
            const functionArgs = toolCall.function!
              .arguments as unknown as string
            const functionName = toolCall.function!.name

            let result: ToolCallResult
            if (
              functionName ===
              DummyForGeneratingTypes.aiToolName.COMPLETE_NOTE_DETAILS
            ) {
              result = await context.handleSuggestion({
                suggestionType: "completion",
                content: JSON.parse(functionArgs),
                threadId: response.thread_id!,
                runId: response.id!,
                toolCallId: toolCall.id!,
              })
            } else if (
              functionName ===
              DummyForGeneratingTypes.aiToolName.SUGGEST_NOTE_TITLE
            ) {
              const { newTitle } = JSON.parse(functionArgs)
              result = await context.handleSuggestion({
                suggestionType: "title",
                content: newTitle,
                threadId: response.thread_id!,
                runId: response.id!,
                toolCallId: toolCall.id!,
              })
            } else {
              result = await context.handleSuggestion({
                suggestionType: "unknown",
                content: { rawJson: functionArgs, functionName: functionName! },
                threadId: response.thread_id!,
                runId: response.id!,
                toolCallId: toolCall.id!,
              })
            }

            results[toolCall.id!] = result
          }

          await aiController.submitToolCallsResult(
            response.thread_id!,
            response.id!,
            results
          )
        } catch (e) {
          if (e instanceof Error && e.message === "Tool call was rejected") {
            await aiController.cancelRun(response.thread_id!, response.id!)
          } else {
            throw e
          }
        }
      },
    },
    "thread.run.step.created": {
      status: "Starting tool execution...",
      handleEvent: async (data) => {
        const response = JSON.parse(data) as RunStep
        if (response.type === "tool_calls") {
          context.set("")
        }
      },
    },
    "thread.run.step.delta": {
      status: "Processing tool call...",
      handleEvent: async (data) => {
        const response = JSON.parse(data) as DeltaOfRunStep
        const delta = response.delta?.step_details?.tool_calls?.[0]?.function
          ?.arguments as unknown as string
        context.append(delta)
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
