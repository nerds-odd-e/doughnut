import {
  type Message,
  type MessageDelta,
  type Run,
  type RunStep,
  type DeltaOfRunStep,
  type NoteDetailsCompletion,
  type TopicTitleReplacement,
  type ToolCallResult,
  DummyForGeneratingTypes,
} from "@/generated/backend"
import type { RestAiControllerService } from "@/generated/backend/services/RestAiControllerService"

export type AiReplyState = {
  handleEvent: (data: string) => Promise<void>
  status: string | undefined
}

export interface AiActionContext {
  set: (text: string) => void
  append: (text: string) => void
  reset: () => Promise<void>
  appendNoteDetails: (
    completion: string,
    threadId: string,
    runId: string,
    toolCallId: string
  ) => Promise<ToolCallResult>
  setTopicTitle: (
    title: string,
    threadId: string,
    runId: string,
    toolCallId: string
  ) => Promise<ToolCallResult>
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
            const functionArgs = JSON.parse(
              toolCall.function!.arguments as unknown as string
            )

            let result: ToolCallResult
            if (
              toolCall.function!.name ===
              DummyForGeneratingTypes.aiToolName.COMPLETE_NOTE_DETAILS
            ) {
              const contentToAppend = functionArgs as NoteDetailsCompletion
              result = await context.appendNoteDetails(
                contentToAppend!.completion,
                response.thread_id!,
                response.id!,
                toolCall.id!
              )
            } else if (
              toolCall.function!.name ===
              DummyForGeneratingTypes.aiToolName.SUGGEST_NOTE_TOPIC_TITLE
            ) {
              const titleGeneration = functionArgs as TopicTitleReplacement
              result = await context.setTopicTitle(
                titleGeneration.newTopic,
                response.thread_id!,
                response.id!,
                toolCall.id!
              )
            } else {
              throw new Error("Unknown tool call")
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
