import type { SimpleNoteSearchResult } from "@generated/backend"
import Builder from "./Builder"
import generateId from "./generateId"

class SimpleNoteSearchResultBuilder extends Builder<SimpleNoteSearchResult> {
  data: Partial<SimpleNoteSearchResult> = {
    titleOrPredicate: "Untitled",
  }

  id(value: number): SimpleNoteSearchResultBuilder {
    this.data.id = value
    return this
  }

  notebookId(value: number): SimpleNoteSearchResultBuilder {
    this.data.notebookId = value
    return this
  }

  titleOrPredicate(value: string): SimpleNoteSearchResultBuilder {
    this.data.titleOrPredicate = value
    return this
  }

  do(): SimpleNoteSearchResult {
    return {
      id: this.data.id ?? generateId(),
      notebookId: this.data.notebookId ?? generateId(),
      titleOrPredicate: this.data.titleOrPredicate ?? "Untitled",
    }
  }
}

export default SimpleNoteSearchResultBuilder
