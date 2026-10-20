import { bookReadingAiReorganizeMethods } from './bookReadingAiReorganizeMethods'
import { bookReadingEpubMethods } from './bookReadingEpubMethods'
import { bookReadingLayoutMethods } from './bookReadingLayoutMethods'
import { bookReadingPdfMethods } from './bookReadingPdfMethods'
import { bookReadingPhoneLayoutMethods } from './bookReadingPhoneLayoutMethods'
import { bookReadingProgressMethods } from './bookReadingProgressMethods'

export type { BookLayoutRow } from './bookReadingShared'

const bookReadingPage = () => ({
  ...bookReadingEpubMethods(),
  ...bookReadingLayoutMethods(),
  ...bookReadingPhoneLayoutMethods(),
  ...bookReadingPdfMethods(),
  ...bookReadingProgressMethods(),
  ...bookReadingAiReorganizeMethods(),
})

export default bookReadingPage
