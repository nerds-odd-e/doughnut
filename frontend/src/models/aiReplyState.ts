import type {
  Message,
  MessageDelta,
  Run,
  RunStep,
  DeltaOfRunStep,
  NoteDetailsCompletion,
  TopicTitleReplacement,
} from "@/generated/backend"

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
  ) => Promise<void>
  setTopicTitle: (
    title: string,
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
        const toolCall =
          response.required_action!.submit_tool_outputs!.tool_calls![0]!
        const functionArgs = JSON.parse(
          toolCall.function!.arguments as unknown as string
        )

        if (toolCall.function!.name === "complete_note_details") {
          const contentToAppend = functionArgs as NoteDetailsCompletion
          await context.appendNoteDetails(
            contentToAppend!.completion,
            response.thread_id!,
            response.id!,
            toolCall.id!
          )
        } else if (toolCall.function!.name === "suggest_note_topic_title") {
          const titleGeneration = functionArgs as TopicTitleReplacement
          await context.setTopicTitle(
            titleGeneration.newTopic,
            response.thread_id!,
            response.id!,
            toolCall.id!
          )
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
