import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'

const { runMineruOutlineSubprocess } = vi.hoisted(() => ({
  runMineruOutlineSubprocess: vi.fn(),
}))

vi.mock('../src/commands/read/mineruOutlineSubprocess.js', () => ({
  runMineruOutlineSubprocess,
}))

import {
  readSlashCommand,
  READ_OUTLINE_ASSISTANT_MAX_CHARS,
} from '../src/commands/read/readSlashCommand.js'

describe('readSlashCommand', () => {
  beforeEach(() => {
    runMineruOutlineSubprocess.mockReset()
    delete process.env.DOUGHNUT_READ_PDF_END_PAGE
  })

  afterEach(() => {
    delete process.env.DOUGHNUT_READ_PDF_END_PAGE
  })

  test('returns truncated assistant message when outline exceeds cap', async () => {
    const long = 'x'.repeat(READ_OUTLINE_ASSISTANT_MAX_CHARS + 50)
    runMineruOutlineSubprocess.mockResolvedValue({
      ok: true,
      outline: long,
      source: 'content_list',
    })
    if (!('run' in readSlashCommand)) {
      throw new Error('expected run slash command')
    }
    const r = await readSlashCommand.run('/tmp/book.pdf')
    expect(r.assistantMessage).toHaveLength(
      READ_OUTLINE_ASSISTANT_MAX_CHARS + 1
    )
    expect(r.assistantMessage.endsWith('…')).toBe(true)
  })

  test('appends note below outline when present', async () => {
    runMineruOutlineSubprocess.mockResolvedValue({
      ok: true,
      outline: 'L1 Foo',
      source: 'epub',
      note: 'no headings in spine',
    })
    if (!('run' in readSlashCommand)) {
      throw new Error('expected run slash command')
    }
    const r = await readSlashCommand.run('book.epub')
    expect(r.assistantMessage).toBe('L1 Foo\n\nno headings in spine')
  })

  test('throws with subprocess error when ok is false', async () => {
    runMineruOutlineSubprocess.mockResolvedValue({
      ok: false,
      error: 'expected .pdf or .epub',
    })
    if (!('run' in readSlashCommand)) {
      throw new Error('expected run slash command')
    }
    await expect(readSlashCommand.run('book.txt')).rejects.toThrow(
      'expected .pdf or .epub'
    )
  })

  test('passes pdfEndPage from DOUGHNUT_READ_PDF_END_PAGE for PDF', async () => {
    process.env.DOUGHNUT_READ_PDF_END_PAGE = '4'
    runMineruOutlineSubprocess.mockResolvedValue({
      ok: true,
      outline: 'ok',
      source: 'content_list',
    })
    if (!('run' in readSlashCommand)) {
      throw new Error('expected run slash command')
    }
    await readSlashCommand.run('x.pdf')
    expect(runMineruOutlineSubprocess).toHaveBeenCalledWith(
      expect.objectContaining({ bookPath: 'x.pdf', pdfEndPage: 4 })
    )
  })
})
