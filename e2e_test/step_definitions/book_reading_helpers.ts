import type { DataTable } from '@cucumber/cucumber'
import type { BookLayoutRow } from '../start/pageObjects/bookReadingPage'

export function unwrapData<T>(result: T | { data: T } | undefined): T {
  if (result && typeof result === 'object' && 'data' in result) {
    return (result as { data: T }).data
  }
  return result as T
}

export function parseBookLayoutTable(data: DataTable): BookLayoutRow[] {
  return data.raw().map((row) => {
    const depth = parseInt(row[0] ?? '0', 10)
    const title = (row[1] ?? '').trim()
    return { depth, title }
  })
}

export function pdfFixtureStem(fixtureFilename: string): string {
  return fixtureFilename.replace(/\.pdf$/i, '')
}

const MAX_BOOK_LAYOUT_DEPTH = 64

export function validatePreorderDepths(depths: number[]): void {
  if (depths.length === 0) {
    return
  }
  if (depths[0] !== 0 || depths[0] > MAX_BOOK_LAYOUT_DEPTH) {
    throw new Error('Suggested depths do not form a valid outline')
  }
  for (let i = 1; i < depths.length; i++) {
    const d = depths[i]!
    if (d < 0 || d > MAX_BOOK_LAYOUT_DEPTH || d > depths[i - 1]! + 1) {
      throw new Error('Suggested depths do not form a valid outline')
    }
  }
}
