import * as fs from 'node:fs'
import * as os from 'node:os'
import * as path from 'node:path'

/** Temp dir with `access-tokens.json` (fake bearer) for tests that need `DOUGHNUT_CONFIG_DIR`. */
export function tempConfigWithToken(namePrefix = 'doughnut-cli-test-'): string {
  const configDir = fs.mkdtempSync(path.join(os.tmpdir(), namePrefix))
  fs.writeFileSync(
    path.join(configDir, 'access-tokens.json'),
    JSON.stringify({
      tokens: [{ label: 't', token: 'fake-bearer' }],
      defaultLabel: 't',
    })
  )
  return configDir
}
