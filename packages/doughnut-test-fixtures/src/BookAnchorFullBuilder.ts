import type { BookAnchorFull } from '@generated/doughnut-backend-api'
import Builder from './Builder'

class BookAnchorFullBuilder extends Builder<BookAnchorFull> {
  data: BookAnchorFull

  constructor() {
    super()
    this.data = {
      id: 1,
      value: '{"page_idx":0}',
    }
  }

  id(id: number): BookAnchorFullBuilder {
    this.data.id = id
    return this
  }

  value(value: string): BookAnchorFullBuilder {
    this.data.value = value
    return this
  }

  pdfOutlineV1Start(
    pageIndex: number,
    bbox?: readonly [number, number, number, number]
  ): BookAnchorFullBuilder {
    const payload: { page_idx: number; bbox?: number[] } = {
      page_idx: pageIndex,
    }
    if (bbox !== undefined) {
      payload.bbox = [...bbox]
    }
    this.data.value = JSON.stringify(payload)
    return this
  }

  static topMathsLikePreorder(): BookAnchorFull[] {
    const v = (id: number, json: string) =>
      new BookAnchorFullBuilder().id(id).value(json).please()
    return [
      v(101, '{"page_idx":0}'),
      v(102, '{"page_idx":0,"bbox":[48,72,564,200]}'),
      v(103, '{"page_idx":0,"bbox":[48,520,564,756]}'),
      v(104, '{"page_idx":1}'),
      v(105, '{"page_idx":1}'),
      v(106, '{"page_idx":1}'),
    ]
  }

  do(): BookAnchorFull {
    return this.data
  }
}

export default BookAnchorFullBuilder
