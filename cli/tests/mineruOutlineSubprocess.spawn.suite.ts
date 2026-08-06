import { join } from 'node:path'
import { describe, expect, test, vi } from 'vitest'
import { runMineruOutlineSubprocess } from '../src/commands/mineruOutline/mineruOutlineSubprocess.js'
import {
  childProcess,
  epubPath,
  fakeChild,
  pdfPath,
  useMineruOutlineSubprocessFixture,
  workDir,
} from './mineruOutlineSubprocess.testHelpers.js'

export function describeMineruOutlineSubprocessSpawn(): void {
  describe('runMineruOutlineSubprocess spawn', () => {
    useMineruOutlineSubprocessFixture()

    test('passes temp --output-dir for PDF', async () => {
      fakeChild((child) => {
        setImmediate(() => {
          child.stdout!.end(
            JSON.stringify({ ok: true, outline: 'x', source: 'content_list' })
          )
          child.stderr!.end('')
          child.emit('close', 0, null)
        })
      })

      await runMineruOutlineSubprocess({
        bookPath: pdfPath,
        cwd: workDir,
        pdfEndPage: 3,
      })

      const args = vi.mocked(childProcess.spawn).mock.calls[0]![1] as string[]
      expect(args).toContain('--output-dir')
      const outIdx = args.indexOf('--output-dir')
      expect(args[outIdx + 1]).toMatch(/^.*doughnut-mineru-out-/)
      expect(args).toContain('--end-page')
      expect(args).toContain('3')
    })

    test('uses DOUGHNUT_MINERU_PYTHON when set', async () => {
      process.env.DOUGHNUT_MINERU_PYTHON = '/opt/venv/bin/python'
      fakeChild((child) => {
        setImmediate(() => {
          child.stdout!.end(
            JSON.stringify({ ok: true, outline: '', source: 'epub' })
          )
          child.stderr!.end('')
          child.emit('close', 0, null)
        })
      })

      await runMineruOutlineSubprocess({ bookPath: epubPath, cwd: workDir })

      expect(vi.mocked(childProcess.spawn).mock.calls[0]![0]).toBe(
        '/opt/venv/bin/python'
      )
    })

    test('fails fast when book path is missing', async () => {
      const result = await runMineruOutlineSubprocess({
        bookPath: join(workDir, 'nope.epub'),
        cwd: workDir,
      })
      expect(result).toEqual({
        ok: false,
        error: expect.stringMatching(/file not found or not readable/),
      })
      expect(vi.mocked(childProcess.spawn)).not.toHaveBeenCalled()
    })
  })
}
