import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, beforeEach } from 'vitest'

/** Shared tmp workspace for lintWorkspace observable-behavior tests. */
export function useLintWorkspaceFixture() {
  let root = ''

  beforeEach(() => {
    root = mkdtempSync(join(tmpdir(), 'doughnut-lintWorkspace-'))
  })

  afterEach(() => {
    rmSync(root, { recursive: true, force: true })
  })

  const write = (relativePath: string, content: string) => {
    const full = join(root, relativePath)
    mkdirSync(join(full, '..'), { recursive: true })
    writeFileSync(full, content, 'utf8')
  }

  /** A well-formed concept, so a test varies only what it is about. */
  const concept = (keys: string, body: string) =>
    `---\n${keys}\n---\n\n# ${body}`

  /** Root listing required once concept-bearing directories must carry index.md. */
  const writeRootIndex = () => write('index.md', '# Workspace\n')

  return {
    workspaceRoot: () => root,
    write,
    concept,
    writeRootIndex,
  }
}
