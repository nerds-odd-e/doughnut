import { describe, expect, test } from 'vitest'
import { contentDispositionFileName } from '../src/sync/contentDispositionFileName.js'

describe('contentDispositionFileName', () => {
  test('reads the quoted filename the backend sends', () => {
    expect(
      contentDispositionFileName('attachment; filename="Ben Notebook.zip"')
    ).toBe('Ben Notebook.zip')
  })

  test('reads an unquoted filename', () => {
    expect(contentDispositionFileName('attachment; filename=Ben.zip')).toBe(
      'Ben.zip'
    )
  })

  test('reads the quoted filename among extra parameters', () => {
    expect(
      contentDispositionFileName(
        'attachment; filename="Ben Notebook.zip"; size=1234'
      )
    ).toBe('Ben Notebook.zip')
  })

  test('is undefined for a missing header', () => {
    expect(contentDispositionFileName(null)).toBeUndefined()
    expect(contentDispositionFileName(undefined)).toBeUndefined()
  })

  test('is undefined when the header has no filename parameter', () => {
    expect(contentDispositionFileName('attachment')).toBeUndefined()
  })
})
