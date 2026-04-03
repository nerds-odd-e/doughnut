import { describe, test, expect, beforeEach, afterEach } from 'vitest'
import { execFileSync } from 'node:child_process'
import { writeFileSync, mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const repoStubPath = join(
  __dirname,
  '..',
  '..',
  'e2e_test',
  'scripts',
  'mineru_outline_e2e_stub.py'
)

describe('mineru_outline_e2e_stub.py', () => {
  let workDir: string
  let pdfPath: string

  beforeEach(() => {
    workDir = mkdtempSync(join(tmpdir(), 'mineru-e2e-stub-'))
    pdfPath = join(workDir, 'book.pdf')
    writeFileSync(pdfPath, '%PDF-1.4\n%%EOF\n')
  })

  afterEach(() => {
    rmSync(workDir, { recursive: true, force: true })
  })

  test('prints ok JSON with layout roots for PDF argv compatible with runMineruOutlineSubprocess', () => {
    const outDir = join(workDir, 'mineru-out')
    const raw = execFileSync(
      process.env.DOUGHNUT_MINERU_PYTHON ?? 'python3',
      [
        repoStubPath,
        pdfPath,
        '--json-result',
        '--output-dir',
        outDir,
        '--end-page',
        '2',
      ],
      { encoding: 'utf8' }
    )
    const parsed = JSON.parse(raw.trim()) as {
      ok: boolean
      outline?: string
      layout?: { roots: { title: string; children?: { title: string }[] }[] }
    }
    expect(parsed.ok).toBe(true)
    expect(parsed.outline).toContain('Stub Part A')
    expect(parsed.layout?.roots?.[0]?.title).toBe('Stub Part A')
    expect(parsed.layout?.roots?.[0]?.children?.[0]?.title).toBe(
      'Stub Section One'
    )
    expect(parsed.layout?.roots?.[1]?.title).toBe('Stub Part B')
  })
})
