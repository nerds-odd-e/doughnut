import type { BookFull, BookRangeFull } from '@generated/doughnut-backend-api'
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
      hasSourceFile: true,
      ranges: [],
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

  hasSourceFile(hasSourceFile: boolean): BookFullBuilder {
    this.data.hasSourceFile = hasSourceFile
    return this
  }

  ranges(ranges: BookRangeFull[]): BookFullBuilder {
    this.data.ranges = ranges
    return this
  }

  do(): BookFull {
    return this.data
  }
}

export default BookFullBuilder
