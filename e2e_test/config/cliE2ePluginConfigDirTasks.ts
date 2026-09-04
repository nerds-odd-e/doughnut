/**
 * Cypress task handlers for CLI config directories (no PTY).
 */

import { mkdtempSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

export function createCliE2ePluginConfigDirTasks() {
  return {
    createCliConfigDir() {
      return mkdtempSync(join(tmpdir(), 'cypress-cli-config-'))
    },
    createCliConfigDirWithGmail(gmailConfig: Record<string, unknown>) {
      const configDir = mkdtempSync(join(tmpdir(), 'cypress-cli-gmail-'))
      writeFileSync(
        join(configDir, 'gmail.json'),
        JSON.stringify(gmailConfig, null, 2)
      )
      return configDir
    },
    /** Same `access-tokens.json` shape as `saveStoredAccessToken` (cli/src/backendApi/accessTokenStorage.ts). */
    writeCliAccessToken({
      configDir,
      token,
    }: {
      configDir: string
      token: string
    }): null {
      writeFileSync(
        join(configDir, 'access-tokens.json'),
        JSON.stringify({ token }, null, 2),
        'utf-8'
      )
      return null
    },
  }
}
