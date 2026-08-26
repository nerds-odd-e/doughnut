import type { ChildProcess } from 'node:child_process'
import { describe, expect, test, vi } from 'vitest'
import { runMineruOutlineSubprocess } from '../src/commands/mineruOutline/mineruOutlineSubprocess.js'
import {
  epubPath,
  fakeChild,
  pdfPath,
  useMineruOutlineSubprocessFixture,
  workDir,
} from './mineruOutlineSubprocess.testHelpers.js'

export function describeMineruOutlineSubprocessErrors(): void {
  describe('runMineruOutlineSubprocess errors', () => {
    useMineruOutlineSubprocessFixture()

    test('maps JSON error and exit code on failure', async () => {
      fakeChild((child) => {
        setImmediate(() => {
          child.stdout!.end(
            JSON.stringify({ ok: false, error: 'do_parse failed: x' })
          )
          child.stderr!.end('trace')
          child.emit('close', 1, null)
        })
      })

      const result = await runMineruOutlineSubprocess({
        bookPath: pdfPath,
        cwd: workDir,
      })

      expect(result).toEqual({
        ok: false,
        error: 'do_parse failed: x',
        exitCode: 1,
      })
    })

    test('times out when subprocess does not finish', async () => {
      fakeChild((child) => {
        child.kill = vi.fn(() => {
          setImmediate(() => child.emit('close', null, 'SIGTERM'))
        }) as ChildProcess['kill']
      })

      const result = await runMineruOutlineSubprocess({
        bookPath: epubPath,
        cwd: workDir,
        timeoutMs: 20,
      })

      expect(result.ok).toBe(false)
      if (!result.ok) {
        expect(result.error).toContain('timed out')
        expect(result.error).not.toContain('DONUT_MINERU_PDF_END_PAGE')
      }
    })

    test('PDF timeout message mentions DONUT_MINERU_PDF_END_PAGE', async () => {
      fakeChild((child) => {
        child.kill = vi.fn(() => {
          setImmediate(() => child.emit('close', null, 'SIGTERM'))
        }) as ChildProcess['kill']
      })

      const result = await runMineruOutlineSubprocess({
        bookPath: pdfPath,
        cwd: workDir,
        timeoutMs: 20,
      })

      expect(result.ok).toBe(false)
      if (!result.ok) {
        expect(result.error).toContain('DONUT_MINERU_PDF_END_PAGE')
      }
    })

    test('returns friendly error when python executable cannot be spawned (ENOENT)', async () => {
      fakeChild((child) => {
        setImmediate(() => {
          const err = Object.assign(new Error('spawn python3 ENOENT'), {
            code: 'ENOENT',
          })
          child.emit('error', err)
        })
      })

      const result = await runMineruOutlineSubprocess({
        bookPath: epubPath,
        cwd: workDir,
      })

      expect(result).toEqual({
        ok: false,
        error: expect.stringMatching(
          /Could not run "python3".*DONUT_MINERU_PYTHON/s
        ),
      })
    })

    test('returns friendly error when python executable is not executable (EACCES)', async () => {
      fakeChild((child) => {
        setImmediate(() => {
          const err = Object.assign(new Error('spawn EACCES'), {
            code: 'EACCES',
          })
          child.emit('error', err)
        })
      })

      const result = await runMineruOutlineSubprocess({
        bookPath: epubPath,
        cwd: workDir,
      })

      expect(result.ok).toBe(false)
      if (!result.ok) {
        expect(result.error).toContain('Permission denied')
        expect(result.error).toContain('DONUT_MINERU_PYTHON')
      }
    })

    test('empty stdout with ModuleNotFoundError stderr uses MinerU install hint', async () => {
      fakeChild((child) => {
        setImmediate(() => {
          child.stdout!.end('')
          child.stderr!.end(
            'Traceback (most recent call last):\n  File "x.py", line 1, in <module>\n    from mineru.cli.common import do_parse\nModuleNotFoundError: No module named \'mineru\'\n'
          )
          child.emit('close', 1, null)
        })
      })

      const result = await runMineruOutlineSubprocess({
        bookPath: epubPath,
        cwd: workDir,
      })

      expect(result.ok).toBe(false)
      if (!result.ok) {
        expect(result.error).toContain(
          'MinerU is missing or could not be imported'
        )
        expect(result.error).toContain('mineru[pipeline]')
        expect(result.error.length).toBeLessThan(4000)
      }
    })
  })
}
