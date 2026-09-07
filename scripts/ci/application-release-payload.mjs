import { statSync } from 'node:fs'
import { join } from 'node:path'

const { DEPLOY_JAR_PATH, FRONTEND_STATIC_DIR, CLI_BUNDLE_SOURCE } = process.env
for (const path of [
  DEPLOY_JAR_PATH,
  join(FRONTEND_STATIC_DIR, 'index.html'),
  CLI_BUNDLE_SOURCE,
]) {
  const file = statSync(path)
  if (!file.isFile() || file.size === 0) {
    throw new Error(`Release payload requires a nonempty file: ${path}`)
  }
}
console.log('Complete application release payload validated')
