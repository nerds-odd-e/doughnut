import { mkdtempSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import embeddedMineruOutlineSource from '../../../../minerui-spike/spike_mineru_phase_a_outline.py'

let materializedPath: string | null = null

export function materializeEmbeddedMineruOutlineScript(): string {
  if (materializedPath !== null) {
    return materializedPath
  }
  const dir = mkdtempSync(join(tmpdir(), 'doughnut-mineru-outline-'))
  const path = join(dir, 'spike_mineru_phase_a_outline.py')
  writeFileSync(path, embeddedMineruOutlineSource, 'utf8')
  materializedPath = path
  return path
}
