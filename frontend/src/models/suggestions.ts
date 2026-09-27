import type { NoteContentCompletion } from "@generated/donut-backend-api"
import type { ToolCallResult } from "./aiReplyState"
import type { NoteStore } from "@/store/noteStore"

interface BaseSuggestion {
  toolCallId: string
}

interface CompletionSuggestion extends BaseSuggestion {
  suggestionType: "completion"
  content: NoteContentCompletion
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
  noteStore: NoteStore
  noteId: string
  suggestionResolver: {
    resolve: (result: ToolCallResult) => void
    reject: (error: Error) => void
  } | null
}
