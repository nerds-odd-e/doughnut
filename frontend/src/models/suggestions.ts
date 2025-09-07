import type { NoteDetailsCompletion, ToolCallResult } from "@generated/backend"
import type { StorageAccessor } from "@/store/createNoteStorage"

interface BaseSuggestion {
  threadId: string
  runId: string
  toolCallId: string
}

interface CompletionSuggestion extends BaseSuggestion {
  suggestionType: "completion"
  content: NoteDetailsCompletion
}

interface TitleSuggestion extends BaseSuggestion {
  suggestionType: "title"
  content: string
}

interface UnknownSuggestion extends BaseSuggestion {
  suggestionType: "unknown"
  content: { rawJson: string; functionName: string }
}

export type Suggestion =
  | CompletionSuggestion
  | TitleSuggestion
  | UnknownSuggestion

export interface SuggestionContext {
  storageAccessor: StorageAccessor
  noteId: string
  suggestionResolver: {
    resolve: (result: ToolCallResult) => void
    reject: (error: Error) => void
  } | null
}
