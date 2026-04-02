import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import { EventEmitter } from 'node:events'
import { PassThrough } from 'node:stream'
import { writeFileSync, mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import type { ChildProcess } from 'node:child_process'
import * as childProcess from 'node:child_process'
import { runMineruOutlineSubprocess } from '../src/commands/read/mineruOutlineSubprocess.js'

vi.mock('node:child_process', async (importOriginal) => {
  const actual = await importOriginal<typeof import('node:child_process')>()
  return {
    ...actual,
    spawn: vi.fn(),
  }
})

function fakeChild(
  onSpawn: (child: EventEmitter & Partial<ChildProcess>) => void
): void {
  vi.mocked(childProcess.spawn).mockImplementation(() => {
    const child = new EventEmitter() as EventEmitter & Partial<ChildProcess>
    child.stdout = new PassThrough()
    child.stderr = new PassThrough()
    child.kill = vi.fn()
    onSpawn(child)
    return child as ChildProcess
  })
}

describe('runMineruOutlineSubprocess', () => {
  let workDir: string
  let epubPath: string
  let pdfPath: string

  beforeEach(() => {
    process.env.DOUGHNUT_MINERU_OUTLINE_SCRIPT =
      '/fake/minerui-spike/spike_mineru_phase_a_outline.py'
    delete process.env.DOUGHNUT_MINERU_PYTHON
    workDir = mkdtempSync(join(tmpdir(), 'mineru-outline-test-'))
    epubPath = join(workDir, 'book.epub')
    pdfPath = join(workDir, 'book.pdf')
    writeFileSync(epubPath, '')
    writeFileSync(pdfPath, '')
  })

  afterEach(() => {
    rmSync(workDir, { recursive: true, force: true })
    vi.mocked(childProcess.spawn).mockReset()
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
        '/fake/minerui-spike/spike_mineru_phase_a_outline.py',
        epubPath,
        '--json-result',
      ]),
      expect.anything()
    )
  })

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
      expect(result.error).toContain('stderr (tail)')
    }
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
    }
  })
})
