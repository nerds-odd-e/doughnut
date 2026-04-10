import type { BookFull, BookBlockFull } from '@generated/doughnut-backend-api'
import Builder from './Builder'
import generateId from './generateId'

class BookFullBuilder extends Builder<BookFull> {
  data: BookFull

  constructor() {
    super()
    this.data = {
      id: generateId(),
      bookName: 'Test Book',
      format: 'pdf',
      notebookId: String(generateId()),
      blocks: [],
    }
  }

  id(id: number): BookFullBuilder {
    this.data.id = id
    return this
  }

  bookName(bookName: string): BookFullBuilder {
    this.data.bookName = bookName
    return this
  }

  format(format: string): BookFullBuilder {
    this.data.format = format
    return this
  }

  notebookId(notebookId: string): BookFullBuilder {
    this.data.notebookId = notebookId
    return this
  }

  blocks(blocks: BookBlockFull[]): BookFullBuilder {
    this.data.blocks = blocks
    return this
  }

  do(): BookFull {
    return this.data
  }
}

export default BookFullBuilder
