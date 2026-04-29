import type {
  NoteSearchResult,
  NoteTopology,
} from '@generated/doughnut-backend-api'
import Builder from './Builder'
import generateId from './generateId'

class NoteSearchResultBuilder extends Builder<NoteSearchResult> {
  data: Partial<NoteSearchResult> = (() => {
    const id = generateId()
    const notebookId = generateId()
    return {
      noteTopology: { id, slug: `s${id}`, title: 'Untitled', notebookId },
      notebookId,
    }
  })()

  id(value: number): NoteSearchResultBuilder {
    const notebookId = this.data.notebookId ?? generateId()
    if (!this.data.noteTopology) {
      this.data.noteTopology = {
        id: value,
        slug: `s${value}`,
        title: 'Untitled',
        notebookId,
      }
    } else {
      this.data.noteTopology.id = value
      this.data.noteTopology.slug = `s${value}`
      this.data.noteTopology.notebookId = notebookId
    }
    return this
  }

  notebookId(value: number): NoteSearchResultBuilder {
    this.data.notebookId = value
    if (this.data.noteTopology) {
      this.data.noteTopology.notebookId = value
    }
    return this
  }

  notebookName(value: string): NoteSearchResultBuilder {
    this.data.notebookName = value
    return this
  }

  title(value: string): NoteSearchResultBuilder {
    const notebookId = this.data.notebookId ?? generateId()
    if (!this.data.noteTopology) {
      const id = generateId()
      this.data.noteTopology = { id, slug: `s${id}`, title: value, notebookId }
    } else {
      this.data.noteTopology.title = value
    }
    return this
  }

  distance(value: number | null | undefined): NoteSearchResultBuilder {
    this.data.distance = value ?? undefined
    return this
  }

  noteTopology(value: NoteTopology): NoteSearchResultBuilder {
    this.data.noteTopology = value
    this.data.notebookId = value.notebookId
    return this
  }

  do(): NoteSearchResult {
    const notebookId = this.data.notebookId ?? generateId()
    const id = this.data.noteTopology?.id ?? generateId()
    return {
      noteTopology: this.data.noteTopology ?? {
        id,
        slug: `s${id}`,
        title: 'Untitled',
        notebookId,
      },
      notebookId,
      notebookName: this.data.notebookName,
      distance: this.data.distance,
    }
  }
}

export default NoteSearchResultBuilder
