import { EventEmitter } from 'node:events'
import { PassThrough } from 'node:stream'
import { writeFileSync, mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import type { ChildProcess } from 'node:child_process'
import * as childProcess from 'node:child_process'
import { afterEach, beforeEach, vi } from 'vitest'

export function fakeChild(
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

export let workDir: string
export let epubPath: string
export let pdfPath: string

export function useMineruOutlineSubprocessFixture(): void {
  beforeEach(() => {
    process.env.DOUGHNUT_MINERU_OUTLINE_SCRIPT =
      '/fake/cli/python/mineru_book_outline.py'
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
    delete process.env.DOUGHNUT_MINERU_PYTHON
  })
}

export { childProcess }
