import type { NoteSearchResult, NoteTopology } from "@generated/backend"
import Builder from "./Builder"
import generateId from "./generateId"

class NoteSearchResultBuilder extends Builder<NoteSearchResult> {
  data: Partial<NoteSearchResult> = {
    noteTopology: {
      id: generateId(),
      titleOrPredicate: "Untitled",
    },
    notebookId: generateId(),
  }

  id(value: number): NoteSearchResultBuilder {
    if (!this.data.noteTopology) {
      this.data.noteTopology = { id: value, titleOrPredicate: "Untitled" }
    } else {
      this.data.noteTopology.id = value
    }
    return this
  }

  notebookId(value: number): NoteSearchResultBuilder {
    this.data.notebookId = value
    return this
  }

  titleOrPredicate(value: string): NoteSearchResultBuilder {
    if (!this.data.noteTopology) {
      this.data.noteTopology = { id: generateId(), titleOrPredicate: value }
    } else {
      this.data.noteTopology.titleOrPredicate = value
    }
    return this
  }

  distance(value: number | null | undefined): NoteSearchResultBuilder {
    this.data.distance = value ?? undefined
    return this
  }

  noteTopology(value: NoteTopology): NoteSearchResultBuilder {
    this.data.noteTopology = value
    return this
  }

  do(): NoteSearchResult {
    const id = this.data.noteTopology?.id ?? generateId()
    return {
      noteTopology: this.data.noteTopology ?? {
        id,
        titleOrPredicate: "Untitled",
      },
      notebookId: this.data.notebookId ?? generateId(),
      distance: this.data.distance,
    }
  }
}

export default NoteSearchResultBuilder
