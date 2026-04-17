import type {
  BookBlockFull,
  PdfLocatorFull,
} from '@generated/doughnut-backend-api'
import Builder from './Builder'

class BookBlockFullBuilder extends Builder<BookBlockFull> {
  data: BookBlockFull

  constructor() {
    super()
    this.data = {
      id: 0,
      depth: 0,
      title: '',
      contentLocators: [],
      contentBlocks: [],
    }
  }

  id(id: number): BookBlockFullBuilder {
    this.data.id = id
    return this
  }

  depth(depth: number): BookBlockFullBuilder {
    this.data.depth = depth
    return this
  }

  title(title: string): BookBlockFullBuilder {
    this.data.title = title
    return this
  }

  contentLocators(contentLocators: PdfLocatorFull[]): BookBlockFullBuilder {
    this.data.contentLocators = contentLocators
    return this
  }

  do(): BookBlockFull {
    return this.data
  }
}

export default BookBlockFullBuilder
