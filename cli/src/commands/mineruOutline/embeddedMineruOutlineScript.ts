import { mkdtempSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { getEmbeddedMineruOutlineSource } from '#mineru-embedded'

let materializedPath: string | null = null

export function materializeEmbeddedMineruOutlineScript(): string {
  if (materializedPath !== null) {
    return materializedPath
  }
  const dir = mkdtempSync(join(tmpdir(), 'doughnut-mineru-outline-'))
  const path = join(dir, 'mineru_book_outline.py')
  writeFileSync(path, getEmbeddedMineruOutlineSource(), 'utf8')
  materializedPath = path
  return path
}
