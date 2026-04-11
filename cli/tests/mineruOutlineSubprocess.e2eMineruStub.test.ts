import { describe, test, expect, beforeEach, afterEach } from 'vitest'
import { writeFileSync, mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { delimiter, dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { runMineruOutlineSubprocess } from '../src/commands/mineruOutline/mineruOutlineSubprocess.js'

const __dirname = dirname(fileURLToPath(import.meta.url))
const repoRoot = join(__dirname, '..', '..')
const mockSite = join(repoRoot, 'e2e_test', 'python_stubs', 'mineru_site')
const outlineScriptPath = join(
  repoRoot,
  'cli',
  'python',
  'mineru_book_outline.py'
)

describe('mineru_book_outline.py with E2E shadow mineru (PYTHONPATH)', () => {
  let workDir: string
  let pdfPath: string
  let prevPythonPath: string | undefined

  beforeEach(() => {
    workDir = mkdtempSync(join(tmpdir(), 'mineru-e2e-stub-'))
    pdfPath = join(workDir, 'book.pdf')
    writeFileSync(pdfPath, '%PDF-1.4\n%%EOF\n')
    prevPythonPath = process.env.PYTHONPATH
    const tail = prevPythonPath?.trim()
    process.env.PYTHONPATH = tail ? `${mockSite}${delimiter}${tail}` : mockSite
  })

  afterEach(() => {
    rmSync(workDir, { recursive: true, force: true })
    if (prevPythonPath === undefined) {
      delete process.env.PYTHONPATH
    } else {
      process.env.PYTHONPATH = prevPythonPath
    }
  })

  test('returns ok JSON with contentList (argv compatible with runMineruOutlineSubprocess)', async () => {
    const result = await runMineruOutlineSubprocess({
      bookPath: pdfPath,
      cwd: repoRoot,
      scriptPath: outlineScriptPath,
    })
    expect(result.ok).toBe(true)
    if (!result.ok) return
    expect(result.outline).toContain('[L2 p0] Code Refactoring')
    expect(result.layout).toBeUndefined()
    const cl = result.contentList
    expect(Array.isArray(cl)).toBe(true)
    expect((cl?.length ?? 0) > 2).toBe(true)

    const headingBlock = cl?.[0] as {
      type?: string
      text_level?: number
      text?: string
    }
    expect(headingBlock?.type).toBe('text')
    expect(typeof headingBlock?.text_level).toBe('number')
    expect(headingBlock?.text).toBe('Code Refactoring')
    const firstBodyBlock = cl?.[1] as {
      type?: string
      text_level?: number
      text?: string
    }
    expect(firstBodyBlock?.type).toBe('text')
    expect(firstBodyBlock?.text_level).toBeUndefined()
    expect(firstBodyBlock?.text).toMatch(/^Refactoring is often explained/)
  })
})
