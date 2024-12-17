import type { NoteDetailsCompletion, ToolCallResult } from "@/generated/backend"
import type { StorageAccessor } from "@/store/createNoteStorage"

export interface Suggestion {
  suggestionType: "completion" | "title" | "unknown"
  content: {
    completion: NoteDetailsCompletion
    title: string
    unknown: { rawJson: string; functionName: string }
  }[Suggestion["suggestionType"]]
  threadId: string
  runId: string
  toolCallId: string
}

export interface SuggestionContext {
  storageAccessor: StorageAccessor
  noteId: string
  suggestionResolver: {
    resolve: (result: ToolCallResult) => void
    reject: (error: Error) => void
  } | null
}

export const createSuggestion = (
  type: Suggestion["suggestionType"],
  content: Suggestion["content"],
  threadId: string,
  runId: string,
  toolCallId: string
): Suggestion => ({
  suggestionType: type,
  content,
  threadId,
  runId,
  toolCallId,
})
