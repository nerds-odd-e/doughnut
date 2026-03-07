import * as os from 'node:os'
import * as path from 'node:path'

export function getConfigDir(): string {
  const dir = process.env.DOUGHNUT_CONFIG_DIR
  if (dir) return dir
  return path.join(os.homedir(), '.config', 'doughnut')
}
