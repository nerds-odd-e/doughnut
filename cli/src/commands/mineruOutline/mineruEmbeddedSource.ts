import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

export function getEmbeddedMineruOutlineSource(): string {
  const here = dirname(fileURLToPath(import.meta.url))
  return readFileSync(
    join(here, '../../../python/mineru_book_outline.py'),
    'utf8'
  )
}
