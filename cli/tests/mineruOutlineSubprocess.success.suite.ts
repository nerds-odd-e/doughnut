import {
  existsSync,
  readFileSync,
  writeFileSync,
  mkdtempSync,
  rmSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { describe, expect, test, vi } from 'vitest'
import { runMineruOutlineSubprocess } from '../src/commands/mineruOutline/mineruOutlineSubprocess.js'
import {
  childProcess,
  epubPath,
  fakeChild,
  useMineruOutlineSubprocessFixture,
  workDir,
} from './mineruOutlineSubprocess.testHelpers.js'

export function describeMineruOutlineSubprocessSuccess(): void {
  describe('runMineruOutlineSubprocess success', () => {
    useMineruOutlineSubprocessFixture()

    test('uses embedded default script path when env unset and cwd has no checkout cli/python', async () => {
      delete process.env.DONUT_MINERU_OUTLINE_SCRIPT
      const isolatedCwd = mkdtempSync(join(tmpdir(), 'mineru-no-repo-'))
      const isolatedEpub = join(isolatedCwd, 'book.epub')
      writeFileSync(isolatedEpub, '')
      try {
        fakeChild((child) => {
          setImmediate(() => {
            child.stdout!.end(
              JSON.stringify({ ok: true, outline: 'x', source: 'epub' })
            )
            child.stderr!.end('')
            child.emit('close', 0, null)
          })
        })

        await runMineruOutlineSubprocess({
          bookPath: isolatedEpub,
          cwd: isolatedCwd,
        })

        const args = vi.mocked(childProcess.spawn).mock.calls[0]![1] as string[]
        const scriptArg = args[0]!
        expect(existsSync(scriptArg)).toBe(true)
        expect(readFileSync(scriptArg, 'utf8')).toContain('Donut CLI')
      } finally {
        rmSync(isolatedCwd, { recursive: true, force: true })
      }
    })

    test('returns trimmed outline when subprocess prints ok JSON', async () => {
      fakeChild((child) => {
        setImmediate(() => {
          child.stdout!.end(
            JSON.stringify({
              ok: true,
              outline: '  [L1 p0] Part  \n',
              source: 'content_list',
            })
          )
          child.stderr!.end('')
          child.emit('close', 0, null)
        })
      })

      const result = await runMineruOutlineSubprocess({
        bookPath: epubPath,
        cwd: workDir,
      })

      expect(result).toEqual({
        ok: true,
        outline: '[L1 p0] Part',
        source: 'content_list',
      })
      expect(vi.mocked(childProcess.spawn)).toHaveBeenCalledWith(
        'python3',
        expect.arrayContaining([
          '/fake/cli/python/mineru_book_outline.py',
          epubPath,
          '--json-result',
        ]),
        expect.anything()
      )
    })

    test('returns contentList when subprocess JSON includes valid contentList', async () => {
      const contentList = [
        {
          type: 'text',
          text: 'Chapter',
          text_level: 1,
          page_idx: 0,
          bbox: [0, 0, 100, 50],
        },
        { type: 'text', text: 'Body', page_idx: 0, bbox: [0, 60, 100, 80] },
      ]
      fakeChild((child) => {
        setImmediate(() => {
          child.stdout!.end(
            JSON.stringify({
              ok: true,
              outline: 'x',
              source: 'content_list',
              contentList,
            })
          )
          child.stderr!.end('')
          child.emit('close', 0, null)
        })
      })

      const result = await runMineruOutlineSubprocess({
        bookPath: epubPath,
        cwd: workDir,
      })

      expect(result.ok).toBe(true)
      if (result.ok) expect(result.contentList).toEqual(contentList)
    })

    test('returns bookLayout when subprocess JSON includes valid bookLayout', async () => {
      const bookLayout = {
        roots: [
          {
            title: 'Root A',
            children: [{ title: 'Child 1' }],
            contentBlocks: [{ type: 'page_number', text: '1', page_idx: 0 }],
          },
        ],
      }
      fakeChild((child) => {
        setImmediate(() => {
          child.stdout!.end(
            JSON.stringify({
              ok: true,
              outline: 'x',
              source: 'stub',
              bookLayout,
            })
          )
          child.stderr!.end('')
          child.emit('close', 0, null)
        })
      })

      const result = await runMineruOutlineSubprocess({
        bookPath: epubPath,
        cwd: workDir,
      })

      expect(result.ok).toBe(true)
      if (result.ok) expect(result.bookLayout).toEqual(bookLayout)
    })
  })
}
