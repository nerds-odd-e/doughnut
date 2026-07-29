import { describe, expect, test } from 'vitest'
import { contentDispositionFileName } from '../src/sync/contentDispositionFileName.js'

describe('contentDispositionFileName', () => {
  test('reads the quoted filename the backend sends', () => {
    expect(
      contentDispositionFileName('attachment; filename="Ben Notebook.zip"')
    ).toBe('Ben Notebook.zip')
  })
})
