import { describe, test, expect, beforeEach, afterEach } from 'vitest'
import { writeFileSync, mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { delimiter, dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { runMineruOutlineSubprocess } from '../src/commands/read/mineruOutlineSubprocess.js'

const __dirname = dirname(fileURLToPath(import.meta.url))
const repoRoot = join(__dirname, '..', '..')
const mockSite = join(repoRoot, 'e2e_test', 'python_stubs', 'mineru_site')
const spikeScriptPath = join(
  repoRoot,
  'minerui-spike',
  'spike_mineru_phase_a_outline.py'
)

describe('spike_mineru_phase_a_outline.py with E2E shadow mineru (PYTHONPATH)', () => {
  let workDir: string
  let pdfPath: string
  let prevPythonPath: string | undefined

  beforeEach(() => {
    workDir = mkdtempSync(join(tmpdir(), 'mineru-spike-mock-'))
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
      scriptPath: spikeScriptPath,
    })
    expect(result.ok).toBe(true)
    if (!result.ok) return
    expect(result.outline).toContain('Stub Part A')
    expect(result.layout?.roots?.[0]?.title).toBe('Stub Part A')
    expect(result.layout?.roots?.[0]?.children?.[0]?.title).toBe(
      'Stub Section One'
    )
    expect(result.layout?.roots?.[1]?.title).toBe('Stub Part B')
  })
})
