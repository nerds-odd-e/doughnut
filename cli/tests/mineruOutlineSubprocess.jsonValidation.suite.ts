import { describe, expect, test } from 'vitest'
import { runMineruOutlineSubprocess } from '../src/commands/mineruOutline/mineruOutlineSubprocess.js'
import {
  epubPath,
  fakeChild,
  useMineruOutlineSubprocessFixture,
  workDir,
} from './mineruOutlineSubprocess.testHelpers.js'

export function describeMineruOutlineSubprocessJsonValidation(): void {
  describe('runMineruOutlineSubprocess JSON validation', () => {
    useMineruOutlineSubprocessFixture()

    test('fails when stdout JSON includes both bookLayout and contentList', async () => {
      fakeChild((child) => {
        setImmediate(() => {
          child.stdout!.end(
            JSON.stringify({
              ok: true,
              outline: 'x',
              source: 'y',
              bookLayout: {
                roots: [{ title: 'A' }],
              },
              contentList: [
                { type: 'text', text: 'x', page_idx: 0, bbox: [0, 0, 1, 1] },
              ],
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
        ok: false,
        error: 'cannot send both bookLayout and contentList in outline JSON',
        exitCode: 0,
      })
    })

    test('fails when bookLayout.roots is empty', async () => {
      fakeChild((child) => {
        setImmediate(() => {
          child.stdout!.end(
            JSON.stringify({
              ok: true,
              outline: 'x',
              source: 'y',
              bookLayout: { roots: [] },
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
        ok: false,
        error: 'bookLayout.roots must be a non-empty array',
        exitCode: 0,
      })
    })

    test('rejects invalid stdout JSON', async () => {
      fakeChild((child) => {
        setImmediate(() => {
          child.stdout!.end('not-json')
          child.stderr!.end('something on stderr')
          child.emit('close', 1, null)
        })
      })

      const result = await runMineruOutlineSubprocess({
        bookPath: epubPath,
        cwd: workDir,
      })

      expect(result.ok).toBe(false)
      if (!result.ok) {
        expect(result.error).toContain('invalid JSON')
        expect(result.error).toContain('stderr:')
        expect(result.error).toContain('something on stderr')
      }
    })

    test('invalid JSON stdout with import stderr keeps invalid-JSON framing', async () => {
      fakeChild((child) => {
        setImmediate(() => {
          child.stdout!.end('not-json')
          child.stderr!.end("ModuleNotFoundError: No module named 'mineru'")
          child.emit('close', 1, null)
        })
      })

      const result = await runMineruOutlineSubprocess({
        bookPath: epubPath,
        cwd: workDir,
      })

      expect(result.ok).toBe(false)
      if (!result.ok) {
        expect(result.error).toContain('invalid JSON on stdout')
      }
    })
  })
}
