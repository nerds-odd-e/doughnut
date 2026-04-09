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

  test('returns ok JSON with layout roots (argv compatible with runMineruOutlineSubprocess)', async () => {
    const result = await runMineruOutlineSubprocess({
      bookPath: pdfPath,
      cwd: repoRoot,
      scriptPath: outlineScriptPath,
    })
    expect(result.ok).toBe(true)
    if (!result.ok) return
    expect(result.outline).toContain('[L2 p0] Code Refactoring')
    expect(result.layout?.roots?.[0]?.title).toBe('Code Refactoring')
    expect(result.layout?.roots?.[0]?.children).toBeUndefined()
    expect(result.layout?.roots?.[4]?.title).toBe(
      '2.2 Refactoring as Strengthening the Code'
    )
    const section3 = result.layout?.roots?.[5]
    expect(section3?.title).toBe(
      '3. Refactoring Is Not Only About Changing Production Code'
    )
    expect(section3?.children?.[0]?.title).toBe(
      '3.1 Can You Refactor Without Tests?'
    )
    expect(section3?.children?.[7]?.title).toBe(
      '6. Why Refactoring Matters More with AI'
    )
  })
})
