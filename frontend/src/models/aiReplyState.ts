import type {
  Message,
  MessageDelta,
  Run,
  NoteDetailsCompletion,
  Note,
} from "@/generated/backend"
import type { StorageAccessor } from "@/store/createNoteStorage"
import type ManagedApi from "@/managedApi/ManagedApi"

export type AiReplyState = {
  handleEvent: (data: string) => Promise<void>
  status: string | undefined
}

export interface AiAction {
  append: (text: string) => void
  reset: () => Promise<void>
}

type AiReplyContext = {
  aiAction: AiAction
  storageAccessor: StorageAccessor
  managedApi: ManagedApi
  note: Note | undefined
}

export const createAiReplyStates = (
  context: AiReplyContext
): Record<string, AiReplyState> => {
  const states: Record<string, AiReplyState> = {
    "thread.message.created": {
      status: "Generating response...",
      handleEvent: async (data) => {
        const response = JSON.parse(data) as Message
        context.aiAction.append(response.content?.[0]?.text?.value || "")
      },
    },
    "thread.message.delta": {
      status: "Writing response...",
      handleEvent: async (data) => {
        const response = JSON.parse(data) as MessageDelta
        const delta = response.delta?.content?.[0]?.text?.value || ""
        context.aiAction.append(delta)
      },
    },
    "thread.run.requires_action": {
      status: "Processing actions...",
      handleEvent: async (data) => {
        if (!context.note) {
          console.error("No note found in conversation")
          return
        }
        const response = JSON.parse(data) as Run
        const contentToAppend = JSON.parse(
          response.required_action!.submit_tool_outputs!.tool_calls![0]!
            .function!.arguments as unknown as string
        ) as NoteDetailsCompletion

        await context.storageAccessor
          .storedApi()
          .appendDetails(context.note!.id, contentToAppend!.completion)

        await context.managedApi.restAiController.submitToolCallResult(
          response.thread_id!,
          response.id!,
          response.required_action!.submit_tool_outputs!.tool_calls![0]!.id!,
          { status: "accepted" }
        )
      },
    },
    done: {
      status: undefined,
      handleEvent: async () => {
        await context.aiAction.reset()
      },
    },
  }

  return states
}
