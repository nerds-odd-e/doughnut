import type {
  Message,
  MessageDelta,
  Run,
  NoteDetailsCompletion,
} from "@/generated/backend"

export type AiReplyState = {
  handleEvent: (data: string) => Promise<void>
  status: string | undefined
}

export interface AiActionContext {
  append: (text: string) => void
  reset: () => Promise<void>
  appendNoteDetails: (
    completion: string,
    threadId: string,
    runId: string,
    toolCallId: string
  ) => Promise<void>
}

export const createAiReplyStates = (
  context: AiActionContext
): Record<string, AiReplyState> => {
  const states: Record<string, AiReplyState> = {
    "thread.message.created": {
      status: "Generating response...",
      handleEvent: async (data) => {
        const response = JSON.parse(data) as Message
        context.append(response.content?.[0]?.text?.value || "")
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
        const contentToAppend = JSON.parse(
          response.required_action!.submit_tool_outputs!.tool_calls![0]!
            .function!.arguments as unknown as string
        ) as NoteDetailsCompletion

        await context.appendNoteDetails(
          contentToAppend!.completion,
          response.thread_id!,
          response.id!,
          response.required_action!.submit_tool_outputs!.tool_calls![0]!.id!
        )
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
