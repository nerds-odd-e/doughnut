import { bookReadingAiReorganizeMethods } from './bookReadingAiReorganizeMethods'
import { bookReadingEpubMethods } from './bookReadingEpubMethods'
import { bookReadingLayoutMethods } from './bookReadingLayoutMethods'
import { bookReadingPdfMethods } from './bookReadingPdfMethods'
import { bookReadingProgressMethods } from './bookReadingProgressMethods'

export type { BookLayoutRow } from './bookReadingShared'

const bookReadingPage = () => ({
  ...bookReadingEpubMethods(),
  ...bookReadingLayoutMethods(),
  ...bookReadingPdfMethods(),
  ...bookReadingProgressMethods(),
  ...bookReadingAiReorganizeMethods(),
})

export default bookReadingPage
